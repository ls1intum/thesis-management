import {
  Button,
  Divider,
  Flex,
  Group,
  ScrollArea,
  Stack,
  Title,
  Text,
  Center,
  Loader,
  Collapse,
} from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import { IInterviewSlot } from '../../requests/responses/interview'
import { useEffect, useState } from 'react'
import SummaryCard from './components/SummaryCard'
import {
  BuildingOfficeIcon,
  CalendarDotsIcon,
  ClockIcon,
  MapPinIcon,
  SubtitlesIcon,
  UsersIcon,
} from '@phosphor-icons/react'
import { doRequest } from '../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { useAuthenticationContext, useUser } from '../../hooks/authentication'
import { ConfettiIcon } from '@phosphor-icons/react/dist/ssr'
import { ITopic } from '../../requests/responses/topic'
import AvatarUserList from '../../components/AvatarUserList/AvatarUserList'
import InterviewProcessProvider from '../../providers/InterviewProcessProvider/InterviewProcessProvider'
import SelectSlotCarousel from './components/SelectSlotCarousel'

const InterviewBookingPage = () => {
  const { processId } = useParams<{ processId: string }>()

  const isSmaller = useIsSmallerBreakpoint('sm')

  const [selectedSlot, setSelectedSlot] = useState<IInterviewSlot | null>(null)

  //Make sure user is logged in
  const user = useUser()
  const auth = useAuthenticationContext()

  useEffect(() => {
    if (!auth.isAuthenticated) {
      auth.login()

      const interval = setInterval(() => {
        auth.login()
      }, 1000)

      return () => clearInterval(interval)
    }
  }, [auth.isAuthenticated])

  const [topicInformation, setTopicInformation] = useState<ITopic | null>(null)

  const fetchTopicInformation = async () => {
    doRequest<ITopic>(
      `/v2/interview-process/${processId}/topic`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setTopicInformation(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  //TODO: Also move to context/provider
  const [pageLoading, setPageLoading] = useState(false)
  const [bookingSuccessful, setBookingSuccessful] = useState(false)

  const bookSlot = async (slotId: string) => {
    setPageLoading(true)
    doRequest<IInterviewSlot>(
      `/v2/interview-process/${processId}/slot/${slotId}/book`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: {
          intervieweeUserId: user?.userId,
        },
      },
      (res) => {
        if (res.ok) {
          setBookingSuccessful(true)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setPageLoading(false)
      },
    )
  }

  useEffect(() => {
    fetchTopicInformation()
  }, [])

  if (!user) {
    return (
      <Center style={{ height: '100%' }}>
        <Text>Please log in to book an interview slot.</Text>
      </Center>
    )
  }

  if (pageLoading) {
    return (
      <Center style={{ height: '100%' }}>
        <Loader />
      </Center>
    )
  }

  if (bookingSuccessful) {
    return (
      <Center style={{ height: '100%' }}>
        <Stack align='center'>
          <ConfettiIcon size={48} color='green' />
          <Text>Your interview slot has been successfully booked!</Text>
        </Stack>
      </Center>
    )
  }

  return (
    <InterviewProcessProvider excludeBookedSlots={true}>
      <Stack gap={'2rem'} h={'100%'}>
        <Title>Select your Interview Slot</Title>
        <Flex
          h={'100%'}
          w={'100%'}
          direction={{ base: 'column', md: 'row' }}
          justify={'space-between'}
          gap={{ base: '1rem', md: '2rem' }}
          style={{ overflow: 'auto' }}
        >
          <Stack h={'100%'} flex={1} gap={'1.5rem'}>
            <SelectSlotCarousel
              selectedSlot={selectedSlot}
              setSelectedSlot={setSelectedSlot}
            ></SelectSlotCarousel>
            <Group justify='end' align='center' py={2}>
              <Button variant='outline'>Not available on any slot</Button>
            </Group>
          </Stack>

          <Divider orientation='vertical' />
          <Stack w={{ base: '100%', md: '25%' }} h={'100%'} gap={'1.5rem'}>
            <Title order={3}>Summary</Title>
            <ScrollArea
              h={'100%'}
              w={'100%'}
              type={isSmaller ? 'never' : 'hover'}
              offsetScrollbars
              flex={1}
            >
              <Stack p={0} h={'100%'}>
                <Collapse in={selectedSlot !== null}>
                  {selectedSlot && (
                    <SummaryCard
                      title={'Selected Interview'}
                      sections={[
                        {
                          title: 'Date',
                          content: (
                            <Text size='xs' pl={'xs'}>
                              {selectedSlot.startDate.toLocaleDateString()}
                            </Text>
                          ),
                          icon: <CalendarDotsIcon />,
                        },
                        {
                          title: 'Time',
                          content: (
                            <Text size='xs' pl={'xs'}>
                              {`${selectedSlot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${selectedSlot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}, ${`${Math.round(
                                (selectedSlot.endDate.getTime() -
                                  selectedSlot.startDate.getTime()) /
                                  60000,
                              )} min`}`}
                            </Text>
                          ),
                          icon: <ClockIcon />,
                        },
                        {
                          title: 'Location',
                          content: (
                            <Text size='xs' pl={'xs'}>
                              {selectedSlot.location || selectedSlot.streamUrl || 'Not specified'}
                            </Text>
                          ),
                          icon: <MapPinIcon />,
                        },
                      ]}
                    ></SummaryCard>
                  )}
                </Collapse>
                {topicInformation && (
                  <SummaryCard
                    title={'Thesis Topic'}
                    sections={[
                      {
                        title: 'Title',
                        content: (
                          <Text size='xs' pl={'xs'}>
                            {topicInformation.title}
                          </Text>
                        ),
                        icon: <SubtitlesIcon />,
                      },
                      {
                        title: 'Research Group',
                        content: (
                          <Text size='xs' pl={'xs'}>
                            {topicInformation.researchGroup.name}
                          </Text>
                        ),
                        icon: <BuildingOfficeIcon />,
                      },
                      {
                        title: 'Advisor(s)',
                        content: <AvatarUserList users={topicInformation.advisors} size='xs' />,
                        icon: <UsersIcon />,
                      },
                      {
                        title: 'Supervisor(s)',
                        content: <AvatarUserList users={topicInformation.supervisors} size='xs' />,
                        icon: <UsersIcon />,
                      },
                    ]}
                  />
                )}
              </Stack>
            </ScrollArea>
            <Button fullWidth onClick={() => selectedSlot && bookSlot(selectedSlot.slotId)}>
              Reserve Interview Slot
            </Button>
          </Stack>
        </Flex>
      </Stack>
    </InterviewProcessProvider>
  )
}

export default InterviewBookingPage
