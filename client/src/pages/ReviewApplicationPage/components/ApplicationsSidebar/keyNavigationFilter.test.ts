import { describe, it, expect } from 'vitest'
import { shouldIgnoreArrowKey } from './keyNavigationFilter'

const makeEvent = (
  init: Partial<KeyboardEventInit> & { target?: EventTarget | null } = {},
): KeyboardEvent => {
  const event = new KeyboardEvent('keydown', { key: 'ArrowRight', ...init })
  if ('target' in init) {
    Object.defineProperty(event, 'target', {
      value: init.target,
      writable: false,
    })
  }
  return event
}

describe('shouldIgnoreArrowKey — issue #572', () => {
  it('does not ignore plain arrow keys with no modifiers and a non-editable target', () => {
    const target = document.createElement('div')
    expect(shouldIgnoreArrowKey(makeEvent({ target }))).toBe(false)
  })

  it.each(['metaKey', 'ctrlKey', 'altKey', 'shiftKey'] as const)(
    'ignores arrow keys held with %s',
    (mod) => {
      const target = document.createElement('div')
      expect(shouldIgnoreArrowKey(makeEvent({ [mod]: true, target }))).toBe(true)
    },
  )

  it('ignores arrow keys when target is an input', () => {
    const target = document.createElement('input')
    expect(shouldIgnoreArrowKey(makeEvent({ target }))).toBe(true)
  })

  it('ignores arrow keys when target is a textarea', () => {
    const target = document.createElement('textarea')
    expect(shouldIgnoreArrowKey(makeEvent({ target }))).toBe(true)
  })

  it('ignores arrow keys when target is a select', () => {
    const target = document.createElement('select')
    expect(shouldIgnoreArrowKey(makeEvent({ target }))).toBe(true)
  })

  it('ignores arrow keys when target reports isContentEditable', () => {
    // jsdom does not fully implement contentEditable, so spoof the
    // getter directly — production code only reads `isContentEditable`.
    const target = document.createElement('div')
    Object.defineProperty(target, 'isContentEditable', { value: true })
    expect(shouldIgnoreArrowKey(makeEvent({ target }))).toBe(true)
  })
})
