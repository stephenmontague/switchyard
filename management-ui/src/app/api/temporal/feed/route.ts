import {
  durationToMillis,
  errorResponse,
  temporalClient,
  temporalNamespace,
  tsToIso,
  tsToMillis,
} from "@/lib/temporal";
import type { FeedItem } from "@/lib/types";

export const dynamic = "force-dynamic";

const FEED_LIMIT = 60;

// Message flows by Temporal primitive: cloud->edge rides DeliverToEdge workflows,
// edge->cloud rides DeliverToCloud standalone activities.
const WORKFLOW_DIRECTION: Record<string, FeedItem["direction"]> = {
  DeliverToEdge: "CLOUD_TO_EDGE",
};
const ACTIVITY_DIRECTION: Record<string, FeedItem["direction"]> = {
  DeliverToCloud: "EDGE_TO_CLOUD",
  // Pre-refactor history: DeliverToEdge briefly ran as a standalone activity.
  DeliverToEdge: "CLOUD_TO_EDGE",
};

const ACTIVITY_STATUS: Record<number, string> = {
  0: "UNSPECIFIED",
  1: "RUNNING",
  2: "COMPLETED",
  3: "FAILED",
  4: "CANCELED",
  5: "TERMINATED",
  6: "TIMED_OUT",
};

export async function GET() {
  try {
    const client = await temporalClient();
    const items: FeedItem[] = [];

    // Cloud -> Edge: DeliverToEdge workflows via visibility.
    const workflows = client.workflow.list({
      query: "WorkflowType = 'DeliverToEdge'",
      pageSize: FEED_LIMIT,
    });
    let count = 0;
    for await (const wf of workflows) {
      const start = wf.startTime ? new Date(wf.startTime).getTime() : null;
      const close = wf.closeTime ? new Date(wf.closeTime).getTime() : null;
      items.push({
        id: wf.workflowId,
        kind: "workflow",
        type: wf.type,
        direction: WORKFLOW_DIRECTION[wf.type] ?? "CLOUD_TO_EDGE",
        status:
          typeof wf.status === "object" ? wf.status.name : String(wf.status),
        startTime: start ? new Date(start).toISOString() : null,
        closeTime: close ? new Date(close).toISOString() : null,
        durationMs: start != null && close != null ? close - start : null,
        runId: wf.runId,
      });
      if (++count >= FEED_LIMIT) break;
    }

    // Edge -> Cloud: DeliverToCloud standalone activities (Server 1.31+ visibility API).
    const resp = await client.connection.workflowService.listActivityExecutions(
      {
        namespace: temporalNamespace(),
        pageSize: FEED_LIMIT,
      },
    );
    for (const a of resp.executions ?? []) {
      const typeName = a.activityType?.name ?? "?";
      const direction = ACTIVITY_DIRECTION[typeName];
      if (!direction) continue; // skip health checks and other noise
      const startMs = tsToMillis(a.scheduleTime);
      const closeMs = tsToMillis(a.closeTime);
      items.push({
        id: a.activityId ?? "?",
        kind: "activity",
        type: typeName,
        direction,
        status: ACTIVITY_STATUS[Number(a.status ?? 0)] ?? "UNSPECIFIED",
        startTime: startMs ? new Date(startMs).toISOString() : null,
        closeTime: closeMs ? new Date(closeMs).toISOString() : null,
        durationMs:
          durationToMillis(a.executionDuration) ??
          (startMs != null && closeMs != null ? closeMs - startMs : null),
        runId: a.runId ?? undefined,
      });
    }

    items.sort((x, y) => (y.startTime ?? "").localeCompare(x.startTime ?? ""));
    return Response.json({
      items: items.slice(0, FEED_LIMIT),
      generatedAt: tsToIso(null) ?? new Date().toISOString(),
    });
  } catch (e) {
    return errorResponse(e);
  }
}
