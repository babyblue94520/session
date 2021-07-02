package com.demo.session;

import org.springframework.stereotype.Service;
import pers.clare.session.SyncSessionServiceImpl;

import javax.sql.DataSource;

//@Service
public class UserSessionServiceImpl extends SyncSessionServiceImpl<UserSession> {
    public UserSessionServiceImpl(DataSource dataSource) {
        super(dataSource);
    }
}
