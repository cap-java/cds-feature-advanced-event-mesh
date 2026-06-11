package com.sap.cds.feature.messaging.aem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sap.cds.services.environment.ApplicationInfo;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.feature.messaging.aem.client.AemManagementClient;
import com.sap.cds.feature.messaging.aem.client.AemValidationClient;
import com.sap.cds.feature.messaging.aem.jms.AemMessagingConnectionProvider;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.environment.CdsProperties.Messaging.MessagingServiceConfig;
import com.sap.cds.services.messaging.TopicMessageEventContext;
import com.sap.cds.services.messaging.jms.BrokerConnection;
import com.sap.cds.services.runtime.CdsRuntime;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import org.apache.qpid.jms.message.JmsBytesMessage;
import org.apache.qpid.jms.message.JmsTextMessage;
import org.apache.qpid.jms.provider.amqp.message.AmqpJmsBytesMessageFacade;
import org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("resource")
class AemMessagingServiceTest {

    @Mock AemManagementClient managementClient;
    @Mock AemValidationClient validationClient;
    @Mock AemMessagingConnectionProvider connectionProvider;
    @Mock BrokerConnection brokerConnection;
    @Mock CdsRuntime runtime;
    @Mock CdsEnvironment environment;
    @Mock ApplicationInfo appInfo;
    @Mock TopicMessageEventContext messageEventContext;

    private static final String MANAGEMENT_URI = "https://broker.example.com:943";

    private AemMessagingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(runtime.getEnvironment()).thenReturn(environment);
        when(environment.getApplicationInfo()).thenReturn(appInfo);
        when(appInfo.getName()).thenReturn("test-app");
        when(appInfo.getId()).thenReturn("test-app-id");

        MessagingServiceConfig config = new MessagingServiceConfig("test-service");
        config.getOutbox().setEnabled(false);

        service = new AemMessagingService(
                config, connectionProvider, managementClient, validationClient, brokerConnection, runtime);

