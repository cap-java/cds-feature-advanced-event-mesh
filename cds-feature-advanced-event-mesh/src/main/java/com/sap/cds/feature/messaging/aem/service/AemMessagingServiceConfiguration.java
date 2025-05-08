package com.sap.cds.feature.messaging.aem.service;

import static com.sap.cds.services.messaging.utils.MessagingOutboxUtils.outboxed;

import com.google.common.base.Strings;
import com.sap.cds.feature.messaging.aem.client.binding.AemManagementOauth2PropertySupplier;
import com.sap.cds.feature.messaging.aem.client.binding.AemValidationOAuth2PropertySupplier;
import com.sap.cds.feature.messaging.aem.jms.AemMessagingConnectionProvider;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.environment.CdsProperties.Messaging;
import com.sap.cds.services.environment.CdsProperties.Messaging.MessagingServiceConfig;
import com.sap.cds.services.messaging.MessagingService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.environment.ServiceBindingUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AemMessagingServiceConfiguration implements CdsRuntimeConfiguration {

  public static final String BINDING_AEM_LABEL = "advanced-event-mesh";
  public static final String BINDING_AEM_VALIDATION_LABEL = "aem-validation-service";
  public static final String AEM_KIND = "aem";
  private static final Logger logger =
      LoggerFactory.getLogger(AemMessagingServiceConfiguration.class);

  @Override
  public void services(CdsRuntimeConfigurer configurer) {
    AemManagementOauth2PropertySupplier.initialize();
    AemValidationOAuth2PropertySupplier.initialize();

    Messaging config =
        configurer.getCdsRuntime().getEnvironment().getCdsProperties().getMessaging();
    List<ServiceBinding> bindings =
        configurer
            .getCdsRuntime()
            .getEnvironment()
            .getServiceBindings()
            .filter(
                binding ->
                    (binding.getName().isPresent()
                            && BINDING_AEM_LABEL.equals(binding.getName().get()))
                        || binding.getTags().contains(BINDING_AEM_LABEL))
            .toList();
    Optional<ServiceBinding> validationBinding =
        configurer
            .getCdsRuntime()
            .getEnvironment()
            .getServiceBindings()
            .filter(binding -> ServiceBindingUtils.matches(binding, BINDING_AEM_VALIDATION_LABEL))
            .findFirst();

    if (bindings.isEmpty()) {
      logger.info("No service bindings with name '{}' found", BINDING_AEM_LABEL);
    } else {
      boolean isSingleBinding = bindings.size() == 1;

      bindings.forEach(
          binding -> {
            logger.debug(
                "Starting the initialization of the advanced-event-mesh service binding '{}'",
                binding.getName().get());

            AemMessagingConnectionProvider sharedConnectionProvider =
                new AemMessagingConnectionProvider(binding);

            // determines whether no configuration is available and the default service
            // should be created
            boolean createDefaultService = true;

            // check the services configured by binding
            List<MessagingServiceConfig> serviceConfigs =
                config.getServicesByBinding(binding.getName().get());

            if (!serviceConfigs.isEmpty()) {
              logger.debug("Service configurations found, not creating default service.");

              createDefaultService = false;
              serviceConfigs.forEach(
                  serviceConfig -> {
                    if (serviceConfig.isEnabled()) {
                      // register the service
                      configurer.service(
                          createMessagingService(
                              binding,
                              validationBinding,
                              sharedConnectionProvider,
                              serviceConfig,
                              configurer.getCdsRuntime()));
                    }
                  });
            }

            // check the services configured by kind if only one service binding is
            // available
            logger.debug(
                "Checking the services configured by kind if only one service binding is available.");
            List<MessagingServiceConfig> serviceConfigsByKind =
                config.getServicesByKind(BINDING_AEM_LABEL);
            serviceConfigsByKind.addAll(config.getServicesByKind(AEM_KIND));

            if (isSingleBinding && !serviceConfigsByKind.isEmpty()) {
              logger.debug(
                  "Service configurations by kind for single service binding found, not creating default service.");
              createDefaultService = false;
              serviceConfigsByKind.forEach(
                  serviceConfig -> {
                    // check that the service is enabled and whether not already found by name or
                    // binding
                    if (serviceConfig.isEnabled()
                        && serviceConfigs.stream()
                            .noneMatch(c -> c.getName().equals(serviceConfig.getName()))) {
                      // register the service
                      configurer.service(
                          createMessagingService(
                              binding,
                              validationBinding,
                              sharedConnectionProvider,
                              serviceConfig,
                              configurer.getCdsRuntime()));
                    }
                  });
            }

            if (createDefaultService) {
              logger.debug("No service configurations found, creating default service.");

              // otherwise create default service instance for the binding
              MessagingServiceConfig defConfig = config.getService(binding.getName().get());

              if (Strings.isNullOrEmpty(defConfig.getBinding())
                  && Strings.isNullOrEmpty(defConfig.getKind())) {
                // register the service
                configurer.service(
                    createMessagingService(
                        binding,
                        validationBinding,
                        sharedConnectionProvider,
                        defConfig,
                        configurer.getCdsRuntime()));
              } else {
                logger.warn(
                    "Could not create service for binding '{}': A configuration with the same name is already defined for another kind or binding.",
                    binding.getName().get());
              }
            }

            logger.debug(
                "Finished the initialization of the advanced-event-mesh service binding '{}'",
                binding.getName().get());
          });
    }
  }

  private MessagingService createMessagingService(
      ServiceBinding binding,
      Optional<ServiceBinding> validationBinding,
      AemMessagingConnectionProvider sharedConnectionProvider,
      MessagingServiceConfig serviceConfig,
      CdsRuntime runtime) {

    ServiceBinding valBnd =
        validationBinding.orElseThrow(
            () -> new ServiceException("No binding for AEM Validation Service found."));
    MessagingService service =
        new AemMessagingService(binding, valBnd, serviceConfig, sharedConnectionProvider, runtime);

    logger.debug(
        "Created messaging service '{}' for binding '{}'",
        serviceConfig.getName(),
        binding.getName().get());

    return outboxed(service, serviceConfig, runtime);
  }
}
