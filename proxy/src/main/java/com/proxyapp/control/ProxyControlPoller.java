package com.proxyapp.control;

import io.temporal.client.WorkflowClient;
import com.proxyapp.routing.RoutingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Queries the control workflow on a short interval and hands changed state to the
 * Reconciler. This poll (plus worker polling) is the proxy's entire control surface —
 * everything rides the egress gRPC connection; no inbound port is ever opened.
 */
public class ProxyControlPoller implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ProxyControlPoller.class);
    private static final long POLL_INTERVAL_MS = 2_000;

    private final WorkflowClient workflowClient;
    private final ProxyControlStarter starter;
    private final Reconciler reconciler;
    private final RoutingState routingState;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "proxy-control-poller");
                t.setDaemon(true);
                return t;
            });
    private volatile boolean running;

    public ProxyControlPoller(WorkflowClient workflowClient, ProxyControlStarter starter,
                              Reconciler reconciler, RoutingState routingState) {
        this.workflowClient = workflowClient;
        this.starter = starter;
        this.reconciler = reconciler;
        this.routingState = routingState;
    }

    @Override
    public void start() {
        running = true;
        executor.scheduleWithFixedDelay(this::pollOnce, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void pollOnce() {
        try {
            ProxyControlWorkflow stub = workflowClient.newWorkflowStub(
                    ProxyControlWorkflow.class, ProxyControlWorkflow.WORKFLOW_ID);
            ProxyControlState state = stub.getState();
            if (state.getVersion() != routingState.appliedVersion()
                    || state.isEnabled() != routingState.enabled()) {
                reconciler.apply(state);
            }
        } catch (Exception e) {
            // Not started yet (fresh namespace) or transient connectivity — ensure and retry.
            log.debug("control poll failed ({}); ensuring control workflow exists", e.toString());
            try {
                starter.ensureStarted();
            } catch (Exception startFailure) {
                log.warn("cannot reach control workflow: {}", startFailure.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // After the Temporal workers (which start on ApplicationReadyEvent-ish phases).
        return Integer.MAX_VALUE - 100;
    }
}
