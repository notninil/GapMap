import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";
import { Agent } from "node:https";

const certPath = process.env.GAPMAP_HTTPS_CERT;
const keyPath = process.env.GAPMAP_HTTPS_KEY;
if (Boolean(certPath) !== Boolean(keyPath)) {
  throw new Error("Set both GAPMAP_HTTPS_CERT and GAPMAP_HTTPS_KEY for local HTTPS");
}
const https = certPath
  ? { cert: readFileSync(certPath), key: readFileSync(keyPath) }
  : undefined;
const target = process.env.API_PROXY_TARGET || (https ? "https://127.0.0.1:8443" : "http://localhost:8080");
const caPath = process.env.GAPMAP_HTTPS_CA;
if (https && (!target.startsWith("https://") || !caPath)) {
  throw new Error("Local HTTPS requires an HTTPS API_PROXY_TARGET and GAPMAP_HTTPS_CA");
}
export default defineConfig({
  plugins: [react()],
  server: {
    https,
    ...(https ? { strictPort: true } : {}),
    allowedHosts: ["terminal.local"],
    proxy: {
      "/api": {
        target,
        changeOrigin: true,
        secure: true,
        ...(target.startsWith("https://") && caPath
          ? { agent: new Agent({ ca: readFileSync(caPath), rejectUnauthorized: true }) }
          : {}),
      },
    },
  },
});
