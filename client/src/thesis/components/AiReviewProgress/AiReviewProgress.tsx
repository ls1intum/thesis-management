import { Group, Loader, Progress, Stack, Text } from '@mantine/core'
import { CheckCircle, XCircle } from '@phosphor-icons/react'
import type { IReviewProgressStep } from '@/core/hooks/useReviewProgress'

interface IAiReviewProgressProps {
  steps: IReviewProgressStep[]
  total: number
}

/**
 * Live checklist of the individual LLM calls an AI review makes (one per category, plus a merge
 * call) — shown under the button that triggered the review while it is in flight.
 */
const AiReviewProgress = ({ steps, total }: IAiReviewProgressProps) => {
  if (steps.length === 0) {
    return null
  }

  const doneCount = steps.filter((step) => step.status !== 'STARTED').length
  const hasFailure = steps.some((step) => step.status === 'FAILED')

  return (
    <Stack gap={4} mt='xs' w='100%'>
      <Progress.Root size='sm'>
        <Progress.Section
          value={total > 0 ? (doneCount / total) * 100 : 0}
          color={hasFailure ? 'red' : 'grape'}
        />
      </Progress.Root>
      <Text size='xs' c='dimmed'>
        {doneCount}/{total || steps.length} AI calls finished
      </Text>
      <Stack gap={2}>
        {steps.map((step) => (
          <Group key={step.stepId} gap={6} wrap='nowrap'>
            {step.status === 'STARTED' && <Loader size={12} />}
            {step.status === 'COMPLETED' && (
              <CheckCircle size={14} weight='fill' color='var(--mantine-color-green-6)' />
            )}
            {step.status === 'FAILED' && (
              <XCircle size={14} weight='fill' color='var(--mantine-color-red-6)' />
            )}
            <Text size='xs' c={step.status === 'FAILED' ? 'red' : 'dimmed'}>
              {step.label}
            </Text>
          </Group>
        ))}
      </Stack>
    </Stack>
  )
}

export default AiReviewProgress
