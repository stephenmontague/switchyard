package com.dummycloud;

import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.api.enums.v1.ActivityIdConflictPolicy;
import io.temporal.api.enums.v1.ActivityIdReusePolicy;
import io.temporal.client.ActivityAlreadyStartedException;
import io.temporal.client.ActivityClient;
import io.temporal.client.StartActivityOptions;
import io.temporal.client.UntypedActivityHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Starts the proxy's {@code DeliverToEdge} standalone activity for any outbound message
 * type. Activity ID = {type}-{businessId} with REJECT_DUPLICATE + USE_EXISTING gives
 * exactly-once dispatch semantics on top of at-least-once execution; because the task
 * lands in Temporal Cloud first, an offline proxy just means the delivery waits.
 */
@Service
public class OutboundDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboundDispatcher.class);
    private static final String DELIVER_TO_EDGE = "DeliverToEdge";

    private final ActivityClient activityClient;
    private final CloudProperties properties;

    public OutboundDispatcher(ActivityClient activityClient, CloudProperties properties) {
        this.activityClient = activityClient;
        this.properties = properties;
    }

    public Map<String, Object> dispatch(String messageType, JsonNode body) {
        String idField = WarehouseCatalog.OUTBOUND_BUSINESS_ID_FIELDS.get(messageType);
        JsonNode idNode = idField == null ? null : body.get(idField);
        if (idNode == null || idNode.asText().isBlank()) {
            throw new IllegalArgumentException("payload must carry '" + idField + "'");
        }
        CanonicalMessage message =
                new CanonicalMessage(messageType, idNode.asText(), body.toString());

        StartActivityOptions options = StartActivityOptions.newBuilder()
                .setId(message.activityId())
                .setTaskQueue(properties.proxy().taskQueue())
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setIdReusePolicy(ActivityIdReusePolicy.ACTIVITY_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
                .build();
        try {
            // Untyped start: the cloud app shares no code with the proxy, only the contract.
            UntypedActivityHandle handle =
                    activityClient.start(DELIVER_TO_EDGE, options, message);
            log.info("dispatched {} (run {})", message.activityId(), handle.getActivityRunId());
            return Map.of("activityId", message.activityId(), "duplicate", false);
        } catch (ActivityAlreadyStartedException e) {
            log.info("duplicate dispatch of {} collapsed", message.activityId());
            return Map.of("activityId", message.activityId(), "duplicate", true);
        }
    }
}
