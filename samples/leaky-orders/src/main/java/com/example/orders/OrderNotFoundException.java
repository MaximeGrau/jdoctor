package com.example.orders;

import java.util.UUID;

/**
 * BUG: nothing maps this exception to an HTTP status, so an unknown id is a 500 error with a stack
 * trace instead of a 404 (JD-EXC-002).
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order " + id + " not found");
    }
}
