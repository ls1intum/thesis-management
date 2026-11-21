import { useMemo, useReducer } from 'react'

interface IUseSignalReturnType {
  signal: Promise<unknown>
  ref: { isTriggerred: boolean }
  triggerSignal: () => unknown
}

export function useSignal(): IUseSignalReturnType {
  // forceRender bumps a counter to force a re-render of the consuming
  // component when the signal is triggered; the counter value itself is not
  // exposed.
  const [, forceRender] = useReducer((x: number) => x + 1, 0)

  return useMemo(() => {
    let externalResolve: (x: boolean) => unknown
    const ref: { isTriggerred: boolean } = { isTriggerred: false }

    const signal = new Promise((resolve) => {
      externalResolve = resolve

      return true
    })

    return {
      signal,
      ref,
      triggerSignal: () => {
        externalResolve(true)
        ref.isTriggerred = true

        forceRender()
      },
    }
  }, [])
}
