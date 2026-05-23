import { use } from 'react'
import { TopicsContext } from './context'

export function useTopicsContext() {
  const data = use(TopicsContext)

  if (!data) {
    throw new Error('TopicsContext not initialized')
  }

  return data
}
