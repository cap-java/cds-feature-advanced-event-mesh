package com.sap.cds.feature.messaging.aem.jms;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.messaging.aem.client.binding.AemAuthorizationServiceView;
import com.sap.cds.feature.messaging.aem.client.binding.AemEndpointView;
import com.sap.cds.feature.messaging.aem.client.binding.AemTokenFetchClient;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.messaging.jms.BrokerConnection;
import com.sap.cds.services.messaging.jms.BrokerConnectionProvider;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import jakarta.jms.Connection;

public class AemMessagingConnectionProvider extends BrokerConnectionProvider {

	private static final Logger logger = LoggerFactory.getLogger(AemMessagingConnectionProvider.class);

	private static final String AEM_TOKEN_FETCH_DESTINATION = "x-cap-aem-token-fetch-destination";
	private static final String SASL_MECHANISM_URI_PARAMETER = "/?amqp.saslMechanisms=XOAUTH2";

	private final ServiceBinding binding;
	private final AemEndpointView endpointView;
	private final AemTokenFetchClient tokenFetchClient;

	public AemMessagingConnectionProvider(ServiceBinding binding) {
		super(binding.getName().get());
		this.binding = binding;
		this.endpointView = new AemEndpointView(binding);
		this.tokenFetchClient = new AemTokenFetchClient(new AemAuthorizationServiceView(binding));
	}

	@Override
	protected BrokerConnection createBrokerConnection(String name, Map<String, String> clientProperties) throws Exception {
		// see https://solace.community/discussion/1677/how-oauth-can-be-used-with-apache-qpid-jms-2-0-amqp
		logger.debug("Retrieving credentials for Basic Auth from	service binding '{}'", binding.getName().get());

		String amqpUri = this.endpointView.getAmqpUriKey().orElseThrow(() -> new ServiceException(
				"AMQP URI key is missing in the service binding. Please check the service binding configuration."));
		amqpUri = amqpUri + SASL_MECHANISM_URI_PARAMETER;

		final BiFunction<Connection, URI, Object> tokenExtension = new BiFunction<>() {
			@Override
			public Object apply(final Connection connection, final URI uri) {
				try {
					String token = tokenFetchClient.fetchToken().orElseThrow(() -> new ServiceException("Token is missing"));

					return token;
				} catch (IOException e) {
					throw new ServiceException(e);
				}
			}
		};

		logger.debug("Creating connection factory fo service binding '{}'", this.binding.getName().get());
		// the password is going to be replaced by the token
		JmsConnectionFactory factory = new JmsConnectionFactory(this.endpointView.getVpn().get(), "token", amqpUri);

		factory.setExtension(JmsConnectionExtensions.PASSWORD_OVERRIDE.toString(), tokenExtension);

		return new BrokerConnection(name, factory);
	}

}
