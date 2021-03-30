package pers.clare.core.session;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import pers.clare.core.util.LocaleUtil;
import pers.clare.core.util.ValueUtil;
import pers.clare.core.util.WebUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;

@Log4j2
public class RequestCache<T extends RequestSession> {
    private static final String SID = "RSESSIONID";

    private RequestSessionService<T> sessionService;
    @Getter
    private HttpServletRequest request;
    @Getter
    private HttpServletResponse response;

    @Getter
    private Long accessTime;
    @Getter
    private String remoteIp;

    private String clientIp;

    private String userAgent;

    private String url;

    private String origin;

    private String referer;

    private String lang;

    private Locale locale;

    private Map<String, String> parameterMap;

    private Map<String, String[]> parametersMap;

    private T session;

    @Getter
    private Cookie sidCookie;

    @Setter
    private boolean ping = false;

    private boolean updateSession = false;

    private boolean refresh = false;

    public void save() {
        updateSession = true;
    }

    RequestCache() {
    }

    public void init(HttpServletRequest request, HttpServletResponse response, RequestSessionService<T> sessionService) {
        this.request = request;
        this.response = response;
        this.sessionService = sessionService;
        this.accessTime = System.currentTimeMillis();
        this.remoteIp = request.getRemoteAddr();
    }

    public void invalidate() {
        if (session == null) {
            session = getSession();
        }
        if (session == null) return;
        session.valid = false;
        sessionService.invalidate(session);
        removeSidCookie();
    }

    public void refreshSession() {
        if (refresh) return;
        refresh = true;
        if (session == null) {
            if (getSession(false) != null) {
                session.setLastAccessTime(accessTime);
            }
        } else {
            if (session.valid) {
                if (!ping) {
                    session.setLastAccessTime(accessTime);
                }
                if (updateSession) {
                    sessionService.update(session);
                    updateSession = false;
                }
            }
        }
    }

    public void finish() {
        this.request = null;
        this.response = null;
        this.sessionService = null;
        this.accessTime = null;
        this.remoteIp = null;
        this.clientIp = null;
        this.userAgent = null;
        this.origin = null;
        this.url = null;
        this.referer = null;
        this.lang = null;
        this.locale = null;
        this.parametersMap = null;
        this.parameterMap = null;
        this.session = null;
        this.sidCookie = null;
        this.ping = false;
        this.updateSession = false;
        this.refresh = false;
    }

    public T getSession() {
        return getSession(false);
    }

    public T getSession(boolean auto) {
        if (session != null && session.valid) {
            return session;
        }
        getSessionId(auto);
        return session;
    }

    public String getSessionId() {
        return getSessionId(false);
    }

    public String getSessionId(boolean auto) {
        if (session == null) {
            Cookie cookie = getSidCookie();
            String id = cookie == null ? null : cookie.getValue();
            session = sessionService.find(id);
        } else {
            if (session.valid) {
                return session.getId();
            }
        }
        if (session == null || !session.valid) {
            if (auto) {
                session = sessionService.create(accessTime, getUserAgent(), getClientIp());
                addCookie(ResponseCookie.from(SID, session.getId())
                        .httpOnly(true)
                        .path("/"));
            } else {
                return null;
            }
        }

        return session.getId();
    }

    public Cookie getCookie(String name) {
        if (name == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private Cookie getSidCookie() {
        if (sidCookie != null) {
            return sidCookie;
        }
        sidCookie = getCookie(SID);
        return sidCookie;
    }

    private void removeSidCookie() {
        addCookie(ResponseCookie.from(SID, "")
                .httpOnly(true)
                .maxAge(0)
                .path("/"));
    }

    private void addCookie(ResponseCookie.ResponseCookieBuilder cookieBuilder) {
        String origin = getOrigin();
        // 處理跨域
        if (!StringUtils.isEmpty(origin)
                && !(getUrl().startsWith(origin) && getUrl().startsWith("/", origin.length()))
        ) {
            cookieBuilder.secure(true).sameSite(SameSiteCookies.NONE.getValue());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public Map<String, String[]> getParametersMap() {
        if (parametersMap == null) {
            parametersMap = request.getParameterMap();
        }
        return parametersMap;
    }

    public Map<String, String> getParameterMap() {
        if (parameterMap == null) {
            parameterMap = ValueUtil.convert(getParametersMap());
        }
        return parameterMap;
    }

    public String getClientIp() {
        if (clientIp == null) {
            clientIp = WebUtil.getClientIp(request);
        }
        return clientIp;
    }

    public String getUserAgent() {
        if (userAgent == null) {
            userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        }
        return userAgent;
    }

    public String getUrl() {
        if (url == null) {
            url = request.getRequestURL().toString();
        }
        return url;
    }

    public String getOrigin() {
        if (origin == null) {
            origin = request.getHeader(HttpHeaders.ORIGIN);
        }
        return origin;
    }

    public String getReferer() {
        if (referer == null) {
            referer = request.getHeader(HttpHeaders.REFERER);
        }
        return referer;
    }

    public String getLang() {
        if (lang == null) {
            lang = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        }
        return lang;
    }

    public Locale getLocale() {
        if (locale != null) return locale;
        if (lang == null) getLang();
        if (lang != null) return locale = LocaleUtil.get(lang);
        return null;
    }
}
