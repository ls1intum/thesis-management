import { useEffect } from 'react'
import { clearAvatarCache } from '@/core/components/CustomAvatar/avatarCache'

/**
 * Drops the cached pictures when the session ends. It is used by the authentication provider, which stays mounted
 * when a session expires, while the protected pages (and the avatars on them) are removed without further notice.
 */
export const useClearAvatarCacheOnSignOut = (isAuthenticated: boolean) => {
  useEffect(() => {
    if (!isAuthenticated) {
      clearAvatarCache()
    }
  }, [isAuthenticated])
}
