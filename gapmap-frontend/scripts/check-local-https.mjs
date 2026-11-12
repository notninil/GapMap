// Local TLS/proxy check only: no Spring Boot, accounts, or database connections.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import https from "node:https";
import { createServer } from "vite";

const cert = readFileSync(process.env.GAPMAP_HTTPS_CERT);
const key = readFileSync(process.env.GAPMAP_HTTPS_KEY);
const ca = readFileSync(process.env.GAPMAP_HTTPS_CA);
const api = https.createServer({ cert, key }, (req, res) => {
  assert.equal(req.url, "/api/health");
  res.setHeader("content-type", "application/json");
  res.setHeader("set-cookie", "gapmap_tls_check=fixture; Secure; HttpOnly; SameSite=Strict; Path=/api");
  res.end(JSON.stringify({ status: "ok", mock: true }));
});
let vite;
try {
  await new Promise((resolve, reject) => {
    api.once("error", reject);
    api.listen(8443, "127.0.0.1", resolve);
  });
  vite = await createServer({ server: { host: "127.0.0.1", port: 5173, strictPort: true } });
  await vite.listen();
  const response = await new Promise((resolve, reject) => {
    https.get("https://127.0.0.1:5173/api/health", { ca, rejectUnauthorized: true }, (res) => {
      let body = "";
      res.on("data", (chunk) => { body += chunk; });
      res.on("end", () => resolve({ status: res.statusCode, headers: res.headers, body }));
    }).on("error", reject);
  });
  assert.equal(response.status, 200);
  assert.deepEqual(JSON.parse(response.body), { status: "ok", mock: true });
  assert.match(response.headers["set-cookie"][0], /Secure; HttpOnly; SameSite=Strict/);
  console.log("PASS: verified HTTPS frontend -> verified HTTPS mock API; cookie attributes preserved.");
} finally {
  if (vite) await vite.close();
  await new Promise((resolve) => api.close(resolve));
}
