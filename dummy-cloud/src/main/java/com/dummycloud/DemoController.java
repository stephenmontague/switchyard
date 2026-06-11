package com.dummycloud;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Demo drivers: dispatch any outbound message type through the proxy. */
@RestController
public class DemoController {

    private final OutboundDispatcher dispatcher;
    private final ConfirmStore confirmStore;

    public DemoController(OutboundDispatcher dispatcher, ConfirmStore confirmStore) {
        this.dispatcher = dispatcher;
        this.confirmStore = confirmStore;
    }

    @PostMapping("/demo/wave-release")
    public Map<String, Object> waveRelease(@RequestBody JsonNode body) {
        return dispatcher.dispatch(WarehouseCatalog.WAVE_RELEASE, body);
    }

    @PostMapping("/demo/putaway")
    public Map<String, Object> putaway(@RequestBody JsonNode body) {
        return dispatcher.dispatch(WarehouseCatalog.CONTAINER_PUTAWAY, body);
    }

    @PostMapping("/demo/cycle-count")
    public Map<String, Object> cycleCount(@RequestBody JsonNode body) {
        return dispatcher.dispatch(WarehouseCatalog.CYCLE_COUNT_REQ, body);
    }

    @GetMapping("/demo/confirms")
    public List<CanonicalMessage> confirms() {
        return confirmStore.all();
    }
}
