package pers.clare.demo.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pers.clare.core.session.RequestSessionService;

import java.time.Duration;

@Service
public class UserRequestSessionService extends RequestSessionService<UserSession> {
    public UserRequestSessionService(
            UserRequestSessionRepository repository
            , SessionNotifyService sessionNotifyService
            , @Value("${session.timeout:1800000}") Duration timeout
    ) {
        super(repository, sessionNotifyService, timeout);
    }
}
