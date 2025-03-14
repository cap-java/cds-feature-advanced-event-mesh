package com.sap.cds.feature.messaging.aem.client.binding;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cds.integration.cloudsdk.rest.client.JsonRestClient;
import com.sap.cloud.security.xsuaa.http.HttpHeaders;
import com.sap.cloud.security.xsuaa.http.MediaType;

public class AemTokenFetchClient extends JsonRestClient {

	public AemTokenFetchClient(String destinationName) {
		super(destinationName, "");
	}

	public Optional<String> fetchToken() throws IOException {
		//JsonNode response = this.getRequest("?grant_type=client_credentials");
		String requestBody = String.format(
				"grant_type=client_credentials&client_id=%s&client_secret=%s",
				URLEncoder.encode("a5fe563c-76a3-4507-b107-63f2a112ee4a", StandardCharsets.UTF_8),
				URLEncoder.encode("ch80Nj]-L[vw[EjZyPB1D-:iRW._:Mm0=", StandardCharsets.UTF_8));
		JsonNode response = this.postRequest(
				"",
				requestBody,
				Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED.value(),
						HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.value()));
		JsonNode accessTokenNode = response.get("access_token");

		return Optional.ofNullable(accessTokenNode).map(JsonNode::asText);
	}

	@Override
	protected void setHeaders(HttpRequest request) {
		super.setHeaders(request);

		request.removeHeaders(CONTENT_TYPE);
		request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED.value());
	}
}
