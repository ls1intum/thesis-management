#!/usr/bin/env node
// Runs from the `preinstall` script in package.json. Aborts the install
// when the user invoked anything other than pnpm (npm, yarn, bun, ...).
//
// Uses only the node binary that the invoking package manager already
// spawned us with — no npx, no network fetch, no unpinned dependency.
//
// Detection uses two env vars (either is sufficient) because some CI
// setups (notably `actions/setup-node` + Corepack) propagate one but not
// the other:
//   • npm_config_user_agent — set by every PM to "<pm>/<version> ...".
//   • npm_execpath — path to the PM's own CLI entrypoint (contains "pnpm").
// We REJECT only when there's a positive signal for a non-pnpm PM, so a
// stripped environment with no signals at all falls through silently
// (e.g., running this file directly via `node scripts/check-pnpm.js`).

const userAgent = process.env.npm_config_user_agent || ''
const execpath = (process.env.npm_execpath || '').toLowerCase()

const isPnpm = userAgent.startsWith('pnpm/') || execpath.includes('pnpm')
const otherPm =
  userAgent.match(/^(npm|yarn|bun|cnpm)\//)?.[1] ||
  (/[\\/]npm-cli\.[cm]?js$/.test(execpath) && 'npm') ||
  (/[\\/]yarn(\.[cm]?js)?$/.test(execpath) && 'yarn') ||
  (/[\\/]bun(\.[cm]?js)?$/.test(execpath) && 'bun')

if (!isPnpm && otherPm) {
  console.error(
    `\n  This project uses pnpm.\n` +
      `  Detected package manager: ${otherPm}\n\n` +
      `  Run instead:\n` +
      `    corepack enable\n` +
      `    pnpm install\n`,
  )
  process.exit(1)
}
