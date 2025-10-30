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
import { useState } from 'react'
import dayjs from 'dayjs'
import { CalendarBlankIcon } from '@phosphor-icons/react'

interface IAddSlotsModalProps {
  slotModalOpen: boolean
  setSlotModalOpen: (open: boolean) => void
}

const AddSlotsModal = ({ slotModalOpen, setSlotModalOpen }: IAddSlotsModalProps) => {
  const [selected, setSelected] = useState<Date[]>([])

  const handleSelect = (date: Date) => {
    const isSelected = selected.some((s) => dayjs(s).isSame(date, 'day'))
    if (isSelected) {
      setSelected((current) => current.filter((d) => !dayjs(d).isSame(date, 'day')))
    } else {
      setSelected((current) => [...current, date])
    }
  }

  return (
    <Modal
      centered
      size='1000px'
      opened={slotModalOpen}
      onClose={() => {
        setSlotModalOpen(false)
        setSelected([])
      }}
      title={<Title order={3}>Add Interview Slot</Title>}
    >
      <Stack gap={'2.5rem'} h={'100%'}>
        <Group justify='flex-start' align='center' gap={'2rem'}>
          <Stack gap={'0.25rem'}>
            <Title order={6} c={'dimmed'}>
              Interview Length
            </Title>
            <SegmentedControl
              data={['15min', '30min', '45min', '60min', '90min']}
              onChange={() => {}}
              color='primary'
            />
          </Stack>
          <Stack gap={'0.25rem'}>
            <Title order={6} c={'dimmed'}>
              Interview Breaks
            </Title>
            <SegmentedControl
              data={['0min', '5min', '10min', '15min', '30min']}
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
          <Stack w={{ base: '100%', sm: 'fit-content' }} h={'100%'} align='center'>
            <Title order={5}>Select Dates</Title>
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
                  <Accordion chevronPosition='left' variant={'separated'}>
                    {selected
                      .sort((a, b) => a.getTime() - b.getTime())
                      .map((date) => (
                        <Accordion.Item key={date.toDateString()} value={date.toDateString()}>
                          <Accordion.Control>
                            <Text>{dayjs(date).format('DD.MM.YYYY')}</Text>
                          </Accordion.Control>
                          <Accordion.Panel>
                            <Stack>
                              <Text>Interview Length:</Text>
                              <Text>Interview Break:</Text>
                            </Stack>
                          </Accordion.Panel>
                        </Accordion.Item>
                      ))}
                  </Accordion>
                  <Button variant='outline' size='xs'>
                    Same Slots for all days
                  </Button>
                </Stack>
              </ScrollArea>
            )}
          </Stack>
        </Flex>

        <Button fullWidth>Save Slots</Button>
      </Stack>
    </Modal>
  )
}
export default AddSlotsModal
