const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function (app) {
  // SSE endpoint — disable buffering/compression for real-time streaming
  app.use(
    "/api/v1/notifications/stream",
    createProxyMiddleware({
      target: "http://localhost:8080",
      changeOrigin: true,
      headers: { Accept: "text/event-stream" },
      onProxyRes: (proxyRes) => {
        proxyRes.headers["Cache-Control"] = "no-cache";
        proxyRes.headers["Content-Type"] = "text/event-stream";
        proxyRes.headers["X-Accel-Buffering"] = "no";
        // Remove content-encoding to prevent compression buffering
        delete proxyRes.headers["content-encoding"];
      },
    })
  );

  // All other API calls
  app.use(
    "/api/v1",
    createProxyMiddleware({
      target: "http://localhost:8080",
      changeOrigin: true,
    })
  );

  // Pipeline facade (the iOS contract) — the dashboard Form gauge reads it too
  app.use(
    "/pipeline/v1",
    createProxyMiddleware({
      target: "http://localhost:8080",
      changeOrigin: true,
    })
  );
};
