import { errorResponse } from "@/lib/temporal";

export const dynamic = "force-dynamic";

const DUMMY_CLOUD_URL = process.env.DUMMY_CLOUD_URL ?? "http://localhost:8091";

export async function GET() {
  try {
    const res = await fetch(`${DUMMY_CLOUD_URL}/demo/confirms`, { cache: "no-store" });
    if (!res.ok) {
      return Response.json({ error: `dummy-cloud returned ${res.status}` }, { status: 502 });
    }
    return Response.json({ confirms: await res.json() });
  } catch (e) {
    return errorResponse(e);
  }
}
