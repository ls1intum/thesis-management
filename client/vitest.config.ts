import { defineConfig } from 'vitest/config'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  test: {
    environment: 'jsdom',
    // Test files import their helpers explicitly (`import { describe, ... }
    // from 'vitest'`) instead of relying on globals, so we keep this off
    // and avoid leaking `describe`/`expect`/`vi` into production source.
    globals: false,
    setupFiles: ['./test/setup.ts'],
    // Component tests live next to the code under src/.
    // The pre-existing pure-Node tests in client/test/*.test.mjs continue
    // running under `node --test` (see the test script in package.json).
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    css: true,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.{test,spec}.{ts,tsx}', 'src/**/*.d.ts', 'src/index.tsx'],
      reporter: ['text', 'lcov', 'html'],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(here, 'src'),
    },
  },
})