        when(managementClient.getEndpoint()).thenReturn(MANAGEMENT_URI);
    }

    // --- removeQueue ---

    @Test
    void removeQueue_delegates_to_management_client() throws IOException {
        service.removeQueue("myqueue");
        verify(managementClient).removeQueue("myqueue");
    }

    @Test
    void removeQueue_skips_management_client_when_skipManagement_enabled() throws IOException {
        MessagingServiceConfig config = new MessagingServiceConfig("skip-service");
        config.getConnection().getProperties().put("skipManagement", "true");
        config.getOutbox().setEnabled(false);
        AemMessagingService svc = new AemMessagingService(
                config, connectionProvider, managementClient, validationClient, brokerConnection, runtime);

        svc.removeQueue("myqueue");

        verifyNoInteractions(managementClient);
    }

    // --- createQueue ---

    @Test
    void createQueue_delegates_to_management_client() throws IOException {
        service.createQueue("newqueue", Collections.emptyMap());
        verify(managementClient).createQueue("newqueue", Collections.emptyMap());
    }

    @Test
    void createQueue_skips_management_client_when_skipManagement_enabled() throws IOException {
        MessagingServiceConfig config = new MessagingServiceConfig("skip-service");
        config.getConnection().getProperties().put("skip-management", "true");
        config.getOutbox().setEnabled(false);
        AemMessagingService svc = new AemMessagingService(
                config, connectionProvider, managementClient, validationClient, brokerConnection, runtime);

        svc.createQueue("newqueue", Collections.emptyMap());

        verifyNoInteractions(managementClient);
    }

    @Test
    void createQueue_creates_dmq_first_when_property_present_and_dmq_missing() throws IOException {
        when(managementClient.getQueue("dead")).thenReturn(null);

        service.createQueue("main", Map.of(AemManagementClient.ATTR_DEAD_MSG_QUEUE, "dead"));

        var inOrder = inOrder(managementClient);
        inOrder.verify(managementClient).getQueue("dead");
        inOrder.verify(managementClient).createQueue("dead", Collections.emptyMap());
        inOrder.verify(managementClient).createQueue("main", Map.of(AemManagementClient.ATTR_DEAD_MSG_QUEUE, "dead"));
    }

    @Test
    void createQueue_skips_dmq_creation_when_dmq_already_exists() throws IOException {
        var existingNode = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        when(managementClient.getQueue("dead")).thenReturn(existingNode);

        service.createQueue("main", Map.of(AemManagementClient.ATTR_DEAD_MSG_QUEUE, "dead"));

        verify(managementClient, never()).createQueue(eq("dead"), any());
        verify(managementClient).createQueue("main", Map.of(AemManagementClient.ATTR_DEAD_MSG_QUEUE, "dead"));
    }

    // --- createQueueSubscription ---

    @Test
    void createQueueSubscription_delegates_to_management_client() throws IOException {
        service.createQueueSubscription("myqueue", "my/topic");
        verify(managementClient).createQueueSubscription("myqueue", "my/topic");
    }

    @Test
    void createQueueSubscription_skips_when_skipManagement_enabled() throws IOException {
        MessagingServiceConfig config = new MessagingServiceConfig("skip-service");
        config.getConnection().getProperties().put("skipManagement", "true");
        config.getOutbox().setEnabled(false);
        AemMessagingService svc = new AemMessagingService(
                config, connectionProvider, managementClient, validationClient, brokerConnection, runtime);

        svc.createQueueSubscription("myqueue", "my/topic");

        verifyNoInteractions(managementClient);
    }

    // --- emitTopicMessage ---

    @Test
    void emitTopicMessage_prefixes_with_topic_colon_slash_slash() throws Exception {
        service.emitTopicMessage("my/topic", messageEventContext);
        verify(brokerConnection).emitTopicMessage("topic://my/topic", messageEventContext);
    }

    @Test
    void emitTopicMessage_triggers_validate_on_first_call() throws Exception {
        service.emitTopicMessage("my/topic", messageEventContext);
        verify(validationClient).validate(MANAGEMENT_URI, null);
    }

    @Test
    void emitTopicMessage_validates_only_once_across_multiple_calls() throws Exception {
        service.emitTopicMessage("topic1", messageEventContext);
        service.emitTopicMessage("topic2", messageEventContext);

        verify(validationClient, times(1)).validate(any(), any());
    }

    @Test
    void emitTopicMessage_throws_service_exception_when_validation_fails() throws Exception {
        doThrow(new IOException("network error")).when(validationClient).validate(any(), any());

        assertThrows(ServiceException.class,
                () -> service.emitTopicMessage("my/topic", messageEventContext));
    }

    @Test
    void emitTopicMessage_passes_subaccount_id_to_validate() throws Exception {
        MessagingServiceConfig config = new MessagingServiceConfig("sub-service");
        config.getConnection().getProperties().put("subaccountId", "acct-123");
        config.getOutbox().setEnabled(false);
        AemMessagingService svc = new AemMessagingService(
                config, connectionProvider, managementClient, validationClient, brokerConnection, runtime);

        svc.emitTopicMessage("my/topic", messageEventContext);

        verify(validationClient).validate(MANAGEMENT_URI, "acct-123");
    }

    // --- stop ---

    @Test
    void stop_closes_connection() throws JMSException {
        service.stop();
        verify(brokerConnection).close();
    }

    @Test
    void stop_is_noop_when_connection_is_null() {
        MessagingServiceConfig config = new MessagingServiceConfig("null-conn-service");
        config.getOutbox().setEnabled(false);
        AemMessagingService svc = new AemMessagingService(
                config, connectionProvider, managementClient, validationClient, null, runtime);

        assertDoesNotThrow(svc::stop);
    }

    // --- getMessageTopic ---

    @Test
    void getMessageTopic_returns_address_for_amqp_text_message() {
        AmqpJmsTextMessageFacade facade = mock(AmqpJmsTextMessageFacade.class);
        JmsTextMessage msg = new JmsTextMessage(facade);

        org.apache.qpid.jms.JmsDestination dest = mock(org.apache.qpid.jms.JmsDestination.class);
        when(dest.getAddress()).thenReturn("my/topic/address");
        when(facade.getDestination()).thenReturn(dest);

        assertEquals("my/topic/address", service.getMessageTopic(msg));
    }

    @Test
    void getMessageTopic_returns_address_for_amqp_bytes_message() throws Exception {
        AmqpJmsBytesMessageFacade facade = mock(AmqpJmsBytesMessageFacade.class);
        JmsBytesMessage msg = new JmsBytesMessage(facade);

        org.apache.qpid.jms.JmsDestination dest = mock(org.apache.qpid.jms.JmsDestination.class);
        when(dest.getAddress()).thenReturn("bytes/topic/address");
        when(facade.getDestination()).thenReturn(dest);

        assertEquals("bytes/topic/address", service.getMessageTopic(msg));
    }

    @Test
    void getMessageTopic_returns_null_for_unknown_message_type() {
        Message unknownMsg = mock(Message.class);
        assertNull(service.getMessageTopic(unknownMsg));
    }

    @Test
    void getMessageTopic_returns_null_for_text_message_with_non_amqp_facade() {
        // JmsTextMessage whose facade is NOT AmqpJmsTextMessageFacade
        var plainFacade = mock(org.apache.qpid.jms.message.facade.JmsTextMessageFacade.class);
        JmsTextMessage msg = new JmsTextMessage(plainFacade);

        assertNull(service.getMessageTopic(msg));
    }
}
