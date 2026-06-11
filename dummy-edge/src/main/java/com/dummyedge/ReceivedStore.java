package com.dummyedge;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Everything the device received from the proxy, for demo verification. */
@Component
public class ReceivedStore {

    private final ConcurrentLinkedQueue<Map<String, String>> received = new ConcurrentLinkedQueue<>();

    public void add(String transport, String channel, String payload) {
        received.add(Map.of("transport", transport, "channel", channel, "payload", payload));
    }

    public List<Map<String, String>> all() {
        return List.copyOf(received);
    }
}
