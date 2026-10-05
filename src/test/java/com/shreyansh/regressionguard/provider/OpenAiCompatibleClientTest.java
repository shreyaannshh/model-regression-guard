package com.shreyansh.regressionguard.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleClientTest {

    // Shaped like a real Groq response, including fields the client doesn't use.
    private static final String OK_BODY = """
            {"id": "chatcmpl-1", "object": "chat.completion", "model": "llama-3.1-8b-instant",
             "system_fingerprint": "fp_abc123",
             "choices": [{"index": 0, "message": {"role": "assistant", "content": "Paris"}, "finish_reason": "stop"}],
             "usage": {"prompt_tokens": 10, "completion_tokens": 1, "total_tokens": 11}}
            """;

    private HttpServer server;
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Starts a fake provider on a free port that always answers with the given status and body. */
    private OpenAiCompatibleClient clientFor(int status, String body, long delayMillis,
                                             String apiKey, Duration timeout) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1";
        return new OpenAiCompatibleClient(new ProviderProperties(baseUrl, apiKey, "test-model", timeout));
    }

    private OpenAiCompatibleClient clientFor(int status, String body) throws IOException {
        return clientFor(status, body, 0, "test-key", Duration.ofSeconds(5));
    }

    @Test
    void returnsTextAndReportedModel() throws Exception {
        ProviderResponse response = clientFor(200, OK_BODY).complete("What is the capital of France?");
        assertEquals("Paris", response.text());
        assertEquals("llama-3.1-8b-instant", response.modelReported());
    }

    @Test
    void sendsModelPromptAndTemperatureZero() throws Exception {
        clientFor(200, OK_BODY).complete("What is the capital of France?");
        String sent = lastRequestBody.get();
        assertTrue(sent.contains("\"model\":\"test-model\""), sent);
        assertTrue(sent.contains("What is the capital of France?"), sent);
        assertTrue(sent.contains("\"temperature\":0.0"), sent);
    }

    @Test
    void sendsBearerTokenWhenKeyIsSet() throws Exception {
        clientFor(200, OK_BODY).complete("hi");
        assertEquals("Bearer test-key", lastAuthHeader.get());
    }

    @Test
    void sendsNoAuthHeaderWhenKeyIsBlank() throws Exception {
        clientFor(200, OK_BODY, 0, "", Duration.ofSeconds(5)).complete("hi");
        assertNull(lastAuthHeader.get());
    }

    @Test
    void httpErrorBecomesProviderExceptionWithStatusAndReason() throws Exception {
        var client = clientFor(429, "{\"error\": {\"message\": \"Rate limit reached\"}}");
        ProviderException e = assertThrows(ProviderException.class, () -> client.complete("hi"));
        assertTrue(e.getMessage().contains("429"), e.getMessage());
        assertTrue(e.getMessage().contains("Rate limit reached"), e.getMessage());
    }

    @Test
    void slowProviderBecomesProviderException() throws Exception {
        var client = clientFor(200, OK_BODY, 2000, "test-key", Duration.ofMillis(300));
        assertThrows(ProviderException.class, () -> client.complete("hi"));
    }

    @Test
    void noChoicesBecomesProviderException() throws Exception {
        var client = clientFor(200, "{\"model\": \"m\", \"choices\": []}");
        assertThrows(ProviderException.class, () -> client.complete("hi"));
    }

    @Test
    void nonJsonBodyBecomesProviderException() throws Exception {
        var client = clientFor(200, "<html>Bad Gateway</html>");
        assertThrows(ProviderException.class, () -> client.complete("hi"));
    }

    @Test
    void modelRequestedComesFromConfig() throws Exception {
        assertEquals("test-model", clientFor(200, OK_BODY).modelRequested());
    }
}