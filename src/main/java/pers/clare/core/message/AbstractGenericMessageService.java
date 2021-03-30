package pers.clare.core.message;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.DisposableBean;
import pers.clare.core.util.JsonUtil;
import pers.clare.core.util.ShutdownUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @param <T>
 */
@Log4j2
public abstract class AbstractGenericMessageService<T> implements GenericMessageService<T>, DisposableBean {

    protected final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    protected final String topic;

    protected final TypeReference<T> type;

    protected boolean string;

    private List<GenericReceiveListener<T>> listeners = new ArrayList<>();

    private final boolean registerListener;

    private MessageService messageService;

    public AbstractGenericMessageService(
            String topic
    ) {
        this(null, topic, true);
    }

    public AbstractGenericMessageService(
            String topic
            , boolean registerListener
    ) {
        this(null, topic, registerListener);
    }

    public AbstractGenericMessageService(
            MessageService messageService
            , String topic
    ) {
        this(messageService, topic, true);
    }

    public AbstractGenericMessageService(
            MessageService messageService
            , String topic
            , boolean registerListener
    ) {
        this.messageService = messageService;
        this.topic = topic;
        this.registerListener = registerListener;

        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        string = String.class.equals(t);
        this.type = string ? null : new JsonUtil.RewriteTypeReference<>(t) {
        };
        registerListener();
    }

    @Override
    public void destroy() throws Exception {
        ShutdownUtil.await(getClass().getSimpleName(), executor, 30);
    }

    private void registerListener() {
        if (messageService == null) return;
        if (registerListener) {
            messageService.listener(this.topic, (body) -> {
                this.receive(body);
            });
        }
    }

    private void receive(String body) {
        if (body == null || listeners.size() == 0) {
            log.trace("body is null or not listener");
            return;
        }
        try {
            T data = string ? (T) body : JsonUtil.decode(body, this.type);
            long time = System.currentTimeMillis();
            MessageData message = new MessageData(time, body, data);
            log.trace("{} {}", time, body);
            for (GenericReceiveListener<T> listener : listeners) {
                executor.execute(() -> {
                    try {
                        listener.accept(message, data);
                    } catch (Exception e) {
                        log.error("{} {}", time, e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.error("{} {}", e.getMessage(), body);
        }
    }

    public T send(T t) {
        if (messageService == null) return t;
        try {
            messageService.send(this.topic, string ? (String) t : JsonUtil.encode(t));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return t;
    }

    public GenericReceiveListener<T> addListener(GenericReceiveListener<T> listener) {
        if (!registerListener) throw new UnsupportedOperationException();
        this.listeners.add(listener);
        return listener;
    }

}
