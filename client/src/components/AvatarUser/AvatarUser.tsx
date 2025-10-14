import { ILightUser } from '../../requests/responses/user'
import { Group, MantineSize, Text } from '@mantine/core'
import { formatUser } from '../../utils/format'
import CustomAvatar from '../CustomAvatar/CustomAvatar'

interface IAvatarUserProps {
  user: ILightUser
  withUniversityId?: boolean
  size?: MantineSize
  textSize?: MantineSize
  textColor?: string
  fontWeight?: number | string
}

const AvatarUser = (props: IAvatarUserProps) => {
  const {
    user,
    withUniversityId = false,
    size = 'sm',
    textSize = 'sm',
    textColor,
    fontWeight,
  } = props

  return (
    <Group
      gap={5}
      preventGrowOverflow
      wrap='nowrap'
      style={{ overflow: 'hidden' }}
      justify='center'
      align='center'
    >
      <CustomAvatar user={user} size={size} />
      <Text
        size={textSize ?? size ?? undefined}
        truncate
        style={{ flex: 1, minWidth: 0 }}
        c={textColor}
        fw={fontWeight}
      >
        {formatUser(user, { withUniversityId })}
      </Text>
    </Group>
  )
}

export default AvatarUser
