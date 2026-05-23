import { use } from 'react'
import { InterviewProcessContext } from './context'

export function useInterviewProcessContext() {
  const data = use(InterviewProcessContext)

  if (!data) {
    throw new Error('InterviewProcessContext not initialized')
  }

  return data
}
