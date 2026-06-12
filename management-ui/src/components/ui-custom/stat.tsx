import { cn } from "@/lib/utils";

/** Small engraved label + mono readout value. */
export function Stat({
  label,
  value,
  tone = "default",
  className,
}: {
  label: string;
  value: React.ReactNode;
  tone?: "default" | "ok" | "warn" | "err";
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col gap-0.5", className)}>
      <span className="text-[9px] font-medium uppercase tracking-[0.16em] text-ink-faint">
        {label}
      </span>
      <span
        className={cn("readout text-[13px] font-medium", {
          "text-ok": tone === "ok",
          "text-warn": tone === "warn",
          "text-err": tone === "err",
        })}
      >
        {value}
      </span>
    </div>
  );
}
