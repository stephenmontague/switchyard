package com.dummycloud;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Copy of the proxy's canonical message DTO (structurally compatible JSON). */
public record CanonicalMessage(String messageType, String businessId, String payload) {

    @JsonIgnore
    public String activityId() {
        return messageType + "-" + businessId;
    }
}
