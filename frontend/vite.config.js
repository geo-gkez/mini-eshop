import { defineConfig, loadEnv } from 'vite'
import Vue from '@vitejs/plugin-vue'
import Vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      Vue({ template: { transformAssetUrls } }),
      Vuetify({ autoImport: true }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        '/api': env.BACKEND_URL ?? 'http://localhost:8080',
      },
    },
  }
})
