package com.shreyansh.regressionguard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shreyansh.regressionguard.provider.FakeProviderClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The full loop over HTTP: register, capture, activate, run.
 * Ordered on purpose: the 409 check needs a server where nothing has been activated yet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RunApiTest {

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

    private static String idFrom(HttpResponse<String> response) {
        String location = response.headers().firstValue("Location").orElseThrow();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    @Test
    @Order(1)
    void runBeforeAnyActivationIsConflict() throws Exception {
        assertEquals(409, post("/runs", null).statusCode());
    }

    @Test
    @Order(2)
    void fullLoopEndsInPassed() throws Exception {
        post("/cases", """
                {"id": "run-hello", "prompt": "Say hello"}
                """);
        HttpResponse<String> captured = post("/baselines", null);
        assertEquals(201, captured.statusCode(), captured.body());
        post("/baselines/" + idFrom(captured) + "/activate", null);

        HttpResponse<String> run = post("/runs", null);
        assertEquals(201, run.statusCode(), run.body());
        assertTrue(run.body().contains("\"status\":\"PASSED\""), run.body());

        HttpResponse<String> fetched = get("/runs/" + idFrom(run));
        assertEquals(200, fetched.statusCode());
        assertTrue(fetched.body().contains("run-hello"));
    }

    @Test
    @Order(3)
    void unknownRunIsNotFound() throws Exception {
        assertEquals(404, get("/runs/does-not-exist").statusCode());
    }
}