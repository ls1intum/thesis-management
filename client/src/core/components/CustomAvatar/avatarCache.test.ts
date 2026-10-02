import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { doRequest } from '@/core/requests/request'
import { clearAvatarCache, loadAvatar } from '@/core/components/CustomAvatar/avatarCache'

vi.mock('@/core/requests/request', () => ({ doRequest: vi.fn() }))

const doRequestMock = vi.mocked(doRequest) as unknown as ReturnType<typeof vi.fn>

const ok = () => Promise.resolve({ ok: true, status: 200, data: new Blob(['x']) })
const failed = (status: number) => Promise.resolve({ ok: false, status, data: undefined })

describe('loadAvatar', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    doRequestMock.mockReset()
    clearAvatarCache()
    vi.stubGlobal(
      'URL',
      Object.assign(URL, { createObjectURL: () => 'blob:test', revokeObjectURL: vi.fn() }),
    )
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('requests a picture once however often it is shown', async () => {
    doRequestMock.mockImplementation(ok)

    const urls = await Promise.all([loadAvatar('/v2/avatars/a'), loadAvatar('/v2/avatars/a')])

    expect(urls).toEqual(['blob:test', 'blob:test'])
    expect(doRequestMock).toHaveBeenCalledTimes(1)
  })

  it('tries again after a rate limit or server error', async () => {
    doRequestMock
      .mockImplementationOnce(() => failed(429))
      .mockImplementationOnce(() => failed(1000))
      .mockImplementationOnce(ok)

    const result = loadAvatar('/v2/avatars/b')
    await vi.runAllTimersAsync()

    expect(await result).toBe('blob:test')
    expect(doRequestMock).toHaveBeenCalledTimes(3)
  })

  it('does not retry a picture that does not exist', async () => {
    doRequestMock.mockImplementation(() => failed(404))

    expect(await loadAvatar('/v2/avatars/c')).toBeUndefined()
    expect(doRequestMock).toHaveBeenCalledTimes(1)
  })

  it('does not remember a failure', async () => {
    doRequestMock.mockImplementationOnce(() => failed(404)).mockImplementationOnce(ok)

    expect(await loadAvatar('/v2/avatars/d')).toBeUndefined()
    expect(await loadAvatar('/v2/avatars/d')).toBe('blob:test')
  })
})
