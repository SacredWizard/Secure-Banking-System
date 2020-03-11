package edu.asu.sbs.config;

public final class Constants {

    public static final String USERNAME_REGEX = "^[_.@A-Za-z0-9-]*$";
    public static final String SSN_REGEX = "^(?!000|666)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$";
    public static final String POSTAL_CODE_REGEX = "^[0-9]{5}(?:-[0-9]{4})?$";
    public static final String PHONE_NUMBER_REGEX = "^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$";
    public static final int PASSWORD_MIN_LENGTH = 4;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final String ACCOUNT_NUMBER_REGEX = "^\\w{1,17}$";

    private Constants() {

    }
}
