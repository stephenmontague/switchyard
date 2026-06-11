package com.proxyapp.control;

import com.proxyapp.routing.EdgeConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Desired state of one proxy install, held durably by the {@link ProxyControlWorkflow}.
 * {@code typeDirections} and {@code tcpPortPool} are seeded by the proxy at first start so
 * signal handlers can validate proposed configs deterministically inside the workflow.
 */
public class ProxyControlState {

    private boolean enabled = true;
    private List<EdgeConfig> devices = new ArrayList<>();
    private long version;
    private String lastError;
    private Map<String, String> typeDirections = new LinkedHashMap<>();
    private List<Integer> tcpPortPool = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<EdgeConfig> getDevices() {
        return devices;
    }

    public void setDevices(List<EdgeConfig> devices) {
        this.devices = devices;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Map<String, String> getTypeDirections() {
        return typeDirections;
    }

    public void setTypeDirections(Map<String, String> typeDirections) {
        this.typeDirections = typeDirections;
    }

    public List<Integer> getTcpPortPool() {
        return tcpPortPool;
    }

    public void setTcpPortPool(List<Integer> tcpPortPool) {
        this.tcpPortPool = tcpPortPool;
    }
}
