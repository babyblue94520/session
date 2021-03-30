package pers.clare.core.session.exception;

public class SessionException extends RuntimeException{

    public SessionException(String message) {
        super(message);
    }

    public SessionException(Throwable e) {
        super(e);
    }
}
