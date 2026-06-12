"use client";

import { Panel } from "@/components/ui-custom/panel";
import { LedLabel } from "@/components/ui-custom/led";
import { Stat } from "@/components/ui-custom/stat";
import { formatAgo } from "@/lib/format";
import type { ControlStateResponse } from "@/lib/types";

export function StatusPanel({ data }: { data: ControlStateResponse }) {
  const { state, liveness } = data;
  const applied = state.applied;
  const proxyUp = liveness.controlPollers > 0;
  const drifted = applied != null && applied.version !== state.version;
  const startedAgo = applied?.startedAt ? Date.now() - new Date(applied.startedAt).getTime() : null;

  return (
    <Panel legend="Proxy state">
      <div className="mb-4 flex items-center justify-between">
        <LedLabel
          state={!proxyUp ? "err" : state.enabled ? "ok" : "warn"}
          label={!proxyUp ? "Unreachable" : state.enabled ? "Enabled" : "Disabled"}
        />
        <span className="chip">{proxyUp ? `${liveness.controlPollers} poller(s)` : "0 pollers"}</span>
      </div>

      <div className="grid grid-cols-2 gap-x-4 gap-y-3">
        <Stat label="Desired config" value={`v${state.version}`} />
        <Stat
          label="Applied config"
          value={applied ? `v${applied.version}` : "—"}
          tone={drifted ? "warn" : "ok"}
        />
        <Stat label="Last seen" value={formatAgo(liveness.lastAccessAgoMs)} />
        <Stat label="Process uptime" value={startedAgo != null ? formatAgo(startedAgo).replace(" ago", "") : "—"} />
        <Stat
          label="Outbound worker"
          value={liveness.dataPollers > 0 ? "polling" : "paused"}
          tone={liveness.dataPollers > 0 ? "ok" : "warn"}
        />
        <Stat label="Devices" value={String(state.devices.length)} />
      </div>

      {drifted && (
        <p className="readout mt-3 border border-warn/40 bg-warn/10 px-2 py-1.5 text-[11px] text-warn">
          proxy is behind desired state — reconcile in progress
        </p>
      )}
      {state.lastError && (
        <p className="readout mt-3 border border-err/40 bg-err/10 px-2 py-1.5 text-[11px] leading-snug text-err">
          last rejection: {state.lastError}
        </p>
      )}
    </Panel>
  );
}
