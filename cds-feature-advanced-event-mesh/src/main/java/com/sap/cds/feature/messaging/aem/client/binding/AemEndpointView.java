package com.sap.cds.feature.messaging.aem.client.binding;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.sap.cds.feature.messaging.aem.service.AemMessagingServiceConfiguration;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * AemEndpointView is a class that provides access to the AMQP URI and URI of an
 * AEM endpoint
 * from a given ServiceBinding. It retrieves the endpoint information from the
 * service binding's
 * credentials.
 */
public class AemEndpointView {
	private static final String ENDPOINTS_KEY = "endpoints";
	private static final String AMQP_URI_KEY = "amqp_uri";
	private static final String URI_KEY = "uri";

	private final ServiceBinding binding;

	public AemEndpointView(ServiceBinding binding) {
		this.binding = binding;
	}

	/**
	 * Retrieves the URI from the AEM endpoint.
	 *
	 * @return an {@link Optional} containing the URI if present, otherwise an empty
	 *         {@link Optional}
	 */
	public Optional<String> getUri() {
		return Optional.ofNullable((String) getAemEndpoint().get(URI_KEY)).map(uri -> uri + "/SEMP/v2/config");
	}

	/**
	 * Retrieves the AMQP URI from the AEM endpoint.
	 *
	 * @return an {@link Optional} containing the AMQP URI if present, otherwise an
	 *         empty {@link Optional}.
	 */
	public Optional<String> getAmqpUri() {
		return Optional.ofNullable((String) getAemEndpoint().get(AMQP_URI_KEY));
	}

	/**
	 * Retrieves the VPN value from the binding credentials.
	 *
	 * @return an {@link Optional} containing the VPN value if present, otherwise an
	 *         empty {@link Optional}
	 */
	public Optional<String> getVpn() {
		return Optional.ofNullable((String) this.binding.getCredentials().get("vpn"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getEndpointsKey() {
		Map<String, Object> endpoints = (Map<String, Object>) binding.getCredentials().get(ENDPOINTS_KEY);

		return endpoints != null ? endpoints : Map.of();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getAemEndpoint() {
		Map<String, Object> endpoints = getEndpointsKey();
		Iterator<?> iterator = endpoints.values().iterator();
		Map<String, Object> aemEndpoint = iterator.hasNext() ? (Map<String, Object>) iterator.next() : null;

		if (endpoints.containsKey(AemMessagingServiceConfiguration.BINDING_AEM_LABEL) && aemEndpoint != null) {
			return aemEndpoint;
		} else {
			return Map.of();
		}
	}

}
