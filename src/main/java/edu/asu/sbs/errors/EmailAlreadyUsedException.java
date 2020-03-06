package edu.asu.sbs.errors;

public class EmailAlreadyUsedException extends RuntimeException {
    private static final long serialVersionUID = -1;

    public EmailAlreadyUsedException() {
        super("Email is already in use!");
    }
}
