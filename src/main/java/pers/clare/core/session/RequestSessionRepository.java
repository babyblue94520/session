package pers.clare.core.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import pers.clare.core.session.exception.SessionException;
import pers.clare.core.util.JsonUtil;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Log4j2
public abstract class RequestSessionRepository<T extends RequestSession> {
    private DataSource ds;

    private final ObjectMapper om = JsonUtil.create();

    private final Class<T> sessionClass;

    public RequestSessionRepository(DataSource ds) {
        this.ds = ds;
        Type type = this.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            this.sessionClass = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            this.sessionClass = (Class<T>) RequestSession.class;
        }
    }

    /**
     * 查詢Session by id
     *
     * @param id
     * @return
     */
    public T find(String id, Long time) {
        Long createTime = null, lastAccessTime = null;
        String username = null, attributes = null;
        Connection conn = null;
        try {
            conn = this.ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "select create_time,last_access_time,username,attributes from `session` where id = ? and last_access_time >= ?");
            ps.setString(1, id);
            ps.setLong(2, time);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                createTime = rs.getLong(1);
                lastAccessTime = rs.getLong(2);
                username = rs.getString(3);
                attributes = rs.getString(4);
            }
        } catch (SQLException e) {
            throw new SessionException(e);
        } finally {
            closeConnection(conn);
        }
        if (createTime == null) {
            return null;
        } else {
            T session = decodeAttributes(attributes);
            session.id = id;
            session.createTime = createTime;
            session.lastAccessTime = lastAccessTime;
            session.username = username;
            return session;
        }
    }

    /**
     * 查詢Username
     *
     * @param id
     * @return
     */
    public String findUsername(String id) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "select username from `session` where id = ?");
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw new SessionException(e);
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 查詢過期SessionID
     *
     * @param time
     * @return
     */
    public List<RequestSessionId> findAllInvalidate(long time) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "select id,username from `session` where last_access_time<?");
            ps.setLong(1, time);
            ResultSet rs = ps.executeQuery();
            List<RequestSessionId> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new RequestSessionId(rs.getString(1), rs.getString(2)));
            }
            return result;
        } catch (SQLException e) {
            throw new SessionException(e);
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 新增 Session
     *
     * @param session
     * @return
     * @throws SQLException
     */
    public T insert(T session) throws SQLException {
        String attributes = encodeAttributes(session);
        Connection conn = null;
        try {
            conn = this.ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "insert into `session`(id,create_time,max_inactive_interval,last_access_time,username,attributes) values(?,?,?,?,?,?)");
            ps.setString(1, session.id);
            ps.setLong(2, session.createTime);
            ps.setLong(3, session.maxInactiveInterval);
            ps.setLong(4, session.lastAccessTime);
            ps.setString(5, session.username);
            ps.setString(6, attributes);
            if (ps.executeUpdate() > 0) {
                return session;
            }
            throw new SessionException("session create failure");
        } catch (SQLException e) {
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    public int update(T session) {
        String attributes = encodeAttributes(session);
        Connection conn = null;
        try {
            conn = this.ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "update `session` set max_inactive_interval=?,last_access_time=?,username=?,attributes=? where id=?");
            ps.setLong(1, session.maxInactiveInterval);
            ps.setLong(2, session.lastAccessTime);
            ps.setString(3, session.username);
            ps.setString(4, attributes);
            ps.setString(5, session.id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new SessionException(e);
        } finally {
            closeConnection(conn);
        }
    }

    public int delete(String id) {
        Connection conn = null;
        try {
            conn = this.ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "delete from `session` where id=?");
            ps.setString(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new SessionException(e);
        } finally {
            closeConnection(conn);
        }
    }

    public int update(Collection<T> list) {
        int count = 0;
        Connection conn = null;
        try {
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "update `session` set max_inactive_interval=?,last_access_time=?,username=?,attributes=? where id=? and last_access_time<?");
            for (T session : list) {
                // 更新活躍的session到資料庫
                String attributes = encodeAttributes(session);
                try {
                    ps.setLong(1, session.maxInactiveInterval);
                    ps.setLong(2, session.lastAccessTime);
                    ps.setString(3, session.username);
                    ps.setString(4, attributes);
                    ps.setString(5, session.id);
                    ps.setLong(6, session.lastAccessTime);
                    count += ps.executeUpdate();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
            return count;
        } catch (SQLException e) {
            throw new SessionException(e);
        } finally {
            closeConnection(conn);
        }
    }

    private String encodeAttributes(T session) {
        try {
            return om.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private T decodeAttributes(String json) {
        try {
            return om.readValue(json, sessionClass);
        } catch (IOException e) {
            throw new SessionException(e);
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
