import { useEffect, useSyncExternalStore } from 'react'

/**
 * Login prompts (passkey registration, profile picture, ...) are modal dialogs. This hook makes sure
 * only one of them is open at a time: the first prompt that wants to open claims the slot, others wait
 * until it is released (dialog closed or component unmounted).
 */
let owner: string | undefined
const listeners = new Set<() => void>()

const setOwner = (next: string | undefined) => {
  owner = next
  listeners.forEach((listener) => listener())
}

const subscribe = (listener: () => void) => {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

const getOwner = () => owner

export function usePromptSlot(id: string, wanted: boolean): boolean {
  const currentOwner = useSyncExternalStore(subscribe, getOwner)

  useEffect(() => {
    if (wanted && owner === undefined) {
      setOwner(id)
    } else if (!wanted && owner === id) {
      setOwner(undefined)
    }
  }, [id, wanted, currentOwner])

  useEffect(
    () => () => {
      if (owner === id) {
        setOwner(undefined)
      }
    },
    [id],
  )

  return wanted && currentOwner === id
}
