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
      {interviewProcess.completed && (
        <Overlay
          color={colorScheme.colorScheme === 'dark' ? 'black' : 'gray.0'}
          backgroundOpacity={colorScheme.colorScheme === 'dark' ? 0.3 : 0.1}
        />
      )}
      <Stack pb={'1rem'} gap={'1.5rem'}>
        <Group>
          <Title order={5} flex={1}>
            {interviewProcess.topicTitle}
          </Title>
          {interviewProcess.completed && (
            <Badge variant='filled' color='gray.6' size='md' radius='sm'>
              Completed
            </Badge>
          )}
        </Group>
        <Stack gap={'0.75rem'}>
          <SimpleGrid cols={{ base: 2, lg: 4 }} spacing='sm'>
            {Object.entries(interviewProcess.statesNumbers).map(([state, number]) => {
              return (
                <Group key={`${interviewProcess.interviewProcessId}-${state}`} gap={'0.5rem'}>
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
            {Object.entries(interviewProcess.statesNumbers).map(([state, number]) => {
              return (
                <Progress.Section
                  key={`${interviewProcess.interviewProcessId}-${state}-${number}`}
                  value={(number / interviewProcess.totalInterviewees) * 100}
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
            {interviewProcess.totalInterviewees}
          </Text>
        </Group>
      </Card.Section>
    </Card>
  )
}

export default InterviewProcessCard
