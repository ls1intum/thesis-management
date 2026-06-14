import { Box, Group, Stack, Title } from '@mantine/core'

interface ITopicAdittionalInformationSectionProps {
  icon: React.ReactNode
  title: string
  content: React.ReactNode
}

const TopicAdittionalInformationSection = ({
  icon,
  title,
  content,
}: ITopicAdittionalInformationSectionProps) => {
  return (
    <Stack gap={'1rem'}>
      <Group gap={'0.5rem'}>
        {icon}
        <Title order={6}>{title}</Title>
      </Group>
      <Box px={'0.5rem'}>{content}</Box>
    </Stack>
  )
}

export default TopicAdittionalInformationSection
