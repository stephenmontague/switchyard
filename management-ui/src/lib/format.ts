// Shared display helpers (client-safe — no Temporal imports).

export function formatDuration(ms: number | null | undefined): string {
  if (ms == null || !Number.isFinite(ms)) return "—";
  if (ms < 1000) return `${Math.round(ms)}ms`;
  if (ms < 60_000) return `${(ms / 1000).toFixed(1)}s`;
  const mins = Math.floor(ms / 60_000);
  if (mins < 60) return `${mins}m ${Math.round((ms % 60_000) / 1000)}s`;
  const hours = Math.floor(mins / 60);
  return `${hours}h ${mins % 60}m`;
}

export function formatClock(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export function formatAgo(ms: number | null | undefined): string {
  if (ms == null || !Number.isFinite(ms)) return "—";
  if (ms < 2000) return "just now";
  if (ms < 60_000) return `${Math.round(ms / 1000)}s ago`;
  if (ms < 3_600_000) return `${Math.round(ms / 60_000)}m ago`;
  return `${(ms / 3_600_000).toFixed(1)}h ago`;
}

export type LedState = "ok" | "warn" | "err" | "busy" | "off";

export function statusLed(status: string): LedState {
  switch (status.toUpperCase()) {
    case "COMPLETED":
      return "ok";
    case "RUNNING":
      return "busy";
    case "FAILED":
    case "TERMINATED":
    case "TIMED_OUT":
    case "TIMEDOUT":
      return "err";
    case "CANCELED":
    case "CANCELLED":
      return "warn";
    default:
      return "off";
  }
}

/** EVENT_TYPE_ACTIVITY_TASK_SCHEDULED -> "Activity Task Scheduled" */
export function prettyConstant(name: string, stripPrefix?: string): string {
  let s = name;
  if (stripPrefix && s.startsWith(stripPrefix)) s = s.slice(stripPrefix.length);
  return s
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(" ");
}
