package com.dummyedge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * HTTP channel of the device: receives WAVE_RELEASE on /pick-tasks and auto-pushes the
 * paired PICK_CONFIRM back to the proxy's /pick-confirm channel.
 */
@RestController
public class HttpDeviceController {

    private static final Logger log = LoggerFactory.getLogger(HttpDeviceController.class);

    private final ReceivedStore receivedStore;
    private final ConfirmPusher confirmPusher;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpDeviceController(ReceivedStore receivedStore, ConfirmPusher confirmPusher) {
        this.receivedStore = receivedStore;
        this.confirmPusher = confirmPusher;
    }

    @PostMapping("/pick-tasks")
    public Map<String, String> pickTasks(@RequestBody JsonNode body) {
        log.info("device received pick tasks: {}", body);
        receivedStore.add("HTTP", "/pick-tasks", body.toString());

        ObjectNode confirm = mapper.createObjectNode();
        confirm.set("orderId", body.get("orderId"));
        confirm.put("status", "PICKED");
        confirm.set("items", body.get("items"));
        confirmPusher.pushHttpPickConfirm(confirm.toString());
        return Map.of("status", "accepted");
    }

    @GetMapping("/received")
    public List<Map<String, String>> received() {
        return receivedStore.all();
    }
}
