package com.shreyansh.regressionguard.store;

/** Thrown when an id points at nothing. The API turns it into 404 Not Found. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String kind, String id) {
        super(kind + " not found: " + id);
    }
}