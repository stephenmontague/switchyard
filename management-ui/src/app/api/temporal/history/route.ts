import { errorResponse, temporalClient, tsToIso } from "@/lib/temporal";
import { prettyConstant } from "@/lib/format";
import proto from "@temporalio/proto";

export const dynamic = "force-dynamic";

const EVENT_TYPE_BY_VALUE: Record<number, string> = Object.fromEntries(
  Object.entries(proto.temporal.api.enums.v1.EventType).map(([name, value]) => [
    value as number,
    name,
  ]),
);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function extractDetail(event: any): string | null {
  // Find the single *EventAttributes payload and surface the most useful field.
  const key = Object.keys(event).find((k) => k.endsWith("EventAttributes") && event[k]);
  if (!key) return null;
  const attrs = event[key];
  if (attrs.activityType?.name) return `activity: ${attrs.activityType.name}`;
  if (attrs.workflowType?.name) return `workflow: ${attrs.workflowType.name}`;
  if (attrs.failure?.message) return `failure: ${attrs.failure.message}`;
  if (attrs.signalName) return `signal: ${attrs.signalName}`;
  if (attrs.taskQueue?.name) return `queue: ${attrs.taskQueue.name}`;
  if (attrs.identity) return `worker: ${attrs.identity}`;
  return null;
}

export async function GET(request: Request) {
  try {
    const url = new URL(request.url);
    const workflowId = url.searchParams.get("workflowId");
    const runId = url.searchParams.get("runId") ?? undefined;
    if (!workflowId) {
      return Response.json({ error: "workflowId is required" }, { status: 400 });
    }
    const client = await temporalClient();
    const handle = client.workflow.getHandle(workflowId, runId);
    const history = await handle.fetchHistory();
    const events = (history.events ?? []).map((e) => ({
      id: Number(e.eventId ?? 0),
      type: prettyConstant(EVENT_TYPE_BY_VALUE[Number(e.eventType ?? 0)] ?? "UNKNOWN", "EVENT_TYPE_"),
      time: tsToIso(e.eventTime),
      detail: extractDetail(e),
    }));
    return Response.json({ events });
  } catch (e) {
    return errorResponse(e);
  }
}
