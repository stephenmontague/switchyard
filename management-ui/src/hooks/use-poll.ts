"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** Polls a JSON endpoint on an interval. Returns latest data, last error, and a refresh fn. */
export function usePoll<T>(url: string, intervalMs: number, paused = false) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const activeRef = useRef(true);

  const tick = useCallback(async () => {
    try {
      const res = await fetch(url, { cache: "no-store" });
      const body = await res.json();
      if (!res.ok) throw new Error(body?.error ?? res.statusText);
      if (activeRef.current) {
        setData(body as T);
        setError(null);
      }
    } catch (e) {
      if (activeRef.current) setError(e instanceof Error ? e.message : String(e));
    }
  }, [url]);

  useEffect(() => {
    activeRef.current = true;
    if (paused) return;
    let cancelled = false;
    const loop = async () => {
      await tick();
      if (!cancelled) timerRef.current = setTimeout(loop, intervalMs);
    };
    void loop();
    return () => {
      cancelled = true;
      activeRef.current = false;
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [tick, intervalMs, paused]);

  return { data, error, refresh: tick };
}
