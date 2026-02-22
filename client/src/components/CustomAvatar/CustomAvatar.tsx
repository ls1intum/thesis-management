import { IMinimalUser } from '../../requests/responses/user'
import { Avatar, MantineSize, type BoxProps } from '@mantine/core'
import { getAvatar } from '../../utils/user'

interface ICustomAvatarProps extends BoxProps {
  user: IMinimalUser
  size?: MantineSize | number
}

export const CustomAvatar = (props: ICustomAvatarProps) => {
  const { user, size, ...other } = props

  return (
    <Avatar
      src={getAvatar(user)}
      name={`${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || undefined}
      color='initials'
      size={size}
      {...other}
    />
  )
}
