package edu.asu.sbs.errors;

public class UserTypeException extends RuntimeException {
    private static final long serialVersionUID = -1L;

    public UserTypeException() {
        super("User Type is Invalid!");
    }
}
