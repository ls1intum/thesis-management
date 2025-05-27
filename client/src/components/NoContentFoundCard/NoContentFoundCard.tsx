import { Card, Stack, Text, Title, Center } from '@mantine/core'
import { ReactNode } from 'react'

interface NoContentFoundCardProps {
  icon: ReactNode
  title: string
  subtle?: string
}

const NoContentFoundCard = ({ icon, title, subtle }: NoContentFoundCardProps) => {
  return (
    <Card
      withBorder
      shadow='sm'
      radius='md'
      w='100%'
      style={{ minHeight: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
    >
      <Center w='100%'>
        <Stack align='center' gap='xs'>
          {icon}
          <Title order={4}>{title}</Title>
          {subtle && (
            <Text c='dimmed' size='sm' ta='center'>
              {subtle}
            </Text>
          )}
        </Stack>
      </Center>
    </Card>
  )
}

export default NoContentFoundCard
