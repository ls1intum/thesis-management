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
  Paper,
  useMantineColorScheme,
} from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import { IInterviewSlot } from '../../requests/responses/interview'
import { useCallback, useEffect, useState } from 'react'
import SummaryCard from './components/SummaryCard'
import {
  BuildingOfficeIcon,
  CalendarDotsIcon,
  ClockIcon,
  MapPinIcon,
  SubtitlesIcon,
  UsersIcon,
  XIcon,
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
import CancelSlotConfirmationModal from '../InterviewTopicOverviewPage/components/CancelSlotConfirmationModal'

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

  const [pageLoading, setPageLoading] = useState(true)
  const [myBooking, setMyBooking] = useState<IInterviewSlot | null>(null)

  const fetchMyBooking = async () => {
    setPageLoading(true)
    doRequest<IInterviewSlot>(
      `/v2/interview-process/${processId}/my-booking`,
      { method: 'GET', requiresAuth: true },
      (res) => {
        if (res.status === 204) {
          setMyBooking(null)
        } else if (res.ok) {
          setMyBooking({
            ...res.data,
            startDate: new Date(res.data.startDate),
            endDate: new Date(res.data.endDate),
          })
          setSelectedSlot(res.data)
        }
        setPageLoading(false)
      },
    )
  }

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
          setMyBooking({
            ...res.data,
            startDate: new Date(res.data.startDate),
            endDate: new Date(res.data.endDate),
          })
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setPageLoading(false)
      },
    )
  }

  useEffect(() => {
    fetchMyBooking().then(() => fetchTopicInformation())
  }, [])

  const [cancelModalOpen, setCancelModalOpen] = useState(false)

  const TopicInformation = topicInformation ? (
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
  ) : null

  const SlotInformation = (slot: IInterviewSlot, title?: string) => (
    <SummaryCard
      title={title || 'Selected Interview'}
      sections={[
        {
          title: 'Date',
          content: (
            <Text size='xs' pl={'xs'}>
              {slot.startDate.toLocaleDateString()}
            </Text>
          ),
          icon: <CalendarDotsIcon />,
        },
        {
          title: 'Time',
          content: (
            <Text size='xs' pl={'xs'}>
              {`${slot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${slot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}, ${`${Math.round(
                (slot.endDate.getTime() - slot.startDate.getTime()) / 60000,
              )} min`}`}
            </Text>
          ),
          icon: <ClockIcon />,
        },
        {
          title: 'Location',
          content: (
            <Text size='xs' pl={'xs'}>
              {slot.location || slot.streamUrl || 'Not specified'}
            </Text>
          ),
          icon: <MapPinIcon />,
        },
      ]}
    ></SummaryCard>
  )

  if (pageLoading) {
    return (
      <Center style={{ height: '100%' }}>
        <Loader />
      </Center>
    )
  }
  if (!user) {
    return (
      <Center style={{ height: '100%' }}>
        <Text>Please log in to book an interview slot.</Text>
      </Center>
    )
  }

  return (
    <InterviewProcessProvider excludeBookedSlots={true} autoFetchInterviewees={false}>
      {myBooking ? (
        <Center style={{ height: '100%' }}>
          <Stack align='center' gap={'2rem'}>
            <Stack gap={'0.5rem'} align='center'>
              <ClockIcon size={60} color='green' />
              <Title order={2}>Interview Scheduled</Title>
              <Text>You have booked an interview slot.</Text>
            </Stack>
            <Stack w={{ xs: '90vw', md: '500px' }} gap={'1rem'}>
              {TopicInformation}
              {SlotInformation(myBooking)}
            </Stack>

            <Paper withBorder p={'md'} radius='md' w={{ xs: '90vw', md: '500px' }}>
              <Stack>
                <Text size='sm' c='dimmed'>
                  Need to reschedule? Cancel this interview and select a new time slot.
                </Text>
                <Button
                  variant='outline'
                  leftSection={<XIcon size={16} />}
                  onClick={() => setCancelModalOpen(true)}
                  color={'red'}
                  size='xs'
                >
                  Cancel Interview
                </Button>
              </Stack>
            </Paper>

            <CancelSlotConfirmationModal
              cancelModalOpen={cancelModalOpen}
              setCancelModalOpen={setCancelModalOpen}
              slot={myBooking}
              onCancelSucessfull={() => {
                setMyBooking(null)
                setSelectedSlot(null)
              }}
            />
          </Stack>
        </Center>
      ) : (
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
                    {selectedSlot && SlotInformation(selectedSlot)}
                  </Collapse>
                  {TopicInformation}
                </Stack>
              </ScrollArea>
              <Button
                fullWidth
                onClick={() => selectedSlot && bookSlot(selectedSlot.slotId)}
                disabled={!selectedSlot}
              >
                Reserve Interview Slot
              </Button>
            </Stack>
          </Flex>
        </Stack>
      )}
    </InterviewProcessProvider>
  )
}

export default InterviewBookingPage
