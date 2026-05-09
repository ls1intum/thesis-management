import dayjs from 'dayjs'

/**
 * Picks the date heading the presentation overview should auto-scroll to.
 *
 * Priority (matches the auto-scroll behaviour from issue #759):
 *   1. today (when there are presentations on today's date)
 *   2. the next upcoming date with presentations
 *   3. the most recent past date as a fallback
 */
export const pickTargetDate = (today: string, dates: readonly string[]): string | undefined => {
  const sorted = [...dates].sort((a, b) => (dayjs(a).isAfter(dayjs(b)) ? 1 : -1))
  return (
    sorted.find((date) => date === today) ??
    sorted.find((date) => !dayjs(date).isBefore(dayjs(today))) ??
    sorted[sorted.length - 1]
  )
}
