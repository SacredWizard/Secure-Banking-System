package edu.asu.sbs.errors;

public class SsnAlreadyUsedException extends RuntimeException {

    private static final long serialVersionUID = -1L;

    public SsnAlreadyUsedException() {
        super("SSN is already in Use");
    }

}
