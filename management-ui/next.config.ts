import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // The Temporal SDK is a native gRPC + protobuf stack — it must load from
  // node_modules at runtime, not be bundled into the server build.
  serverExternalPackages: ["@temporalio/client", "@temporalio/proto", "@temporalio/common"],
};

export default nextConfig;
