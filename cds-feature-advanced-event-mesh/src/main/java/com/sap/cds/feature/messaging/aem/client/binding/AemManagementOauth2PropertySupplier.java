package com.sap.cds.feature.messaging.aem.client.binding;

import com.sap.cds.feature.messaging.aem.service.AemMessagingServiceConfiguration;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2PropertySupplier;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2ServiceBindingDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;

public class AemManagementOauth2PropertySupplier implements OAuth2PropertySupplier {

	private static boolean initialized = false;

	private final ServiceBinding binding;
	private final AemAuthenticationServiceView authenticationServiceView;
	private final AemEndpointView endpointView;

	public AemManagementOauth2PropertySupplier(@Nonnull ServiceBindingDestinationOptions options) {
		this.binding = options.getServiceBinding();
		this.authenticationServiceView = new AemAuthenticationServiceView(binding);
		this.endpointView = new AemEndpointView(binding);
	}

	public static synchronized void initialize() {
		if (!initialized) {
			OAuth2ServiceBindingDestinationLoader.registerPropertySupplier(
					options -> (options.getServiceBinding().getName().isPresent() &&
							AemMessagingServiceConfiguration.BINDING_AEM_LABEL.equals(options.getServiceBinding().getName().get()))
					|| options.getServiceBinding().getTags().contains(AemMessagingServiceConfiguration.BINDING_AEM_LABEL),
					AemManagementOauth2PropertySupplier::new);
			initialized = true;
		}
	}

	@Override
	public boolean isOAuth2Binding() {
		return isAemBinding(this.binding) && areOAuth2ParametersPresent(this.binding);
	}

	@Nonnull
	@Override
	public URI getServiceUri() {
		String uri = endpointView.getUri()
				.orElseThrow(() -> new ServiceException("Service endpoint not found in binding"));

		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new ServiceException("Invalid Service URI: " + uri, e);
		}
	}

	@Nonnull
	@Override
	public URI getTokenUri() {
		try {
			return new URI(this.authenticationServiceView.getTokenEndpoint().get());
		} catch (URISyntaxException e) {
			throw new ServiceException(e.getMessage(), e);
		}
	}

	@Nonnull
	@Override
	public com.sap.cloud.security.config.ClientIdentity getClientIdentity() {
		return new AemClientIdentity(this.authenticationServiceView.getClientId().get(),
				this.authenticationServiceView.getClientSecret().get());
	}

	private boolean isAemBinding(ServiceBinding binding) {
		return (binding.getName().isPresent() &&
				AemMessagingServiceConfiguration.BINDING_AEM_LABEL.equals(binding.getName().get()))
				|| binding.getTags().contains(AemMessagingServiceConfiguration.BINDING_AEM_LABEL);
	}

	private boolean areOAuth2ParametersPresent(ServiceBinding binding) {
		return this.authenticationServiceView.getTokenEndpoint().isPresent()
				&& this.authenticationServiceView.getClientId().isPresent()
				&& this.authenticationServiceView.getClientSecret().isPresent();
	}

}
