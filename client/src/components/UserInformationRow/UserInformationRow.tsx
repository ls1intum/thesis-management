import { Group, Stack, Text } from '@mantine/core'
import { ILightUser } from '../../requests/responses/user'
import CustomAvatar from '../CustomAvatar/CustomAvatar'

type IUserInformationRowProps = {
  firstName?: string
  lastName?: string
  username?: string
  email?: string
  disableMessage?: string
  user?: ILightUser
}
const UserInformationRow = ({
  firstName,
  lastName,
  username = 'No username',
  email = 'No email',
  disableMessage,
  user,
}: IUserInformationRowProps) => {
  return (
    <Group gap='xs' wrap='nowrap' align='center' justify='flex-start'>
      {user && <CustomAvatar user={user} size={30} />}

      <Stack gap={0}>
        <Group gap='xs'>
          <Text size='sm' fw={500}>
            {firstName && lastName ? `${firstName} ${lastName}` : 'No Name Provided'}
          </Text>
          {disableMessage && (
            <Text size='xs' c='red'>
              {disableMessage}
            </Text>
          )}
        </Group>
        <Text size='xs' c='dimmed'>
          @{username} • {email}
        </Text>
      </Stack>
    </Group>
  )
}

export default UserInformationRow
