import {
  Card,
  Group,
  Stack,
  Title,
  Text,
  Divider,
  SimpleGrid,
  Progress,
  Overlay,
  Badge,
  useMantineColorScheme,
} from '@mantine/core'
import { IInterviewProcess, InterviewState } from '../../../requests/responses/interview'
import { useHover } from '@mantine/hooks'
import { getInterviewStateColor } from '../../../utils/format'

interface IInterviewProcessCardProps {
  interviewProcess: IInterviewProcess
  onClick?: () => void
}

const InterviewProcessCard = ({ interviewProcess, onClick }: IInterviewProcessCardProps) => {
  const { hovered, ref } = useHover()

  const colorScheme = useMantineColorScheme()

  const interviewProcessSorted = {
    ...interviewProcess,
    statesNumbers: Object.fromEntries(
      Object.entries(interviewProcess.statesNumbers).sort(
        (a: [string, number], b: [string, number]) => {
          const aState = a[0]
          const bState = b[0]
          const order = ['uncontacted', 'invited', 'scheduled', 'completed']
          const ai = order.indexOf(aState.toLowerCase())
          const bi = order.indexOf(bState.toLowerCase())
          if (ai === -1 && bi === -1) return aState.localeCompare(bState)
          if (ai === -1) return 1
          if (bi === -1) return -1
          return ai - bi
        },
      ),
    ),
  }

  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      h='100%'
      w='100%'
      style={{ cursor: 'pointer' }}
      ref={ref}
      onClick={onClick}
    >
      {interviewProcessSorted.completed && (
        <Overlay
          color={colorScheme.colorScheme === 'dark' ? 'black' : 'gray.0'}
          backgroundOpacity={colorScheme.colorScheme === 'dark' ? 0.3 : 0.1}
        />
      )}
      <Stack pb={'1rem'} gap={'1.5rem'}>
        <Group>
          <Title order={5} flex={1}>
            {interviewProcessSorted.topicTitle}
          </Title>
          {interviewProcessSorted.completed && (
            <Badge variant='filled' color='gray.6' size='md' radius='sm'>
              Completed
            </Badge>
          )}
        </Group>
        <Stack gap={'0.75rem'}>
          <SimpleGrid cols={{ base: 2, lg: 4 }} spacing='sm'>
            {Object.entries(interviewProcessSorted.statesNumbers).map(([state, number]) => {
              return (
                <Group key={`${interviewProcessSorted.interviewProcessId}-${state}`} gap={'0.5rem'}>
                  <Divider
                    orientation='vertical'
                    size='lg'
                    color={getInterviewStateColor(state as InterviewState)}
                  />
                  <Stack gap={0}>
                    <Text size='lg' fw={700}>
                      {number}
                    </Text>
                    <Text
                      size='sm'
                      fw={500}
                      c={colorScheme.colorScheme === 'dark' ? 'gray.3' : 'gray.7'}
                    >
                      {state}
                    </Text>
                  </Stack>
                </Group>
              )
            })}
          </SimpleGrid>

          <Progress.Root size={16} radius='lg' w='100%'>
            {Object.entries(interviewProcessSorted.statesNumbers).map(([state, number]) => {
              return (
                <Progress.Section
                  key={`${interviewProcessSorted.interviewProcessId}-${state}-${number}`}
                  value={(number / interviewProcessSorted.totalInterviewees) * 100}
                  color={getInterviewStateColor(state as InterviewState)}
                ></Progress.Section>
              )
            })}
          </Progress.Root>
        </Stack>
      </Stack>
      <Card.Section withBorder inheritPadding py={'0.5rem'}>
        <Group justify='space-between' align='center'>
          <Text size='sm' c={'dimmed'} fw={400}>
            Total Interviewees
          </Text>
          <Text size='sm' fw={700}>
            {interviewProcessSorted.totalInterviewees}
          </Text>
        </Group>
      </Card.Section>
    </Card>
  )
}

export default InterviewProcessCard
