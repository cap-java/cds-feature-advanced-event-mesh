package com.sap.cds.feature.messaging.aem.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.mockserver.matchers.MatchType;
import org.mockserver.verify.VerificationTimes;

class AemManagementClientIT {

    private static final String VPN = "test-vpn";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ClientAndServer mockServer;
    private AemManagementClient client;
    private String managementUri;

    @BeforeEach
    void setUp() {
        mockServer = ClientAndServer.startClientAndServer(0);
        managementUri = "https://broker.example.com:943";
        HttpDestination destination = DefaultHttpDestination
                .builder("http://localhost:" + mockServer.getPort())
                .build();
        client = new AemManagementClient(destination, managementUri, VPN);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    // --- getEndpoint ---

    @Test
    void getEndpoint_returns_management_uri() {
        assertEquals(managementUri, client.getEndpoint());
    }

    // --- createQueue ---

    @Test
    void createQueue_creates_queue_when_not_exists() throws IOException {
        String queuePath = "/SEMP/v2/config/msgVpns/test-vpn/queues/myqueue";
        String queuesPath = "/SEMP/v2/config/msgVpns/test-vpn/queues";

        // GET returns 404 → getQueue returns null
        mockServer.when(request().withMethod("GET").withPath(queuePath))
                .respond(response().withStatusCode(404).withReasonPhrase("Not Found"));

        // POST succeeds
        mockServer.when(request().withMethod("POST").withPath(queuesPath))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        assertDoesNotThrow(() -> client.createQueue("myqueue", Collections.emptyMap()));

        mockServer.verify(request().withMethod("POST").withPath(queuesPath));
    }

    @Test
    void createQueue_skips_post_when_queue_already_exists() throws IOException {
        String queuePath = "/SEMP/v2/config/msgVpns/test-vpn/queues/existing";

        ObjectNode queueNode = MAPPER.createObjectNode();
        queueNode.put("queueName", "existing");

        mockServer.when(request().withMethod("GET").withPath(queuePath))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(MAPPER.writeValueAsString(queueNode)));

        client.createQueue("existing", Collections.emptyMap());

        // no POST should have been made
        mockServer.verify(request().withMethod("POST"), VerificationTimes.exactly(0));
    }

    // --- removeQueue ---

    @Test
    void removeQueue_sends_delete_to_correct_path() throws IOException {
        String path = "/SEMP/v2/config/msgVpns/test-vpn/queues/myqueue";
        mockServer.when(request().withMethod("DELETE").withPath(path))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.removeQueue("myqueue");

        mockServer.verify(request().withMethod("DELETE").withPath(path));
    }

    @Test
    void removeQueue_url_encodes_queue_name() throws IOException {
        // queue name with a slash should be percent-encoded
        String encodedPath = "/SEMP/v2/config/msgVpns/test-vpn/queues/my%2Fqueue";
        mockServer.when(request().withMethod("DELETE").withPath(encodedPath))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.removeQueue("my/queue");

        mockServer.verify(request().withMethod("DELETE").withPath(encodedPath));
    }

    // --- createQueueSubscription ---

    @Test
    void createQueueSubscription_subscribes_when_not_yet_subscribed() throws IOException {
        String subsPath = "/SEMP/v2/config/msgVpns/test-vpn/queues/myqueue/subscriptions";

        // GET subscriptions → empty data array
        mockServer.when(request().withMethod("GET").withPath(subsPath))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"data\":[]}"));

        mockServer.when(request().withMethod("POST").withPath(subsPath))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.createQueueSubscription("myqueue", "topic://my/topic");

        mockServer.verify(request().withMethod("POST").withPath(subsPath));
    }

    @Test
    void createQueueSubscription_skips_post_when_already_subscribed() throws IOException {
        String subsPath = "/SEMP/v2/config/msgVpns/test-vpn/queues/myqueue/subscriptions";

        String existingData = String.format(
                "{\"data\":[{\"msgVpnName\":\"%s\",\"queueName\":\"myqueue\",\"subscriptionTopic\":\"my/topic\"}]}",
                VPN);

        mockServer.when(request().withMethod("GET").withPath(subsPath))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(existingData));

        client.createQueueSubscription("myqueue", "topic://my/topic");

        mockServer.verify(request().withMethod("POST"), VerificationTimes.exactly(0));
    }

    // --- isTopicSubscribed ---

    @Test
    void isTopicSubscribed_returns_true_when_matching_entry_present() throws Exception {
        String json = String.format(
                "{\"data\":[{\"msgVpnName\":\"%s\",\"queueName\":\"q\",\"subscriptionTopic\":\"my/topic\"}]}",
                VPN);
        var node = MAPPER.readTree(json);

        assertTrue(client.isTopicSubscribed(node, "q", "topic://my/topic"));
    }

    @Test
    void isTopicSubscribed_strips_topic_prefix_before_comparing() throws Exception {
        String json = String.format(
                "{\"data\":[{\"msgVpnName\":\"%s\",\"queueName\":\"q\",\"subscriptionTopic\":\"my/topic\"}]}",
                VPN);
        var node = MAPPER.readTree(json);

        // both with and without prefix must match the stored raw topic
        assertTrue(client.isTopicSubscribed(node, "q", "my/topic"));
    }

    @Test
    void isTopicSubscribed_returns_false_when_queue_name_differs() throws Exception {
        String json = String.format(
                "{\"data\":[{\"msgVpnName\":\"%s\",\"queueName\":\"other\",\"subscriptionTopic\":\"my/topic\"}]}",
                VPN);
        var node = MAPPER.readTree(json);

        assertFalse(client.isTopicSubscribed(node, "q", "topic://my/topic"));
    }

    @Test
    void isTopicSubscribed_returns_false_when_topic_differs() throws Exception {
        String json = String.format(
                "{\"data\":[{\"msgVpnName\":\"%s\",\"queueName\":\"q\",\"subscriptionTopic\":\"other/topic\"}]}",
                VPN);
        var node = MAPPER.readTree(json);

        assertFalse(client.isTopicSubscribed(node, "q", "topic://my/topic"));
    }

    @Test
    void isTopicSubscribed_returns_false_on_empty_data_array() throws Exception {
        var node = MAPPER.readTree("{\"data\":[]}");

        assertFalse(client.isTopicSubscribed(node, "q", "topic://my/topic"));
    }

    @Test
    void isTopicSubscribed_returns_false_when_no_data_key() throws Exception {
        var node = MAPPER.readTree("{}");

        assertFalse(client.isTopicSubscribed(node, "q", "topic://my/topic"));
    }

    // --- getEndpoint missing URI is tested via the constructor logic ---

    @Test
    void createQueue_posts_required_attributes() throws IOException {
        String queuePath = "/SEMP/v2/config/msgVpns/test-vpn/queues/newq";
        String queuesPath = "/SEMP/v2/config/msgVpns/test-vpn/queues";

        mockServer.when(request().withMethod("GET").withPath(queuePath))
                .respond(response().withStatusCode(404).withReasonPhrase("Not Found"));

        mockServer.when(request().withMethod("POST").withPath(queuesPath)
                .withBody(org.mockserver.model.JsonBody.json(
                        "{\"queueName\":\"newq\",\"permission\":\"consume\",\"ingressEnabled\":true,\"egressEnabled\":true}",
                        MatchType.ONLY_MATCHING_FIELDS)))
                .respond(response().withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{}"));

        client.createQueue("newq", Collections.emptyMap());

        mockServer.verify(request().withMethod("POST").withPath(queuesPath));
    }
}
