package com.proxyapp.routing;

/**
 * The proxy's live view of the last successfully applied control state. Written only by
 * the Reconciler; read on hot paths (activities, ingress).
 */
public final class RoutingState {

    private volatile RouteTable table;
    private volatile boolean enabled;
    private volatile long appliedVersion = -1;

    public RoutingState(MessageCatalog catalog) {
        this.table = RouteTable.empty(catalog);
    }

    public RouteTable table() {
        return table;
    }

    public boolean enabled() {
        return enabled;
    }

    public long appliedVersion() {
        return appliedVersion;
    }

    public void update(RouteTable table, boolean enabled, long version) {
        this.table = table;
        this.enabled = enabled;
        this.appliedVersion = version;
    }
}
