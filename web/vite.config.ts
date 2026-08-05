import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Proxy /api -> backend Spring (porta 8080) durante o desenvolvimento.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
})
