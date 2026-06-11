package com.dummyedge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP channel of the device: receives CONTAINER_PUTAWAY on its listen port, acks, and
 * auto-pushes the paired PUTAWAY_CONFIRM to the proxy's TCP ingress port.
 */
@Component
public class TcpDeviceServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TcpDeviceServer.class);

    private final EdgeProperties properties;
    private final ReceivedStore receivedStore;
    private final ConfirmPusher confirmPusher;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "edge-tcp");
        t.setDaemon(true);
        return t;
    });
    private ServerSocket serverSocket;

    public TcpDeviceServer(EdgeProperties properties, ReceivedStore receivedStore,
                           ConfirmPusher confirmPusher) {
        this.properties = properties;
        this.receivedStore = receivedStore;
        this.confirmPusher = confirmPusher;
    }

    @Override
    public void start() {
        try {
            serverSocket = new ServerSocket(properties.tcpListenPort());
        } catch (IOException e) {
            throw new IllegalStateException("cannot open device TCP port " + properties.tcpListenPort(), e);
        }
        executor.execute(this::acceptLoop);
        log.info("device TCP channel listening on port {}", properties.tcpListenPort());
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                executor.execute(() -> handle(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    log.warn("device TCP accept failed: {}", e.getMessage());
                }
                return;
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            socket.setSoTimeout(10_000);
            String payload = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("device received putaway over TCP: {}", payload.trim());
            receivedStore.add("TCP", String.valueOf(properties.tcpListenPort()), payload);

            socket.getOutputStream().write("ACK\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            JsonNode body = mapper.readTree(payload);
            ObjectNode confirm = mapper.createObjectNode();
            confirm.set("containerId", body.get("containerId"));
            confirm.put("status", "PUTAWAY_COMPLETE");
            confirmPusher.pushTcpPutawayConfirm(confirm.toString());
        } catch (IOException e) {
            log.warn("device TCP connection failed: {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // closing is best-effort
        }
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return serverSocket != null && !serverSocket.isClosed();
    }
}
