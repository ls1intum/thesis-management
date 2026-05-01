import { execFileSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, test } from 'node:test'
import assert from 'node:assert/strict'

// Pure-Node tests for `client/public/generate-runtime-env.js`.
//
// Regression test for #782: setting RESEARCH_GROUPS_LOCATION (and TOPIC_VIEWS_OPTIONS)
// on the client container had no effect because they were missing from the script's
// ALLOWED_ENVIRONMENT_VARIABLES list. These tests run the script in a temp
// directory with controlled env vars and verify the generated runtime-env.js.

const SCRIPT = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'generate-runtime-env.js')

function runGenerator(env) {
  const dir = mkdtempSync(join(tmpdir(), 'tm-runtime-env-'))
  try {
    execFileSync('node', [SCRIPT], {
      cwd: dir,
      env: { ...env, PATH: process.env.PATH ?? '' },
      stdio: 'pipe',
    })
    const generated = readFileSync(join(dir, 'runtime-env.js'), 'utf8')
    const match = generated.match(/window\.RUNTIME_ENVIRONMENT_VARIABLES=(\{.*?\});/s)
    assert.ok(match, `runtime-env.js did not match expected shape:\n${generated}`)
    return JSON.parse(match[1])
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

describe('generate-runtime-env.js — allowed env vars', () => {
  test('RESEARCH_GROUPS_LOCATION is exposed to the client (regression for #782)', () => {
    const payload = '{"AACHEN":"Aachen","HEILBRONN":"Heilbronn"}'
    const out = runGenerator({ RESEARCH_GROUPS_LOCATION: payload })
    assert.equal(out.RESEARCH_GROUPS_LOCATION, payload)
  })

  test('TOPIC_VIEWS_OPTIONS is exposed to the client', () => {
    const payload = '{"OPEN":"Open","ARCHIVED":"Archived"}'
    const out = runGenerator({ TOPIC_VIEWS_OPTIONS: payload })
    assert.equal(out.TOPIC_VIEWS_OPTIONS, payload)
  })

  test('previously-allowlisted vars still flow through', () => {
    const out = runGenerator({
      SERVER_HOST: 'https://api.example.com',
      KEYCLOAK_HOST: 'https://kc.example.com',
      KEYCLOAK_REALM_NAME: 'thesis',
      KEYCLOAK_CLIENT_ID: 'thesis-client',
      CHAIR_NAME: 'Test Chair',
      APPLICATION_TITLE: 'Test',
    })
    assert.deepEqual(out, {
      SERVER_HOST: 'https://api.example.com',
      KEYCLOAK_HOST: 'https://kc.example.com',
      KEYCLOAK_REALM_NAME: 'thesis',
      KEYCLOAK_CLIENT_ID: 'thesis-client',
      CHAIR_NAME: 'Test Chair',
      APPLICATION_TITLE: 'Test',
    })
  })

  test('non-allowlisted vars are not exposed', () => {
    // Defense-in-depth: arbitrary host env vars (e.g. AWS_SECRET_ACCESS_KEY) must not
    // leak into the browser-visible runtime config.
    const out = runGenerator({
      SECRET_TOKEN: 'should-not-be-exposed',
      RESEARCH_GROUPS_LOCATION: '{"AACHEN":"Aachen"}',
    })
    assert.equal(out.SECRET_TOKEN, undefined)
    assert.equal(out.RESEARCH_GROUPS_LOCATION, '{"AACHEN":"Aachen"}')
  })

  test('empty env produces empty config', () => {
    const out = runGenerator({})
    assert.deepEqual(out, {})
  })
})
