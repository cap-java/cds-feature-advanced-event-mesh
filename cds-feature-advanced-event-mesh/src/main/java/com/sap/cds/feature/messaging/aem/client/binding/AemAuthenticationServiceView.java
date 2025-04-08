package com.sap.cds.feature.messaging.aem.client.binding;

import java.util.Map;
import java.util.Optional;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * AemAuthenticationServiceView provides a view of the authentication service
 * credentials from a given ServiceBinding.
 */
public class AemAuthenticationServiceView {
	private static final String CLIENTSECRET_KEY = "clientsecret";
	private static final String CLIENTID_KEY = "clientid";
	private static final String TOKENENDPOINT_KEY = "tokenendpoint";
	private static final String AUTHENTICATION_SERVICE_KEY = "authentication-service";

	private final ServiceBinding binding;

	public AemAuthenticationServiceView(ServiceBinding binding) {
		this.binding = binding;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getAuthenticationService() {
		if (binding.getCredentials().containsKey(AUTHENTICATION_SERVICE_KEY)) {
			return (Map<String, Object>) binding.getCredentials().get(AUTHENTICATION_SERVICE_KEY);
		} else {
			return Map.of();
		}
	}

	/**
	 * Checks if the token endpoint, client ID, and client secret are present.
	 *
	 * @return {@code true} if the token endpoint, client ID, and client secret are
	 *         present; {@code false} otherwise.
	 */
	public boolean isTokenEndpointPresent() {
		return getTokenEndpoint().isPresent() && getClientId().isPresent() && getClientSecret().isPresent();
	}

	/**
	 * Retrieves the token endpoint URL from the authorization service.
	 *
	 * @return an {@link Optional} containing the token endpoint URL if present,
	 *         otherwise an empty {@link Optional}.
	 */
	public Optional<String> getTokenEndpoint() {
		return Optional.ofNullable((String) this.getAuthenticationService().get(TOKENENDPOINT_KEY));
	}

	/**
	 * Retrieves the client ID from the authorization service.
	 *
	 * @return an {@link Optional} containing the client ID if present, otherwise an
	 *         empty {@link Optional}
	 */
	public Optional<String> getClientId() {
		return Optional.ofNullable((String) this.getAuthenticationService().get(CLIENTID_KEY));
	}

	/**
	 * Retrieves the client secret from the authorization service.
	 *
	 * @return an {@link Optional} containing the client secret if present,
	 *         otherwise an empty {@link Optional}
	 */
	public Optional<String> getClientSecret() {
		return Optional.ofNullable((String) this.getAuthenticationService().get(CLIENTSECRET_KEY));
	}
}
