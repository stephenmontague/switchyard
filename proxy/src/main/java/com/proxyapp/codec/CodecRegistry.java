package com.proxyapp.codec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Codecs by name; the catalog entry selects which codec a type uses. */
public final class CodecRegistry {

    private final Map<String, MessageCodec> codecs;

    public CodecRegistry(List<MessageCodec> codecs) {
        this.codecs = codecs.stream()
                .collect(Collectors.toMap(MessageCodec::name, Function.identity()));
    }

    public MessageCodec require(String name) {
        MessageCodec codec = codecs.get(name);
        if (codec == null) {
            throw new IllegalArgumentException(
                    "unknown codec '" + name + "', available: " + codecs.keySet());
        }
        return codec;
    }
}
