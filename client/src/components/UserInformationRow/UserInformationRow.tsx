import { Group, Text } from '@mantine/core'
import { ILightUser } from '../../requests/responses/user'
import CustomAvatar from '../CustomAvatar/CustomAvatar'

type IUserInformationRowProps = {
  firstName?: string
  lastName?: string
  username?: string
  email?: string
  disableMessage?: String
  user?: ILightUser
}
const UserInformationRow = ({
  firstName,
  lastName,
  username,
  email,
  disableMessage,
  user,
}: IUserInformationRowProps) => {
  return (
    <Group gap='xs' wrap='nowrap' align='center' justify='flex-start'>
      {user && <CustomAvatar user={user} size={30} />}

      <div>
        <Group gap='xs'>
          <Text size='sm' fw={500}>
            {firstName} {lastName}
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
      </div>
    </Group>
  )
}

export default UserInformationRow
