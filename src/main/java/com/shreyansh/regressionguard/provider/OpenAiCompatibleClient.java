package com.shreyansh.regressionguard.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Calls POST {baseUrl}/chat/completions, the format OpenAI, Groq, Gemini and most self-hosted servers accept. */
@Component
public class OpenAiCompatibleClient implements ProviderClient {

    private final ProviderProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleClient(ProviderProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory);
        if (properties.hasApiKey()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
        }
        this.restClient = builder.build();
    }

    @Override
    public String modelRequested() {
        return properties.model();
    }

    @Override
    public ProviderResponse complete(String prompt) {
        ChatRequest request = new ChatRequest(
                properties.model(),
                List.of(new ChatMessage("user", prompt)),
                0.0);

        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientResponseException e) {
            // The provider answered with an error status: 401 bad key, 404 unknown model, 429 rate limit.
            throw new ProviderException(
                    "provider returned HTTP " + e.getStatusCode().value() + snippet(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            // No usable answer: timeout, connection refused, or a body that isn't the expected JSON.
            throw new ProviderException("provider call failed: " + e.getMessage(), e);
        }
        return toProviderResponse(response);
    }

    static ProviderResponse toProviderResponse(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ProviderException("provider returned no choices");
        }
        ChatMessage message = response.choices().get(0).message();
        if (message == null || message.content() == null) {
            throw new ProviderException("provider returned no message content");
        }
        // An empty string is still an answer. The property rules decide whether it's acceptable.
        return new ProviderResponse(message.content(), response.model());
    }

    /** The first 200 characters of an error body, on one line. Providers explain errors there. */
    private static String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return ": " + (oneLine.length() > 200 ? oneLine.substring(0, 200) + "..." : oneLine);
    }

    // Wire format of the chat completions API. Only the fields this tool uses are listed;
    // ignoreUnknown keeps it working when providers add new fields.

    public record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(String model, List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(ChatMessage message) {
    }
}