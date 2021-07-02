package com.demo.controller;

import com.demo.data.repository.SessionRepository;
import com.demo.session.UserSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pers.clare.session.RequestCache;
import pers.clare.session.RequestCacheHolder;
import pers.clare.session.util.JsonUtil;

import javax.sql.DataSource;

@Log4j2
@RestController
@RequestMapping("session")
public class SessionController implements InitializingBean {
    @Autowired
    private SessionRepository sessionRepository;

//    @Autowired
//    private SyncSessionService<SyncSession> syncSessionService;

    @Autowired
    private DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
//        syncSessionService.addInvalidateListeners((id, username, type) -> {
//            log.info("{} {} {}", id, username, type);
//        });
    }

    @PostMapping
    public String login(
            String username
    ) throws JsonProcessingException {
        RequestCache<UserSession> requestCache = RequestCacheHolder.get(UserSession.class);
        UserSession session = requestCache.getSession(false);
        if (session == null) {
            session = requestCache.getSession(true);
            session.setUsername(username);
        }
        return JsonUtil.encode(sessionRepository.getOne(session.getId()));
    }

    @PutMapping
    public String test(
            String test
    ) throws JsonProcessingException {
        RequestCache<UserSession> requestCache = RequestCacheHolder.get(UserSession.class);
        UserSession session = requestCache.getSession(false);
        if (session == null) {
            return null;
        } else {
            session.setTest(test);
            // disable SerializationFeature.FAIL_ON_EMPTY_BEANS
            return JsonUtil.encode(sessionRepository.findById(session.getId()).orElse(null));
        }
    }

    @DeleteMapping
    public String logout() throws JsonProcessingException {
        RequestCache<UserSession> requestCache = RequestCacheHolder.get(UserSession.class);
        UserSession session = requestCache.getSession(false);
        if (session == null) {
            return null;
        } else {
            session.invalidate();
            // disable SerializationFeature.FAIL_ON_EMPTY_BEANS

            return JsonUtil.encode(sessionRepository.findById(session.getId()).orElse(null));
        }
    }
}
