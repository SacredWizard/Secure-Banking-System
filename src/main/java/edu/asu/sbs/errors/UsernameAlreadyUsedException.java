package edu.asu.sbs.errors;

public class UsernameAlreadyUsedException extends RuntimeException {

    private static final long serialVersionUID = -1L;

    public UsernameAlreadyUsedException() {
        super("UserName already used");
    }
}
