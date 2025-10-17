import { Button, Divider, Flex, Group, ScrollArea, Stack, Title } from '@mantine/core'
import { PlusIcon } from '@phosphor-icons/react'
import { IInterviewProcess, IUpcomingInterview } from '../../requests/responses/interview'
import InterviewProcessCard from './components/InterviewProcessCard'
import { useIsSmallerBreakpoint } from '../../hooks/theme'

const InterviewOverviewPage = () => {
  // TODO: Replace with real data from API
  const interviewProcesses: IInterviewProcess[] = [
    {
      interviewProcessId: '1',
      topicTitle: 'Integrating Gender Sensitivity and Adaptive Learning in Education Games',
      completed: false,
      totalInterviewees: 17,
      statesNumbers: {
        Uncontacted: 3,
        Invited: 4,
        Scheduled: 1,
        Completed: 9,
      },
    },
    {
      interviewProcessId: '2',
      topicTitle: 'Sustainable Software Engineering for RAG based Knowledge Management',
      completed: false,
      totalInterviewees: 18,
      statesNumbers: {
        Uncontacted: 9,
        Invited: 0,
        Scheduled: 4,
        Completed: 5,
      },
    },
    {
      interviewProcessId: '3',
      topicTitle: 'Benchmarking LLMs in an Educational Setting',
      completed: false,
      totalInterviewees: 6,
      statesNumbers: {
        Uncontacted: 3,
        Invited: 3,
        Scheduled: 2,
        Completed: 0,
      },
    },
    {
      interviewProcessId: '3',
      topicTitle: 'LLM Based Feedback Suggestion',
      completed: true,
      totalInterviewees: 6,
      statesNumbers: {
        Uncontacted: 3,
        Invited: 3,
        Scheduled: 2,
        Completed: 0,
      },
    },
  ]

  const upcomingInterviews: IUpcomingInterview[] = [
    {
      intervieweeId: '1',
      firstName: 'Alice',
      lastName: 'Johnson',
      avatarLink: '',
      startDate: new Date('2024-07-10T10:00:00'),
      endDate: new Date('2024-07-10T11:00:00'),
      location: 'Room 101',
    },
    {
      intervieweeId: '2',
      firstName: 'Bob',
      lastName: 'Smith',
      avatarLink: '',
      startDate: new Date('2024-07-11T14:30:00'),
      endDate: new Date('2024-07-11T15:30:00'),
      streamUrl: 'https://example.com/stream/bob-smith',
    },
  ]

  const isSmaller = useIsSmallerBreakpoint('sm')

  return (
    <Stack h={'100%'} gap={'2rem'}>
      <Title>Interviews</Title>

      <Flex h={'100%'} w={'100%'} direction={'row'} justify={'space-between'} gap={'2rem'}>
        <Stack h={'100%'} flex={1} gap={'1.5rem'}>
          <Group w={'100%'} justify='space-between' align='center' gap={5}>
            <Title order={3}>Interview Topics</Title>
            <Button size={isSmaller ? 'xs' : 'sm'} radius={isSmaller ? 'xl' : 'sm'}>
              <Group wrap='nowrap'>
                <PlusIcon size={'16px'} />
                {isSmaller ? undefined : 'New Interview Process'}
              </Group>
            </Button>
          </Group>
          <ScrollArea h={'100%'} w={'100%'}>
            <Stack p={0} gap={'1rem'}>
              {interviewProcesses.map((process) => (
                <InterviewProcessCard key={process.interviewProcessId} interviewProcess={process} />
              ))}
            </Stack>
          </ScrollArea>
        </Stack>
        <Divider orientation='vertical' />
        <Stack w={{ base: '40%', md: '33%' }} h={'100%'}>
          <Title order={3}>Upcoming Interviews</Title>
        </Stack>
      </Flex>
    </Stack>
  )
}

export default InterviewOverviewPage
