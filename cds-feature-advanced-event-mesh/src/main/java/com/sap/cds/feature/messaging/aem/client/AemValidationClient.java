package com.sap.cds.feature.messaging.aem.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;

public class AemValidationClient extends RestClient {

	public AemValidationClient(ServiceBinding binding) {
		super(ServiceBindingDestinationOptions.forService(binding).build());
	}

	public void validate(String managementUri) throws IOException, URISyntaxException {
		URI uri = new URI(managementUri);
		String payload = this.mapper.writeValueAsString(Map.of("hostName", uri.getHost()));

		// The response is not used, only the status code is relevant. If there is a status code not equal to 200,
		// an exception is thrown which means that the validation failed.
		postRequest("", payload, Map.of());
	}
}
