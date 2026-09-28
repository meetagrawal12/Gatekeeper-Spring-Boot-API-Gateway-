import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The proxy makes the browser talk only to the dev server (same origin).
// Dropping the Origin header stops the backend treating these calls as cross-origin.
const dropOrigin = (proxy) => {
  proxy.on("proxyReq", (proxyReq) => proxyReq.removeHeader("origin"));
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8081", changeOrigin: true, configure: dropOrigin, rewrite: (p) => p.replace(/^\/api/, "") },
      "/svc/gateway": { target: "http://localhost:8080", changeOrigin: true, configure: dropOrigin, rewrite: (p) => p.replace(/^\/svc\/gateway/, "") },
      "/svc/analytics": { target: "http://localhost:8082", changeOrigin: true, configure: dropOrigin, rewrite: (p) => p.replace(/^\/svc\/analytics/, "") },
    },
  },
});