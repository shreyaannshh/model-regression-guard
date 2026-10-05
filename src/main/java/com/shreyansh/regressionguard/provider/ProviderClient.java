package com.shreyansh.regressionguard.provider;

/**
 * The only part of the tool that talks to the outside world.
 * Swapping providers is a config change; swapping protocols is a new implementation of this interface.
 */
public interface ProviderClient {

    /** Sends one prompt at temperature 0. Throws ProviderException when no usable answer comes back. */
    ProviderResponse complete(String prompt);

    /** The model name sent with every request, for example an alias. Stored on baseline sets and runs. */
    String modelRequested();
}