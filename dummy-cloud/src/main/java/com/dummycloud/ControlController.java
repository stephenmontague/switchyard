package com.dummycloud;

import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Control-plane drivers: remote on/off + hot config, signalled through Temporal Cloud.
 * This is the contract the Part 2 management UI will use — the cloud never talks to the
 * proxy directly.
 */
@RestController
public class ControlController {

    private final WorkflowClient workflowClient;
    private final CloudProperties properties;

    public ControlController(WorkflowClient workflowClient, CloudProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    private WorkflowStub controlStub() {
        return workflowClient.newUntypedWorkflowStub(properties.proxy().controlWorkflowId());
    }

    @PostMapping("/control/enable")
    public Map<String, Object> enable() {
        controlStub().signal("enable");
        return state();
    }

    @PostMapping("/control/disable")
    public Map<String, Object> disable() {
        controlStub().signal("disable");
        return state();
    }

    /** Body: JSON array of EdgeConfig, same shape the proxy's seed file uses. */
    @PostMapping("/control/apply-config")
    public Map<String, Object> applyConfig(@RequestBody JsonNode devices) {
        controlStub().signal("applyConfig", devices);
        return state();
    }

    @PostMapping("/control/remove-device/{deviceId}")
    public Map<String, Object> removeDevice(@org.springframework.web.bind.annotation.PathVariable String deviceId) {
        controlStub().signal("removeDevice", deviceId);
        return state();
    }

    @GetMapping("/control/state")
    public Map<String, Object> state() {
        JsonNode state = controlStub().query("getState", JsonNode.class);
        return Map.of("state", state);
    }
}
