import { useCallback, useEffect, useRef, useState } from 'react'
import type { IReviewProgressEvent } from '@/core/ws/reviewProgressSocket'
import { subscribeToReviewProgress } from '@/core/ws/reviewProgressSocket'

export interface IReviewProgressStep {
  stepId: string
  label: string
  status: 'STARTED' | 'COMPLETED' | 'FAILED'
}

/**
 * Tracks the live per-call progress of one AI review job at a time. `start(jobId)` resets the
 * step list and subscribes; the returned unsubscribe function (also called automatically on
 * unmount) should be invoked once the request the job belongs to has settled.
 */
export function useReviewProgress() {
  const [steps, setSteps] = useState<IReviewProgressStep[]>([])
  const [total, setTotal] = useState(0)
  const unsubscribeRef = useRef<() => void>(undefined)

  const start = useCallback((jobId: string) => {
    unsubscribeRef.current?.()
    setSteps([])
    setTotal(0)

    const unsubscribe = subscribeToReviewProgress(jobId, (event: IReviewProgressEvent) => {
      setTotal(event.total)
      setSteps((prev) => {
        if (event.status === 'STARTED') {
          return [
            ...prev,
            { stepId: event.stepId, label: event.label ?? event.stepId, status: 'STARTED' },
          ]
        }
        return prev.map((step) =>
          step.stepId === event.stepId ? { ...step, status: event.status } : step,
        )
      })
    })

    unsubscribeRef.current = unsubscribe
    return unsubscribe
  }, [])

  const stop = useCallback(() => {
    unsubscribeRef.current?.()
    unsubscribeRef.current = undefined
  }, [])

  useEffect(() => stop, [stop])

  return { steps, total, start, stop }
}
