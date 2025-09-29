package com.sap.cds.feature.messaging.aem.client;

import com.google.common.base.Strings;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class AemValidationClient extends RestClient {

  public AemValidationClient(ServiceBinding binding) {
    super(ServiceBindingDestinationOptions.forService(binding).build());
  }

  public void validate(String managementUri, String subaccountId) throws IOException, URISyntaxException {
    URI uri = new URI(managementUri);
    Map<String, String> payloadMap = new HashMap<>(Map.of("hostName", uri.getHost()));

    if (!Strings.isNullOrEmpty(subaccountId)) {
      payloadMap.put("subaccountId", subaccountId);
    }

    String payload = this.mapper.writeValueAsString(payloadMap);

    // The response is not used, only the status code is relevant. If there is a status code not
    // equal to 200,
    // an exception is thrown which means that the validation failed.
    postRequest("", payload, Map.of());
  }
}
