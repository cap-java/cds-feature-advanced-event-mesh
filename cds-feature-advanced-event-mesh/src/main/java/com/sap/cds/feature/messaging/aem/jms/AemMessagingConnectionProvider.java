package com.sap.cds.feature.messaging.aem.jms;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.http.HttpHeaders;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.messaging.aem.client.binding.AemEndpointView;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.messaging.jms.BrokerConnection;
import com.sap.cds.services.messaging.jms.BrokerConnectionProvider;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationProperty;
import com.sap.cloud.sdk.cloudplatform.connectivity.OnBehalfOf;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;

import jakarta.jms.Connection;

public class AemMessagingConnectionProvider extends BrokerConnectionProvider {

	private static final Logger logger = LoggerFactory.getLogger(AemMessagingConnectionProvider.class);

	private static final String SASL_MECHANISM_URI_PARAMETER = "/?amqp.saslMechanisms=XOAUTH2";

	private final ServiceBinding binding;
	private final Destination destination;

	public AemMessagingConnectionProvider(ServiceBinding binding) {
		super(binding.getName().get());
		this.binding = binding;

		AemEndpointView endpointView = new AemEndpointView(binding);
		String amqpUri = endpointView.getAmqpUri().orElseThrow(() -> new ServiceException(
				"AMQP URI key is missing in the service binding. Please check the service binding configuration."));
		amqpUri = amqpUri + SASL_MECHANISM_URI_PARAMETER;

		ServiceBindingDestinationOptions options = ServiceBindingDestinationOptions.forService(binding).
				onBehalfOf(OnBehalfOf.TECHNICAL_USER_CURRENT_TENANT).build();

		this.destination = DefaultHttpDestination.fromDestination(
						ServiceBindingDestinationLoader.defaultLoaderChain().getDestination(options))
				.uri(amqpUri)
				.property("vpn", endpointView.getVpn().get())
				.build();
	}

	@Override
	protected BrokerConnection createBrokerConnection(String name, Map<String, String> clientProperties) throws Exception {
		// see https://solace.community/discussion/1677/how-oauth-can-be-used-with-apache-qpid-jms-2-0-amqp
		logger.debug("Retrieving credentials for Basic Auth from	service binding '{}'", binding.getName().get());

		final BiFunction<Connection, URI, Object> tokenExtension = new BiFunction<>() {
			@Override
			public Object apply(final Connection connection, final URI uri) {
				String token = fetchToken().orElseThrow(() -> new ServiceException("Token is missing"));

				return token;
			}
		};

		logger.debug("Creating connection factory fo service binding '{}'", this.binding.getName().get());
		// the password is going to be replaced by the token
		JmsConnectionFactory factory = new JmsConnectionFactory(destination.get("vpn", String.class).get(), "token", destination.get(DestinationProperty.URI).get());

		factory.setExtension(JmsConnectionExtensions.PASSWORD_OVERRIDE.toString(), tokenExtension);

		return new BrokerConnection(name, factory);
	}

	private Optional<String> fetchToken() {
		Optional<String> token = this.destination.asHttp()
				.getHeaders()
				.stream()
				.filter(h -> h.getName().equals(HttpHeaders.AUTHORIZATION))
				.findFirst()
				.map(h -> getToken(h.getValue()));
		return token;
	}

	private String getToken(String value) {
		String token = null;
		if (value != null) {
			String[] parts = value.split(" ");
			token = parts.length > 1 ? parts[1] : null;
		}

		return token;
	}

}
