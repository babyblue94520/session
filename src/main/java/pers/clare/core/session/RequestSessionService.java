package pers.clare.core.session;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import pers.clare.core.lock.IdLock;
import pers.clare.core.message.GenericMessageService;
import pers.clare.core.session.constant.InvalidateType;
import pers.clare.core.session.constant.SessionNotifyAction;
import pers.clare.core.session.exception.SessionException;
import pers.clare.core.session.listener.RequestSessionInvalidateListener;
import pers.clare.core.util.ShutdownUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Log4j2
public abstract class RequestSessionService<T extends RequestSession> implements InitializingBean,DisposableBean {
    public static final String SPLIT = ",";
    // session id 長度
    public static final int ID_LENGTH = 32;

    // 當 session id 重複時，嘗試重建次數
    public static final int MAX_RETRY_INSERT = 5;

    // session refresh count effective time
    public static final long REFRESH_COUNT_EFFECTIVE_TIME = 3000;

    // ID 鎖
    private IdLock<Object> idLock = new IdLock<>() {
    };

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // 本地 session 緩存
    private final ConcurrentMap<String, T> sessions = new ConcurrentHashMap<>();

    // 阻止重复执行
    private final ConcurrentMap<String, Long> invalidates = new ConcurrentHashMap<>();


    private final List<RequestSessionInvalidateListener> invalidateListeners = new ArrayList<>();

    // 反建構的 session class
    private final Class<T> sessionClass;

    // session 最大存活時間
    private final long maxInactiveInterval;

    // 本地緩存 session 時間
    private final long updateInterval;

    // 更新 session 排程間隔
    private final long delay;

    // session 實際管理
    private RequestSessionRepository<T> repository;

    private GenericMessageService<String> sessionNotifyService;

