"use client";

// The console centerpiece: a live schematic of the data path. Animated dashes flow when
// the corresponding leg is healthy; the on-prem firewall is drawn explicitly because the
// whole architecture exists to never open a hole in it.

type FlowProps = {
  cloudUp: boolean; // dummy-cloud reachable (demo dispatch path)
  proxyUp: boolean; // control-queue pollers seen recently
  enabled: boolean; // data plane enabled (soft switch)
  restartPending: boolean;
};

function Node({
  x,
  label,
  sub,
  state,
}: {
  x: number;
  label: string;
  sub: string;
  state: "ok" | "warn" | "err" | "busy";
}) {
  const ledFill =
    state === "ok" ? "var(--ok)" : state === "warn" ? "var(--warn)" : state === "busy" ? "var(--signal)" : "var(--err)";
  return (
    <g transform={`translate(${x}, 28)`}>
      <rect width="168" height="64" fill="var(--panel)" stroke="var(--ink)" strokeWidth="1.5" />
      <rect x="4" y="4" width="160" height="56" fill="none" stroke="var(--hairline)" strokeWidth="1" />
      <circle cx="20" cy="32" r="5" fill={ledFill} stroke="var(--ink)" strokeWidth="1">
        {state === "busy" && (
          <animate attributeName="opacity" values="1;0.25;1" dur="0.9s" repeatCount="indefinite" />
        )}
      </circle>
      <text x="36" y="29" fontFamily="var(--font-plex-mono)" fontSize="12" fontWeight="600" fill="var(--ink)" letterSpacing="1.5">
        {label}
      </text>
      <text x="36" y="45" fontFamily="var(--font-plex-mono)" fontSize="8.5" fill="var(--ink-faint)" letterSpacing="1">
        {sub}
      </text>
    </g>
  );
}

function Leg({ x1, x2, active, broken }: { x1: number; x2: number; active: boolean; broken: boolean }) {
  const y = 60;
  if (broken) {
    return (
      <g>
        <line x1={x1} y1={y} x2={x2} y2={y} stroke="var(--err)" strokeWidth="1.5" className="flow-line-static" />
        <text x={(x1 + x2) / 2} y={y - 8} textAnchor="middle" fontFamily="var(--font-plex-mono)" fontSize="11" fontWeight="700" fill="var(--err)">
          ✕
        </text>
      </g>
    );
  }
  return (
    <line
      x1={x1}
      y1={y}
      x2={x2}
      y2={y}
      stroke={active ? "var(--signal)" : "var(--ink-faint)"}
      strokeWidth="1.5"
      className={active ? "flow-line" : "flow-line-static"}
    />
  );
}

export function FlowDiagram({ cloudUp, proxyUp, enabled, restartPending }: FlowProps) {
  const proxyState = restartPending ? "busy" : !proxyUp ? "err" : enabled ? "ok" : "warn";
  const proxySub = restartPending
    ? "restarting…"
    : !proxyUp
      ? "unreachable"
      : enabled
        ? "worker polling"
        : "soft-disabled";
  return (
    <svg viewBox="0 0 920 120" className="w-full" role="img" aria-label="Cloud to edge data path">
      {/* firewall boundary */}
      <line x1="575" y1="6" x2="575" y2="114" stroke="var(--ink)" strokeWidth="1" strokeDasharray="2 4" />
      <text x="575" y="14" textAnchor="middle" fontFamily="var(--font-plex-mono)" fontSize="8" fill="var(--ink-faint)" letterSpacing="2">
        ON-PREM FIREWALL
      </text>
      <text x="583" y="78" fontFamily="var(--font-plex-mono)" fontSize="7.5" fill="var(--ink-faint)" letterSpacing="1">
        egress only
      </text>

      <Leg x1={188} x2={250} active={cloudUp} broken={!cloudUp} />
      <Leg x1={418} x2={630} active={proxyUp && enabled} broken={!proxyUp} />
      <Leg x1={798} x2={850} active={proxyUp && enabled} broken={false} />
      {/* edge stub continues off-panel */}
      <line x1="850" y1="60" x2="912" y2="60" stroke="var(--hairline)" strokeWidth="1.5" />

      <Node x={20} label="CLOUD APP" sub={cloudUp ? "dispatching" : "offline"} state={cloudUp ? "ok" : "err"} />
      <Node x={250} label="TEMPORAL" sub="durable backbone" state="ok" />
      <Node x={630} label="PROXY" sub={proxySub} state={proxyState} />
      <g transform="translate(850, 44)">
        <rect width="62" height="32" fill="var(--panel-sunken)" stroke="var(--ink)" strokeWidth="1" />
        <text x="31" y="20" textAnchor="middle" fontFamily="var(--font-plex-mono)" fontSize="9" fill="var(--ink-soft)" letterSpacing="1">
          EDGE
        </text>
      </g>
    </svg>
  );
}
