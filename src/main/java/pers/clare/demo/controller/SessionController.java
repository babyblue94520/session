package pers.clare.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pers.clare.core.session.RequestCache;
import pers.clare.core.session.RequestCacheHolder;
import pers.clare.core.util.JsonUtil;
import pers.clare.demo.data.repository.SessionRepository;
import pers.clare.demo.session.UserSession;

@RestController
@RequestMapping("session")
public class SessionController {
    @Autowired
    private SessionRepository sessionRepository;

    @PostMapping
    public String login(
            String username
    ) throws JsonProcessingException {
        RequestCache<UserSession> requestCache = RequestCacheHolder.get();
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
        RequestCache<UserSession> requestCache = RequestCacheHolder.get();
        UserSession session = requestCache.getSession(false);
        if (session == null) {
            return null;
        } else {
            session.setTest(test);
            // disable SerializationFeature.FAIL_ON_EMPTY_BEANS
            return JsonUtil.encode(sessionRepository.getOne(session.getId()));
        }
    }

    @DeleteMapping("test")
    public String logout() throws JsonProcessingException {
        RequestCache<UserSession> requestCache = RequestCacheHolder.get();
        UserSession session = requestCache.getSession(false);
        if (session == null) {
            return null;
        }else{
            session.invalidate();
            // disable SerializationFeature.FAIL_ON_EMPTY_BEANS

            return JsonUtil.encode(sessionRepository.findById(session.getId()).orElse(null));
        }
    }
}
