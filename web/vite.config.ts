/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
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
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/sdui-demo': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ably-token': {
        target: 'https://identity.nba.com',
        changeOrigin: true,
        rewrite: () => '/rttoken',
        secure: true,
      },
    },
  },
});
