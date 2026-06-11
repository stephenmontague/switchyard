package com.proxyapp.routing;

import com.proxyapp.profile.WarehouseProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigValidatorTest {

    private final MessageCatalog catalog = new WarehouseProfile().catalog();
    private final List<Integer> pool = IntStream.rangeClosed(6000, 6010).boxed().toList();

    private EdgeConfig validDevice() {
        return new EdgeConfig("mhe-1", "http://edge:8082", "10.0.0.5", 2222, "u", "p", List.of(
                new RouteBinding(WarehouseProfile.WAVE_RELEASE, Transport.HTTP, Channel.path("/pick-tasks")),
                new RouteBinding(WarehouseProfile.PICK_CONFIRM, Transport.HTTP, Channel.path("/pick-confirm")),
                new RouteBinding(WarehouseProfile.PUTAWAY_CONFIRM, Transport.TCP, Channel.port(6001))));
    }

    @Test
    void validConfigPasses() {
        assertThat(ConfigValidator.validate(catalog, pool, List.of(validDevice()))).isEmpty();
    }

    @Test
    void inboundTcpPortMustBeInPool() {
        EdgeConfig device = new EdgeConfig("mhe-1", null, null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.PUTAWAY_CONFIRM, Transport.TCP, Channel.port(7777))));
        List<String> errors = ConfigValidator.validate(catalog, pool, List.of(device));
        assertThat(errors).singleElement().asString()
                .contains("7777").contains("port pool");
    }

    @Test
    void inboundChannelCollisionAcrossDevicesIsRejected() {
        EdgeConfig a = new EdgeConfig("a", null, null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.PUTAWAY_CONFIRM, Transport.TCP, Channel.port(6001))));
        EdgeConfig b = new EdgeConfig("b", null, null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.PICK_CONFIRM, Transport.TCP, Channel.port(6001))));
        List<String> errors = ConfigValidator.validate(catalog, pool, List.of(a, b));
        assertThat(errors).singleElement().asString().contains("collision");
    }

    @Test
    void sameChannelValueOnDifferentTransportsDoesNotCollide() {
        EdgeConfig device = new EdgeConfig("a", "http://e", null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.PICK_CONFIRM, Transport.HTTP, Channel.path("/confirm")),
                new RouteBinding(WarehouseProfile.CYCLE_COUNT_CONFIRM, Transport.FTP, Channel.folder("/confirm"))));
        assertThat(ConfigValidator.validate(catalog, pool, List.of(device))).isEmpty();
    }

    @Test
    void unknownMessageTypeIsRejected() {
        EdgeConfig device = new EdgeConfig("a", "http://e", null, null, null, null, List.of(
                new RouteBinding(MessageType.of("MYSTERY"), Transport.HTTP, Channel.path("/x"))));
        List<String> errors = ConfigValidator.validate(catalog, pool, List.of(device));
        assertThat(errors).singleElement().asString().contains("unknown message type MYSTERY");
    }

    @Test
    void transportAndChannelKindMustAgree() {
        EdgeConfig device = new EdgeConfig("a", "http://e", null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.WAVE_RELEASE, Transport.TCP, Channel.path("/x"))));
        List<String> errors = ConfigValidator.validate(catalog, pool, List.of(device));
        assertThat(errors).singleElement().asString().contains("requires a PORT channel");
    }

    @Test
    void outboundBindingsRequireDeviceInfrastructure() {
        EdgeConfig noBaseUrl = new EdgeConfig("a", null, null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.WAVE_RELEASE, Transport.HTTP, Channel.path("/x"))));
        assertThat(ConfigValidator.validate(catalog, pool, List.of(noBaseUrl)))
                .singleElement().asString().contains("baseUrl");

        EdgeConfig noHost = new EdgeConfig("b", null, null, null, null, null, List.of(
                new RouteBinding(WarehouseProfile.CONTAINER_PUTAWAY, Transport.TCP, Channel.port(9001))));
        assertThat(ConfigValidator.validate(catalog, pool, List.of(noHost)))
                .singleElement().asString().contains("host");

        // outbound TCP ports are on the device, not the proxy: pool does not apply
        EdgeConfig outboundPortOutsidePool = new EdgeConfig("c", null, "10.0.0.9", null, null, null,
                List.of(new RouteBinding(WarehouseProfile.CONTAINER_PUTAWAY, Transport.TCP, Channel.port(9001))));
        assertThat(ConfigValidator.validate(catalog, pool, List.of(outboundPortOutsidePool))).isEmpty();
    }

    @Test
    void duplicateDeviceIdsAreRejected() {
        List<String> errors = ConfigValidator.validate(catalog, pool,
                List.of(validDevice(), validDevice()));
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("duplicate deviceId"));
    }

    @Test
    void multiTypeResolverOnlyAllowedOnFtp() {
        EdgeConfig device = new EdgeConfig("a", "http://e", null, null, null, null, List.of(
                new RouteBinding(null, Transport.HTTP, Channel.path("/mixed"),
                        new ResolverConfig("filename-pattern", java.util.Map.of()))));
        List<String> errors = ConfigValidator.validate(catalog, pool, List.of(device));
        assertThat(errors).singleElement().asString().contains("only supported on FTP");
    }
}
