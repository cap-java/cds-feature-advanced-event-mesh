package com.sap.cds.feature.messaging.aem.client.binding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.sap.cds.feature.messaging.aem.service.AemMessagingServiceConfiguration;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.utils.environment.ServiceBindingUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultOAuth2PropertySupplier;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2PropertySupplier;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2ServiceBindingDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;

public class AemManagementOauth2PropertySupplier extends DefaultOAuth2PropertySupplier {

	private static boolean initialized = false;

	private final ServiceBinding binding;
	private final AemAuthenticationServiceView authenticationServiceView;
	private final AemEndpointView endpointView;

	public static synchronized void initialize() {
		if (!initialized) {
			OAuth2ServiceBindingDestinationLoader.registerPropertySupplier(
					options -> ServiceBindingUtils.matches(options.getServiceBinding(),
							AemMessagingServiceConfiguration.BINDING_AEM_LABEL),
					AemManagementOauth2PropertySupplier::new);
			initialized = true;
		}
	}

	public AemManagementOauth2PropertySupplier(@Nonnull ServiceBindingDestinationOptions options) {
		super(options);
		this.binding = options.getServiceBinding();
		this.authenticationServiceView = new AemAuthenticationServiceView(binding);
		this.endpointView = new AemEndpointView(binding);
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
		Optional<String> serviceName = binding.getName();

		return serviceName.map(name -> name.equals(AemMessagingServiceConfiguration.BINDING_AEM_LABEL)).orElse(false);
	}

	private boolean areOAuth2ParametersPresent(ServiceBinding binding) {
		return this.authenticationServiceView.getTokenEndpoint().isPresent()
				&& this.authenticationServiceView.getClientId().isPresent()
				&& this.authenticationServiceView.getClientSecret().isPresent();
	}

}
