import { Card, Group, Stack, Title, useMantineColorScheme, Text } from '@mantine/core'

interface ISummaryCardProps {
  title: string
  sections?: {
    title: string
    icon?: React.ReactNode
    content: React.ReactNode | null
  }[]
}

const SummaryCard = ({ title, sections }: ISummaryCardProps) => {
  const colorScheme = useMantineColorScheme()

  return (
    <Card withBorder radius='md' p={'0.75rem'}>
      <Stack>
        <Title order={6} c={colorScheme.colorScheme === 'dark' ? 'dark.2' : 'gray.8'}>
          {title}
        </Title>
        {sections?.map((section) => (
          <Stack key={section.title} gap={'0.25rem'}>
            <Group gap={'0.25rem'} align='center'>
              {section.icon}
              <Text fw={600} size='sm'>
                {section.title}
              </Text>
            </Group>
            {section.content}
          </Stack>
        ))}
      </Stack>
    </Card>
  )
}
export default SummaryCard
