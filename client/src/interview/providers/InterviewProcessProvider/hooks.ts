import { use } from 'react'
import { InterviewProcessContext } from '@/interview/providers/InterviewProcessProvider/context'

export function useInterviewProcessContext() {
  const data = use(InterviewProcessContext)

  if (!data) {
    throw new Error('InterviewProcessContext not initialized')
  }

  return data
}
