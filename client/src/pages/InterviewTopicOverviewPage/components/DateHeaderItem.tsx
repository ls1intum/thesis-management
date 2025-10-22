import { Stack, Text, Title } from '@mantine/core'

interface IDateHeaderItemProps {
  date: string
}

export const DateHeaderItem = ({ date }: IDateHeaderItemProps) => {
  return (
    <Stack gap={0} p={0}>
      <Text fw={600} c={'dimmed'} size='xs'>
        {new Date(date).toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase()}
      </Text>
      <Title order={3}>{new Date(date).toLocaleDateString('en-US', { day: '2-digit' })}</Title>
    </Stack>
  )
}
