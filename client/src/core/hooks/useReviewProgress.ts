import { useCallback, useEffect, useRef, useState } from 'react'
import type { IReviewProgressEvent } from '@/core/ws/reviewProgressSocket'
import { subscribeToReviewProgress } from '@/core/ws/reviewProgressSocket'

export interface IReviewProgressStep {
  stepId: string
  label: string
  status: 'STARTED' | 'COMPLETED' | 'FAILED'
}

/**
 * Tracks the live per-call progress of one AI review job at a time. `start(jobId)` resets the step
 * list, subscribes, and resolves once the subscription is confirmed (or a bounded wait elapses) —
 * await it before firing the request the job belongs to, since progress events are not replayed.
 * `stop()` (also called automatically on unmount) should be invoked once that request has settled.
 */
export function useReviewProgress() {
  const [steps, setSteps] = useState<IReviewProgressStep[]>([])
  const [total, setTotal] = useState(0)
  const unsubscribeRef = useRef<() => void>(undefined)

  const stop = useCallback(() => {
    try {
      unsubscribeRef.current?.()
    } finally {
      unsubscribeRef.current = undefined
    }
  }, [])

  const start = useCallback(
    (jobId: string): Promise<void> => {
      stop()
      setSteps([])
      setTotal(0)

      const { ready, unsubscribe } = subscribeToReviewProgress(
        jobId,
        (event: IReviewProgressEvent) => {
          if (event.status === 'SUBSCRIBED') {
            return
          }
          const status = event.status

          setTotal(event.total)
          setSteps((prev) => {
            if (status === 'STARTED') {
              return [...prev, { stepId: event.stepId, label: event.label ?? event.stepId, status }]
            }
            return prev.map((step) => (step.stepId === event.stepId ? { ...step, status } : step))
          })
        },
      )

      unsubscribeRef.current = unsubscribe
      return ready
    },
    [stop],
  )

  useEffect(() => stop, [stop])

  return { steps, total, start, stop }
}
