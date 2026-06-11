package com.sap.cds.feature.messaging.aem.client.binding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AemValidationOAuth2PropertySupplierTest {

    @Mock private ServiceBinding binding;

    private ServiceBindingDestinationOptions options;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(binding.getName()).thenReturn(java.util.Optional.of("aem-validation-service"));
        when(binding.getTags()).thenReturn(java.util.List.of("aem-validation-service"));
        options = ServiceBindingDestinationOptions.forService(binding).build();
    }

    private Map<String, Object> fullCredentials() {
        return Map.of("handshake", Map.of(
                "uri", "https://handshake.example.com/handshake",
                "oa2", Map.of(
                        "tokenendpoint", "https://token.example.com/oauth/token",
                        "clientid", "my-client-id",
                        "clientsecret", "my-client-secret")));
    }

    // --- isOAuth2Binding ---

    @Test
    void isOAuth2Binding_returns_true_when_all_credentials_present() {
        when(binding.getCredentials()).thenReturn(fullCredentials());
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        assertTrue(supplier.isOAuth2Binding());
    }

    @Test
    void isOAuth2Binding_returns_false_when_handshake_key_missing() {
        when(binding.getCredentials()).thenReturn(Map.of());
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        assertFalse(supplier.isOAuth2Binding());
    }

    @Test
    void isOAuth2Binding_returns_false_when_oa2_key_missing() {
        when(binding.getCredentials()).thenReturn(
                Map.of("handshake", Map.of("uri", "https://handshake.example.com/handshake")));
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        assertFalse(supplier.isOAuth2Binding());
    }

    @Test
    void isOAuth2Binding_returns_false_when_token_endpoint_missing() {
        when(binding.getCredentials()).thenReturn(Map.of("handshake", Map.of(
                "uri", "https://handshake.example.com/handshake",
                "oa2", Map.of("clientid", "id", "clientsecret", "secret"))));
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        assertFalse(supplier.isOAuth2Binding());
    }

    // --- getServiceUri ---

    @Test
    void getServiceUri_returns_correct_uri() throws Exception {
        when(binding.getCredentials()).thenReturn(fullCredentials());
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        URI uri = supplier.getServiceUri();

        assertEquals("https://handshake.example.com/handshake", uri.toString());
    }

    @Test
    void getServiceUri_throws_service_exception_for_invalid_uri() {
        when(binding.getCredentials()).thenReturn(Map.of("handshake", Map.of(
                "uri", "not a valid %%uri",
                "oa2", Map.of("tokenendpoint", "https://t.e.c/t", "clientid", "id", "clientsecret", "s"))));
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        assertThrows(ServiceException.class, supplier::getServiceUri);
    }

    // --- getTokenUri ---

    @Test
    void getTokenUri_returns_correct_uri() throws Exception {
        when(binding.getCredentials()).thenReturn(fullCredentials());
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        URI uri = supplier.getTokenUri();

        assertEquals("https://token.example.com/oauth/token", uri.toString());
    }

    @Test
    void getTokenUri_throws_service_exception_for_invalid_uri() {
        when(binding.getCredentials()).thenReturn(Map.of("handshake", Map.of(
                "uri", "https://handshake.example.com/handshake",
                "oa2", Map.of("tokenendpoint", "not valid %%uri", "clientid", "id", "clientsecret", "s"))));
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        assertThrows(ServiceException.class, supplier::getTokenUri);
    }

    // --- getClientIdentity ---

    @Test
    void getClientIdentity_returns_correct_id_and_secret() {
        when(binding.getCredentials()).thenReturn(fullCredentials());
        var supplier = new AemValidationOAuth2PropertySupplier(options);

        var identity = supplier.getClientIdentity();

        assertEquals("my-client-id", identity.getId());
        assertEquals("my-client-secret", identity.getSecret());
    }
}
