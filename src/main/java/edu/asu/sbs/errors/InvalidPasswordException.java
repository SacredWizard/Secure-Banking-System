package edu.asu.sbs.errors;

public class InvalidPasswordException extends GenericRuntimeException {

    private static final long serialVersionUID = -1L;

    public InvalidPasswordException() {
        super("Invalid Password");
    }
}
