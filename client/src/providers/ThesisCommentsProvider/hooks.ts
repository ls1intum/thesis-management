import { use } from 'react'
import { ThesisCommentsContext } from './context'

export function useThesisCommentsContext() {
  const data = use(ThesisCommentsContext)

  if (!data) {
    throw new Error('ThesisCommentsContext not initialized')
  }

  return data
}
