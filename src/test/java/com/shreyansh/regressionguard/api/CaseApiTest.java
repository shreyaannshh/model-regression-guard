package com.shreyansh.regressionguard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CaseApiTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/cases"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // The server is shared across tests, so every test uses its own case id.

    @Test
    void createdCaseIsReturnedWithItsRuleNames() throws Exception {
        HttpResponse<String> response = post("""
                {"id": "refund-json", "prompt": "Return the refund decision as JSON",
                 "rules": [{"type": "validJson"}, {"type": "maxWords", "limit": 200}]}
                """);
        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("MaxWords(200)"));
        assertTrue(response.headers().firstValue("Location").orElse("").endsWith("/cases/refund-json"));
    }

    @Test
    void duplicateIdIsConflict() throws Exception {
        String body = """
                {"id": "dup-case", "prompt": "Say hello"}
                """;
        assertEquals(201, post(body).statusCode());
        assertEquals(409, post(body).statusCode());
    }

    @Test
    void unknownRuleTypeIsBadRequest() throws Exception {
        HttpResponse<String> response = post("""
                {"id": "bad-type", "prompt": "Say hello", "rules": [{"type": "maxLength", "limit": 5}]}
                """);
        assertEquals(400, response.statusCode());
    }

    @Test
    void invalidRuleIsBadRequestWithAReason() throws Exception {
        HttpResponse<String> response = post("""
                {"id": "bad-limit", "prompt": "Say hello", "rules": [{"type": "maxWords", "limit": 0}]}
                """);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("limit"));
    }

    @Test
    void blankPromptIsBadRequest() throws Exception {
        assertEquals(400, post("""
                {"id": "no-prompt", "prompt": "  "}
                """).statusCode());
    }

    @Test
    void idWithSpacesIsBadRequest() throws Exception {
        assertEquals(400, post("""
                {"id": "has spaces", "prompt": "Say hello"}
                """).statusCode());
    }

    @Test
    void listIncludesCreatedCase() throws Exception {
        post("""
                {"id": "listed-case", "prompt": "Say hello"}
                """);
        HttpResponse<String> response = get("/cases");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("listed-case"));
    }

    @Test
    void unknownCaseIsNotFound() throws Exception {
        assertEquals(404, get("/cases/does-not-exist").statusCode());
    }
}