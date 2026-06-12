import { controlHandle, errorResponse, temporalClient, workerLiveness } from "@/lib/temporal";
import type { ProxyControlState } from "@/lib/types";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const client = await temporalClient();
    const [state, liveness] = await Promise.all([
      controlHandle(client).query<ProxyControlState>("getState"),
      workerLiveness(client),
    ]);
    return Response.json({ state, liveness });
  } catch (e) {
    return errorResponse(e);
  }
}
