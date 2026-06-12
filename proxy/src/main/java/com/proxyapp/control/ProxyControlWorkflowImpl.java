package com.proxyapp.control;

import com.proxyapp.routing.ConfigValidator;
import com.proxyapp.routing.EdgeConfig;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@WorkflowImpl(taskQueues = "${proxy.control-task-queue}")
public class ProxyControlWorkflowImpl implements ProxyControlWorkflow {

    /** Continue-as-new after this many accepted/rejected changes, or daily, to bound history. */
    private static final int MAX_CHANGES_PER_RUN = 500;
    private static final Duration MAX_RUN_DURATION = Duration.ofHours(24);

    private static final Logger log = Workflow.getLogger(ProxyControlWorkflowImpl.class);

    private final ProxyControlState state;
    private int changes;

    @WorkflowInit
    public ProxyControlWorkflowImpl(ProxyControlState initialState) {
        // Initialized in the constructor so signals delivered before run() see valid state.
        this.state = initialState != null ? initialState : new ProxyControlState();
    }

    @Override
    public void run(ProxyControlState initialState) {
        Workflow.await(MAX_RUN_DURATION, () -> changes >= MAX_CHANGES_PER_RUN);
        Workflow.continueAsNew(state);
    }

    @Override
    public void enable() {
        state.setEnabled(true);
        accept("enable");
    }

    @Override
    public void disable() {
        state.setEnabled(false);
        accept("disable");
    }

    @Override
    public void applyConfig(List<EdgeConfig> devices) {
        List<String> errors = ConfigValidator.validate(
                state.getTypeDirections(), state.getTcpPortPool(), devices);
        if (!errors.isEmpty()) {
            reject("applyConfig", errors);
            return;
        }
        state.setDevices(new ArrayList<>(devices));
        accept("applyConfig");
    }

    @Override
    public void upsertDevice(EdgeConfig device) {
        List<EdgeConfig> proposed = new ArrayList<>(state.getDevices());
        proposed.removeIf(d -> d.deviceId() != null && d.deviceId().equals(device.deviceId()));
        proposed.add(device);
        List<String> errors = ConfigValidator.validate(
                state.getTypeDirections(), state.getTcpPortPool(), proposed);
        if (!errors.isEmpty()) {
            reject("upsertDevice", errors);
            return;
        }
        state.setDevices(proposed);
        accept("upsertDevice");
    }

    @Override
    public void removeDevice(String deviceId) {
        List<EdgeConfig> proposed = new ArrayList<>(state.getDevices());
        boolean removed = proposed.removeIf(d -> deviceId.equals(d.deviceId()));
        if (!removed) {
            reject("removeDevice", List.of("no device with id " + deviceId));
            return;
        }
        state.setDevices(proposed);
        accept("removeDevice");
    }

    @Override
    public void requestShutdown() {
        requestLifecycle(ProxyControlState.LIFECYCLE_SHUTDOWN);
    }

    @Override
    public void requestRestart() {
        requestLifecycle(ProxyControlState.LIFECYCLE_RESTART);
    }

    @Override
    public void ackLifecycle(String requestId) {
        if (requestId == null || !requestId.equals(state.getLifecycleRequestId())) {
            return; // stale or replayed ack for a command that was already superseded
        }
        log.info("lifecycle command '{}' acknowledged by proxy", state.getLifecycleCommand());
        state.setLifecycleCommand(ProxyControlState.LIFECYCLE_NONE);
        state.setLifecycleRequestId(null);
        changes++;
    }

    @Override
    public void reportApplied(AppliedStatus status) {
        state.setApplied(status);
        changes++;
    }

    @Override
    public ProxyControlState getState() {
        return state;
    }

    private void requestLifecycle(String command) {
        state.setLifecycleCommand(command);
        state.setLifecycleRequestId(Workflow.randomUUID().toString());
        changes++;
        log.info("lifecycle command '{}' requested ({})", command, state.getLifecycleRequestId());
    }

    private void accept(String change) {
        state.setVersion(state.getVersion() + 1);
        state.setLastError(null);
        changes++;
        log.info("control change '{}' accepted, version now {}", change, state.getVersion());
    }

    private void reject(String change, List<String> errors) {
        // Rejected changes never go live; the reason is surfaced on the queryable state.
        state.setLastError(change + " rejected: " + String.join("; ", errors));
        changes++;
        log.warn("control change '{}' rejected: {}", change, errors);
    }
}
