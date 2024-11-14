package dk.superawesome.exceptions;

public class RequestException extends Exception {

    public RequestException(Exception cause) {
        super(cause);
    }

    public RequestException() {

    }
}
