import { Card, Group, Stack, Title, Text, Divider } from '@mantine/core'
import { useHover } from '@mantine/hooks'
import { IUpcomingInterview } from '../../../requests/responses/interview'
import CustomAvatar from '../../../components/CustomAvatar/CustomAvatar'
import { CalendarBlankIcon, ClockIcon, WebcamIcon } from '@phosphor-icons/react'
import { MapPinIcon } from '@phosphor-icons/react/dist/ssr'

interface IUpcomingInterviewCardProps {
  upcomingInterview: IUpcomingInterview
}

const UpcomingInterviewCard = ({ upcomingInterview }: IUpcomingInterviewCardProps) => {
  const { hovered, ref } = useHover()

  const getInterviewInfoItem = (icon: React.ReactNode, text: string, link?: string) => {
    return (
      <Group gap={'0.25rem'}>
        {icon}
        {link ? (
          <a href={link} target='_blank' rel='noopener noreferrer' style={{ color: 'inherit' }}>
            <Text c='dimmed' size='xs'>
              {text}
            </Text>
          </a>
        ) : (
          <Text c='dimmed' size='xs'>
            {text}
          </Text>
        )}
      </Group>
    )
  }

  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      h='100%'
      w='100%'
      style={{ cursor: 'pointer' }}
      ref={ref}
      onClick={() => {}}
    >
      <Stack gap={'1rem'}>
        <Group wrap='nowrap' gap={'1rem'} align='center'>
          <CustomAvatar user={upcomingInterview.user} size={50} />
          <Stack gap={'0.5rem'}>
            <Title order={6}>
              {`${upcomingInterview.user.firstName} ${upcomingInterview.user.lastName}`}
            </Title>
            <Text c='dimmed' size='xs'>
              {upcomingInterview.topicTitle}
            </Text>
          </Stack>
        </Group>
        <Group>
          <Divider orientation='vertical' size={'md'} />
          <Stack gap={'0.25rem'}>
            {getInterviewInfoItem(
              <CalendarBlankIcon color='gray' />,
              upcomingInterview.startDate.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              }),
            )}
            {getInterviewInfoItem(
              <ClockIcon color='gray' />,
              `${upcomingInterview.startDate.toLocaleString(undefined, {
                hour: 'numeric',
                minute: 'numeric',
              })} - ${upcomingInterview.endDate.toLocaleString(undefined, {
                hour: 'numeric',
                minute: 'numeric',
              })}`,
            )}

            {upcomingInterview.location &&
              getInterviewInfoItem(<MapPinIcon color='gray' />, upcomingInterview.location)}
            {upcomingInterview.streamUrl &&
              getInterviewInfoItem(
                <WebcamIcon color='gray' />,
                'Virtual',
                upcomingInterview.streamUrl,
              )}
          </Stack>
        </Group>
      </Stack>
    </Card>
  )
}

export default UpcomingInterviewCard
