import { useContext } from 'react'
import { InterviewProcessContext } from './context'

export function useInterviewProcessContext() {
  const data = useContext(InterviewProcessContext)

  if (!data) {
    throw new Error('InterviewProcessContext not initialized')
  }

  return data
}
