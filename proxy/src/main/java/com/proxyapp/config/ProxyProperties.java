package com.proxyapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap-only configuration. Operational config (devices, routing, enabled) lives in
 * the control workflow; only what's needed to reach Temporal and describe the site
 * infrastructure stays local.
 */
@ConfigurationProperties(prefix = "proxy")
public record ProxyProperties(String taskQueue, String controlTaskQueue, String profile,
                              Cloud cloud, Ingress ingress, Seed seed) {

    public record Cloud(String baseUrl) {
    }

    /**
     * Site infrastructure, set once at install by IT.
     *
     * @param tcpPortPool inbound TCP ports available for routing bindings, as a range
     *                    ("6000-6010") or comma list ("6000,6001")
     */
    public record Ingress(String tcpPortPool, int ftpPort, String ftpRoot,
                          String ftpUser, String ftpPassword) {
    }

    /** Demo convenience: device config used to seed a brand-new control workflow. */
    public record Seed(String devicesResource) {
    }

    public List<Integer> tcpPortPool() {
        List<Integer> pool = new ArrayList<>();
        String spec = ingress == null ? null : ingress.tcpPortPool();
        if (spec == null || spec.isBlank()) {
            return pool;
        }
        for (String part : spec.split(",")) {
            String range = part.trim();
            int dash = range.indexOf('-');
            if (dash > 0) {
                int from = Integer.parseInt(range.substring(0, dash).trim());
                int to = Integer.parseInt(range.substring(dash + 1).trim());
                for (int p = from; p <= to; p++) {
                    pool.add(p);
                }
            } else if (!range.isEmpty()) {
                pool.add(Integer.parseInt(range));
            }
        }
        return pool;
    }
}
