package pers.clare.demo.session;

import lombok.Getter;
import pers.clare.core.session.RequestSession;

public class UserSession extends RequestSession {
    @Getter
    private String test = "";

    public void setTest(String test) {
        this.test = test;
        save();
    }
}
