package com.shreyansh.regressionguard.store;

public class DuplicateIdException extends RuntimeException {

    /** Thrown when something is saved under an id that is already taken. The API turns it into 409 Conflict. */
    public DuplicateIdException(String kind, String id) {
        super(kind + "already exists: " + id);
    }
    
}
