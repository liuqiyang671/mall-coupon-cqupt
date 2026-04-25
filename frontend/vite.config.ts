import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      '/api/merchant-admin': {
        target: 'http://localhost:10010',
        changeOrigin: true
      },
      '/api/user': {
        target: 'http://localhost:10010',
        changeOrigin: true
      },
      '/api/engine': {
        target: 'http://localhost:10020',
        changeOrigin: true
      },
      '/api/settlement': {
        target: 'http://localhost:10030',
        changeOrigin: true
      },
      '/api/distribution': {
        target: 'http://localhost:10040',
        changeOrigin: true
      },
      '/api/search': {
        target: 'http://localhost:10050',
        changeOrigin: true
      }
    }
  }
})
