import { Stack, Text, Title } from '@mantine/core'

interface IDateHeaderItemProps {
  date: string
  size?: 'md' | 'lg'
  disabled?: boolean
}

export const DateHeaderItem = ({ date, size = 'md', disabled = false }: IDateHeaderItemProps) => {
  const getTextSize = () => {
    switch (size) {
      case 'lg':
        return 'md'
      default:
        return 'xs'
    }
  }

  const getTitleSize = () => {
    switch (size) {
      case 'lg':
        return 2
      default:
        return 3
    }
  }

  return (
    <Stack gap={0} p={0} opacity={disabled ? 0.35 : 1}>
      <Text fw={600} c={'dimmed'} size={getTextSize()}>
        {new Date(date).toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase()}
      </Text>
      <Title order={getTitleSize()}>
        {new Date(date).toLocaleDateString('en-US', { day: '2-digit' })}
      </Title>
    </Stack>
  )
}
