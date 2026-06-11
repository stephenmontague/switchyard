package com.proxyapp.routing;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilenamePatternResolverTest {

    private final FilenamePatternResolver resolver = new FilenamePatternResolver();

    private MessageTypeResolver.InboundContext context(String filename) {
        return new MessageTypeResolver.InboundContext(Transport.FTP, "mixed", filename, new byte[0]);
    }

    @Test
    void firstMatchingPatternWins() {
        Map<String, String> patterns = new LinkedHashMap<>();
        patterns.put("PC-.*\\.json", "PICK_CONFIRM");
        patterns.put("PA-.*\\.json", "PUTAWAY_CONFIRM");
        ResolverConfig config = new ResolverConfig(FilenamePatternResolver.KIND, patterns);

        assertThat(resolver.resolve(config, context("PC-1001.json")))
                .contains(MessageType.of("PICK_CONFIRM"));
        assertThat(resolver.resolve(config, context("PA-7.json")))
                .contains(MessageType.of("PUTAWAY_CONFIRM"));
        assertThat(resolver.resolve(config, context("other.txt"))).isEmpty();
    }

    @Test
    void noFilenameMeansNoResolution() {
        ResolverConfig config = new ResolverConfig(FilenamePatternResolver.KIND,
                Map.of(".*", "PICK_CONFIRM"));
        assertThat(resolver.resolve(config, context(null))).isEmpty();
    }
}
