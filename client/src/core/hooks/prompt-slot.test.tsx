import { describe, expect, test } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { usePromptSlot } from '@/core/hooks/prompt-slot'

describe('usePromptSlot', () => {
  test('grants the slot to a prompt that wants to open', () => {
    const { result } = renderHook(() => usePromptSlot('a', true))

    expect(result.current).toBe(true)
  })

  test('does not grant the slot to a prompt that does not want to open', () => {
    const { result } = renderHook(() => usePromptSlot('a', false))

    expect(result.current).toBe(false)
  })

  test('lets only one prompt be open and hands the slot over when it closes', () => {
    const first = renderHook(({ wanted }) => usePromptSlot('first', wanted), {
      initialProps: { wanted: true },
    })
    const second = renderHook(() => usePromptSlot('second', true))

    expect(first.result.current).toBe(true)
    expect(second.result.current).toBe(false)

    act(() => first.rerender({ wanted: false }))

    expect(first.result.current).toBe(false)
    expect(second.result.current).toBe(true)
  })

  test('releases the slot when the owning prompt unmounts', () => {
    const first = renderHook(() => usePromptSlot('first', true))
    const second = renderHook(() => usePromptSlot('second', true))

    expect(second.result.current).toBe(false)

    act(() => first.unmount())

    expect(second.result.current).toBe(true)
  })
})
