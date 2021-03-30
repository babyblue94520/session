package pers.clare.core.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RequestCacheHolder {

    private static final ThreadLocal<RequestCache<? extends RequestSession>> cache =
            new NamedThreadLocal<>("Request Cache Holder");

    public static <T extends RequestSession> RequestCache<T> get() {
        RequestCache req = cache.get();
        if (req == null) throw new IllegalArgumentException("RequestCacheHolder not init");
        return req;
    }

    public static RequestCache init(
            HttpServletRequest request
            , HttpServletResponse response
            , RequestSessionService sessionService
    ) {
        RequestCache req = cache.get();
        if (req == null) {
            cache.set((req = new RequestCache()));
        }
        req.init(request, response, sessionService);
        return req;
    }
}


