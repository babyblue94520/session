package com.demo.session;

import lombok.Getter;
import pers.clare.session.SyncSession;

public class UserSession extends SyncSession {
    @Getter
    private String test = "";

    public void setTest(String test) {
        this.test = test;
        save();
    }
}
