/**
 * Picks the date heading the presentation overview should auto-scroll to.
 *
 * Priority (matches the auto-scroll behaviour from issue #759):
 *   1. today (when there are presentations on today's date)
 *   2. the next upcoming date with presentations
 *   3. the most recent past date as a fallback
 *
 * Inputs are guaranteed to be `YYYY-MM-DD` strings (the caller derives them
 * via `dayjs(...).format('YYYY-MM-DD')` over Map keys), so a plain string
 * compare is enough — no need to re-parse via dayjs.
 */
export const pickTargetDate = (today: string, dates: readonly string[]): string | undefined => {
  const sorted = [...dates].sort((a, b) => a.localeCompare(b))
  return (
    sorted.find((date) => date === today) ??
    sorted.find((date) => date >= today) ??
    sorted[sorted.length - 1]
  )
}
