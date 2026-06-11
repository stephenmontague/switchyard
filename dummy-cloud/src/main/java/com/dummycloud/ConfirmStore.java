package com.dummycloud;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** In-memory store of inbound messages the proxy delivered, for demo verification. */
@Component
public class ConfirmStore {

    private final ConcurrentLinkedQueue<CanonicalMessage> received = new ConcurrentLinkedQueue<>();

    public void add(CanonicalMessage message) {
        received.add(message);
    }

    public List<CanonicalMessage> all() {
        return List.copyOf(received);
    }
}
