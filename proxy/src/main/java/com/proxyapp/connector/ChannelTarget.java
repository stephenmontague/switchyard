package com.proxyapp.connector;

/** Where an outbound send goes, expressed per transport. */
public sealed interface ChannelTarget {

    record HttpTarget(String url) implements ChannelTarget {
    }

    record TcpTarget(String host, int port) implements ChannelTarget {
    }

    /**
     * @param filename deterministic per-delivery name (the activity id) so an activity
     *                 retry overwrites the same remote file instead of duplicating it
     */
    record FtpTarget(String host, int port, String user, String password,
                     String folder, String filename) implements ChannelTarget {
    }
}
