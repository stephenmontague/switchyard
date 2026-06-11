package com.proxyapp.ingress;

import com.proxyapp.routing.Transport;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP ingress: the listen port is the channel. Accept loops run on a dedicated executor
 * (never Tomcat or Temporal poller threads); the Reconciler opens/closes ports as config
 * changes. Protocol: device writes the payload and half-closes; we reply
 * {@code ACK <activityId>} only after Temporal accepted the enqueue, else {@code ERR ...}.
 */
public class TcpSocketServer {

    private static final Logger log = LoggerFactory.getLogger(TcpSocketServer.class);

    private final InboundGateway gateway;
    private final AtomicInteger threadSeq = new AtomicInteger();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tcp-ingress-" + threadSeq.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    private final Map<Integer, ServerSocket> listeners = new ConcurrentHashMap<>();

    public TcpSocketServer(InboundGateway gateway) {
        this.gateway = gateway;
    }

    public synchronized void reconcile(Set<Integer> desiredPorts) {
        for (Integer port : new HashSet<>(listeners.keySet())) {
            if (!desiredPorts.contains(port)) {
                closeListener(port);
            }
        }
        for (Integer port : desiredPorts) {
            listeners.computeIfAbsent(port, this::openListener);
        }
    }

    public Set<Integer> activePorts() {
        return Set.copyOf(listeners.keySet());
    }

    private ServerSocket openListener(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            executor.execute(() -> acceptLoop(serverSocket, port));
            log.info("TCP ingress listening on port {}", port);
            return serverSocket;
        } catch (IOException e) {
            log.error("cannot open TCP ingress port {}: {}", port, e.getMessage());
            return null;
        }
    }

    private void closeListener(int port) {
        ServerSocket serverSocket = listeners.remove(port);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // closing is best-effort
            }
            log.info("TCP ingress port {} closed", port);
        }
    }

    private void acceptLoop(ServerSocket serverSocket, int port) {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                executor.execute(() -> handleConnection(socket, port));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    log.warn("accept failed on TCP port {}: {}", port, e.getMessage());
                }
                return;
            }
        }
    }

    private void handleConnection(Socket socket, int port) {
        try (socket) {
            socket.setSoTimeout(15_000);
            byte[] raw = socket.getInputStream().readAllBytes();
            String reply;
            try {
                InboundGateway.EnqueueResult result =
                        gateway.handle(Transport.TCP, Integer.toString(port), null, raw);
                reply = "ACK " + result.activityId() + "\n";
            } catch (IngressException e) {
                reply = "ERR " + e.reason() + " " + e.getMessage() + "\n";
            } catch (Exception e) {
                log.error("TCP ingress on port {} failed", port, e);
                reply = "ERR INTERNAL\n";
            }
            socket.getOutputStream().write(reply.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
        } catch (IOException e) {
            log.warn("TCP connection on port {} dropped: {}", port, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        reconcile(Set.of());
        executor.shutdownNow();
    }
}
