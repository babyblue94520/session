package pers.clare.demo.session;

import org.springframework.stereotype.Service;
import pers.clare.core.session.RequestSessionRepository;

import javax.sql.DataSource;

@Service
public class UserRequestSessionRepository extends RequestSessionRepository<UserSession> {
    public UserRequestSessionRepository(DataSource dataSource) {
        super(dataSource);
    }
}
