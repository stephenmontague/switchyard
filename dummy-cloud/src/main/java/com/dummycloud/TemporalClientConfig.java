package com.dummycloud;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The cloud app is a Temporal client only — it dispatches work and signals; no worker. */
@Configuration
public class TemporalClientConfig {

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs serviceStubs(CloudProperties properties) {
        return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder()
                .setTarget(properties.temporal().target())
                .build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs, CloudProperties properties) {
        return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder()
                .setNamespace(properties.temporal().namespace())
                .build());
    }

    @Bean
    public ActivityClient activityClient(WorkflowServiceStubs stubs, CloudProperties properties) {
        return ActivityClient.newInstance(stubs, ActivityClientOptions.newBuilder()
                .setNamespace(properties.temporal().namespace())
                .build());
    }
}
