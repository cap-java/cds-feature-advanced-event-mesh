package com.sap.cds.feature.messaging.aem.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;

class RestClientIT {

    private ClientAndServer mockServer;
    private RestClient client;

    @BeforeEach
    void setUp() {
        mockServer = ClientAndServer.startClientAndServer(0);
        HttpDestination destination = DefaultHttpDestination
                .builder("http://localhost:" + mockServer.getPort())
                .build();
        client = new RestClient(destination);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    // --- getRequest ---

    @Test
    void getRequest_returns_parsed_json() throws IOException {
        mockServer.when(request().withMethod("GET").withPath("/data"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"key\":\"value\"}"));

        JsonNode result = client.getRequest("/data");

        assertEquals("value", result.get("key").asText());
    }

    @Test
    void getRequest_throws_service_exception_on_4xx() {
        mockServer.when(request().withMethod("GET").withPath("/missing"))
                .respond(response().withStatusCode(404).withReasonPhrase("Not Found"));

        assertThrows(ServiceException.class, () -> client.getRequest("/missing"));
    }

    @Test
    void getRequest_throws_service_exception_on_5xx() {
        mockServer.when(request().withMethod("GET").withPath("/error"))
                .respond(response().withStatusCode(500).withReasonPhrase("Internal Server Error"));

        assertThrows(ServiceException.class, () -> client.getRequest("/error"));
    }

    @Test
    void getRequest_returns_empty_object_when_no_body() throws IOException {
        mockServer.when(request().withMethod("GET").withPath("/empty"))
                .respond(response().withStatusCode(204));

        JsonNode result = client.getRequest("/empty");

        assertTrue(result.isEmpty());
    }

    @Test
    void getRequest_throws_io_exception_on_non_json_content_type() {
        mockServer.when(request().withMethod("GET").withPath("/text"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.TEXT_PLAIN)
                        .withBody("hello"));

        assertThrows(IOException.class, () -> client.getRequest("/text"));
    }

    // --- postRequest (map overload) ---

    @Test
    void postRequest_map_sends_json_body_and_returns_parsed_response() throws IOException {
        mockServer.when(request().withMethod("POST").withPath("/items"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"created\":true}"));

        JsonNode result = client.postRequest("/items", Map.of("name", "queue1"));

        assertTrue(result.get("created").asBoolean());
    }

    @Test
    void postRequest_map_throws_service_exception_on_error() {
        mockServer.when(request().withMethod("POST").withPath("/fail"))
                .respond(response().withStatusCode(400).withReasonPhrase("Bad Request"));

        assertThrows(ServiceException.class,
                () -> client.postRequest("/fail", Map.of("x", "y")));
    }

    // --- postRequest (string overload) ---

    @Test
    void postRequest_string_sends_body_with_content_type_and_accept_headers() throws IOException {
        mockServer.when(request().withMethod("POST").withPath("/handshake"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        JsonNode result = client.postRequest("/handshake", "{\"hostName\":\"broker.example.com\"}", Map.of());

        assertNotNull(result);
    }

    @Test
    void postRequest_string_applies_extra_headers() throws IOException {
        mockServer.when(
                request().withMethod("POST").withPath("/custom")
                        .withHeader("X-Custom", "myvalue"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        JsonNode result = client.postRequest("/custom", "{}", Map.of("X-Custom", "myvalue"));

        assertNotNull(result);
    }

    @Test
    void postRequest_string_handles_empty_response_body() throws IOException {
        mockServer.when(request().withMethod("POST").withPath("/created"))
                .respond(response().withStatusCode(201));

        JsonNode result = client.postRequest("/created", "{}", Map.of());

        assertTrue(result.isEmpty());
    }

    // --- deleteRequest ---

    @Test
    void deleteRequest_success_returns_empty_object() throws IOException {
        mockServer.when(request().withMethod("DELETE").withPath("/queues/myqueue"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        JsonNode result = client.deleteRequest("/queues/myqueue");

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteRequest_throws_service_exception_on_error() {
        mockServer.when(request().withMethod("DELETE").withPath("/queues/gone"))
                .respond(response().withStatusCode(500).withReasonPhrase("Server Error"));

        assertThrows(ServiceException.class, () -> client.deleteRequest("/queues/gone"));
    }

    @Test
    void deleteRequest_no_entity_returns_empty_object() throws IOException {
        mockServer.when(request().withMethod("DELETE").withPath("/queues/q2"))
                .respond(response().withStatusCode(204));

        JsonNode result = client.deleteRequest("/queues/q2");

        assertTrue(result.isEmpty());
    }
}
