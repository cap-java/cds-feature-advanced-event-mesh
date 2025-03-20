package com.sap.cds.feature.messaging.aem.service;

import static com.sap.cds.feature.messaging.aem.client.AemManagementClient.ATTR_DEAD_MSG_QUEUE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.qpid.jms.message.JmsBytesMessage;
import org.apache.qpid.jms.message.JmsTextMessage;
import org.apache.qpid.jms.provider.amqp.message.AmqpJmsBytesMessageFacade;
import org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.messaging.aem.client.AemManagementClient;
import com.sap.cds.feature.messaging.aem.client.AemValidationClient;
import com.sap.cds.feature.messaging.aem.jms.AemMessagingConnectionProvider;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.environment.CdsProperties.Messaging.MessagingServiceConfig;
import com.sap.cds.services.messaging.TopicMessageEventContext;
import com.sap.cds.services.messaging.jms.BrokerConnection;
import com.sap.cds.services.messaging.service.AbstractMessagingService;
import com.sap.cds.services.messaging.service.MessagingBrokerQueueListener;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

public class AemMessagingService extends AbstractMessagingService {
	private static final Logger logger = LoggerFactory.getLogger(AemMessagingService.class);

	private final AemMessagingConnectionProvider connectionProvider;
	private final AemManagementClient managementClient;
	private final Optional<ServiceBinding> validationBinding;

	private volatile BrokerConnection connection;
	private volatile Boolean aemBrokerValidated = false;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	protected AemMessagingService(ServiceBinding binding, Optional<ServiceBinding> validationBinding,
			MessagingServiceConfig serviceConfig, AemMessagingConnectionProvider connectionProvider, CdsRuntime runtime) {

		super(serviceConfig, runtime);

		this.connectionProvider = connectionProvider;
		this.managementClient = new AemManagementClient(binding);
		this.validationBinding = validationBinding;
	}

	@Override
	public void init() {
		logger.debug("Creating the broker connection asynchronously with topic subscriptions.");
		this.asyncConnectionInitialization(connection -> {
			this.connection = connection;
			super.init();

			logger.debug("The broker connection has been created.");
		});
	}

	@Override
	public void stop() {
		logger.debug("Stopping the broker connection...");

		if (connection != null) {
			try {
				connection.close();
				logger.debug("The broker connection has been stopped.");
			} catch (JMSException e) {
				// ignored
				logger.debug("An error occurred while stopping the broker connection.", e);
			}
		} else {
			logger.debug("No broker connection available..");
		}
	}

	@Override
	protected void removeQueue(String name) throws IOException {
		managementClient.removeQueue(name);
	}

	@Override
	protected void createQueue(String name, Map<String, Object> properties) throws IOException {
		if (properties.containsKey(ATTR_DEAD_MSG_QUEUE)) {
			String dmQueue = (String) properties.get(ATTR_DEAD_MSG_QUEUE);
			if (managementClient.getQueue(dmQueue) == null) {
				managementClient.createQueue(dmQueue, Collections.emptyMap());
			}
		}
		managementClient.createQueue(name, properties);
	}

	@Override
	protected void createQueueSubscription(String queue, String topic) throws IOException {
		managementClient.createQueueSubscription(queue, topic);
	}

	@Override
	protected void registerQueueListener(String queue, MessagingBrokerQueueListener listener) throws IOException {
		connection.registerQueueListener(queue, listener, this::getMessageTopic);
	}

	@Override
	protected void emitTopicMessage(String topic, TopicMessageEventContext messageEventContext) {
		this.validate(this.managementClient.getEndpoint());
		this.connection.emitTopicMessage("topic://" + topic, messageEventContext);
	}

	private void asyncConnectionInitialization(Consumer<BrokerConnection> connectionConsumer) {
		connectionProvider.asyncConnectionInitialization(serviceConfig, connectionConsumer);
	}

	private String getMessageTopic(Message message) {
		if (message instanceof JmsTextMessage textMessage) {
			if (textMessage.getFacade() instanceof AmqpJmsTextMessageFacade textMessageFacade) {
				return textMessageFacade.getDestination().getAddress();
			}
		} else if (message instanceof JmsBytesMessage bytesMessage
				&& bytesMessage.getFacade() instanceof AmqpJmsBytesMessageFacade bytesMessageFacade) {
			return bytesMessageFacade.getDestination().getAddress();
		}
		return null;
	}

	private void validate(String endpoint) {
			if (!this.aemBrokerValidated) {
				ServiceBinding binding = this.validationBinding.orElseThrow(() -> new ServiceException("No binding for AEM Validation Service found."));
				AemValidationClient validationClient = new AemValidationClient(binding);

				try {
					validationClient.validate(endpoint);
					this.aemBrokerValidated = true;
				} catch (IOException | URISyntaxException e) {
					throw new ServiceException("Failed to validate the AEM endpoint.", e);
				}
			}
	}

}
