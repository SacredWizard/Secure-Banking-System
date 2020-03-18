package edu.asu.sbs.security;

import javax.naming.AuthenticationException;

public class UserNotActivatedException extends AuthenticationException {

    private static final long serialVersionUID = -1L;

    public UserNotActivatedException(String message) {
        super(message);
    }
}
