import { Group, Paper, Stack, Title, Text } from '@mantine/core'
import { ILightUser } from '../../requests/responses/user'
import CustomAvatar from '../CustomAvatar/CustomAvatar'
import { formateStudyProgram, formatThesisType } from '../../utils/format'

interface IUserCardProps {
  user: ILightUser
  semester?: number
}

const UserCard = ({ user, semester }: IUserCardProps) => {
  return (
    <Paper withBorder radius='md' p='md'>
      <Group align='center'>
        <CustomAvatar user={user} size={48} />
        <Stack flex={1} gap={'0.25rem'}>
          <Title order={5}>
            {user.firstName} {user.lastName}
          </Title>
          <Text c='dimmed' size='sm'>
            {`${formateStudyProgram(user.studyProgram ?? '')} ${formatThesisType(user.studyDegree)}${semester ? ` - ${semester} Semester` : ''}`}
          </Text>
        </Stack>
      </Group>
    </Paper>
  )
}

export default UserCard
