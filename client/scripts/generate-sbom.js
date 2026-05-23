#!/usr/bin/env node
// Generates a CycloneDX SBOM for the client into `sbom/bom.json` using
// pnpm's built-in `pnpm sbom` command. The SBOM is committed and reused
// across builds; regeneration is skipped when `pnpm-lock.yaml` hasn't
// changed (hash compared against `sbom/.lock-hash`), so this script is a
// no-op in the common case. Pass `--force` to regenerate unconditionally.

const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { spawnSync } = require('node:child_process')

const ROOT = path.resolve(__dirname, '..')
// Hash inputs: the lockfile defines the dep graph, and package.json
// supplies `name`/`version` which the generator embeds into the SBOM's
// metadata.component (bumping just the version would otherwise leave a
// stale SBOM with the old version + a still-matching lockfile hash).
// Order matches the shell `cat ... | shasum` in the CI verification step.
const HASH_INPUTS = [
  path.join(ROOT, 'pnpm-lock.yaml'),
  path.join(ROOT, 'package.json'),
]
const SBOM_DIR = path.join(ROOT, 'sbom')
const BOM_FILE = path.join(SBOM_DIR, 'bom.json')
const HASH_FILE = path.join(SBOM_DIR, '.lock-hash')

const force = process.argv.includes('--force')

for (const f of HASH_INPUTS) {
  if (!fs.existsSync(f)) {
    console.error(`${path.relative(ROOT, f)} not found; run \`pnpm install\` first.`)
    process.exit(1)
  }
}

const hasher = crypto.createHash('sha256')
for (const f of HASH_INPUTS) hasher.update(fs.readFileSync(f))
const currentHash = hasher.digest('hex')

const cachedHash = fs.existsSync(HASH_FILE)
  ? fs.readFileSync(HASH_FILE, 'utf8').trim()
  : null

if (!force && currentHash === cachedHash && fs.existsSync(BOM_FILE)) {
  console.log('SBOM up-to-date (pnpm-lock.yaml + package.json unchanged).')
  process.exit(0)
}

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
// we're invoked via `pnpm run sbom:gen`, those vars convince the nested
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

// `pnpm sbom` writes the SBOM to stdout. We persist it ourselves so we can
// strip non-deterministic fields (timestamp, serialNumber) — without that
// every regeneration would dirty the committed file even when nothing
// dependency-related changed.
const parsed = JSON.parse(result.stdout)
delete parsed.serialNumber
if (parsed.metadata) {
  delete parsed.metadata.timestamp
}
parsed.metadata = parsed.metadata || {}
parsed.metadata.component = parsed.metadata.component || {}
parsed.metadata.component.name = pkg.name
parsed.metadata.component.version = pkg.version

// Byte-wise string comparison — `localeCompare` is locale-dependent and
// can reorder entries between machines (e.g. de_DE dev vs en_US CI),
// which would dirty the committed SBOM and break the hash invariant.
const byString = (a, b) => (a < b ? -1 : a > b ? 1 : 0)
const refOf = (x) => x.ref || x['bom-ref'] || ''
const sortByRef = (a, b) => byString(refOf(a), refOf(b))
if (Array.isArray(parsed.components)) {
  parsed.components.sort(sortByRef)
}
if (Array.isArray(parsed.dependencies)) {
  parsed.dependencies.sort(sortByRef)
  for (const dep of parsed.dependencies) {
    if (Array.isArray(dep.dependsOn)) dep.dependsOn.sort(byString)
  }
}

fs.writeFileSync(BOM_FILE, JSON.stringify(parsed, null, 2) + '\n')

fs.writeFileSync(HASH_FILE, currentHash + '\n')
console.log(`SBOM regenerated at ${path.relative(ROOT, BOM_FILE)}.`)
