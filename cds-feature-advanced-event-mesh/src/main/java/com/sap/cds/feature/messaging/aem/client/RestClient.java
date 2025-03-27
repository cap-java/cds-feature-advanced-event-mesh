package com.sap.cds.feature.messaging.aem.client;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;

class RestClient {

	private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

	private final HttpDestination destination;

	protected final ObjectMapper mapper = new ObjectMapper();

	RestClient(ServiceBindingDestinationOptions bindingDestinationOptions) {
		this.destination = ServiceBindingDestinationLoader.defaultLoaderChain().getDestination(bindingDestinationOptions);
	}

	public JsonNode getRequest(String path) throws IOException {
		HttpGet get = new HttpGet(path);
		try {
			return handleJsonResponse(handleRequest(get));
		} finally {
			get.releaseConnection();
		}
	}

	public JsonNode postRequest(String path, Map<String, Object>  data) throws IOException {
		String strData = mapper.writer().writeValueAsString(data);
		return postRequest(path, strData, null);
	}

	public JsonNode postRequest(String path, String data, Map<String, Object> headers) throws IOException {
		HttpPost post = new HttpPost(path);

		if (headers != null) {
			headers.forEach((k, v) -> post.setHeader(k, v.toString()));
		}

		post.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
		post.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON.toString());

		if (data != null) {
			post.setEntity(new StringEntity(data, APPLICATION_JSON));
		}
		try {
			return handleJsonResponse(handleRequest(post));
		} finally {
			post.releaseConnection();
		}
	}

	public JsonNode deleteRequest(String path) throws IOException {
		HttpDelete del = new HttpDelete(path);
		try {
			return handleJsonResponse(handleRequest(del));
		} finally {
			del.releaseConnection();
		}
	}

	private CloseableHttpResponse handleRequest(HttpRequestBase request) throws IOException {
		HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);
		return (CloseableHttpResponse) httpClient.execute(request);
	}

	private JsonNode handleJsonResponse(CloseableHttpResponse response) throws IOException {
		try (CloseableHttpResponse resp = response) {
			int code = resp.getStatusLine().getStatusCode();

			logger.debug("Responded with status code '{}'", code);

			if (code >= 200 && code <= 207) {
				String contentType = APPLICATION_JSON.toString();

				if (resp.getEntity() != null) {
					if (resp.getEntity().getContentType() != null) {
						contentType = resp.getEntity().getContentType().getValue();
					}
					if (contentType.startsWith(APPLICATION_JSON.toString())) {
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
