import {
  Card,
  Divider,
  Group,
  Paper,
  Stack,
  Title,
  Text,
  useMantineColorScheme,
} from '@mantine/core'
import {
  IIntervieweeLightWithNextSlot,
  InterviewState,
} from '../../../requests/responses/interview'
import CustomAvatar from '../../../components/CustomAvatar/CustomAvatar'
import { getInterviewStateColor, scoreColorTranslate } from '../../../utils/format'
import { Link } from 'react-router'

interface IIntervieweeCardProps {
  interviewee: IIntervieweeLightWithNextSlot
  navigationLink: string
  flex?: number
}

const IntervieweeCard = ({ interviewee, navigationLink, flex }: IIntervieweeCardProps) => {
  const checkState = () => {
    return interviewee.score && interviewee.score >= 0
      ? InterviewState.COMPLETED
      : interviewee.nextSlot != null
        ? InterviewState.SCHEDULED
        : interviewee.lastInvited != null
          ? InterviewState.INVITED
          : InterviewState.UNCONTACTED
  }

  const state: InterviewState = checkState()

  const colorScheme = useMantineColorScheme()

  return (
    <Paper
      withBorder
      bg={scoreColorTranslate(interviewee.score ?? -1)}
      radius='md'
      component={Link}
      to={navigationLink}
      flex={flex}
    >
      <Card p={0} ml={8} radius='md'>
        <Stack p={0} gap={0}>
          <Group px={'1.5rem'} py={'0.75rem'} justify='space-between' align='center'>
            <Group miw={350} gap={'0.75rem'}>
              <CustomAvatar user={interviewee.user} size={32} />
              <Title order={5} lineClamp={1}>
                {interviewee.user.firstName} {interviewee.user.lastName}
              </Title>
            </Group>
            <Divider orientation='vertical' size={'sm'} />
            <Group flex={1}></Group>
          </Group>
          <Divider />
          <Group px={'1.5rem'} py={'0.75rem'} justify='space-between' align='center'>
            <Group gap={'0.5rem'}>
              <Divider orientation='vertical' size='lg' color={getInterviewStateColor(state)} />
              <Stack gap={0}>
                <Text
                  size='sm'
                  fw={500}
                  c={colorScheme.colorScheme === 'dark' ? 'gray.3' : 'gray.7'}
                >
                  {state}
                </Text>
              </Stack>
            </Group>
          </Group>
        </Stack>
      </Card>
    </Paper>
  )
}
export default IntervieweeCard
