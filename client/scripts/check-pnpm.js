#!/usr/bin/env node
// Runs from the `preinstall` script in package.json. Aborts the install
// when the user invoked anything other than pnpm (npm, yarn, bun, ...).
//
// Uses only the node binary that the invoking package manager already
// spawned us with — no npx, no network fetch, no unpinned dependency.

const userAgent = process.env.npm_config_user_agent || ''
const packageManager = userAgent.split(' ')[0].split('/')[0]

if (packageManager !== 'pnpm') {
  console.error(
    `\n  This project uses pnpm.\n` +
      `  Detected package manager: ${packageManager || 'unknown'}\n\n` +
      `  Run instead:\n` +
      `    corepack enable\n` +
      `    pnpm install\n`,
  )
  process.exit(1)
}
