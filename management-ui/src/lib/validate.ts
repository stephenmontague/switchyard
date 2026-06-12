import type { Direction, EdgeConfig, RouteBinding } from "@/lib/types";

// Mirrors com.proxyapp.routing.ConfigValidator so the wizard can reject a bad config
// before signaling. The control workflow re-validates authoritatively on receipt.

const EXPECTED_KIND: Record<string, string> = {
  HTTP: "PATH",
  TCP: "PORT",
  FTP: "FOLDER",
};

export function validateConfig(
  typeDirections: Record<string, Direction>,
  tcpPortPool: number[],
  devices: EdgeConfig[],
): string[] {
  const errors: string[] = [];
  const deviceIds = new Set<string>();
  const inboundChannelOwners = new Map<string, string>();
  const pool = new Set(tcpPortPool);

  for (const device of devices) {
    if (!device.deviceId || device.deviceId.trim() === "") {
      errors.push("device with missing deviceId");
      continue;
    }
    if (deviceIds.has(device.deviceId)) {
      errors.push(`duplicate deviceId: ${device.deviceId}`);
    }
    deviceIds.add(device.deviceId);
    for (const binding of device.bindings) {
      validateBinding(typeDirections, pool, inboundChannelOwners, device, binding, errors);
    }
  }
  return errors;
}

function validateBinding(
  typeDirections: Record<string, Direction>,
  pool: Set<number>,
  inboundChannelOwners: Map<string, string>,
  device: EdgeConfig,
  binding: RouteBinding,
  errors: string[],
): void {
  const id = device.deviceId;
  if (!binding.transport || !binding.channel) {
    errors.push(`${id}: binding missing transport or channel`);
    return;
  }
  const expectedKind = EXPECTED_KIND[binding.transport];
  if (binding.channel.kind !== expectedKind) {
    errors.push(
      `${id}: ${binding.transport} binding requires a ${expectedKind} channel, got ${binding.channel.kind}`,
    );
    return;
  }

  const isMultiType = binding.messageType == null && binding.resolver != null;
  if (isMultiType) {
    if (binding.transport !== "FTP") {
      errors.push(`${id}: multi-type resolver bindings are only supported on FTP folders`);
    }
    claimInbound(inboundChannelOwners, device, binding, `multi-type:${binding.channel.value}`, errors);
    return;
  }

  if (binding.messageType == null) {
    errors.push(`${id}: binding missing messageType`);
    return;
  }
  const direction = typeDirections[binding.messageType];
  if (!direction) {
    errors.push(`${id}: unknown message type ${binding.messageType}`);
    return;
  }

  if (direction === "EDGE_TO_CLOUD") {
    if (binding.transport === "TCP") {
      const port = Number(binding.channel.value);
      if (!pool.has(port)) {
        const sorted = [...pool].sort((a, b) => a - b);
        errors.push(
          `${id}: inbound TCP port ${port} for ${binding.messageType} is outside the available port pool [${sorted.join(", ")}]`,
        );
      }
    }
    claimInbound(inboundChannelOwners, device, binding, binding.messageType, errors);
  } else {
    switch (binding.transport) {
      case "HTTP":
        if (!device.baseUrl || device.baseUrl.trim() === "") {
          errors.push(
            `${id}: outbound HTTP binding for ${binding.messageType} requires the device baseUrl`,
          );
        }
        break;
      case "TCP":
        if (!device.host || device.host.trim() === "") {
          errors.push(
            `${id}: outbound TCP binding for ${binding.messageType} requires the device host`,
          );
        }
        break;
      case "FTP":
        if (!device.host || device.host.trim() === "" || device.ftpPort == null) {
          errors.push(
            `${id}: outbound FTP binding for ${binding.messageType} requires the device host and ftpPort`,
          );
        }
        break;
    }
  }
}

function claimInbound(
  owners: Map<string, string>,
  device: EdgeConfig,
  binding: RouteBinding,
  claimant: string,
  errors: string[],
): void {
  // Inbound channels are proxy-wide resources: one channel carries exactly one type.
  const key = `${binding.transport}|${binding.channel.value}`;
  const previous = owners.get(key);
  if (previous !== undefined) {
    errors.push(
      `inbound channel collision on ${binding.transport} ${binding.channel.kind}=${binding.channel.value}: already used by ${previous}`,
    );
  } else {
    owners.set(key, `${device.deviceId}/${claimant}`);
  }
}
