package com.sap.cds.feature.messaging.aem.client.binding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.sap.cds.services.ServiceException;

public class AemTokenFetchClientTest {

	@Mock
	private AemAuthorizationServiceView authorizationServiceView;

	@Mock
	private CloseableHttpClient httpClient;

	@Mock
	private CloseableHttpResponse response;

	@Mock
	private StatusLine statusLine;

	private AemTokenFetchClient tokenFetchClient;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		tokenFetchClient = new AemTokenFetchClient(authorizationServiceView);
	}

	@Test
	void testFetchToken_Failure() throws IOException {
		when(authorizationServiceView.getClientId()).thenReturn(Optional.of("test-client-id"));
		when(authorizationServiceView.getClientSecret()).thenReturn(Optional.of("test-client-secret"));
		when(authorizationServiceView.getTokenEndpoint()).thenReturn(Optional.of("http://example.com/token"));

		when(this.statusLine.getStatusCode()).thenReturn(400);
		when(response.getStatusLine()).thenReturn(this.statusLine);

		when(response.getStatusLine().getStatusCode()).thenReturn(400);
		when(response.getStatusLine().getReasonPhrase()).thenReturn("Bad Request");
		when(httpClient.execute(any())).thenReturn(response);

		assertThrows(ServiceException.class, () -> tokenFetchClient.fetchToken());
	}

	@Test
	void testFetchToken_InvalidJsonResponse() throws IOException {
		when(authorizationServiceView.getClientId()).thenReturn(Optional.of("test-client-id"));
		when(authorizationServiceView.getClientSecret()).thenReturn(Optional.of("test-client-secret"));
		when(authorizationServiceView.getTokenEndpoint()).thenReturn(Optional.of("http://example.com/token"));

		String invalidJsonResponse = "Invalid JSON";
		when(this.statusLine.getStatusCode()).thenReturn(200);
		when(response.getStatusLine()).thenReturn(this.statusLine);

		when(response.getEntity()).thenReturn(new StringEntity(invalidJsonResponse));
		when(httpClient.execute(any())).thenReturn(response);

		assertThrows(ServiceException.class, () -> tokenFetchClient.fetchToken());
	}
}
