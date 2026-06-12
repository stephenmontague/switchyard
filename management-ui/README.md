# management-ui — Switchyard

Operations console for the [proxy](../proxy): lifecycle control, guided routing config,
and live Temporal visibility, aimed at a solutions consultant rather than a developer.

**Architecture rule: the UI never talks to the proxy.** Every command is a **signal** to
the `proxy-control` workflow and every readout is a **query** (or Temporal visibility
API). The proxy reports its applied state back over its own egress connection, so the
on-prem firewall only ever passes the one outbound gRPC channel.

| Screen       | What it does                                                                 |
| ------------ | ---------------------------------------------------------------------------- |
| **Console**  | Live data-path schematic, desired vs applied state, enable/disable (soft) and restart/shutdown (hard, via supervisor) |
| **Routes**   | Device list; 3-step wizard (template → site values → review) with validation mirroring `ConfigValidator`; raw-JSON advanced mode |
| **Temporal** | Live feed of `DeliverToEdge` workflows + `DeliverToCloud` standalone activities, expandable event history, control-workflow inspector |
| **Dispatch** | Demo drivers for all three transports with auto-randomized business IDs (re-use an ID to demo idempotent dedup) |

Stack: Next.js (App Router) + TypeScript, ShadCN primitives restyled with a custom
Tailwind theme, `@temporalio/client` in server-side API routes only.

Run from the repo root: `just run-ui` (port **3000**). For the RESTART button to work,
run the proxy with `just run-proxy-managed`. Defaults match the local demo stack;
override via `.env.local` (see [.env.example](.env.example)).
