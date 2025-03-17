package com.sap.cds.feature.messaging.aem.jms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cds.feature.messaging.aem.client.binding.AemAuthorizationServiceView;
import com.sap.cds.feature.messaging.aem.client.binding.AemEndpointView;
import com.sap.cds.feature.messaging.aem.client.binding.AemTokenFetchClient;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.messaging.jms.BrokerConnection;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import jakarta.jms.Connection;

public class AemMessagingConnectionProviderTest {

	@Mock
	private ServiceBinding serviceBinding;

	@Mock
	private AemEndpointView endpointView;

	@Mock
	private AemTokenFetchClient tokenFetchClient;

	private AemMessagingConnectionProvider connectionProvider;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(serviceBinding.getName()).thenReturn(Optional.of("test-binding"));

		connectionProvider = new AemMessagingConnectionProvider(serviceBinding);
	}

	@Test
	void testCreateBrokerConnection_Success() throws Exception {
		when(endpointView.getAmqpUriKey()).thenReturn(Optional.of("amqp://example.com"));
		when(endpointView.getVpn()).thenReturn(Optional.of("test-vpn"));
		when(serviceBinding.getCredentials()).thenReturn(
				Map.of("endpoints", Map.of("advanced-event-mesh", Map.of("amqp_uri", "amqp://example.com")),
						"vpn", "test-vpn")
		);
		when(tokenFetchClient.fetchToken()).thenReturn(Optional.of("test-token"));

		BrokerConnection connection = connectionProvider.createBrokerConnection("test-connection", Map.of());

		assertNotNull(connection);
		assertEquals("test-connection", connection.getName());
	}

	@Test
	void testCreateBrokerConnection_MissingAmqpUri() {
		when(endpointView.getAmqpUriKey()).thenReturn(Optional.empty());

		ServiceException exception = assertThrows(ServiceException.class, () -> {
			connectionProvider.createBrokerConnection("test-connection", Map.of());
		});

		assertEquals("AMQP URI key is missing in the service binding. Please check the service binding configuration.", exception.getMessage());
	}

	@Test
	void testCreateBrokerConnection_MissingToken() throws IOException {
		when(endpointView.getAmqpUriKey()).thenReturn(Optional.of("amqp://example.com"));
		when(endpointView.getVpn()).thenReturn(Optional.of("test-vpn"));
		when(tokenFetchClient.fetchToken()).thenReturn(Optional.empty());

		ServiceException exception = assertThrows(ServiceException.class, () -> {
			connectionProvider.createBrokerConnection("test-connection", Map.of());
		});

		assertEquals("AMQP URI key is missing in the service binding. Please check the service binding configuration.", exception.getMessage());
	}

	@Test
	void testCreateBrokerConnection_TokenFetch() throws IOException {
		when(endpointView.getAmqpUriKey()).thenReturn(Optional.of("amqp://example.com"));
		when(endpointView.getVpn()).thenReturn(Optional.of("test-vpn"));
		when(serviceBinding.getCredentials()).thenReturn(
				Map.of("endpoints", Map.of("advanced-event-mesh", Map.of("amqp_uri", "amqp://example.com")),
						"vpn", "test-vpn")
		);
		when(tokenFetchClient.fetchToken()).thenThrow(new IOException("IO error"));

		assertDoesNotThrow(() -> {
			connectionProvider.createBrokerConnection("test-connection", Map.of());
		});
	}
}
