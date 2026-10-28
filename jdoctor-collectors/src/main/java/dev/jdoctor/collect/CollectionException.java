package dev.jdoctor.collect;

public class CollectionException extends RuntimeException {

    public CollectionException(String message) {
        super(message);
    }

    public CollectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
