package com.proxyapp.connector;

import com.proxyapp.routing.Transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Raw TCP framing: connect, write the payload, half-close, then read the device's ack until
 * EOF. A missing/invalid ack fails the send so the activity retries — raw TCP has no
 * store-and-forward of its own.
 */
public class TcpConnector implements Connector {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    @Override
    public Transport transport() {
        return Transport.TCP;
    }

    @Override
    public void send(ChannelTarget target, byte[] payload) {
        ChannelTarget.TcpTarget tcp = (ChannelTarget.TcpTarget) target;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(tcp.host(), tcp.port()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            socket.getOutputStream().write(payload);
            socket.getOutputStream().flush();
            socket.shutdownOutput();
            String ack = readAll(socket.getInputStream());
            if (!ack.startsWith("ACK")) {
                throw new ConnectorSendException("TCP " + tcp.host() + ":" + tcp.port()
                        + " did not ack (got '" + ack.trim() + "')");
            }
        } catch (IOException e) {
            throw new ConnectorSendException("TCP send to " + tcp.host() + ":" + tcp.port() + " failed", e);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
