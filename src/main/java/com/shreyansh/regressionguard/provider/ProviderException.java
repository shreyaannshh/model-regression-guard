package com.shreyansh.regressionguard.provider;

/** The call failed: timeout, HTTP error, unreadable body. Becomes the ERROR verdict in a run. */
public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}