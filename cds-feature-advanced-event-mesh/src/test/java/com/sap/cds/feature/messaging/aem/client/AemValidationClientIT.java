package com.sap.cds.feature.messaging.aem.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;

class AemValidationClientIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ClientAndServer mockServer;
    private AemValidationClient client;

    @BeforeEach
    void setUp() {
        mockServer = ClientAndServer.startClientAndServer(0);
        HttpDestination destination = DefaultHttpDestination
                .builder("http://localhost:" + mockServer.getPort())
                .build();
        client = new AemValidationClient(destination);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    @Test
    void validate_sends_only_host_name_when_subaccount_id_is_null() throws Exception {
        mockServer.when(request().withMethod("POST"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.validate("https://broker.example.com:943", null);

        HttpRequest[] recorded = mockServer.retrieveRecordedRequests(request().withMethod("POST"));
        assertEquals(1, recorded.length);
        JsonNode body = MAPPER.readTree(recorded[0].getBodyAsString());
        assertEquals("broker.example.com", body.get("hostName").asText());
        assertFalse(body.has("subaccountId"), "subaccountId must not be present when null");
    }

    @Test
    void validate_sends_host_name_and_subaccount_id_when_provided() throws Exception {
        mockServer.when(request().withMethod("POST"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.validate("https://broker.example.com:943", "my-subaccount-42");

        HttpRequest[] recorded = mockServer.retrieveRecordedRequests(request().withMethod("POST"));
        assertEquals(1, recorded.length);
        JsonNode body = MAPPER.readTree(recorded[0].getBodyAsString());
        assertEquals("broker.example.com", body.get("hostName").asText());
        assertEquals("my-subaccount-42", body.get("subaccountId").asText());
    }

    @Test
    void validate_omits_subaccount_id_when_empty_string() throws Exception {
        mockServer.when(request().withMethod("POST"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.validate("https://broker.example.com:943", "");

        HttpRequest[] recorded = mockServer.retrieveRecordedRequests(request().withMethod("POST"));
        assertEquals(1, recorded.length);
        JsonNode body = MAPPER.readTree(recorded[0].getBodyAsString());
        assertFalse(body.has("subaccountId"), "subaccountId must not be present for empty string");
    }

    @Test
    void validate_extracts_host_from_full_uri() throws Exception {
        mockServer.when(request().withMethod("POST"))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.validate("https://mr-connection-xyz.messaging.solace.cloud:943/some/path", null);

        HttpRequest[] recorded = mockServer.retrieveRecordedRequests(request().withMethod("POST"));
        assertEquals(1, recorded.length);
        JsonNode body = MAPPER.readTree(recorded[0].getBodyAsString());
        assertEquals("mr-connection-xyz.messaging.solace.cloud", body.get("hostName").asText());
    }

    @Test
    void validate_throws_io_exception_when_server_returns_error() {
        mockServer.when(request().withMethod("POST"))
                .respond(response().withStatusCode(403).withReasonPhrase("Forbidden"));

        assertThrows(ServiceException.class,
                () -> client.validate("https://broker.example.com:943", null));
    }

    @Test
    void validate_throws_uri_syntax_exception_for_invalid_uri() {
        assertThrows(URISyntaxException.class,
                () -> client.validate("not a valid uri %%", null));
    }
}
