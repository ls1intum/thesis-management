import {
  Button,
  Center,
  Divider,
  Flex,
  Group,
  Loader,
  ScrollArea,
  Stack,
  Title,
  Text,
} from '@mantine/core'
import { ChatCircleSlashIcon, PlusIcon } from '@phosphor-icons/react'
import { IInterviewProcess, IUpcomingInterview } from '../../requests/responses/interview'
import InterviewProcessCard from './components/InterviewProcessCard'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import UpcomingInterviewCard from './components/UpcomingInterviewCard'
import { useNavigate } from 'react-router'
import CreateInterviewProcess from './components/CreateInterviewProcess'
import { useEffect, useState } from 'react'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { doRequest } from '../../requests/request'
import { PaginationResponse } from '../../requests/responses/pagination'

const InterviewOverviewPage = () => {
  // TODO: Replace with real data from API
  const [upcomingInterviews, setUpcomingInterviews] = useState<IUpcomingInterview[]>([])

  const [interviewProcessesLoading, setInterviewProcessesLoading] = useState(false)
  const [interviewProcesses, setInterviewProcesses] = useState<IInterviewProcess[]>([])

  const isSmaller = useIsSmallerBreakpoint('sm')

  const navigate = useNavigate()

  const [createModalOpened, setCreateModalOpened] = useState(false)

  const fetchUpcomingInterviews = async () => {
    doRequest<IUpcomingInterview[]>(
      `/v2/interview-process/upcoming-interviews`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          const sorted = res.data.sort((a, b) => {
            return new Date(a.slot.startDate).getTime() - new Date(b.slot.startDate).getTime()
          })
          setUpcomingInterviews(sorted)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const fetchMyInterviewProcesses = async () => {
    setInterviewProcessesLoading(true)
    doRequest<PaginationResponse<IInterviewProcess>>(
      '/v2/interview-process',
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setInterviewProcesses(res.data.content)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setInterviewProcessesLoading(false)
      },
    )
  }

  useEffect(() => {
    fetchMyInterviewProcesses()
    fetchUpcomingInterviews()
  }, [])

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
            <Button
              size={isSmaller ? 'xs' : 'sm'}
              radius={isSmaller ? 'xl' : 'sm'}
              onClick={() => setCreateModalOpened(true)}
            >
              <Group wrap='nowrap'>
                <PlusIcon size={'16px'} />
                {isSmaller ? undefined : 'New Interview Process'}
              </Group>
            </Button>
          </Group>
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Stack p={0} gap={'1rem'}>
              {interviewProcessesLoading ? (
                <Center>
                  <Loader />
                </Center>
              ) : interviewProcesses.length === 0 ? (
                <Center h={'100%'} mih={isSmaller ? '30vh' : '50vh'}>
                  <Stack justify='center' align='center' h={'100%'}>
                    <ChatCircleSlashIcon size={60} />
                    <Stack gap={'0.5rem'} justify='center' align='center'>
                      <Title order={5}>No Interview Processes Found</Title>
                      <Text c='dimmed' ta={'center'}>
                        Add a interviewee while reviewing application or just create a process
                        directly
                      </Text>
                    </Stack>
                  </Stack>
                </Center>
              ) : (
                interviewProcesses.map((process) => (
                  <InterviewProcessCard
                    key={`card-${process.interviewProcessId}`}
                    interviewProcess={process}
                    onClick={() => {
                      navigate(`/interviews/${process.interviewProcessId}`)
                    }}
                  />
                ))
              )}
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
                  key={interview.slot.bookedBy?.intervieweeId}
                  upcomingInterview={interview}
                  onClick={() => {
                    navigate(
                      `/interviews/${interview.interviewProcessId}/interviewee/${interview.slot.bookedBy?.intervieweeId}`,
                    )
                  }}
                />
              ))}
            </Stack>
          </ScrollArea>
        </Stack>
      </Flex>
      <CreateInterviewProcess
        opened={createModalOpened}
        onClose={() => setCreateModalOpened(false)}
        setInterviewProcesses={setInterviewProcesses}
      />
      {/*TODO: ADD PAGINATION*/}
    </Stack>
  )
}

export default InterviewOverviewPage
