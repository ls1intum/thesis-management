import { ILightUser } from '../../requests/responses/user'
import { MantineSize, Stack } from '@mantine/core'
import AvatarUser from '../AvatarUser/AvatarUser'

interface IAvatarUserListProps {
  users: ILightUser[]
  withUniversityId?: boolean
  size?: MantineSize
  textSize?: MantineSize
  oneLine?: boolean
  textColor?: string
  fontWeight?: number | string
}

const AvatarUserList = (props: IAvatarUserListProps) => {
  const { users, withUniversityId, size, textSize, oneLine = false, textColor, fontWeight } = props

  if (oneLine && users.length > 0) {
    return (
      <AvatarUser
        user={users[0]}
        withUniversityId={withUniversityId}
        size={size}
        textSize={textSize}
        textColor={textColor}
        fontWeight={fontWeight}
      />
    )
  }

  return (
    <Stack gap='xs'>
      {users.map((user) => (
        <AvatarUser
          key={user.userId}
          user={user}
          withUniversityId={withUniversityId}
          size={size}
          textSize={textSize}
          textColor={textColor}
          fontWeight={fontWeight}
        />
      ))}
    </Stack>
  )
}

export default AvatarUserList
