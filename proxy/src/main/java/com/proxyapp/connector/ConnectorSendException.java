package com.proxyapp.connector;

/** A failed outbound send; surfaces to Temporal as a retryable activity failure. */
public class ConnectorSendException extends RuntimeException {

    public ConnectorSendException(String message) {
        super(message);
    }

    public ConnectorSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
