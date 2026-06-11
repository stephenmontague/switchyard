package com.dummycloud;

import java.util.Map;

/**
 * Cloud-side copy of the Warehouse reference catalog: outbound type -> business-id field.
 * The cloud app owns the catalog (layer 1 of the 3-layer config); the proxy ships the same
 * profile.
 */
public final class WarehouseCatalog {

    public static final String WAVE_RELEASE = "WAVE_RELEASE";
    public static final String CONTAINER_PUTAWAY = "CONTAINER_PUTAWAY";
    public static final String CYCLE_COUNT_REQ = "CYCLE_COUNT_REQ";

    public static final Map<String, String> OUTBOUND_BUSINESS_ID_FIELDS = Map.of(
            WAVE_RELEASE, "orderId",
            CONTAINER_PUTAWAY, "containerId",
            CYCLE_COUNT_REQ, "countId");

    private WarehouseCatalog() {
    }
}
