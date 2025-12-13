import {
  Modal,
  Title,
  Stack,
  Group,
  SegmentedControl,
  Divider,
  Flex,
  ScrollArea,
  Button,
  Accordion,
  Text,
  Center,
} from '@mantine/core'
import { Calendar } from '@mantine/dates'
import { useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { CalendarBlankIcon } from '@phosphor-icons/react'
import CollapsibleDateCard from './CollapsibleDateCard'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { IInterviewSlot } from '../../../requests/responses/interview'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError, showSimpleSuccess } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'

interface IAddSlotsModalProps {
  slotModalOpen: boolean
  setSlotModalOpen: (open: boolean) => void
  interviewSlotItems?: Record<string, IInterviewSlot[]>
  updateInterviewSlots: (newSlots: IInterviewSlot[]) => void
}

const AddSlotsModal = ({
  slotModalOpen,
  setSlotModalOpen,
  interviewSlotItems,
  updateInterviewSlots,
}: IAddSlotsModalProps) => {
  const { processId } = useParams<{ processId: string }>()

  //Show existing slots on selected dates or just selected dates
  const [selected, setSelected] = useState<Date[]>(
    interviewSlotItems ? Object.keys(interviewSlotItems).map((key) => new Date(key)) : [],
  )

  const [openDates, setOpenDates] = useState<string[]>(
    interviewSlotItems
      ? Object.keys(interviewSlotItems).map((key) => new Date(key).toDateString())
      : [],
  )

  const [modalSlots, setModalSlots] = useState<Record<string, IInterviewSlot[]>>(
    interviewSlotItems ? { ...interviewSlotItems } : {},
  )

  useEffect(() => {
    setSelected(
      interviewSlotItems ? Object.keys(interviewSlotItems).map((key) => new Date(key)) : [],
    )
    setOpenDates(
      interviewSlotItems
        ? Object.keys(interviewSlotItems).map((key) => new Date(key).toDateString())
        : [],
    )
    setModalSlots(interviewSlotItems ? { ...interviewSlotItems } : {})
  }, [interviewSlotItems])

  const isSmaller = useIsSmallerBreakpoint('sm')

  const [duration, setDuration] = useState<number>(30) // in minutes

  const [sameSlotsForAllDays, setSameSlotsForAllDays] = useState(false)

  const handleSelect = (date: Date) => {
    const isSelected = selected.some((s) => dayjs(s).isSame(date, 'day'))
    if (isSelected) {
      setSelected((current) => current.filter((d) => !dayjs(d).isSame(date, 'day')))
      setOpenDates((current) => current.filter((d) => d !== date.toDateString()))
    } else {
      setSelected((current) => [...current, date])
      setOpenDates((current) => [...current, date.toDateString()])
    }
  }

  const onClose = () => {
    setSlotModalOpen(false)
    setSelected(
      interviewSlotItems ? Object.keys(interviewSlotItems).map((key) => new Date(key)) : [],
    )
    setOpenDates(
      interviewSlotItems
        ? Object.keys(interviewSlotItems).map((key) => new Date(key).toDateString())
        : [],
    )
  }

  const [saveLoading, setSaveLoading] = useState(false)

  const saveNewSlots = async () => {
    setSaveLoading(true)
    doRequest<IInterviewSlot[]>(
      `/v2/interview-process/interview-slots`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          interviewProcessId: processId,
          interviewSlots: Object.values(modalSlots).flat(),
        },
      },
      (res) => {
        if (res.ok) {
          updateInterviewSlots(res.data)
          onClose()
          showSimpleSuccess('Interview slots added successfully')
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setSaveLoading(false)
      },
    )
  }

  return (
    <Modal
      centered
      size='1000px'
      opened={slotModalOpen}
      onClose={() => {
        onClose()
      }}
      title={<Title order={3}>Add Interview Slot</Title>}
    >
      <Stack gap={'2.5rem'} h={'70vh'}>
        <Group justify='flex-start' align='center' gap={isSmaller ? '0.5rem' : '2rem'}>
          <Stack gap={'0.25rem'}>
            <Title order={6} c={'dimmed'}>
              Interview Length
            </Title>
            <SegmentedControl
              data={['30min', '45min', '60min', '90min', 'Custome']}
              onChange={(durationString) => {
                if (durationString === 'Custome') {
                  return
                }
                const durationValue = parseInt(durationString.replace('min', ''))
                setDuration(durationValue)
              }}
              defaultValue={[30, 45, 60, 90].includes(duration) ? `${duration}min` : 'Custome'}
              color='primary'
            />
          </Stack>
          <Stack gap={'0.25rem'}>
            <Title order={6} c={'dimmed'}>
              Interview Breaks
            </Title>
            <SegmentedControl
              data={['0min', '5min', '10min', '15min', '30min', 'Custome']}
              onChange={() => {}}
              color='primary'
            />
          </Stack>
        </Group>

        <Flex
          h={'100%'}
          w={'100%'}
          direction={{ base: 'column', sm: 'row' }}
          justify={'space-between'}
          gap={{ base: '1rem', md: '2rem' }}
          style={{ overflow: 'auto' }}
        >
          <Stack w={{ base: '100%', sm: 'fit-content' }} h={'100%'}>
            <Title order={5}>Select Dates</Title>
            <Center w={'100%'}>
              <Calendar
                minDate={new Date()}
                getDayProps={(date) => {
                  const isPast = dayjs(date).isBefore(dayjs(), 'day')
                  return {
                    disabled: isPast,
                    selected: selected.some((s) => dayjs(s).isSame(date, 'day')),
                    onClick: isPast ? undefined : () => handleSelect(new Date(date)),
                  }
                }}
                highlightToday
                size={'md'}
              />
            </Center>
          </Stack>
          <Divider orientation='vertical' />
          <Stack h={'100%'} flex={1}>
            <Title order={5}>Create Slots</Title>
            {selected.length === 0 ? (
              <Center h={'100%'} mih={'250px'}>
                {/* TODO: FIGURE OUT HOW TO USE 100% of the height */}
                <Stack justify='center' align='center' h={'100%'}>
                  <CalendarBlankIcon size={40} />
                  <Stack gap={'0.5rem'} justify='center' align='center'>
                    <Title order={6}>No Date selected</Title>
                    <Text c='dimmed'>Please select one or multiple dates in the calendar</Text>
                  </Stack>
                </Stack>
              </Center>
            ) : (
              <ScrollArea h={'100%'} w={'100%'} type={'hover'} offsetScrollbars>
                <Stack mih={'350px'} justify='space-between'>
                  {/* TODO: FIGURE OUT HOW TO USE 100% of the height */}
                  <Accordion
                    chevronPosition='left'
                    variant={'unstyled'}
                    multiple
                    value={openDates}
                    onChange={setOpenDates}
                  >
                    {selected
                      .sort((a, b) => a.getTime() - b.getTime())
                      .map((date) => (
                        <CollapsibleDateCard
                          key={date.toDateString()}
                          date={date}
                          duration={duration}
                          slots={modalSlots?.[date.toISOString().split('T')[0]]}
                          addNewSlots={(newSlots: IInterviewSlot[]) => {
                            setModalSlots((prev) => ({
                              ...prev,
                              [date.toISOString().split('T')[0]]: newSlots,
                            }))
                          }}
                        />
                      ))}
                  </Accordion>
                  <Button
                    variant='outline'
                    size='xs'
                    onClick={() => {
                      setSameSlotsForAllDays(!sameSlotsForAllDays)
                    }}
                  >
                    Same Slots for all days
                  </Button>
                </Stack>
              </ScrollArea>
            )}
          </Stack>
        </Flex>

        <Button
          fullWidth
          onClick={() => {
            saveNewSlots()
          }}
        >
          Save Slots
        </Button>
      </Stack>
    </Modal>
  )
}
export default AddSlotsModal
