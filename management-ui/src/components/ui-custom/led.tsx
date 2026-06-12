import type { LedState } from "@/lib/format";
import { cn } from "@/lib/utils";

export function Led({ state, className }: { state: LedState; className?: string }) {
  return <span className={cn("led", className)} data-state={state} aria-hidden />;
}

export function LedLabel({
  state,
  label,
  className,
}: {
  state: LedState;
  label: string;
  className?: string;
}) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <Led state={state} />
      <span className="readout text-[11px] uppercase tracking-[0.12em]">{label}</span>
    </span>
  );
}
