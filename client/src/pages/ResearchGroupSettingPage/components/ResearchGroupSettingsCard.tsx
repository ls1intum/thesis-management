import { Card, Stack, Title, Text } from '@mantine/core'
import { ReactNode } from 'react'

interface IResearchGroupSettingsCardProps {
  title: string
  subtle?: string
  children: ReactNode
}

export function ResearchGroupSettingsCard({
  title,
  subtle,
  children,
}: IResearchGroupSettingsCardProps) {
  return (
    <Card
      bg='transparent'
      withBorder
      shadow='sm'
      radius='md'
      w='100%'
      style={{ display: 'flex', flexDirection: 'column' }}
    >
      <Stack>
        <Stack gap={5}>
          <Title order={3}>{title}</Title>
          {subtle && (
            <Text size='sm' c='dimmed'>
              {subtle}
            </Text>
          )}
        </Stack>
        {children}
      </Stack>
    </Card>
  )
}
