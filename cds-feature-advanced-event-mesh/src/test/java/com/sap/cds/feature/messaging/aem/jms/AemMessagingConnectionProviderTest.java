package com.sap.cds.feature.messaging.aem.jms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.services.ServiceException;
import org.junit.jupiter.api.Test;

public class AemMessagingConnectionProviderTest {

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
}
