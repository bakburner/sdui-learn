import express from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
const PORT = 3000;
const SDUI_SERVER = process.env.SDUI_SERVER || 'http://localhost:8080';
const VITE_DEV_SERVER = process.env.VITE_DEV_SERVER || 'http://localhost:5173';
const isDev = process.env.NODE_ENV !== 'production';

// Security headers
app.use((req, res, next) => {
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Proxy Ably token requests to avoid CORS issues in the browser
app.use('/ably-token', createProxyMiddleware({
  target: 'https://identity.nba.com',
  changeOrigin: true,
  pathRewrite: { '^/ably-token': '/rttoken' },
  secure: true,
}));

// Proxy API requests to SDUI composition server
app.use('/api', createProxyMiddleware({
  target: SDUI_SERVER,
  changeOrigin: true,
  pathRewrite: {
    '^/api': '', // Remove /api prefix when forwarding
  },
  onProxyReq: (proxyReq, req, res) => {
    console.log(`[Proxy] ${req.method} ${req.url} -> ${SDUI_SERVER}${req.url.replace('/api', '')}`);
  },
  onError: (err, req, res) => {
    console.error('[Proxy Error]', err.message);
    res.status(502).json({ error: 'SDUI server unavailable' });
  },
}));

if (isDev) {
  // In development, proxy all non-/api requests to Vite dev server
  app.use(createProxyMiddleware({
    target: VITE_DEV_SERVER,
    changeOrigin: true,
    ws: true, // Enable WebSocket proxy for HMR
    onError: (err, req, res) => {
      console.error('[Vite Proxy Error]', err.message);
      res.status(502).send(`
        <html><body style="background:#1a1a2e;color:#fff;font-family:sans-serif;padding:40px;">
          <h1>⚠️ Vite dev server not running</h1>
          <p>Make sure to start Vite first: <code>npm run dev:client</code></p>
          <p>Or use <code>npm run dev</code> to start both servers.</p>
        </body></html>
      `);
    },
  }));
} else {
  // In production, serve static files from dist/
  app.use(express.static(join(__dirname, 'dist')));
  app.get('*', (req, res) => {
    res.sendFile(join(__dirname, 'dist', 'index.html'));
  });
}

app.listen(PORT, () => {
  console.log(`\n🏀 SDUI Web Server running at http://localhost:${PORT}`);
  console.log(`   Proxying /api/* -> ${SDUI_SERVER}`);
  if (isDev) {
    console.log(`   Proxying SPA -> ${VITE_DEV_SERVER} (dev mode)`);
    console.log(`\n   Open http://localhost:${PORT} in your browser\n`);
  }
});

