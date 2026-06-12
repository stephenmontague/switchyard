package com.proxyapp.codec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Short, stable hash of a payload, used as a dedup fallback when a codec can't extract a
 * business id. Same bytes in → same id out, so duplicate edge pushes still collapse to one
 * activity execution.
 */
final class ContentHash {

    private ContentHash() {
    }

    static String of(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
