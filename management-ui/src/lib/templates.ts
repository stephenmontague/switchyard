import type { ChannelKind, EdgeConfig, RouteBinding, Transport } from "@/lib/types";

// Mirrors the proxy's built-in device templates (com.proxyapp.profile.WarehouseProfile).
// A template pre-fills message types, transports, and channel layout; the wizard only
// asks for site values. PORT channels compute as basePort + portOffset, but every
// computed channel stays editable before review.

export interface TemplateBinding {
  messageType: string;
  transport: Transport;
  kind: ChannelKind;
  value: string | null;
  portOffset: number;
}

export interface DeviceTemplateDef {
  id: string;
  name: string;
  description: string;
  bindings: TemplateBinding[];
}

export const DEVICE_TEMPLATES: DeviceTemplateDef[] = [
  {
    id: "warehouse-mhe-standard",
    name: "Standard MHE controller",
    description:
      "Typical material-handling layout: picks over HTTP, putaway over raw TCP, cycle counts via FTP folders.",
    bindings: [
      { messageType: "WAVE_RELEASE", transport: "HTTP", kind: "PATH", value: "/pick-tasks", portOffset: 0 },
      { messageType: "PICK_CONFIRM", transport: "HTTP", kind: "PATH", value: "/pick-confirm", portOffset: 0 },
      { messageType: "CONTAINER_PUTAWAY", transport: "TCP", kind: "PORT", value: null, portOffset: 0 },
      { messageType: "PUTAWAY_CONFIRM", transport: "TCP", kind: "PORT", value: null, portOffset: 1 },
      { messageType: "CYCLE_COUNT_REQ", transport: "FTP", kind: "FOLDER", value: "cycle-count", portOffset: 0 },
      { messageType: "CYCLE_COUNT_CONFIRM", transport: "FTP", kind: "FOLDER", value: "cycle-count-confirm", portOffset: 0 },
    ],
  },
];

export interface SiteValues {
  deviceId: string;
  baseUrl: string;
  host: string;
  basePort: number;
  ftpPort: number | null;
  ftpUser: string;
  ftpPassword: string;
}

export function materialize(template: DeviceTemplateDef, site: SiteValues): EdgeConfig {
  const bindings: RouteBinding[] = template.bindings.map((b) => ({
    messageType: b.messageType,
    transport: b.transport,
    channel: {
      kind: b.kind,
      value: b.kind === "PORT" ? String(site.basePort + b.portOffset) : (b.value ?? ""),
    },
    resolver: null,
  }));
  const usesFtp = template.bindings.some((b) => b.transport === "FTP");
  return {
    deviceId: site.deviceId,
    baseUrl: site.baseUrl || null,
    host: site.host || null,
    ftpPort: usesFtp ? site.ftpPort : null,
    ftpUser: usesFtp && site.ftpUser ? site.ftpUser : null,
    ftpPassword: usesFtp && site.ftpPassword ? site.ftpPassword : null,
    bindings,
  };
}
