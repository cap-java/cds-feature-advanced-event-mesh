package com.sap.cds.feature.messaging.aem.jms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Header;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AemMessagingConnectionProviderTest {

    @Mock private ServiceBinding binding;
    @Mock private Destination destination;
    @Mock private HttpDestination httpDestination;

    private AemMessagingConnectionProvider provider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(binding.getName()).thenReturn(Optional.of("my-aem-binding"));
        when(destination.asHttp()).thenReturn(httpDestination);

        provider = new AemMessagingConnectionProvider(binding, destination);
    }

    // --- getToken ---

    @Test
    void getToken_extracts_token_from_bearer_header() {
        String token = provider.getToken("Bearer my-jwt-token");
        assertEquals("my-jwt-token", token);
    }

    @Test
    void getToken_returns_null_for_null_input() {
        assertNull(provider.getToken(null));
    }

    @Test
    void getToken_returns_null_when_no_space_in_value() {
        assertNull(provider.getToken("BearerWithoutSpace"));
    }

    @Test
    void getToken_returns_null_for_empty_string() {
        assertNull(provider.getToken(""));
    }

    // --- fetchToken ---

    @Test
    void fetchToken_extracts_bearer_token_from_authorization_header() {
        when(httpDestination.getHeaders()).thenReturn(
                List.of(new Header("Authorization", "Bearer jwt-abc-123")));

        Optional<String> token = provider.fetchToken();

        assertTrue(token.isPresent());
        assertEquals("jwt-abc-123", token.get());
    }

    @Test
    void fetchToken_returns_empty_when_no_authorization_header() {
        when(httpDestination.getHeaders()).thenReturn(List.of(new Header("X-Other", "value")));

        Optional<String> token = provider.fetchToken();

        assertFalse(token.isPresent());
    }

    @Test
    void fetchToken_returns_empty_when_no_headers_at_all() {
        when(httpDestination.getHeaders()).thenReturn(List.of());

        Optional<String> token = provider.fetchToken();

        assertFalse(token.isPresent());
    }
}
