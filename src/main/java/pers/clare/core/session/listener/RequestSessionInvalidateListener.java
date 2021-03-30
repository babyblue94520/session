package pers.clare.core.session.listener;

public interface RequestSessionInvalidateListener {
    /**
     * @param sessionId
     * @param username
     * @param type   {@link pers.clare.core.session.constant.InvalidateType}
     * @param count
     */
    void onInvalidate(String sessionId, String username, int type, int count);
}
