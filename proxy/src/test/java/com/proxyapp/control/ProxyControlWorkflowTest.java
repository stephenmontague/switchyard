package com.proxyapp.control;

import com.proxyapp.profile.WarehouseProfile;
import com.proxyapp.routing.Channel;
import com.proxyapp.routing.EdgeConfig;
import com.proxyapp.routing.RouteBinding;
import com.proxyapp.routing.Transport;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyControlWorkflowTest {

    private static final String TASK_QUEUE = "control-test";

    private TestWorkflowEnvironment env;
    private ProxyControlWorkflow workflow;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        env.newWorker(TASK_QUEUE).registerWorkflowImplementationTypes(ProxyControlWorkflowImpl.class);
        env.start();

        ProxyControlState seed = new ProxyControlState();
        seed.setTypeDirections(new WarehouseProfile().catalog().typeDirections());
        seed.setTcpPortPool(IntStream.rangeClosed(6000, 6010).boxed().toList());

        workflow = env.getWorkflowClient().newWorkflowStub(ProxyControlWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(ProxyControlWorkflow.WORKFLOW_ID)
                        .setTaskQueue(TASK_QUEUE)
                        .build());
        WorkflowClient.start(workflow::run, seed);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void enableDisableFlipTheFlagAndBumpVersion() {
        workflow.disable();
        ProxyControlState state = workflow.getState();
        assertThat(state.isEnabled()).isFalse();
        assertThat(state.getVersion()).isEqualTo(1);

        workflow.enable();
        state = workflow.getState();
        assertThat(state.isEnabled()).isTrue();
        assertThat(state.getVersion()).isEqualTo(2);
    }

    @Test
    void validConfigIsApplied() {
        workflow.applyConfig(List.of(device(6001)));
        ProxyControlState state = workflow.getState();
        assertThat(state.getDevices()).hasSize(1);
        assertThat(state.getVersion()).isEqualTo(1);
        assertThat(state.getLastError()).isNull();
    }

    @Test
    void invalidConfigIsRejectedWithClearMessageAndNeverGoesLive() {
        workflow.applyConfig(List.of(device(6001)));

        workflow.applyConfig(List.of(device(7777))); // out of pool
        ProxyControlState state = workflow.getState();
        assertThat(state.getLastError()).contains("applyConfig rejected").contains("7777");
        // previous good config stays live, version unchanged
        assertThat(state.getDevices()).hasSize(1);
        assertThat(binding(state).channel()).isEqualTo(Channel.port(6001));
        assertThat(state.getVersion()).isEqualTo(1);
    }

    @Test
    void upsertAndRemoveDevice() {
        workflow.applyConfig(List.of(device(6001)));
        workflow.upsertDevice(device(6002));
        ProxyControlState state = workflow.getState();
        assertThat(state.getDevices()).hasSize(1);
        assertThat(binding(state).channel()).isEqualTo(Channel.port(6002));

        workflow.removeDevice("mhe-1");
        assertThat(workflow.getState().getDevices()).isEmpty();

        workflow.removeDevice("mhe-1");
        assertThat(workflow.getState().getLastError()).contains("no device with id mhe-1");
    }

    private static RouteBinding binding(ProxyControlState state) {
        return state.getDevices().get(0).bindings().get(0);
    }

    private static EdgeConfig device(int inboundPort) {
        return new EdgeConfig("mhe-1", "http://edge:8082", "10.0.0.5", null, null, null, List.of(
                new RouteBinding(WarehouseProfile.PUTAWAY_CONFIRM, Transport.TCP,
                        Channel.port(inboundPort))));
    }
}