    {
        Type type = this.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            sessionClass = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            sessionClass = (Class<T>) RequestSession.class;
        }
    }

    public RequestSessionService(
            RequestSessionRepository<T> repository
            , GenericMessageService<String> sessionNotifyService
            , Duration timeout
    ) {
        this.repository = repository;
        this.sessionNotifyService = sessionNotifyService;

        // 監聽 session 清除或註銷事件
        if (sessionNotifyService != null) {
            sessionNotifyService.addListener((message, data) -> receive(message.getOrigin()));
        }
        this.maxInactiveInterval = timeout.toMillis();
        this.updateInterval = maxInactiveInterval / 2;
        this.delay = maxInactiveInterval / 10;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 排程檢查 Session 狀態
        executor.scheduleWithFixedDelay(() -> {
            try {
                batchUpdate();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
        ShutdownUtil.await(getClass().getSimpleName(), executor, 30);
    }

    private void receive(String body) {
        log.debug("session event body:{}", body);
        String[] ss = body.split(SPLIT);
        if (ss.length < 2) return;
        String action = ss[0];
        String id = ss[1];
        if (id == null || id.length() != ID_LENGTH) return;
        switch (action) {
            case SessionNotifyAction.INVALIDATE:
                handleInvalidate(id, ss[2], Integer.valueOf(ss[3]));
                break;
            case SessionNotifyAction.CLEAR_CACHE:
                handleClear(id);
                break;
        }
    }

    public T find(String id) {
        if (id == null || id.length() != ID_LENGTH) return null;
        long now = System.currentTimeMillis();
        T session = findFromLocal(id, now);
        if (session != null) return session;
        // 從資料庫查詢 Session
        synchronized (idLock.getLock(id)) {
            session = findFromLocal(id, now);
            if (session != null) return session;
            session = repository.find(id, now - maxInactiveInterval);
            if (session != null) {
                session.maxInactiveInterval = maxInactiveInterval;
                sessions.put(id, session);
            }
        }
        if (session == null) idLock.remove(id);
        return session;
    }

    public T create(
            long accessTime
            , String userAgent
            , String ip
    ) {
        try {
            T session = sessionClass.getDeclaredConstructor().newInstance();
            session.createTime = accessTime;
            session.maxInactiveInterval = maxInactiveInterval;
            session.lastAccessTime = accessTime;
            session.lastUpdateAccessTime = accessTime;

            session.userAgent = userAgent;
            session.ip = ip;
            return doInsert(session, 0);
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    private T doInsert(T session, int count) {
        try {
            session.id = generateUUIDString(UUID.randomUUID());
            repository.insert(session);
            sessions.put(session.id, session);
            return session;
        } catch (SQLIntegrityConstraintViolationException e) {
            // retry where uuid is exist
            if (e.getErrorCode() == 1062) {
                if (MAX_RETRY_INSERT < count) {
                    throw new SessionException(e);
                }
                return doInsert(session, count + 1);
            } else {
                throw new SessionException(e);
            }
        } catch (Exception e) {
            throw new SessionException(e);
        }
    }

    public void update(T session) {
        if (session == null) return;
        int count = repository.update(session);
        if (count != 0) {
            synchronized (idLock.getLock(session.id)) {
                session.refresh++;
                session.lastCountRefresh = System.currentTimeMillis();
            }
            notifyClear(session.id);
        }
    }

    private void batchUpdate() {
        log.debug("batchUpdate start...");
        long t = System.currentTimeMillis();
        long now = t;
        long check = now - maxInactiveInterval;
        long originSize = sessions.size();
        if (sessions.size() > 0) {
            List<T> updates = new ArrayList<>();
            for (T session : sessions.values()) {
                // 更新活躍 Session 的 lastAccessTime 到資料庫
                if (session.lastAccessTime == session.lastUpdateAccessTime) continue;
                if (session.lastAccessTime + updateInterval > now) {
                    session.lastUpdateAccessTime = session.lastAccessTime;
                    updates.add(session);
                }
            }

            // 更新
            t = System.currentTimeMillis();
            int count = repository.update(updates);
            log.debug("update session:{} real:{} {}ms", updates.size(), count, System.currentTimeMillis() - t);
        }
        // 註銷
        List<RequestSessionId> ids = repository.findAllInvalidate(check);
        if (ids.size() == 0) {
            log.debug("invalidate session:0 0ms");
        } else {
            String id;
            int count;
            for (RequestSessionId requestSessionId : ids) {
                try {
                    id = requestSessionId.getId();
                    sessions.remove(id);
                    idLock.remove(id);
                    count = repository.delete(id);
                    if (count > 0) {
                        triggerInvalidateEvent(id, requestSessionId.getUsername(), InvalidateType.SYSTEM, count);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            log.debug("invalidate session:{} {}ms", ids.size(), System.currentTimeMillis() - t);
        }
        log.debug("batchUpdate {}>{} {}ms", originSize, sessions.size(), (System.currentTimeMillis() - now));
    }

    void invalidate(T session) {
        if (session == null) return;
        invalidate(session.id, InvalidateType.MANUAL);
    }

    public void invalidate(String id) {
        if (id == null || id.length() != ID_LENGTH) return;
        invalidate(id, InvalidateType.SYSTEM);
    }

    private void invalidate(String id, int type) {
        if (id == null || id.length() != ID_LENGTH) return;
        T session = sessions.remove(id);
        String username;
        if (session == null) {
            username = repository.findUsername(id);
        } else {
            session.valid = false;
            username = session.getUsername();
        }
        idLock.remove(id);
        triggerInvalidateEvent(id, username, type, repository.delete(id));
    }

    public void addInvalidateListeners(RequestSessionInvalidateListener listener) {
        if (listener != null) invalidateListeners.add(listener);
    }

    private void triggerInvalidateEvent(String id, String username, int type, int count) {
        if (invalidateListeners.size() > 0) {
            for (RequestSessionInvalidateListener invalidateListener : invalidateListeners) {
                invalidateListener.onInvalidate(id, username, type, count);
            }
        }
        notifyInvalidate(id, username, type);
    }

    private void notifyInvalidate(String id, String name, int type) {
        if (sessionNotifyService == null) return;
        try {
            invalidates.put(id, System.currentTimeMillis());
            sessionNotifyService.send(SessionNotifyAction.INVALIDATE + SPLIT + id + SPLIT + name + SPLIT + type);
        } catch (Exception e) {
            log.error(e.getMessage());
            invalidates.remove(id);
        }
    }

    private void handleInvalidate(String id, String username, int type) {
        if (invalidates.remove(id) != null) return;
        sessions.remove(id);
        idLock.remove(id);
        if (invalidateListeners.size() > 0) {
            for (RequestSessionInvalidateListener invalidateListener : invalidateListeners) {
                invalidateListener.onInvalidate(id, username, type, 0);
            }
        }
    }

    private void handleClear(String id) {
        synchronized (idLock.getLock(id)) {
            T session = sessions.get(id);
            if (session == null) return;
            // 減少多餘的移除行為
            if (session.refresh != 0
                    && session.refresh-- >= 0
                    && System.currentTimeMillis() - session.lastCountRefresh < REFRESH_COUNT_EFFECTIVE_TIME
            ) {
                return;
            }
            log.debug("do clear {}", id);
            session.refresh = 0;
            sessions.remove(id);
        }
    }

    private void notifyClear(String id) {
        if (sessionNotifyService == null) return;
        sessionNotifyService.send(SessionNotifyAction.CLEAR_CACHE + SPLIT + id);

    }

    private T findFromLocal(String id, long now) {
        T session = sessions.get(id);
        if (session == null) return null;
        if (!session.valid) return null;
        // 檢查是否超時
        if (session.lastAccessTime + maxInactiveInterval < now) {
            sessions.remove(id);
            return null;
        }
        return session;
    }

    public static String generateUUIDString(UUID uuid) {
        return (digits(uuid.getMostSignificantBits() >> 32, 8) +
                digits(uuid.getMostSignificantBits() >> 16, 4) +
                digits(uuid.getMostSignificantBits(), 4) +
                digits(uuid.getLeastSignificantBits() >> 48, 4) +
                digits(uuid.getLeastSignificantBits(), 12));
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }
}
