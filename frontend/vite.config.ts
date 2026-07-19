import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // Storybook 判定
  const isStorybook = process.argv.some((arg) =>
    arg.includes('storybook')
  )

  return {
    plugins: [
      vue(),

      // Storybook では DevTools を無効化
      !isStorybook && mode === 'development' && ''/*vueDevTools()*/,
    ].filter(Boolean),

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