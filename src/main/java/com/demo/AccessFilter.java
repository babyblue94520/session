package com.demo;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.demo.session.UserSession;
import pers.clare.session.SyncSession;
import pers.clare.session.SyncSessionService;

import javax.servlet.*;
import java.io.IOException;

/**
 * 訪問權限過濾
 */
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class AccessFilter implements Filter {
    @Autowired
    private SyncSessionService syncSessionService;

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("AccessFilter startup");
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
        SyncSession session = syncSessionService.create(System.currentTimeMillis(),"","");
        chain.doFilter(req, res);
    }
}
