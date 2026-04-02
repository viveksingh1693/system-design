import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/events": "http://localhost:8080",
      "/shows": "http://localhost:8080",
      "/bookings": "http://localhost:8080",
      "/v3": "http://localhost:8080",
      "/swagger-ui": "http://localhost:8080",
      "/swagger-ui.html": "http://localhost:8080",
      "/h2-console": "http://localhost:8080"
    }
  },
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts"
  }
});
