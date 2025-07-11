package usercommons.implementation.exception;

public class UserCommonsException extends Exception {
    public UserCommonsException(String message) {
        super(message);
    }

    public UserCommonsException( String message, Exception e ) {
        super(message, e);
    }
}
