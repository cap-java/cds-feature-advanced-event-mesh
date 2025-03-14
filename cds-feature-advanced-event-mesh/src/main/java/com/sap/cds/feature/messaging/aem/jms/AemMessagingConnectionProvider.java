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
import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.exception.DestinationAccessException;
import com.sap.cloud.sdk.cloudplatform.connectivity.exception.DestinationNotFoundException;

import jakarta.jms.Connection;

public class AemMessagingConnectionProvider extends BrokerConnectionProvider {

	private static final Logger logger = LoggerFactory.getLogger(AemMessagingConnectionProvider.class);

	private static final String AEM_TOKEN_FETCH_DESTINATION = "x-cap-aem-token-fetch-destination";
	private static final String SASL_MECHANISM_URI_PARAMETER = "/?amqp.saslMechanisms=XOAUTH2";

	private final ServiceBinding binding;
	private final AemAuthorizationServiceView authorizationServiceView;
	private final AemEndpointView endpointView;
	private AemTokenFetchClient tokenFetchClient;

	public AemMessagingConnectionProvider(ServiceBinding binding) {
		super(binding.getName().get());
		this.binding = binding;
		this.authorizationServiceView = new AemAuthorizationServiceView(binding);
		this.endpointView = new AemEndpointView(binding);

		if (this.authorizationServiceView.isTokenEndpointPresent()) {
			this.registerTokenFetchDestination();
			this.tokenFetchClient = new AemTokenFetchClient(AEM_TOKEN_FETCH_DESTINATION);
		}
	}

	@Override
	protected BrokerConnection createBrokerConnection(String name, Map<String, String> clientProperties) throws Exception {
		// see https://solace.community/discussion/1677/how-oauth-can-be-used-with-apache-qpid-jms-2-0-amqp
		logger.debug("Retrieving credentials for Basic Auth from	service binding '{}'", binding.getName().get());

		String amqpUri = this.endpointView.getAmqpUriKey().orElseThrow(() -> new ServiceException(
				"AMQP URI key is missing in the service binding. Please check the service binding configuration."));
		amqpUri = amqpUri + SASL_MECHANISM_URI_PARAMETER;

		// TODO: What if IAS is used?
		final BiFunction<Connection, URI, Object> tokenExtension = new BiFunction<>() {
			private volatile String token = null;
			private long currentTokenExpirationTime; // 10 minutes

			@Override
			public Object apply(final Connection connection, final URI uri) {
				long currentTime = System.currentTimeMillis();

				if (currentTime > currentTokenExpirationTime) {
					try {
						this.token = tokenFetchClient.fetchToken().orElseThrow(() -> new ServiceException("Token is missing"));
						this.currentTokenExpirationTime = currentTime + 10 * 60 * 1000; // 10 minutes
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				return this.token;
			}
		};

		logger.debug("Creating connection factory fo service binding '{}'",
				this.binding.getName().get());
		JmsConnectionFactory factory = new JmsConnectionFactory(this.endpointView.getVpn().get(), "token", amqpUri);
		factory.setExtension(JmsConnectionExtensions.PASSWORD_OVERRIDE.toString(), tokenExtension);

		return new BrokerConnection(name, factory);
	}

	private void registerTokenFetchDestination() {
		if (!destinationExists()) {
			HttpDestination destination = DefaultHttpDestination
					.builder(this.authorizationServiceView.getTokenEndpoint().get())
					.name(AEM_TOKEN_FETCH_DESTINATION)
					// TODO remove the commented code if it working without
					//.authenticationType(AuthenticationType.NO_AUTHENTICATION)
					.authenticationType(AuthenticationType.BASIC_AUTHENTICATION)
					.basicCredentials(this.authorizationServiceView.getClientId().get(), this.authorizationServiceView.getClientSecret().get())
					.build();
			DefaultDestinationLoader loader = new DefaultDestinationLoader().registerDestination(destination);

			DestinationAccessor.appendDestinationLoader(loader);
		}
	}

	private boolean destinationExists() {
		try {
			DestinationAccessor.getDestination(AEM_TOKEN_FETCH_DESTINATION);
			return true;
		} catch (DestinationAccessException | DestinationNotFoundException e) {
			// Empty by design
		}

		return false;
	}

}
