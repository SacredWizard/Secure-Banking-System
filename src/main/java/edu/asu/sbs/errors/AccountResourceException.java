package edu.asu.sbs.errors;

public class AccountResourceException extends RuntimeException {
    public AccountResourceException(String message) {
        super(message);
    }
}
