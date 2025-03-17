package com.sap.cds.feature.messaging.aem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.messaging.aem.service.AemMessagingService;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cds.services.environment.CdsProperties.Messaging.MessagingServiceConfig;
import com.sap.cds.services.impl.environment.SimplePropertiesProvider;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

public class AemMessagingServiceConfigurationTest {

	@Test
	void testDefaultServConfiguration() {
		CdsProperties properties = new CdsProperties();
		properties.getOutbox().getInMemory().setEnabled(false);
		CdsRuntimeConfigurer configurer = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties));

		configurer.serviceConfigurations();
		configurer.eventHandlerConfigurations();

		List<AemMessagingService> services = configurer.getCdsRuntime().getServiceCatalog().getServices(AemMessagingService.class).toList();

		assertEquals(1, services.size());
		assertEquals("advanced-event-mesh", services.get(0).getName());
	}

	@Test
	void testNoServiceBindings() {
		CdsProperties properties = new CdsProperties();
		CdsRuntimeConfigurer configurer = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties));

		configurer.serviceConfigurations();
		configurer.eventHandlerConfigurations();

		List<AemMessagingService> services = configurer.getCdsRuntime().getServiceCatalog().getServices(AemMessagingService.class).toList();
		assertTrue(services.isEmpty());
	}

	@Test
	void testSingleServConfiguration() {
		CdsProperties properties = new CdsProperties();
		MessagingServiceConfig config = new MessagingServiceConfig("cfg");
		config.setBinding("advanced-event-mesh");
		config.getOutbox().setEnabled(false);
		properties.getMessaging().getServices().put(config.getName(), config);

		assertEquals(0, properties.getMessaging().getServicesByBinding("").size());
		assertEquals(1, properties.getMessaging().getServicesByBinding("advanced-event-mesh").size());
		assertEquals(0, properties.getMessaging().getServicesByKind("").size());
		assertEquals(0, properties.getMessaging().getServicesByKind("aem").size());

		CdsRuntimeConfigurer configurer = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties));

		configurer.serviceConfigurations();
		configurer.eventHandlerConfigurations();

		List<AemMessagingService> services = configurer.getCdsRuntime().getServiceCatalog().getServices(AemMessagingService.class).collect(Collectors.toList());

		assertEquals(1, services.size());
		assertEquals("cfg", services.get(0).getName());
	}

	@Test
	void testMultipleServiceConfigurations() {
		CdsProperties properties = new CdsProperties();
		MessagingServiceConfig config1 = new MessagingServiceConfig("cfg1");
		config1.setBinding("advanced-event-mesh");
		config1.getOutbox().setEnabled(false);

		MessagingServiceConfig config2 = new MessagingServiceConfig("cfg2");
		config2.setBinding("advanced-event-mesh");
		config2.getOutbox().setEnabled(false);

		properties.getMessaging().getServices().put(config1.getName(), config1);
		properties.getMessaging().getServices().put(config2.getName(), config2);

		CdsRuntimeConfigurer configurer = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties));

		configurer.serviceConfigurations();
		configurer.eventHandlerConfigurations();

		List<AemMessagingService> services = configurer.getCdsRuntime().getServiceCatalog().getServices(AemMessagingService.class).collect(Collectors.toList());

		assertEquals(2, services.size());
		assertTrue(services.stream().anyMatch(service -> "cfg1".equals(service.getName())));
		assertTrue(services.stream().anyMatch(service -> "cfg2".equals(service.getName())));
	}

	@Test
	void testServiceConfigurationWithInvalidBinding() {
		CdsProperties properties = new CdsProperties();
		MessagingServiceConfig config = new MessagingServiceConfig("cfg");
		config.setBinding("invalid-binding");
		config.getOutbox().setEnabled(false);
		properties.getMessaging().getServices().put(config.getName(), config);

		CdsRuntimeConfigurer configurer = CdsRuntimeConfigurer.create(new SimplePropertiesProvider(properties));

		configurer.serviceConfigurations();
		configurer.eventHandlerConfigurations();

		List<AemMessagingService> services = configurer.getCdsRuntime().getServiceCatalog().getServices(AemMessagingService.class).collect(Collectors.toList());

		assertTrue(services.isEmpty(), "Expected no services to be configured with invalid binding");
	}
}
