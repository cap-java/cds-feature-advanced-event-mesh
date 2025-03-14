package com.sap.cds.feature.messaging.aem.client.binding;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.security.xsuaa.http.HttpHeaders;
import com.sap.cloud.security.xsuaa.http.MediaType;

public class AemTokenFetchClient {

	private static final Logger logger = LoggerFactory.getLogger(AemTokenFetchClient.class);

	private final AemAuthorizationServiceView authorizationServiceView;
	private final ObjectMapper mapper = new ObjectMapper();

	public AemTokenFetchClient(AemAuthorizationServiceView authorizationServiceView) {
		this.authorizationServiceView = authorizationServiceView;
	}

	public Optional<String> fetchToken() throws IOException {
		String requestBody = String.format(
				"grant_type=client_credentials&client_id=%s&client_secret=%s",
				URLEncoder.encode(this.authorizationServiceView.getClientId().get(), StandardCharsets.UTF_8),
				URLEncoder.encode(this.authorizationServiceView.getClientSecret().get(), StandardCharsets.UTF_8));

		HttpPost post = new HttpPost(this.authorizationServiceView.getTokenEndpoint().get());
		post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED.value());
		post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.value());

		post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

		try(CloseableHttpResponse response = handleRequest(post)) {
			JsonNode jsonNode =  handleJsonResponse(response);
			JsonNode accessTokenNode = jsonNode.get("access_token");

			return Optional.ofNullable(accessTokenNode).map(JsonNode::asText);
		} finally {
			post.releaseConnection();
		}
	}

	private CloseableHttpResponse handleRequest(HttpRequestBase request) throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			return httpClient.execute(request);
		} catch (ClientProtocolException e)	{
			throw new ServiceException(e.getMessage(), e);
		}
	}

	private JsonNode handleJsonResponse(CloseableHttpResponse response) throws IOException {
		try (CloseableHttpResponse resp = response){
			int code = resp.getStatusLine().getStatusCode();

			logger.debug("Responded with status code '{}'", code);

			if (code >= 200 && code <= 207) {
				String contentType = MediaType.APPLICATION_JSON.value();

				if (resp.getEntity() != null) {
					if (resp.getEntity().getContentType() != null) {
						contentType = resp.getEntity().getContentType().getValue();
					}
					if (contentType.startsWith(MediaType.APPLICATION_JSON.value())) {
						String jsonData = EntityUtils.toString(resp.getEntity());
						return mapper.readValue(jsonData, JsonNode.class);
					} else {
						throw new IOException("Unexpected response format: Expected JSON but found '" + contentType + "'");
					}
				} else {
					return mapper.readValue("{}", JsonNode.class);
				}
			} else {
				String reason = resp.getStatusLine().getReasonPhrase();
				throw new ServiceException("Unexpected request HTTP response (" + code + ") " + reason);
			}
		}
	}

}
