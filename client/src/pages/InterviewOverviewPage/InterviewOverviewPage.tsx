import { Button, Divider, Flex, Group, ScrollArea, Stack, Title } from '@mantine/core'
import { PlusIcon } from '@phosphor-icons/react'
import { IInterviewProcess, IUpcomingInterview } from '../../requests/responses/interview'
import InterviewProcessCard from './components/InterviewProcessCard'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import UpcomingInterviewCard from './components/UpcomingInterviewCard'
import { useNavigate } from 'react-router'

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
      interviewProcessId: '4',
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
      interviewProcessId: '1',
      user: {
        userId: '2',
        universityId: 'ge45toc',
        avatar: null,
        matriculationNumber: '087262524',
        firstName: 'Alice',
        lastName: 'Smith',
        email: 'alice.smith@example.com',
        studyDegree: 'Master',
        studyProgram: 'Computer Science',
        customData: null,
        joinedAt: '2024-07-11T14:30:00',
        groups: [],
      },
      topicTitle: 'Integrating Gender Sensitivity and Adaptive Learning in Education Games',
      startDate: new Date('2024-07-10T10:00:00'),
      endDate: new Date('2024-07-10T11:00:00'),
      location: 'Room 101',
    },
    {
      intervieweeId: '2',
      interviewProcessId: '2',
      user: {
        userId: '2',
        universityId: 'ge45toc',
        avatar: null,
        matriculationNumber: '087262524',
        firstName: 'Bob',
        lastName: 'Smith',
        email: 'bob.smith@example.com',
        studyDegree: 'Master',
        studyProgram: 'Computer Science',
        customData: null,
        joinedAt: '2024-07-11T14:30:00',
        groups: [],
      },
      startDate: new Date('2024-07-11T14:30:00'),
      endDate: new Date('2024-07-11T15:30:00'),
      topicTitle: 'Sustainable Software Engineering for RAG based Knowledge Management',
      streamUrl: 'https://example.com/stream/bob-smith',
    },
    {
      intervieweeId: '3',
      interviewProcessId: '2',
      user: {
        userId: '2',
        universityId: 'ge45toc',
        avatar: null,
        matriculationNumber: '087262524',
        firstName: 'Charlie',
        lastName: 'Smith',
        email: 'charlie.smith@example.com',
        studyDegree: 'Master',
        studyProgram: 'Computer Science',
        customData: null,
        joinedAt: '2024-07-11T14:30:00',
        groups: [],
      },
      startDate: new Date('2024-07-11T14:30:00'),
      endDate: new Date('2024-07-11T15:30:00'),
      topicTitle: 'Sustainable Software Engineering for RAG based Knowledge Management',
      streamUrl: 'https://example.com/stream/bob-smith',
    },
    {
      intervieweeId: '4',
      interviewProcessId: '2',
      user: {
        userId: '2',
        universityId: 'ge45toc',
        avatar: null,
        matriculationNumber: '087262524',
        firstName: 'Diana',
        lastName: 'Smith',
        email: 'diana.smith@example.com',
        studyDegree: 'Master',
        studyProgram: 'Computer Science',
        customData: null,
        joinedAt: '2024-07-11T14:30:00',
        groups: [],
      },
      startDate: new Date('2024-07-11T14:30:00'),
      endDate: new Date('2024-07-11T15:30:00'),
      topicTitle: 'Sustainable Software Engineering for RAG based Knowledge Management',
      streamUrl: 'https://example.com/stream/bob-smith',
    },
  ]

  const isSmaller = useIsSmallerBreakpoint('sm')

  const navigate = useNavigate()

  return (
    <Stack h={'100%'} gap={'2rem'}>
      <Title>Interviews</Title>

      <Flex
        h={'100%'}
        w={'100%'}
        direction={{ base: 'column', md: 'row' }}
        justify={'space-between'}
        gap={{ base: '1rem', md: '2rem' }}
        style={{ overflow: 'auto' }}
      >
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
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Stack p={0} gap={'1rem'}>
              {interviewProcesses.map((process) => (
                <InterviewProcessCard
                  key={process.interviewProcessId}
                  interviewProcess={process}
                  onClick={() => {
                    navigate(`/interviews/${process.interviewProcessId}`)
                  }}
                />
              ))}
            </Stack>
          </ScrollArea>
        </Stack>
        <Divider orientation='vertical' />
        <Stack w={{ base: '100%', md: '33%' }} h={'100%'} gap={'1.5rem'}>
          <Title order={3} h={36}>
            Upcoming Interviews
          </Title>
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Stack p={0} gap={'1rem'}>
              {upcomingInterviews.map((interview) => (
                <UpcomingInterviewCard
                  key={interview.intervieweeId}
                  upcomingInterview={interview}
                  onClick={() => {
                    navigate(
                      `/interviews/${interview.interviewProcessId}/interviewee/${interview.intervieweeId}`,
                    )
                  }}
                />
              ))}
            </Stack>
          </ScrollArea>
        </Stack>
      </Flex>
    </Stack>
  )
}

export default InterviewOverviewPage
