package com.shreyansh.regressionguard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.regressionguard.provider.FakeProviderClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaselineApiTest {

    /** Replaces the real Groq client in this test, so no network call and no key is needed. */
    @TestConfiguration
    static class FakeProviderConfig {
        @Bean
        @Primary
        FakeProviderClient fakeProviderClient() {
            return new FakeProviderClient().answer("Say hello", "Hello!");
        }
    }

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(publisher)
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void captureCreatesAnInactiveSetAndActivateMakesItActive() throws Exception {
        post("/cases", """
                {"id": "api-hello", "prompt": "Say hello"}
                """);

        HttpResponse<String> captured = post("/baselines", null);
        assertEquals(201, captured.statusCode(), captured.body());
        assertTrue(captured.body().contains("\"active\":false"), captured.body());

        String location = captured.headers().firstValue("Location").orElseThrow();
        String setId = location.substring(location.lastIndexOf('/') + 1);

        HttpResponse<String> activated = post("/baselines/" + setId + "/activate", null);
        assertEquals(200, activated.statusCode(), activated.body());
        assertTrue(activated.body().contains("\"active\":true"), activated.body());

        assertTrue(get("/baselines/" + setId).body().contains("\"active\":true"));
    }

    @Test
    void activatingAnUnknownSetIsNotFound() throws Exception {
        assertEquals(404, post("/baselines/does-not-exist/activate", null).statusCode());
    }

    @Test
    void unknownSetIsNotFound() throws Exception {
        assertEquals(404, get("/baselines/does-not-exist").statusCode());
    }
}