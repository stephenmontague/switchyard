package com.proxyapp.temporal.activity;

import com.proxyapp.model.CanonicalMessage;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Outbound data path, started as a standalone activity by the cloud client with
 * Activity ID {@code {messageType}-{businessId}} (reuse REJECT_DUPLICATE + conflict
 * USE_EXISTING) so duplicate cloud dispatches collapse to one execution.
 */
@ActivityInterface
public interface DeliverToEdgeActivity {

    String ACTIVITY_TYPE = "DeliverToEdge";

    @ActivityMethod(name = ACTIVITY_TYPE)
    void deliver(CanonicalMessage message);
}
