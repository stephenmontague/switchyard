package com.dummycloud;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud")
public record CloudProperties(Temporal temporal, Proxy proxy) {

    public record Temporal(String target, String namespace) {
    }

    public record Proxy(String taskQueue, String controlTaskQueue, String controlWorkflowId) {
    }
}
