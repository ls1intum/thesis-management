import { useCallback, useEffect, useRef, useState } from 'react'

// Keys that scroll the page — a keydown from one of these arms a click-lock
// release (see below). Unrelated keys (plain typing) are ignored.
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

// Space/arrow keys also edit text and move the caret, so a keydown inside an
// editable control must never arm a release.
const isEditableTarget = (target: EventTarget | null): boolean =>
  target instanceof HTMLInputElement ||
  target instanceof HTMLTextAreaElement ||
  target instanceof HTMLSelectElement ||
  (target instanceof HTMLElement && target.isContentEditable)

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
  // Whether a user scroll gesture has "armed" a lock release (see the effect).
  const armedRef = useRef(false)

  const setActiveSection = useCallback((id: string) => {
    lockedRef.current = true
    // Discard any gesture armed before this click so the programmatic scroll
    // that follows can't immediately release the fresh lock.
    armedRef.current = false
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

    // The page may scroll on the window or on a scrollable ancestor (AppShell.Main).
    const scrollAncestor = (() => {
      let el: HTMLElement | null = elements[elements.length - 1].parentElement
      while (el) {
        const overflowY = getComputedStyle(el).overflowY
        if (overflowY === 'auto' || overflowY === 'scroll') {
          return el
        }
        el = el.parentElement
      }
      return null
    })()

    // Combined vertical scroll offset. Horizontal gestures (e.g. swiping the
    // nav's own ScrollArea) don't change this, so they won't release the lock.
    const getVerticalScroll = () => window.scrollY + (scrollAncestor?.scrollTop ?? 0)

    const isScrolledToBottom = () => {
      // When an ancestor is the scroll container, the window itself usually
      // doesn't overflow, so its bottom check reads as permanently true — check
      // the ancestor exclusively and only fall back to the window when there's
      // no scroll ancestor.
      if (scrollAncestor) {
        return (
          Math.ceil(scrollAncestor.scrollTop + scrollAncestor.clientHeight) >=
          scrollAncestor.scrollHeight - 2
        )
      }
      const doc = document.documentElement
      return Math.ceil(window.innerHeight + window.scrollY) >= doc.scrollHeight - 2
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

    // Releasing the click lock takes two signals: a scroll gesture that "arms"
    // the release, and an actual change in the vertical scroll offset. Requiring
    // both means the programmatic smooth scroll (no gesture) and horizontal nav
    // swipes (no vertical change) leave the lock intact, while a real vertical
    // scroll by the user hands control back to the scroll spy. Arming is safe to
    // do generously (release still needs the offset to move), so `pointerdown`
    // covers dragging the window's or the ancestor's vertical scrollbar — which
    // fires none of the other gestures.
    armedRef.current = false
    let lastScroll = getVerticalScroll()

    const onScroll = () => {
      const current = getVerticalScroll()
      if (lockedRef.current && armedRef.current && current !== lastScroll) {
        lockedRef.current = false
        armedRef.current = false
      }
      lastScroll = current
      // Fallback for the "scrolled past the observer's active band" case for
      // short final sections that never trip the intersection.
      pickActive()
    }
    window.addEventListener('scroll', onScroll, { passive: true, capture: true })

    const arm = () => {
      armedRef.current = true
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (!isEditableTarget(event.target) && SCROLL_KEYS.has(event.key)) {
        armedRef.current = true
      }
    }
    window.addEventListener('wheel', arm, { passive: true, capture: true })
    window.addEventListener('touchmove', arm, { passive: true, capture: true })
    window.addEventListener('pointerdown', arm, { passive: true, capture: true })
    window.addEventListener('keydown', onKeyDown, { passive: true, capture: true })

    return () => {
      observer.disconnect()
      window.removeEventListener('scroll', onScroll, true)
      window.removeEventListener('wheel', arm, true)
      window.removeEventListener('touchmove', arm, true)
      window.removeEventListener('pointerdown', arm, true)
      window.removeEventListener('keydown', onKeyDown, true)
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- `key` (joined ids) captures the array identity
  }, [key, topOffset])

  return [activeId, setActiveSection]
}
