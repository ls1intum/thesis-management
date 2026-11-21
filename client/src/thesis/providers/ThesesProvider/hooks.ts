import { use } from 'react'
import { ThesesContext } from '@/thesis/providers/ThesesProvider/context'

export function useThesesContext() {
  const data = use(ThesesContext)

  if (!data) {
    throw new Error('ThesesContext not initialized')
  }

  return data
}
