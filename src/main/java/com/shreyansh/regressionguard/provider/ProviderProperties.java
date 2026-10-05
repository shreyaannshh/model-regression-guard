package com.shreyansh.regressionguard.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "provider")
public record ProviderProperties(String baseUrl, String apiKey, String model, Duration timeout) {
    public ProviderProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("provider.base-url must be set");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("provider.model must be set");
        }

        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        apiKey = apiKey == null ? "" : apiKey.trim();
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    public boolean hasApiKey() {
        return !apiKey.isEmpty();
    }

    @Override
    public String toString() {
        return "ProviderProperties[baseUrl=" + baseUrl + ", apiKey=" + (hasApiKey() ? "****" : "<none>") + ", model="
                + model + ", timeout=" + timeout + "]";
    }
}
