package com.proxyapp.profile;

import com.proxyapp.routing.CatalogEntry;
import com.proxyapp.routing.ChannelKind;
import com.proxyapp.routing.DeviceTemplate;
import com.proxyapp.routing.DeviceTemplate.TemplateBinding;
import com.proxyapp.routing.Direction;
import com.proxyapp.routing.MessageCatalog;
import com.proxyapp.routing.MessageType;
import com.proxyapp.routing.Transport;

import java.util.List;

/**
 * The reference/demo profile: a cloud WMS talking to on-prem MHE. This is an example of a
 * profile, not part of the core — swap it and the same proxy connects anything to anything.
 */
public final class WarehouseProfile implements Profile {

    public static final String NAME = "warehouse";

    public static final MessageType CONTAINER_PUTAWAY = MessageType.of("CONTAINER_PUTAWAY");
    public static final MessageType PUTAWAY_CONFIRM = MessageType.of("PUTAWAY_CONFIRM");
    public static final MessageType WAVE_RELEASE = MessageType.of("WAVE_RELEASE");
    public static final MessageType PICK_CONFIRM = MessageType.of("PICK_CONFIRM");
    public static final MessageType CYCLE_COUNT_REQ = MessageType.of("CYCLE_COUNT_REQ");
    public static final MessageType CYCLE_COUNT_CONFIRM = MessageType.of("CYCLE_COUNT_CONFIRM");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public MessageCatalog catalog() {
        return new MessageCatalog(List.of(
                new CatalogEntry(CONTAINER_PUTAWAY, Direction.CLOUD_TO_EDGE, "json", null, "containerId"),
                new CatalogEntry(PUTAWAY_CONFIRM, Direction.EDGE_TO_CLOUD, "json", "/api/putaway-confirm", "containerId"),
                new CatalogEntry(WAVE_RELEASE, Direction.CLOUD_TO_EDGE, "json", null, "orderId"),
                new CatalogEntry(PICK_CONFIRM, Direction.EDGE_TO_CLOUD, "json", "/api/pick-confirm", "orderId"),
                new CatalogEntry(CYCLE_COUNT_REQ, Direction.CLOUD_TO_EDGE, "json", null, "countId"),
                new CatalogEntry(CYCLE_COUNT_CONFIRM, Direction.EDGE_TO_CLOUD, "json", "/api/cycle-count-confirm", "countId")));
    }

    @Override
    public List<DeviceTemplate> deviceTemplates() {
        // Typical MHE layout: picks over HTTP, putaway over raw TCP, counts via FTP folders.
        return List.of(new DeviceTemplate("warehouse-mhe-standard", "Standard MHE controller",
                List.of(
                        new TemplateBinding(WAVE_RELEASE, Transport.HTTP, ChannelKind.PATH, "/pick-tasks", 0),
                        new TemplateBinding(PICK_CONFIRM, Transport.HTTP, ChannelKind.PATH, "/pick-confirm", 0),
                        new TemplateBinding(CONTAINER_PUTAWAY, Transport.TCP, ChannelKind.PORT, null, 0),
                        new TemplateBinding(PUTAWAY_CONFIRM, Transport.TCP, ChannelKind.PORT, null, 1),
                        new TemplateBinding(CYCLE_COUNT_REQ, Transport.FTP, ChannelKind.FOLDER, "cycle-count", 0),
                        new TemplateBinding(CYCLE_COUNT_CONFIRM, Transport.FTP, ChannelKind.FOLDER, "cycle-count-confirm", 0))));
    }
}
