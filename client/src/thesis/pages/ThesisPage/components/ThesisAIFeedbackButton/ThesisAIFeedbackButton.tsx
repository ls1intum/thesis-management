import { Button, Stack } from '@mantine/core'
import { Robot } from '@phosphor-icons/react'
import { doRequest } from '@/core/requests/request'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import type { IThesis } from '@/thesis/requests/responses/thesis'
import { ApiError } from '@/core/requests/handler'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { useReviewProgress } from '@/core/hooks/useReviewProgress'
import AiReviewProgress from '@/thesis/components/AiReviewProgress/AiReviewProgress'

interface IThesisAIFeedbackButtonProps {
  type: 'PROPOSAL' | 'THESIS'
  disabled?: boolean
}

/**
 * Student-facing "Get AI feedback" trigger. Fires the auto endpoint, which runs the review
 * pipeline server-side and persists each finding as a ThesisFeedback row with source=AI. On
 * success, the thesis provider is refreshed so the feedback overview shows the new items.
 *
 * Renders nothing when AI features are disabled — the server would not register the
 * `/v2/ai-review/**` endpoints, so the button would only 404.
 */
const ThesisAIFeedbackButton = ({ type, disabled }: IThesisAIFeedbackButtonProps) => {
  const { thesis } = useLoadedThesisContext()
  const { steps, total, start, stop } = useReviewProgress()

  const [loading, onClick] = useThesisUpdateAction(async () => {
    const jobId = crypto.randomUUID()
    start(jobId)
    try {
      const response = await doRequest<IThesis>('/v2/ai-review/auto', {
        method: 'POST',
        requiresAuth: true,
        data: {
          thesisId: thesis.thesisId,
          reviewType: type,
          jobId,
        },
      })

      if (response.ok) {
        return response.data
      } else {
        throw new ApiError(response)
      }
    } finally {
      stop()
    }
  }, 'AI feedback generated')

  if (!GLOBAL_CONFIG.ai_enabled) {
    return null
  }

  return (
    <Stack gap={0} align='flex-start'>
      <Button
        variant='outline'
        color='grape'
        leftSection={<Robot size={16} />}
        loading={loading}
        disabled={disabled}
        onClick={onClick}
      >
        Get AI Feedback
      </Button>
      {loading && <AiReviewProgress steps={steps} total={total} />}
    </Stack>
  )
}

export default ThesisAIFeedbackButton
