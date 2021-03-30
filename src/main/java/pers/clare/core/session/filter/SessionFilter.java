package pers.clare.core.session.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import pers.clare.core.session.RequestCache;
import pers.clare.core.session.RequestCacheHolder;
import pers.clare.core.session.RequestSessionService;
import pers.clare.demo.session.UserSession;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class SessionFilter implements Filter {
    @Autowired
    private RequestSessionService<UserSession> requestSessionService;

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("SessionFilter startup");
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(
            ServletRequest req
            , ServletResponse res
            , FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        RequestCache<UserSession> requestCache = RequestCacheHolder.init(request, response, requestSessionService);
        try {
            chain.doFilter(request, response);
            requestCache.refreshSession();
        } finally {
            requestCache.finish();
        }
    }
}
