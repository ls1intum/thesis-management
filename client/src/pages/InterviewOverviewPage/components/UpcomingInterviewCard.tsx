import { Card, Group, Stack, Title, Text, Divider } from '@mantine/core'
import { useHover } from '@mantine/hooks'
import { IUpcomingInterview } from '../../../requests/responses/interview'
import CustomAvatar from '../../../components/CustomAvatar/CustomAvatar'
import InterviewSlotInformation from '../../../components/InterviewSlotInformation/InterviewSlotInformation'

interface IUpcomingInterviewCardProps {
  upcomingInterview: IUpcomingInterview
  onClick?: () => void
}

const UpcomingInterviewCard = ({ upcomingInterview, onClick }: IUpcomingInterviewCardProps) => {
  const { hovered, ref } = useHover()

  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      h='100%'
      w='100%'
      style={{ cursor: 'pointer' }}
      ref={ref}
      onClick={onClick}
    >
      <Stack gap={'1rem'}>
        <Group wrap='nowrap' gap={'1rem'} align='center'>
          {upcomingInterview.slot.bookedBy?.user && (
            <CustomAvatar user={upcomingInterview.slot.bookedBy?.user} size={50} />
          )}
          <Stack gap={'0.5rem'}>
            <Title order={6}>
              {`${upcomingInterview.slot.bookedBy?.user.firstName} ${upcomingInterview.slot.bookedBy?.user.lastName}`}
            </Title>
            <Text c='dimmed' size='xs'>
              {upcomingInterview.topicTitle}
            </Text>
          </Stack>
        </Group>
        <Group>
          <Divider orientation='vertical' size={'md'} />
          <InterviewSlotInformation slot={upcomingInterview.slot} />
        </Group>
      </Stack>
    </Card>
  )
}

export default UpcomingInterviewCard
