package com.sap.cds.feature.messaging.aem.client.binding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import com.sap.cds.feature.messaging.aem.service.AemMessagingServiceConfiguration;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.utils.environment.ServiceBindingUtils;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultOAuth2PropertySupplier;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2PropertySupplier;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2ServiceBindingDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;
import com.sap.cloud.security.config.ClientIdentity;

public class AemValidationOAuth2PropertySupplier implements OAuth2PropertySupplier {

	private static boolean initialized = false;

	private final CredentialsView credentialsView;

	public static synchronized void initialize() {
		if (!initialized) {
			OAuth2ServiceBindingDestinationLoader.registerPropertySupplier(
					options -> ServiceBindingUtils.matches(options.getServiceBinding(), AemMessagingServiceConfiguration.BINDING_AEM_VALIDATION_LABEL),
					AemValidationOAuth2PropertySupplier::new);
			initialized = true;
		}
	}

	protected AemValidationOAuth2PropertySupplier(ServiceBindingDestinationOptions options) {
		this.credentialsView = new CredentialsView(options.getServiceBinding().getCredentials());
	}

	@Override
	public boolean isOAuth2Binding() {
		return this.credentialsView.getServiceUri().isPresent()
				&& this.credentialsView.getTokenUri().isPresent()
				&& this.credentialsView.getClientId().isPresent()
				&& this.credentialsView.getClientSecret().isPresent();
	}

	@Override
	public URI getServiceUri() {
		try {
			return new URI(this.credentialsView.getServiceUri().get());
		} catch (URISyntaxException e) {
			throw new ServiceException("Invalid AEM Validation Service URI.", e);
		}
	}

	@Override
	public URI getTokenUri() {
		String uri = this.credentialsView.getTokenUri().get();

		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new ServiceException("Invalid AEM Validation Service token endpoint URI.", e);
		}
	}

	@Override
	public ClientIdentity getClientIdentity() {
		return new AemClientIdentity(this.credentialsView.getClientId().get(), this.credentialsView.getClientSecret().get());
	}

	private record CredentialsView(Map<String, Object> credentials) {

		public Optional<String> getServiceUri() {
			return getHandshake().map(handshake -> (String) handshake.get("uri"));
		}

		public Optional<String> getTokenUri() {
			return getOAuth2().map(oa2 -> (String) oa2.get("tokenendpoint"));
		}

		public Optional<String> getClientId() {
			return getOAuth2().map(oa2 -> (String) oa2.get("clientid"));
		}

		public Optional<String> getClientSecret() {
			return getOAuth2().map(oa2 -> (String) oa2.get("clientsecret"));
		}

		@SuppressWarnings("unchecked")
		private Optional<Map<String, Object>> getHandshake() {
			return Optional.ofNullable((Map<String, Object>) credentials.get("handshake"));
		}

		@SuppressWarnings("unchecked")
		private Optional<Map<String, Object>> getOAuth2() {
			return getHandshake().map(handshake -> (Map<String, Object>) handshake.get("oa2"));
		}

	}
}
