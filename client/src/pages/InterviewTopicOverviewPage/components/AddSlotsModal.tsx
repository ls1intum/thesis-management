import {
  Modal,
  Title,
  Stack,
  Group,
  SegmentedControl,
  Divider,
  Flex,
  ScrollArea,
} from '@mantine/core'
import { Calendar } from '@mantine/dates'
import { useState } from 'react'
import dayjs from 'dayjs'

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
      onClose={() => setSlotModalOpen(false)}
      title={<Title order={3}>Add Interview Slot</Title>}
    >
      <Stack gap={'2rem'}>
        <Group justify='flex-start' align='center'>
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
          <Stack w={{ base: '100%', sm: 'fit-content' }} h={'100%'} gap={'1.5rem'}>
            <Calendar
              getDayProps={(date) => ({
                selected: selected.some((s) => dayjs(s).isSame(date, 'day')),
                onClick: () => handleSelect(new Date(date)),
              })}
              highlightToday
              size={'md'}
            />
          </Stack>
          <Divider orientation='vertical' />
          <Stack h={'100%'} flex={1} gap={'1.5rem'}>
            <Title order={5}>Create Slots</Title>
            <ScrollArea h={'100%'} w={'100%'} type={'hover'} offsetScrollbars>
              <Stack></Stack>
            </ScrollArea>
          </Stack>
        </Flex>
      </Stack>
    </Modal>
  )
}
export default AddSlotsModal
