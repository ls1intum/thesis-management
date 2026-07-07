#!/usr/bin/env node
// Generates a CycloneDX SBOM for the client into `sbom/bom.json` using
// pnpm's built-in `pnpm sbom` command. The SBOM is generated at
// Docker-build time in CI (see build_docker.yml) and is not tracked in
// the repo — the output directory is gitignored.

const fs = require('node:fs')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const ROOT = path.resolve(__dirname, '..')
const SBOM_DIR = path.join(ROOT, 'sbom')
const BOM_FILE = path.join(SBOM_DIR, 'bom.json')

fs.mkdirSync(SBOM_DIR, { recursive: true })

const pkg = JSON.parse(fs.readFileSync(path.join(ROOT, 'package.json'), 'utf8'))

// Resolve the pnpm entry point and run it via the current Node binary.
// `npm_execpath` (set by pnpm) points at the .cjs entry, which isn't
// directly executable on its own — invoking it through `process.execPath`
// avoids EACCES and any PATH-resolution surprises.
const pnpmEntry = process.env.npm_execpath && process.env.npm_execpath.includes('pnpm')
  ? process.env.npm_execpath
  : null

// Strip every `npm_*` variable that pnpm leaks into child processes. When
// we're invoked via `pnpm run sbom`, those vars convince the nested
// `pnpm sbom` call that it is itself a lifecycle script, triggering a
// recursive deps-verification that fails. A clean environment makes the
// nested pnpm behave like a fresh top-level invocation.
const cleanEnv = Object.fromEntries(
  Object.entries(process.env).filter(([k]) => !k.startsWith('npm_') && k !== 'INIT_CWD'),
)

const pnpmArgs = [
  'sbom',
  '--sbom-format', 'cyclonedx',
  '--sbom-spec-version', '1.6',
  '--sbom-type', 'application',
  '--lockfile-only',
]

const result = pnpmEntry
  ? spawnSync(process.execPath, [pnpmEntry, ...pnpmArgs], { cwd: ROOT, encoding: 'utf8', env: cleanEnv })
  : spawnSync('pnpm', pnpmArgs, { cwd: ROOT, encoding: 'utf8', env: cleanEnv, shell: process.platform === 'win32' })

if (result.error) {
  console.error('spawn error:', result.error.message)
  process.exit(1)
}
if (result.status !== 0) {
  process.stderr.write(result.stderr || '')
  console.error(`pnpm sbom failed (status=${result.status}).`)
  process.exit(result.status ?? 1)
}

// `pnpm sbom` writes the SBOM to stdout. We persist it ourselves so we
// can inject the pkg name/version — the raw output omits them under
// `--lockfile-only`.
const parsed = JSON.parse(result.stdout)
parsed.metadata = parsed.metadata || {}
parsed.metadata.component = parsed.metadata.component || {}
parsed.metadata.component.name = pkg.name
parsed.metadata.component.version = pkg.version

fs.writeFileSync(BOM_FILE, JSON.stringify(parsed, null, 2) + '\n')
console.log(`SBOM written to ${path.relative(ROOT, BOM_FILE)}.`)
