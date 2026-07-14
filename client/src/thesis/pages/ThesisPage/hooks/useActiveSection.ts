import { useEffect, useState } from 'react'

/**
 * Tracks which of the given section anchor ids is currently in view. Uses
 * IntersectionObserver with a top offset so a section counts as "active" once
 * it scrolls above the sticky navbar rather than only when centered.
 */
export function useActiveSection(sectionIds: string[], topOffset = 100): string | null {
  const [activeId, setActiveId] = useState<string | null>(sectionIds[0] ?? null)
  const key = sectionIds.join('|')

  useEffect(() => {
    if (sectionIds.length === 0) {
      return
    }

    const elements = sectionIds
      .map((id) => document.getElementById(id))
      .filter((el): el is HTMLElement => el !== null)

    if (elements.length === 0) {
      return
    }

    const visibility = new Map<string, number>()

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          visibility.set(entry.target.id, entry.isIntersecting ? entry.intersectionRatio : 0)
        }

        let best: { id: string; top: number } | null = null
        for (const el of elements) {
          if ((visibility.get(el.id) ?? 0) > 0) {
            const top = el.getBoundingClientRect().top
            if (!best || top < best.top) {
              best = { id: el.id, top }
            }
          }
        }

        if (best) {
          setActiveId(best.id)
        }
      },
      {
        rootMargin: `-${topOffset}px 0px -40% 0px`,
        threshold: [0, 0.1, 0.25, 0.5, 0.75, 1],
      },
    )

    elements.forEach((el) => observer.observe(el))
    return () => observer.disconnect()
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- `key` (joined ids) captures the array identity
  }, [key, topOffset])

  return activeId
}
