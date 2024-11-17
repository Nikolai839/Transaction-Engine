package dk.superawesome.core.exceptions;

public class RequestException extends Exception {

    public RequestException(Exception cause) {
        super(cause);
    }

    public RequestException() {

    }
}
