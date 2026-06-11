package com.proxyapp.temporal.activity;

import com.proxyapp.model.CanonicalMessage;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Inbound data path, started as a standalone activity by the proxy's ingress gateway with
 * Activity ID {@code {messageType}-{businessId}} so duplicate edge pushes collapse to one
 * execution.
 */
@ActivityInterface
public interface DeliverToCloudActivity {

    String ACTIVITY_TYPE = "DeliverToCloud";

    @ActivityMethod(name = ACTIVITY_TYPE)
    void deliver(CanonicalMessage message);
}
