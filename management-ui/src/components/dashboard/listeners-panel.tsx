"use client";

import { Panel } from "@/components/ui-custom/panel";
import type { ProxyControlState } from "@/lib/types";

function Group({ label, items }: { label: string; items: string[] }) {
  return (
    <div>
      <div className="rule-label mb-1.5">{label}</div>
      {items.length === 0 ? (
        <span className="readout text-[11px] text-ink-faint">none</span>
      ) : (
        <div className="flex flex-wrap gap-1.5">
          {items.map((item) => (
            <span key={item} className="chip">
              {item}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

/** What the proxy reports as actually live — not what we wish were live. */
export function ListenersPanel({ state }: { state: ProxyControlState }) {
  const applied = state.applied;
  return (
    <Panel legend="Applied listeners">
      {!applied ? (
        <p className="readout text-[11px] text-ink-faint">
          no applied-state report from the proxy yet
        </p>
      ) : (
        <div className="flex flex-col gap-3">
          <Group label="http paths" items={applied.httpPaths} />
          <Group label="tcp ports" items={applied.tcpPorts.map(String)} />
          <Group label="ftp folders" items={applied.ftpFolders} />
        </div>
      )}
    </Panel>
  );
}
