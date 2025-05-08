package com.sap.cds.feature.messaging.aem.client.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AemAuthenticationServiceViewTest {

  @Mock private ServiceBinding serviceBinding;

  private AemAuthenticationServiceView authenticationServiceView;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    authenticationServiceView = new AemAuthenticationServiceView(serviceBinding);
  }

  @Test
  void testIsTokenEndpointPresent() {
    Map<String, Object> credentials =
        Map.of(
            "authentication-service",
            Map.of(
                "tokenendpoint", "http://example.com/token",
                "clientid", "test-client-id",
                "clientsecret", "test-client-secret"));
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    assertTrue(authenticationServiceView.isTokenEndpointPresent());
  }

  @Test
  void testIsTokenEndpointPresent_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    assertFalse(authenticationServiceView.isTokenEndpointPresent());
  }

  @Test
  void testGetTokenEndpoint() {
    Map<String, Object> credentials =
        Map.of("authentication-service", Map.of("tokenendpoint", "http://example.com/token"));
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    Optional<String> tokenEndpoint = authenticationServiceView.getTokenEndpoint();
    assertTrue(tokenEndpoint.isPresent());
    assertEquals("http://example.com/token", tokenEndpoint.get());
  }

  @Test
  void testGetTokenEndpoint_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    Optional<String> tokenEndpoint = authenticationServiceView.getTokenEndpoint();
    assertFalse(tokenEndpoint.isPresent());
  }

  @Test
  void testGetClientId() {
    Map<String, Object> credentials =
        Map.of("authentication-service", Map.of("clientid", "test-client-id"));
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    Optional<String> clientId = authenticationServiceView.getClientId();
    assertTrue(clientId.isPresent());
    assertEquals("test-client-id", clientId.get());
  }

  @Test
  void testGetClientId_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    Optional<String> clientId = authenticationServiceView.getClientId();
    assertFalse(clientId.isPresent());
  }

  @Test
  void testGetClientSecret() {
    Map<String, Object> credentials =
        Map.of("authentication-service", Map.of("clientsecret", "test-client-secret"));
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    Optional<String> clientSecret = authenticationServiceView.getClientSecret();
    assertTrue(clientSecret.isPresent());
    assertEquals("test-client-secret", clientSecret.get());
  }

  @Test
  void testGetClientSecret_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    Optional<String> clientSecret = authenticationServiceView.getClientSecret();
    assertFalse(clientSecret.isPresent());
  }
}
