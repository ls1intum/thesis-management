import { Group, Stack, Title, Text } from '@mantine/core'
import { formatDate } from '../../../utils/format'
import { CalendarBlankIcon } from '@phosphor-icons/react'

interface IDateItemAdditionalInformationProps {
  label: string
  date: string
}

const DateItemAdditionalInformation = ({ label, date }: IDateItemAdditionalInformationProps) => {
  return (
    <Stack gap={'0.5rem'}>
      <Title order={6} c={'gray.7'}>
        {label}
      </Title>
      <Group gap={'0.5rem'}>
        <CalendarBlankIcon size={16} />
        <Text>{formatDate(date, { withTime: false })}</Text>
      </Group>
    </Stack>
  )
}

export default DateItemAdditionalInformation
