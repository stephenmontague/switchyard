package com.proxyapp.routing;

import com.proxyapp.profile.WarehouseProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTemplateTest {

    @Test
    void materializeFillsChannelsFromSiteValues() {
        DeviceTemplate template = new WarehouseProfile().deviceTemplates().get(0);
        EdgeConfig device = template.materialize(new DeviceTemplate.SiteValues(
                "mhe-7", "http://10.1.2.3:8082", "10.1.2.3", 6000, 2222, "u", "p"));

        assertThat(device.deviceId()).isEqualTo("mhe-7");
        assertThat(device.bindings()).hasSize(6);
        assertThat(binding(device, "WAVE_RELEASE").channel()).isEqualTo(Channel.path("/pick-tasks"));
        assertThat(binding(device, "CONTAINER_PUTAWAY").channel()).isEqualTo(Channel.port(6000));
        assertThat(binding(device, "PUTAWAY_CONFIRM").channel()).isEqualTo(Channel.port(6001));
        assertThat(binding(device, "CYCLE_COUNT_REQ").channel()).isEqualTo(Channel.folder("cycle-count"));
    }

    @Test
    void materializedTemplateValidatesCleanlyAgainstItsProfile() {
        WarehouseProfile profile = new WarehouseProfile();
        EdgeConfig device = profile.deviceTemplates().get(0).materialize(new DeviceTemplate.SiteValues(
                "mhe-7", "http://10.1.2.3:8082", "10.1.2.3", 6000, 2222, "u", "p"));
        List<Integer> pool = IntStream.rangeClosed(6000, 6010).boxed().toList();

        assertThat(ConfigValidator.validate(profile.catalog(), pool, List.of(device))).isEmpty();
    }

    private static RouteBinding binding(EdgeConfig device, String type) {
        return device.bindings().stream()
                .filter(b -> b.messageType().value().equals(type))
                .findFirst().orElseThrow();
    }
}
