import {
  CONTROL_WORKFLOW_ID,
  controlHandle,
  errorResponse,
  temporalClient,
} from "@/lib/temporal";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const client = await temporalClient();
    const desc = await controlHandle(client).describe();
    return Response.json({
      workflowId: CONTROL_WORKFLOW_ID,
      runId: desc.runId,
      status: desc.status.name,
      startTime: desc.startTime ? new Date(desc.startTime).toISOString() : null,
      historyLength: Number(desc.historyLength ?? 0),
      taskQueue: desc.taskQueue,
    });
  } catch (e) {
    return errorResponse(e);
  }
}
