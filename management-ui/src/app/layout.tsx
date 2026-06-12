import type { Metadata } from "next";
import { IBM_Plex_Mono, Space_Grotesk } from "next/font/google";
import { Toaster } from "@/components/ui/sonner";
import { Header } from "@/components/shell/header";
import "./globals.css";

const display = Space_Grotesk({
  variable: "--font-display",
  subsets: ["latin"],
});

const plexMono = IBM_Plex_Mono({
  variable: "--font-plex-mono",
  subsets: ["latin"],
  weight: ["400", "500", "600"],
});

export const metadata: Metadata = {
  title: "Switchyard — Cloud↔Edge Proxy Console",
  description: "Operations console for the Temporal-backed cloud↔edge proxy",
};

const TEMPORAL_ADDRESS = process.env.TEMPORAL_ADDRESS ?? "localhost:7233";
const TEMPORAL_NAMESPACE = process.env.TEMPORAL_NAMESPACE ?? "default";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${display.variable} ${plexMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <Header address={TEMPORAL_ADDRESS} namespace={TEMPORAL_NAMESPACE} />
        <main className="mx-auto w-full max-w-6xl flex-1 px-6 pb-16 pt-10">
          {children}
        </main>
        <footer className="border-t border-hairline bg-panel px-6 py-3">
          <div className="mx-auto flex max-w-6xl items-center justify-between">
            <span className="readout text-[10px] tracking-[0.16em] text-ink-faint uppercase">
              Switchyard · every command rides the egress gRPC channel — no inbound ports
            </span>
            <span className="readout text-[10px] text-ink-faint">
              {TEMPORAL_ADDRESS} · ns/{TEMPORAL_NAMESPACE}
            </span>
          </div>
        </footer>
        <Toaster position="bottom-right" />
      </body>
    </html>
  );
}
