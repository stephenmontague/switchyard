package com.proxyapp.routing;

/**
 * One message type as defined by the cloud-side profile. Shipped/managed by the cloud-app
 * operator; the customer never edits this layer.
 *
 * @param type            the message type key
 * @param direction       flow direction
 * @param codec           codec name used on the edge side, e.g. "json"
 * @param cloudEndpoint   for EDGE_TO_CLOUD types: path on the cloud base URL the proxy posts to
 * @param businessIdField field inside the decoded payload that carries the business id
 *                        (dedup handle); null falls back to a payload hash
 */
public record CatalogEntry(MessageType type, Direction direction, String codec,
                           String cloudEndpoint, String businessIdField) {
}
