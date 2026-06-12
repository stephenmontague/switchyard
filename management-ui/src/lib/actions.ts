"use client";

import type { ControlStateResponse } from "@/lib/types";

/** POST a control-plane action — every one becomes a signal on the proxy-control workflow. */
export async function postSignal(action: string, arg?: unknown): Promise<void> {
  const res = await fetch("/api/control/signal", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ action, arg }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json?.error ?? res.statusText);
}

export async function fetchControlState(): Promise<ControlStateResponse> {
  const res = await fetch("/api/control/state", { cache: "no-store" });
  const json = await res.json();
  if (!res.ok) throw new Error(json?.error ?? res.statusText);
  return json as ControlStateResponse;
}

/**
 * After a config signal, watch the control state until the workflow accepts (version
 * bumps) or rejects (lastError set). The workflow validates inside the signal handler,
 * so one of the two always happens quickly.
 */
export async function awaitConfigOutcome(
  prevVersion: number,
): Promise<{ accepted: boolean; message: string }> {
  for (let attempt = 0; attempt < 8; attempt++) {
    await new Promise((r) => setTimeout(r, 600));
    const { state } = await fetchControlState();
    if (state.version > prevVersion) {
      return { accepted: true, message: `config v${state.version} accepted` };
    }
    if (state.lastError) {
      return { accepted: false, message: state.lastError };
    }
  }
  return { accepted: false, message: "no response from control workflow (timed out)" };
}
