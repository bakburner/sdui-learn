/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const sduiServer = env.SDUI_SERVER || 'https://sdui-prototype.tools.internal.nba.com';

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@sdui/models': path.resolve(__dirname, './src/generated/SduiModels.ts'),
        '@': path.resolve(__dirname, './src'),
      },
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: sduiServer,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          secure: true,
        },
        '/sdui-demo': {
          target: sduiServer,
          changeOrigin: true,
          secure: true,
        },
        '/ably-token': {
          target: 'https://identity.nba.com',
          changeOrigin: true,
          rewrite: () => '/rttoken',
          secure: true,
        },
      },
    },
  };
});
