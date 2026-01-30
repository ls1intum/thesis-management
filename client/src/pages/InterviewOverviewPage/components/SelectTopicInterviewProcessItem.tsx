import { Badge, Divider, Group, Stack, Text, useMantineColorScheme } from '@mantine/core'
import { ITopicInterviewProcess } from '../../../requests/responses/interview'
import { useHover } from '@mantine/hooks'

interface ISelectTopicInterviewProcessItemProps {
  topic: ITopicInterviewProcess
  setSelectedTopic: (topic: ITopicInterviewProcess) => void
  isLastItem: boolean
}

const SelectTopicInterviewProcessItem = ({
  topic,
  setSelectedTopic,
  isLastItem,
}: ISelectTopicInterviewProcessItemProps) => {
  const { hovered, ref } = useHover()
  const colorScheme = useMantineColorScheme()

  return (
    <Stack
      key={topic.topicTitle}
      gap={0}
      pr={5}
      style={{ cursor: topic.interviewProcessExists ? 'not-allowed' : 'pointer' }}
      bg={
        hovered && !topic.interviewProcessExists
          ? colorScheme.colorScheme === 'dark'
            ? 'dark.6'
            : 'gray.2'
          : undefined
      }
    >
      <Group
        justify='space-between'
        align='center'
        onClick={() => {
          if (!topic.interviewProcessExists) {
            setSelectedTopic(topic)
          }
        }}
        p={'1rem'}
        ref={ref}
      >
        <Text size='sm' fw={600} c={topic.interviewProcessExists ? 'dimmed' : undefined}>
          {topic.topicTitle}
        </Text>
        {topic.interviewProcessExists && (
          <Badge color={'gray'} radius={'sm'}>
            Process exists
          </Badge>
        )}
      </Group>
      {!isLastItem && <Divider />}
    </Stack>
  )
}

export default SelectTopicInterviewProcessItem
