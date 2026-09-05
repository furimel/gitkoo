import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {resolve} from 'node:path'
import {fileURLToPath} from 'node:url'

// This file is ESM, so __dirname does not exist; import.meta gives the same thing.
const here = fileURLToPath(new URL('.', import.meta.url))

/*
 * Builds the client into build/frontend, which the Gradle `frontend` task copies
 * into the jar's static resources. Nothing generated is ever written under src/.
 *
 * `base` matches WebMvcConfig: everything static is served under /assets/, because
 * a two-segment path like /css/app.css is claimed by the /{username}/{name}
 * repository route before the resource handler ever sees it.
 */
export default defineConfig({
  plugins: [react()],
  base: '/assets/app/',
  build: {
    outDir: resolve(here, '../build/frontend'),
    emptyOutDir: true,
    manifest: true,
    // The server reads the manifest to find the hashed entry names, so there is
    // no index.html to generate - Java writes the shell.
    rollupOptions: {
      input: resolve(here, 'src/main.tsx'),
    },
    // Primer is large; a single bundle keeps the page to two requests and lets the
    // server hand back one script tag.
    chunkSizeWarningLimit: 1500,
  },
  resolve: {
    alias: {
      '@': resolve(here, 'src'),
    },
  },
})
