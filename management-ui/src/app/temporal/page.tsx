"use client";

import { useMemo, useState } from "react";
import { FeedTable } from "@/components/feed/feed-table";
import { Panel } from "@/components/ui-custom/panel";
import { Stat } from "@/components/ui-custom/stat";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { usePoll } from "@/hooks/use-poll";
import { formatClock } from "@/lib/format";
import type { FeedItem } from "@/lib/types";

interface ControlInfo {
  workflowId: string;
  runId: string;
  status: string;
  startTime: string | null;
  historyLength: number;
  taskQueue: string;
}

const DIRECTIONS = [
  { value: "all", label: "Both directions" },
  { value: "CLOUD_TO_EDGE", label: "Cloud ▸ Edge" },
  { value: "EDGE_TO_CLOUD", label: "Edge ▸ Cloud" },
];

const STATUSES = [
  { value: "all", label: "Any status" },
  { value: "RUNNING", label: "Running" },
  { value: "COMPLETED", label: "Completed" },
  { value: "FAILED", label: "Failed" },
  { value: "TIMED_OUT", label: "Timed out" },
];

export default function TemporalPage() {
  const [paused, setPaused] = useState(false);
  const [direction, setDirection] = useState("all");
  const [status, setStatus] = useState("all");
  const feed = usePoll<{ items: FeedItem[] }>("/api/temporal/feed", 3000, paused);
  const controlInfo = usePoll<ControlInfo>("/api/temporal/control-info", 15000);

  const items = useMemo(() => {
    let list = feed.data?.items ?? [];
    if (direction !== "all") list = list.filter((i) => i.direction === direction);
    if (status !== "all") list = list.filter((i) => i.status === status);
    return list;
  }, [feed.data, direction, status]);

  return (
    <div className="flex flex-col gap-7">
      <Panel legend="Message traffic">
        <div className="mb-3 flex flex-wrap items-center gap-2">
          <Select value={direction} onValueChange={setDirection}>
            <SelectTrigger size="sm" className="readout w-44 border-hairline-strong text-[11px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {DIRECTIONS.map((d) => (
                <SelectItem key={d.value} value={d.value} className="readout text-[12px]">
                  {d.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger size="sm" className="readout w-40 border-hairline-strong text-[11px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {STATUSES.map((s) => (
                <SelectItem key={s.value} value={s.value} className="readout text-[12px]">
                  {s.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <div className="flex-1" />
          <span className="readout text-[10px] text-ink-faint">
            {items.length} shown · refresh {paused ? "paused" : "3s"}
          </span>
          <Button
            size="sm"
            variant="secondary"
            className="btn-hard font-mono text-[10px] uppercase tracking-[0.12em]"
            onClick={() => setPaused((p) => !p)}
          >
            {paused ? "Resume" : "Pause"}
          </Button>
        </div>
        {feed.error && !feed.data ? (
          <p className="readout py-6 text-center text-[12px] text-err">
            cannot reach Temporal: {feed.error}
          </p>
        ) : (
          <FeedTable items={items} />
        )}
        <p className="mt-3 text-[11px] leading-snug text-ink-faint">
          Cloud▸edge messages run as <span className="readout">DeliverToEdge</span> workflows (click a
          row for event history, including the <span className="readout">TransmitToDevice</span> call to
          the device). Edge▸cloud messages run as <span className="readout">DeliverToCloud</span>{" "}
          standalone activities.
        </p>
      </Panel>

      <Panel legend="Control workflow">
        {controlInfo.data ? (
          <div className="grid grid-cols-2 gap-x-4 gap-y-3 md:grid-cols-5">
            <Stat label="Workflow ID" value={controlInfo.data.workflowId} />
            <Stat
              label="Status"
              value={controlInfo.data.status}
              tone={controlInfo.data.status === "RUNNING" ? "ok" : "warn"}
            />
            <Stat label="Started" value={formatClock(controlInfo.data.startTime)} />
            <Stat label="History events" value={String(controlInfo.data.historyLength)} />
            <Stat label="Task queue" value={controlInfo.data.taskQueue} />
          </div>
        ) : (
          <p className="readout text-[11px] text-ink-faint">
            {controlInfo.error ?? "loading…"}
          </p>
        )}
        <p className="mt-3 text-[11px] leading-snug text-ink-faint">
          The singleton state-holder the cloud signals and the proxy polls. It continues-as-new
          periodically to bound history.
        </p>
      </Panel>
    </div>
  );
}
