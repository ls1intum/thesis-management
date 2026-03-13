import { useContext, useEffect, useRef, useState } from 'react'
import { IMinimalUser } from '../../requests/responses/user'
import { Avatar, MantineSize, type BoxProps } from '@mantine/core'
import { getAvatar, getAvatarPath } from '../../utils/user'
import { AuthenticationContext } from '../../providers/AuthenticationContext/context'
import { doRequest } from '../../requests/request'

interface ICustomAvatarProps extends BoxProps {
  user: IMinimalUser
  size?: MantineSize | number
}

export const CustomAvatar = (props: ICustomAvatarProps) => {
  const { user, size, ...other } = props
  const auth = useContext(AuthenticationContext)
  const isAuthenticated = auth?.isAuthenticated ?? false
  const [blobUrl, setBlobUrl] = useState<string | undefined>(undefined)
  const blobUrlRef = useRef<string | undefined>(undefined)

  const avatarPath = getAvatarPath(user)

  useEffect(() => {
    if (!avatarPath || !isAuthenticated) {
      setBlobUrl(undefined)
      return
    }

    const abort = doRequest<Blob>(
      avatarPath,
      { method: 'GET', requiresAuth: true, responseType: 'blob' },
      (response) => {
        if (response.ok) {
          const url = URL.createObjectURL(response.data)
          blobUrlRef.current = url
          setBlobUrl(url)
        }
      },
    )

    return () => {
      abort()
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current)
        blobUrlRef.current = undefined
      }
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
