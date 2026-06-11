package com.proxyapp.ingress;

/** An inbound delivery the gateway cannot enqueue; the reason maps to a transport-level nack. */
public class IngressException extends RuntimeException {

    public enum Reason {
        /** Install is soft-disabled; edge should retry later. */
        DISABLED,
        /** No inbound binding for this channel. */
        UNKNOWN_CHANNEL,
        /** Multi-type channel and the resolver could not determine a type. */
        UNRESOLVED_TYPE
    }

    private final Reason reason;

    public IngressException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
