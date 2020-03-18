package edu.asu.sbs.errors;

public class PhoneNumberAlreadyUsedException extends RuntimeException {

    private static final long serialVersionUID = -1L;

    public PhoneNumberAlreadyUsedException() {
        super("Phone Number Already Used");
    }
}
