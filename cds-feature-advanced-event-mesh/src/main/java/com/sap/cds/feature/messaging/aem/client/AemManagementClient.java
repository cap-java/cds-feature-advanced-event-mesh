package com.sap.cds.feature.messaging.aem.client;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cds.feature.messaging.aem.client.binding.AemEndpointView;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.ServiceBindingDestinationOptions;

public class AemManagementClient extends RestClient {
	private static final Logger logger = LoggerFactory.getLogger(AemManagementClient.class);

	private static final String API_BASE = "/SEMP/v2/config/msgVpns/%s";
	private static final String API_QUEUE = API_BASE + "/queues";
	private static final String API_QUEUE_NAME = API_BASE + "/queues/%s";
	private static final String API_QUEUE_NAME_SUBSCRIPTION = API_BASE + "/queues/%s/subscriptions";

	private static final String ATTR_EGRESS_ENABLED = "egressEnabled";
	private static final String ATTR_INGRESS_ENABLED = "ingressEnabled";
	public  static final String ATTR_DEAD_MSG_QUEUE = "deadMsgQueue";
	private static final String ATTR_PERMISSION = "permission";
	private static final String ATTR_QUEUE_NAME = "queueName";
	private static final String ATTR_SUBSCRIPTION_TOPIC = "subscriptionTopic";
	private static final String VAL_CONSUME = "consume";

	private final AemEndpointView endpointView;
	private final String vpn;
	private final String owner;

	public AemManagementClient(ServiceBinding binding) {
		super(ServiceBindingDestinationOptions.forService(binding).build());
		this.endpointView = new AemEndpointView(binding);
		this.vpn = getVpn();
		this.owner = getOwner();
	}

	public String getEndpoint() {
		return this.endpointView.getUri().orElseThrow(() -> new ServiceException("Management endpoint not available in binding"));
	}

	public void removeQueue(String queue) throws IOException {
		logger.debug("Removing queue {}", queue);

		deleteRequest(uri(API_QUEUE_NAME, this.vpn, queue));

		logger.debug("Successfully removed queue {}", queue);
	}

	public JsonNode getQueue(String name) throws IOException {
		try {
			logger.debug("Retrieving information for queue {}", name);

			JsonNode result = getRequest(uri(API_QUEUE_NAME, this.vpn, name));

			logger.debug("Successfully retrieved information for queue {}: {}", name, result.asText());

			return result;
		} catch (Exception e) {
			logger.error("Failed to retrieve information for queue {}", name, e);
			return null;
		}
	}

	public void createQueue(String name, Map<String, Object> properties) throws IOException {
		// We have to read the queue	first to check if it exists; only create it if it doesn't
		logger.debug("Checking if queue {} exists", name);
		JsonNode queue = getQueue(name);

		if (queue == null) {
			logger.debug("Queue {} does not exist, creating it", name);

			Map<String, Object> attributes = new HashMap<>(properties);
			attributes.put(ATTR_QUEUE_NAME, name);
			attributes.put(ATTR_PERMISSION, VAL_CONSUME);
			attributes.put(ATTR_INGRESS_ENABLED, true);
			attributes.put(ATTR_EGRESS_ENABLED, true);

			postRequest(uri(API_QUEUE, this.vpn), attributes);
		}
	}

	public JsonNode getQueueSubscription(String queue) throws IOException {
		logger.debug("Retrieving information for queue subscription {}", queue);
		JsonNode result = getRequest(uri(API_QUEUE_NAME_SUBSCRIPTION, this.vpn, queue));

		return result;
	}

	public void createQueueSubscription(String queue, String topic) throws IOException {
		logger.debug("Checking if queue {} is subscribed to topic {}", queue, topic);

		if (!isTopicSubscribed(getQueueSubscription(queue), queue, topic)) {
			logger.debug("Queue {} is not subscribed to topic {}, subscribing it", queue, topic);

			Map<String, Object> attributes = Map.of(ATTR_SUBSCRIPTION_TOPIC, topic);
			postRequest(uri(API_QUEUE_NAME_SUBSCRIPTION, this.vpn, queue), attributes);
		}
	}

	private String uri(String path, Object... args) {
		return String.format(
				path,
				(Object[]) Arrays.stream(args).map(Object::toString).map(this::urlEncode).toArray(String[]::new));
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public boolean isTopicSubscribed(JsonNode jsonNode, String queueName, String topic) {
		String rawTopic = topic.replace("topic://", "");
		if (jsonNode.has("data")) {
			for (JsonNode dataNode : jsonNode.get("data")) {
				if (dataNode.has("msgVpnName") && dataNode.has("queueName") && dataNode.has("subscriptionTopic")) {
					String nodeMsgVpnName = dataNode.get("msgVpnName").asText();
					String nodeQueueName = dataNode.get("queueName").asText();
					String nodeSubscriptionTopic = dataNode.get("subscriptionTopic").asText();

					if (this.getVpn().equals(nodeMsgVpnName) && queueName.equals(nodeQueueName) && rawTopic.equals(nodeSubscriptionTopic)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private String getVpn() {
		return this.endpointView.getVpn().get();
	}

	private String getOwner() {
		return getVpn()	;
	}

}
