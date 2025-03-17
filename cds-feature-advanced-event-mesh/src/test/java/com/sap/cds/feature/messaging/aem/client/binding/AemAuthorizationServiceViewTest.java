package com.sap.cds.feature.messaging.aem.client.binding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AemAuthorizationServiceViewTest {

    @Mock
    private ServiceBinding serviceBinding;

    private AemAuthorizationServiceView authorizationServiceView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authorizationServiceView = new AemAuthorizationServiceView(serviceBinding);
    }

    @Test
    void testIsTokenEndpointPresent() {
        Map<String, Object> credentials = Map.of(
            "authentication-service", Map.of(
                "tokenendpoint", "http://example.com/token",
                "clientid", "test-client-id",
                "clientsecret", "test-client-secret"
            )
        );
        when(serviceBinding.getCredentials()).thenReturn(credentials);

        assertTrue(authorizationServiceView.isTokenEndpointPresent());
    }

    @Test
    void testIsTokenEndpointPresent_NotPresent() {
        when(serviceBinding.getCredentials()).thenReturn(Map.of());

        assertFalse(authorizationServiceView.isTokenEndpointPresent());
    }

    @Test
    void testGetTokenEndpoint() {
        Map<String, Object> credentials = Map.of(
            "authentication-service", Map.of("tokenendpoint", "http://example.com/token")
        );
        when(serviceBinding.getCredentials()).thenReturn(credentials);

        Optional<String> tokenEndpoint = authorizationServiceView.getTokenEndpoint();
        assertTrue(tokenEndpoint.isPresent());
        assertEquals("http://example.com/token", tokenEndpoint.get());
    }

    @Test
    void testGetTokenEndpoint_NotPresent() {
        when(serviceBinding.getCredentials()).thenReturn(Map.of());

        Optional<String> tokenEndpoint = authorizationServiceView.getTokenEndpoint();
        assertFalse(tokenEndpoint.isPresent());
    }

    @Test
    void testGetClientId() {
        Map<String, Object> credentials = Map.of(
            "authentication-service", Map.of("clientid", "test-client-id")
        );
        when(serviceBinding.getCredentials()).thenReturn(credentials);

        Optional<String> clientId = authorizationServiceView.getClientId();
        assertTrue(clientId.isPresent());
        assertEquals("test-client-id", clientId.get());
    }

    @Test
    void testGetClientId_NotPresent() {
        when(serviceBinding.getCredentials()).thenReturn(Map.of());

        Optional<String> clientId = authorizationServiceView.getClientId();
        assertFalse(clientId.isPresent());
    }

    @Test
    void testGetClientSecret() {
        Map<String, Object> credentials = Map.of(
            "authentication-service", Map.of("clientsecret", "test-client-secret")
        );
        when(serviceBinding.getCredentials()).thenReturn(credentials);

        Optional<String> clientSecret = authorizationServiceView.getClientSecret();
        assertTrue(clientSecret.isPresent());
        assertEquals("test-client-secret", clientSecret.get());
    }

    @Test
    void testGetClientSecret_NotPresent() {
        when(serviceBinding.getCredentials()).thenReturn(Map.of());

        Optional<String> clientSecret = authorizationServiceView.getClientSecret();
        assertFalse(clientSecret.isPresent());
    }
}
