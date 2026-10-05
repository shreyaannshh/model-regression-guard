package com.shreyansh.regressionguard.provider;

/**
 * @param text          the model's answer
 * @param modelReported the model name the provider says it used; may differ from the one requested, may be null
 */
public record ProviderResponse(String text, String modelReported) {
}