import { Badge, Flex, Group, Stack, Text } from '@mantine/core'
import { ILightUser } from '../../requests/responses/user'
import CustomAvatar from '../CustomAvatar/CustomAvatar'

type IUserInformationRowProps = {
  firstName?: string
  lastName?: string
  username?: string
  email?: string
  disableMessage?: string
  user?: ILightUser
  researchGroupAdmin?: boolean
}
const UserInformationRow = ({
  firstName,
  lastName,
  username = 'No username',
  email = 'No email',
  disableMessage,
  user,
  researchGroupAdmin = false,
}: IUserInformationRowProps) => {
  return (
    <Group gap='xs' wrap='nowrap' align='center' justify='flex-start'>
      {user && <CustomAvatar user={user} size={30} />}

      <Stack gap={0}>
        <Flex
          gap={{ base: '0px', sm: 'xs' }}
          align={{ base: 'flex-start', sm: 'center' }}
          justify={{ base: 'center', sm: 'flex-start' }}
          direction={{ base: 'column', sm: 'row' }}
        >
          <Text size='sm' fw={500}>
            {firstName && lastName ? `${firstName} ${lastName}` : 'No Name Provided'}
          </Text>
          {researchGroupAdmin && (
            <>
              <Badge variant='outline' mb={5} my={5} style={{ flexShrink: 0 }} size='xs'>
                Group Admin
              </Badge>
            </>
          )}
          {disableMessage && (
            <Text size='xs' c='red'>
              {disableMessage}
            </Text>
          )}
        </Flex>

        <Text size='xs' c='dimmed'>
          @{username} • {email}
        </Text>
      </Stack>
    </Group>
  )
}

export default UserInformationRow
