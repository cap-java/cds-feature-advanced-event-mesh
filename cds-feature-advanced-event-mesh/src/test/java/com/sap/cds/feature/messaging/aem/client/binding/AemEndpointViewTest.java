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

public class AemEndpointViewTest {

  @Mock private ServiceBinding serviceBinding;

  private AemEndpointView endpointView;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    endpointView = new AemEndpointView(serviceBinding);
  }

  @Test
  void testGetUri() {
    Map<String, Object> credentials =
        Map.of("endpoints", Map.of("advanced-event-mesh", Map.of("uri", "http://example.com")));
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    Optional<String> uri = endpointView.getUri();
    assertTrue(uri.isPresent());
    assertEquals("http://example.com", uri.get());
  }

  @Test
  void testGetUri_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    Optional<String> uri = endpointView.getUri();
    assertFalse(uri.isPresent());
  }

  @Test
  void testGetAmqpUri() {
    Map<String, Object> credentials =
        Map.of(
            "endpoints", Map.of("advanced-event-mesh", Map.of("amqp_uri", "amqp://example.com")));
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    Optional<String> amqpUri = endpointView.getAmqpUri();
    assertTrue(amqpUri.isPresent());
    assertEquals("amqp://example.com", amqpUri.get());
  }

  @Test
  void testGetAmqpUri_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    Optional<String> amqpUri = endpointView.getAmqpUri();
    assertFalse(amqpUri.isPresent());
  }

  @Test
  void testGetVpn() {
    Map<String, Object> credentials = Map.of("vpn", "test-vpn");
    when(serviceBinding.getCredentials()).thenReturn(credentials);

    Optional<String> vpn = endpointView.getVpn();
    assertTrue(vpn.isPresent());
    assertEquals("test-vpn", vpn.get());
  }

  @Test
  void testGetVpn_NotPresent() {
    when(serviceBinding.getCredentials()).thenReturn(Map.of());

    Optional<String> vpn = endpointView.getVpn();
    assertFalse(vpn.isPresent());
  }
}
