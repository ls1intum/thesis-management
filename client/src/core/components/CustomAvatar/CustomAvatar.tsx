import { use, useEffect, useState } from 'react'
import type { IMinimalUser } from '@/core/user/requests/responses/user'
import type { MantineSize } from '@mantine/core'
import { Avatar, type BoxProps } from '@mantine/core'
import { getAvatar, getAvatarPath } from '@/core/utils/user'
import { AuthenticationContext } from '@/core/providers/AuthenticationContext/context'
import { clearAvatarCache, loadAvatar } from '@/core/components/CustomAvatar/avatarCache'

interface ICustomAvatarProps extends BoxProps {
  user: IMinimalUser
  size?: MantineSize | number
}

export const CustomAvatar = (props: ICustomAvatarProps) => {
  const { user, size, ...other } = props
  const auth = use(AuthenticationContext)
  const isAuthenticated = auth?.isAuthenticated ?? false
  const [blobUrl, setBlobUrl] = useState<string | undefined>(undefined)

  const avatarPath = getAvatarPath(user)

  useEffect(() => {
    if (!isAuthenticated) {
      clearAvatarCache()
    }

    if (!avatarPath || !isAuthenticated) {
      setBlobUrl(undefined)
      return
    }

    let cancelled = false

    void loadAvatar(avatarPath).then((url) => {
      if (!cancelled) {
        setBlobUrl(url)
      }
    })

    return () => {
      cancelled = true
    }
  }, [avatarPath, isAuthenticated])

  const src = isAuthenticated ? blobUrl : getAvatar(user)

  return (
    <Avatar
      src={src}
      name={`${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || undefined}
      color='initials'
      size={size}
      {...other}
    />
  )
}
