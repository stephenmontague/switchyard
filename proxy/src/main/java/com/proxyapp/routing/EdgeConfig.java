package com.proxyapp.routing;

import java.util.List;

/**
 * One edge target (device/machine/network endpoint) and its routing bindings.
 * Site-infrastructure fields ({@code baseUrl}, {@code host}, FTP credentials) are set once
 * at install time; {@code bindings} is the ops-editable layer.
 *
 * @param deviceId    unique id of this edge target within the install
 * @param baseUrl     HTTP base URL of the device (outbound HTTP), e.g. http://192.168.1.50:8082
 * @param host        host/IP for raw TCP and FTP (outbound)
 * @param ftpPort     device FTP port (outbound FTP), null if unused
 * @param ftpUser     device FTP user (outbound FTP)
 * @param ftpPassword device FTP password (outbound FTP)
 * @param bindings    per-message-type channel bindings
 */
public record EdgeConfig(String deviceId, String baseUrl, String host, Integer ftpPort,
                         String ftpUser, String ftpPassword, List<RouteBinding> bindings) {

    public List<RouteBinding> bindings() {
        return bindings == null ? List.of() : bindings;
    }
}
