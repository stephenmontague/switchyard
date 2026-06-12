import { controlHandle, errorResponse, temporalClient } from "@/lib/temporal";

export const dynamic = "force-dynamic";

// One endpoint for all control-plane commands. Everything is a signal to the
// proxy-control workflow — the UI never reaches the proxy's network.
const SIGNALS: Record<string, { name: string; takesArg: boolean }> = {
  enable: { name: "enable", takesArg: false },
  disable: { name: "disable", takesArg: false },
  restart: { name: "requestRestart", takesArg: false },
  shutdown: { name: "requestShutdown", takesArg: false },
  "apply-config": { name: "applyConfig", takesArg: true },
  "upsert-device": { name: "upsertDevice", takesArg: true },
  "remove-device": { name: "removeDevice", takesArg: true },
};

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const action = String(body?.action ?? "");
    const spec = SIGNALS[action];
    if (!spec) {
      return Response.json({ error: `unknown action '${action}'` }, { status: 400 });
    }
    const client = await temporalClient();
    const handle = controlHandle(client);
    if (spec.takesArg) {
      await handle.signal(spec.name, body.arg);
    } else {
      await handle.signal(spec.name);
    }
    return Response.json({ ok: true, action });
  } catch (e) {
    return errorResponse(e);
  }
}
