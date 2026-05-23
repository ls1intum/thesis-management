import { mkdir, writeFile, unlink, readFile } from 'fs/promises'
import { tmpdir } from 'os'
import { join } from 'path'
import { test as base } from '@playwright/test'

const LOCK_DIR = join(tmpdir(), 'tm-e2e-locks')

async function ensureLockDir(): Promise<void> {
  await mkdir(LOCK_DIR, { recursive: true })
}

function isPidAlive(pid: number): boolean {
  try {
    process.kill(pid, 0)
    return true
  } catch (err) {
    return (err as NodeJS.ErrnoException).code === 'EPERM'
  }
}

async function tryReclaimStaleLock(path: string): Promise<void> {
  const contents = await readFile(path, 'utf8').catch(() => '')
  const pid = Number.parseInt(contents.split('-')[0] ?? '', 10)
  if (Number.isFinite(pid) && pid > 0 && !isPidAlive(pid)) {
    await unlink(path).catch(() => undefined)
  }
}

async function acquire(name: string): Promise<() => Promise<void>> {
  await ensureLockDir()
  const path = join(LOCK_DIR, `${name}.lock`)
  const stamp = `${process.pid}-${Date.now()}-${Math.random()}`
  const deadline = Date.now() + 5 * 60_000
  while (Date.now() < deadline) {
    try {
      await writeFile(path, stamp, { flag: 'wx' })
      return async () => {
        await unlink(path).catch(() => undefined)
      }
    } catch (err) {
      if ((err as NodeJS.ErrnoException).code !== 'EEXIST') throw err
      await tryReclaimStaleLock(path)
      await new Promise((r) => setTimeout(r, 250))
    }
  }
  throw new Error(`Failed to acquire lock "${name}" within 5 minutes`)
}

/**
 * Worker-scoped fixture that serializes tests across files mutating the same
 * backend resource (e.g. the seeded thesis d000-0003). Playwright runs files
 * in parallel across workers, so per-file `describe.serial` is not enough —
 * import `test` from this module in any file whose tests mutate thesis 3,
 * and the worker holds the filesystem lock for its entire lifetime.
 *
 * Lock files include the holder's PID so a stale lock from a crashed worker
 * is reclaimed automatically on the next acquire attempt.
 */
export const test = base.extend<object, { thesis3Lock: void }>({
  thesis3Lock: [
    async ({}, use) => {
      const release = await acquire('thesis-d000-0003')
      try {
        await use()
      } finally {
        await release()
      }
    },
    { scope: 'worker', auto: true },
  ],
})

export { expect } from '@playwright/test'
