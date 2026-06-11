package com.dummyedge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edge")
public record EdgeProperties(Proxy proxy, int tcpListenPort, int ftpListenPort, String ftpRoot,
                             String ftpUser, String ftpPassword, String cycleCountFolder,
                             long confirmDelayMs) {

    public record Proxy(String httpBase, String pickConfirmPath, String tcpHost,
                        int putawayConfirmPort, String ftpHost, int ftpPort, String ftpUser,
                        String ftpPassword, String cycleCountConfirmFolder) {
    }
}
