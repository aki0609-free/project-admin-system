import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],

    worker: {
      format: 'es',
    },

    optimizeDeps: {
      include: [
        'vue-demi',
        'vuetify',
        'monaco-editor',
        'vue-monaco-editor',
        'prettier',
        'prettier/plugins/estree',
        'prettier/plugins/babel',
      ],
    },

    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },

    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
      },
    },

    build: {
      target: 'esnext',
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            vuetify: ['vuetify'],
          },
        },
      },
    },
  }
})
