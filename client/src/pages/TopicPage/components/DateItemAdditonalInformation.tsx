import { Group, Stack, Title, Text } from '@mantine/core'
import { CalendarBlank } from 'phosphor-react'
import { formatDate } from '../../../utils/format'

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
        <CalendarBlank size={16} />
        <Text>{formatDate(date, { withTime: false })}</Text>
      </Group>
    </Stack>
  )
}

export default DateItemAdditionalInformation
