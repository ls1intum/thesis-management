import { useCallback, useEffect, useRef, useState } from 'react'

// Keys that scroll the page — a keydown from one of these counts as the user
// taking over navigation and releases a click lock (see below). Plain typing in
// an input must not release it, so unrelated keys are ignored.
const SCROLL_KEYS = new Set([
  'ArrowUp',
  'ArrowDown',
  'PageUp',
  'PageDown',
  'Home',
  'End',
  ' ',
  'Spacebar',
])

/**
 * Tracks which of the given section anchor ids is currently in view. Uses
 * IntersectionObserver with a top offset so a section counts as "active" once
 * it scrolls above the sticky navbar rather than only when centered.
 *
 * Returns the active id plus a `setActiveSection` setter. Call the setter when
 * the user clicks a nav item: it highlights that section immediately and locks
 * the scroll spy so it can't snap the highlight back. Without the lock, clicking
 * a nav item on a short/collapsed page (where the target can't scroll all the
 * way to the top) would leave whatever sits at the top — usually "Overview" —
 * highlighted, which is misleading. The lock is released as soon as the user
 * scrolls the page themselves, after which the scroll spy resumes tracking.
 */
export function useActiveSection(
  sectionIds: string[],
  topOffset = 100,
): [string | null, (id: string) => void] {
  const [activeId, setActiveId] = useState<string | null>(sectionIds[0] ?? null)
  const key = sectionIds.join('|')
  const lockedRef = useRef(false)

  const setActiveSection = useCallback((id: string) => {
    lockedRef.current = true
    setActiveId(id)
  }, [])

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

    const isScrolledToBottom = () => {
      // Handles both window scrolling and any scrollable ancestor with fixed height.
      const doc = document.documentElement
      const winBottom = Math.ceil(window.innerHeight + window.scrollY) >= doc.scrollHeight - 2
      if (winBottom) {
        return true
      }
      // Also check the closest scroll ancestor of the last section (AppShell.Main).
      let el: HTMLElement | null = elements[elements.length - 1].parentElement
      while (el) {
        const overflowY = getComputedStyle(el).overflowY
        if (overflowY === 'auto' || overflowY === 'scroll') {
          return Math.ceil(el.scrollTop + el.clientHeight) >= el.scrollHeight - 2
        }
        el = el.parentElement
      }
      return false
    }

    const pickActive = () => {
      // A click lock pins the highlight to the user's chosen section until they
      // scroll the page themselves, so don't override it here.
      if (lockedRef.current) {
        return
      }

      if (isScrolledToBottom()) {
        setActiveId(elements[elements.length - 1].id)
        return
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
    }

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          visibility.set(entry.target.id, entry.isIntersecting ? entry.intersectionRatio : 0)
        }
        pickActive()
      },
      {
        rootMargin: `-${topOffset}px 0px -40% 0px`,
        threshold: [0, 0.1, 0.25, 0.5, 0.75, 1],
      },
    )

    elements.forEach((el) => observer.observe(el))

    // Fallback scroll listener catches the "scrolled past the observer's active
    // band" case for short final sections that never trip the intersection.
    const onScroll = () => pickActive()
    window.addEventListener('scroll', onScroll, { passive: true, capture: true })

    // A genuine user-initiated scroll releases the click lock so the scroll spy
    // resumes tracking the real scroll position.
    const releaseLock = () => {
      if (lockedRef.current) {
        lockedRef.current = false
        pickActive()
      }
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (SCROLL_KEYS.has(event.key)) {
        releaseLock()
      }
    }
    window.addEventListener('wheel', releaseLock, { passive: true, capture: true })
    window.addEventListener('touchmove', releaseLock, { passive: true, capture: true })
    window.addEventListener('keydown', onKeyDown, { passive: true, capture: true })

    return () => {
      observer.disconnect()
      window.removeEventListener('scroll', onScroll, true)
      window.removeEventListener('wheel', releaseLock, true)
      window.removeEventListener('touchmove', releaseLock, true)
      window.removeEventListener('keydown', onKeyDown, true)
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- `key` (joined ids) captures the array identity
  }, [key, topOffset])

  return [activeId, setActiveSection]
}
