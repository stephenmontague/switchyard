package com.proxyapp.connector;

import com.proxyapp.routing.Transport;

/**
 * Outbound transport SPI. Sends run inside Temporal activities: they are retried
 * at-least-once, so every implementation must tolerate redelivery of the same payload.
 */
public interface Connector {

    Transport transport();

    void send(ChannelTarget target, byte[] payload);
}
