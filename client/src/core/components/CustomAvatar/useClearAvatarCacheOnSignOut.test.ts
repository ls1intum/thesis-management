import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { doRequest } from '@/core/requests/request'
import { loadAvatar } from '@/core/components/CustomAvatar/avatarCache'
import { useClearAvatarCacheOnSignOut } from '@/core/components/CustomAvatar/useClearAvatarCacheOnSignOut'

vi.mock('@/core/requests/request', () => ({ doRequest: vi.fn() }))

describe('useClearAvatarCacheOnSignOut', () => {
  const revokeObjectURL = vi.fn()

  beforeEach(() => {
    revokeObjectURL.mockReset()
    URL.createObjectURL = vi.fn(() => 'blob:cached')
    URL.revokeObjectURL = revokeObjectURL
    vi.mocked(doRequest as (...args: unknown[]) => unknown).mockResolvedValue({
      ok: true,
      status: 200,
      data: new Blob(['x']),
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('revokes the cached pictures when the session ends, without any avatar mounted', async () => {
    await loadAvatar('/v2/avatars/signed-out-test')

    const { rerender } = renderHook(({ signedIn }) => useClearAvatarCacheOnSignOut(signedIn), {
      initialProps: { signedIn: true },
    })
    expect(revokeObjectURL).not.toHaveBeenCalled()

    rerender({ signedIn: false })

    await vi.waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith('blob:cached'))
  })
})
