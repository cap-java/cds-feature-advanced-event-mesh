package com.sap.cds.feature.messaging.aem.jms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Header;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AemMessagingConnectionProviderTest {

  @Mock private ServiceBinding binding;
  @Mock private Destination destination;
  @Mock private HttpDestination httpDestination;

  private AemMessagingConnectionProvider provider;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(binding.getName()).thenReturn(Optional.of("my-aem-binding"));
    when(destination.asHttp()).thenReturn(httpDestination);

    provider = new AemMessagingConnectionProvider(binding, destination);
  }

  // --- getToken ---

  @Test
  void getToken_extracts_token_from_bearer_header() {
    String token = provider.getToken("Bearer my-jwt-token");
    assertEquals("my-jwt-token", token);
  }

  @Test
  void getToken_returns_null_for_null_input() {
    assertNull(provider.getToken(null));
  }

  @Test
  void getToken_returns_null_when_no_space_in_value() {
    assertNull(provider.getToken("BearerWithoutSpace"));
  }

  @Test
  void getToken_returns_null_for_empty_string() {
    assertNull(provider.getToken(""));
  }

  // --- fetchToken ---

  @Test
  void fetchToken_extracts_bearer_token_from_authorization_header() {
    when(httpDestination.getHeaders())
        .thenReturn(List.of(new Header("Authorization", "Bearer jwt-abc-123")));

    Optional<String> token = provider.fetchToken();

    assertTrue(token.isPresent());
    assertEquals("jwt-abc-123", token.get());
  }

  @Test
  void fetchToken_returns_empty_when_no_authorization_header() {
    when(httpDestination.getHeaders()).thenReturn(List.of(new Header("X-Other", "value")));

    Optional<String> token = provider.fetchToken();

    assertFalse(token.isPresent());
  }

  @Test
  void fetchToken_returns_empty_when_no_headers_at_all() {
    when(httpDestination.getHeaders()).thenReturn(List.of());

    Optional<String> token = provider.fetchToken();

    assertFalse(token.isPresent());
  }

  // --- validateAmqpUri ---

  @Test
  void validateAmqpUri_acceptsAmqpsWithMatchingHost() {
    assertDoesNotThrow(
        () ->
            AemMessagingConnectionProvider.validateAmqpUri(
                "amqps://broker.example.com:5671",
                "https://broker.example.com:943",
                "my-binding"));
  }

  @Test
  void validateAmqpUri_acceptsAmqpsWhenManagementUriIsNull() {
    assertDoesNotThrow(
        () ->
            AemMessagingConnectionProvider.validateAmqpUri(
                "amqps://broker.example.com:5671", null, "my-binding"));
  }

  @Test
  void validateAmqpUri_isCaseInsensitiveOnScheme() {
    assertDoesNotThrow(
        () ->
            AemMessagingConnectionProvider.validateAmqpUri(
                "AMQPS://broker.example.com:5671", null, "my-binding"));
  }

  @Test
  void validateAmqpUri_rejectsPlainAmqp() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AemMessagingConnectionProvider.validateAmqpUri(
                    "amqp://broker.example.com:5672", null, "my-binding"));
    assertTrue(ex.getMessage().contains("amqps"), ex.getMessage());
    assertTrue(ex.getMessage().contains("my-binding"), ex.getMessage());
  }

  @Test
  void validateAmqpUri_rejectsUnexpectedScheme() {
    assertThrows(
        ServiceException.class,
        () ->
            AemMessagingConnectionProvider.validateAmqpUri(
                "ws://broker.example.com", null, "my-binding"));
  }

  @Test
  void validateAmqpUri_rejectsMalformedUri() {
    assertThrows(
        ServiceException.class,
        () ->
            AemMessagingConnectionProvider.validateAmqpUri(
                "::not a uri::", null, "my-binding"));
  }

  @Test
  void validateAmqpUri_rejectsHostMismatchAgainstManagementUri() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AemMessagingConnectionProvider.validateAmqpUri(
                    "amqps://attacker.example.com:5671",
                    "https://broker.example.com:943",
                    "my-binding"));
    assertTrue(ex.getMessage().contains("attacker.example.com"), ex.getMessage());
    assertTrue(ex.getMessage().contains("broker.example.com"), ex.getMessage());
  }

  @Test
  void validateAmqpUri_toleratesMalformedManagementUri() {
    assertDoesNotThrow(
        () ->
            AemMessagingConnectionProvider.validateAmqpUri(
                "amqps://broker.example.com:5671", "::not a uri::", "my-binding"));
  }

  @Test
  void validateAmqpUri_rejectsAmqpUriWithoutHost() {
    // Opaque URI parses successfully but has no host component.
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AemMessagingConnectionProvider.validateAmqpUri(
                    "amqps:opaque-body", null, "my-binding"));
    assertTrue(ex.getMessage().contains("my-binding"), ex.getMessage());
    assertTrue(ex.getMessage().contains("host"), ex.getMessage());
  }

  @Test
  void validateAmqpUri_rejectsManagementUriWithoutHost() {
    // Management URI parses but yields a null host — must not silently pass the check.
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AemMessagingConnectionProvider.validateAmqpUri(
                    "amqps://broker.example.com:5671", "https:opaque-body", "my-binding"));
    assertTrue(ex.getMessage().contains("my-binding"), ex.getMessage());
    assertTrue(ex.getMessage().contains("host"), ex.getMessage());
  }
}
