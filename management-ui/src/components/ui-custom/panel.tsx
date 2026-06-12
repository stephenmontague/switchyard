import { cn } from "@/lib/utils";

/** Schematic-style framed block with an engraved legend, like an instrument panel callout. */
export function Panel({
  legend,
  className,
  children,
}: {
  legend: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <section className={cn("panel p-4 pt-5", className)}>
      <span className="panel-legend">{legend}</span>
      {children}
    </section>
  );
}
