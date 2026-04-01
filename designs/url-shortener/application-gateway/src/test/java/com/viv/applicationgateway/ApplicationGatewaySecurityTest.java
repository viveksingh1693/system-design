package com.viv.applicationgateway;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationGatewaySecurityTest {

    private static HttpServer backendStub;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startBackend() throws IOException {
        backendStub = HttpServer.create(new InetSocketAddress(0), 0);
        backendStub.createContext("/api/v1/urls/demo", exchange ->
                respond(exchange, 200, "application/json", "{\"shortCode\":\"demo\"}"));
        backendStub.createContext("/abc123", exchange -> {
            exchange.getResponseHeaders().add("Location", "https://example.com");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        backendStub.start();
    }

    @AfterAll
    static void stopBackend() {
        if (backendStub != null) {
            backendStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.backend.base-url", () -> "http://localhost:" + backendStub.getAddress().getPort());
        registry.add("app.security.username", () -> "gateway");
        registry.add("app.security.password", () -> "secret");
        registry.add("app.security.role", () -> "EDGE_ADMIN");
    }

    @Test
    void shouldRejectUnauthenticatedApiRequests() {
        webTestClient.get()
                .uri("/api/v1/urls/demo")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // @Test
    // void shouldProxyAuthenticatedApiRequests() {
    //     webTestClient.mutateWith(mockHttpBasic("gateway", "secret"))
    //             .get()
    //             .uri("/api/v1/urls/demo")
    //             .exchange()
    //             .expectStatus().isOk()
    //             .expectBody()
    //             .jsonPath("$.shortCode").isEqualTo("demo");
    // }

    @Test
    void shouldAllowRedirectWithoutAuthentication() {
        webTestClient.get()
                .uri("/abc123")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().location("https://example.com");
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }
}
