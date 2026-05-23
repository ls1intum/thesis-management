/**
 * Returns true when an arrow-key keydown event should be ignored by the
 * sidebar's "navigate between applications" handler.
 *
 * We skip:
 *   - any modifier-key combo (Cmd/Ctrl/Alt + arrow, Shift + arrow for text
 *     selection) so OS / browser shortcuts keep working
 *   - events whose target is a text input, textarea, select, or any
 *     contentEditable element — otherwise typing in a comment field would
 *     also page through applications
 */
export const shouldIgnoreArrowKey = (event: KeyboardEvent): boolean => {
  if (event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) {
    return true
  }
  const target = event.target as HTMLElement | null
  if (
    target instanceof HTMLInputElement ||
    target instanceof HTMLTextAreaElement ||
    target instanceof HTMLSelectElement ||
    target?.isContentEditable
  ) {
    return true
  }
  return false
}
