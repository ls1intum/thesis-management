/**
 * Calculate a weighted grade from grade components.
 * Regular components contribute via weighted average (weights sum to 100),
 * bonus components are added directly to the result.
 * The result is clamped to [1.0, 5.0] and rounded to 1 decimal place.
 */
export function calculateGradeFromComponents(
  components: Array<{ weight: number; isBonus: boolean; grade: number }>,
): number {
  let weightedSum = 0
  let bonusSum = 0

  for (const c of components) {
    if (c.isBonus) {
      bonusSum += c.grade
    } else {
      weightedSum += c.weight * c.grade
    }
  }

  let calculated = weightedSum / 100 + bonusSum
  calculated = Math.max(1.0, Math.min(5.0, calculated))
  return Math.round(calculated * 10) / 10
}
