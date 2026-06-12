"use client";

import { Fragment, useState } from "react";
import { Led } from "@/components/ui-custom/led";
import { formatClock, formatDuration, statusLed } from "@/lib/format";
import type { FeedItem } from "@/lib/types";
import { cn } from "@/lib/utils";

interface HistoryEvent {
  id: number;
  type: string;
  time: string | null;
  detail: string | null;
}

function DirectionGlyph({ direction }: { direction: FeedItem["direction"] }) {
  const outbound = direction === "CLOUD_TO_EDGE";
  return (
    <span
      className={cn(
        "readout inline-flex items-center gap-1 text-[10px] font-semibold tracking-wide",
        outbound ? "text-steel" : "text-signal-deep",
      )}
    >
      {outbound ? "CLOUD ▸ EDGE" : "EDGE ▸ CLOUD"}
    </span>
  );
}

function HistoryRows({ workflowId, runId }: { workflowId: string; runId?: string }) {
  const [events, setEvents] = useState<HistoryEvent[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (events === null && error === null) {
    const params = new URLSearchParams({ workflowId });
    if (runId) params.set("runId", runId);
    fetch(`/api/temporal/history?${params}`, { cache: "no-store" })
      .then(async (r) => {
        const j = await r.json();
        if (!r.ok) throw new Error(j?.error ?? r.statusText);
        setEvents(j.events);
      })
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }

  if (error) {
    return <p className="readout px-3 py-2 text-[11px] text-err">history unavailable: {error}</p>;
  }
  if (!events) {
    return <p className="readout px-3 py-2 text-[11px] text-ink-faint">loading event history…</p>;
  }
  return (
    <ol className="flex flex-col gap-0.5 px-3 py-2">
      {events.map((e) => (
        <li key={e.id} className="readout flex items-baseline gap-3 text-[11px]">
          <span className="w-6 shrink-0 text-right text-ink-faint">{e.id}</span>
          <span className="w-20 shrink-0 text-ink-faint">{formatClock(e.time)}</span>
          <span className="font-medium">{e.type}</span>
          {e.detail && <span className="truncate text-ink-faint">{e.detail}</span>}
        </li>
      ))}
    </ol>
  );
}

export function FeedTable({
  items,
  compact = false,
  emptyText = "no traffic yet — dispatch something",
}: {
  items: FeedItem[];
  compact?: boolean;
  emptyText?: string;
}) {
  const [expanded, setExpanded] = useState<string | null>(null);

  if (items.length === 0) {
    return <p className="readout py-4 text-center text-[11px] text-ink-faint">{emptyText}</p>;
  }

  return (
    <table className="w-full border-collapse">
      <thead>
        <tr className="border-b border-ink/60 text-left">
          <th className="w-5 pb-1.5" />
          <th className="readout pb-1.5 text-[9px] font-semibold uppercase tracking-[0.16em] text-ink-faint">
            Message
          </th>
          <th className="readout pb-1.5 text-[9px] font-semibold uppercase tracking-[0.16em] text-ink-faint">
            Path
          </th>
          {!compact && (
            <th className="readout pb-1.5 text-[9px] font-semibold uppercase tracking-[0.16em] text-ink-faint">
              Via
            </th>
          )}
          <th className="readout pb-1.5 text-right text-[9px] font-semibold uppercase tracking-[0.16em] text-ink-faint">
            Start
          </th>
          <th className="readout pb-1.5 text-right text-[9px] font-semibold uppercase tracking-[0.16em] text-ink-faint">
            Took
          </th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const key = `${item.kind}:${item.id}:${item.runId ?? ""}`;
          const expandable = item.kind === "workflow";
          const isOpen = expanded === key;
          return (
            <Fragment key={key}>
              <tr
                className={cn(
                  "border-b border-hairline/70 transition-colors",
                  expandable && "cursor-pointer hover:bg-accent/50",
                  isOpen && "bg-accent/50",
                )}
                onClick={() => expandable && setExpanded(isOpen ? null : key)}
                title={expandable ? "click for event history" : `standalone activity · ${item.status}`}
              >
                <td className="py-1.5">
                  <Led state={statusLed(item.status)} />
                </td>
                <td className="readout max-w-55 truncate py-1.5 pr-3 text-[12px] font-medium">
                  {item.id}
                </td>
                <td className="py-1.5 pr-3">
                  <DirectionGlyph direction={item.direction} />
                </td>
                {!compact && (
                  <td className="readout py-1.5 pr-3 text-[11px] text-ink-faint">
                    {item.kind === "workflow" ? `${item.type} workflow` : `${item.type} activity`}
                  </td>
                )}
                <td className="readout py-1.5 text-right text-[11px] text-ink-soft">
                  {formatClock(item.startTime)}
                </td>
                <td className="readout py-1.5 text-right text-[11px]">
                  {item.status === "RUNNING" ? (
                    <span className="text-signal">running</span>
                  ) : (
                    formatDuration(item.durationMs)
                  )}
                </td>
              </tr>
              {isOpen && (
                <tr className="border-b border-hairline bg-panel-sunken/60">
                  <td colSpan={compact ? 5 : 6}>
                    <HistoryRows workflowId={item.id} runId={item.runId} />
                  </td>
                </tr>
              )}
            </Fragment>
          );
        })}
      </tbody>
    </table>
  );
}
