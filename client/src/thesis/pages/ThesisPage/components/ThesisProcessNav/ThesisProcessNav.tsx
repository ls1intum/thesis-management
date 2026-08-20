import { Fragment, type CSSProperties } from 'react'
import { Box, Group, ScrollArea, UnstyledButton, useMantineTheme } from '@mantine/core'
import { CaretRight } from '@phosphor-icons/react'
import { useActiveSection } from '@/thesis/pages/ThesisPage/hooks/useActiveSection'
import {
  ENVIRONMENT_BANNER_HEIGHT,
  isEnvironmentBannerVisible,
} from '@/core/components/EnvironmentBanner/EnvironmentBanner'

export const APP_SHELL_HEADER_HEIGHT = 50
export const getAppShellHeaderOffset = () =>
  APP_SHELL_HEADER_HEIGHT + (isEnvironmentBannerVisible() ? ENVIRONMENT_BANNER_HEIGHT : 0)

export interface IThesisProcessNavStep {
  id: string
  label: string
  isCurrent?: boolean
  isCompleted?: boolean
  isOverview?: boolean
}

interface IThesisProcessNavProps {
  steps: IThesisProcessNavStep[]
}

const scrollToSection = (id: string) => {
  const el = document.getElementById(id)
  if (!el) {
    return
  }
  // Reveal a collapsed accordion so the user doesn't land on a section whose
  // body is still hidden — but only when the accordion is *fully* collapsed.
  // In a single-select accordion one item is already open, so clicking a closed
  // control there would merely toggle the open sibling shut (e.g. flipping the
  // Files/Comments panels in the Writing section on every nav click).
  el.querySelectorAll<HTMLButtonElement>(
    'button.mantine-Accordion-control[aria-expanded="false"]',
  ).forEach((btn) => {
    const root = btn.closest('.mantine-Accordion-item')?.parentElement
    const siblingHasOpenPanel = root
      ? Array.from(root.children)
          .filter((child) => child.classList.contains('mantine-Accordion-item'))
          .map((item) => item.querySelector('.mantine-Accordion-control'))
          .some((control) => control?.getAttribute('aria-expanded') === 'true')
      : false
    if (!siblingHasOpenPanel) {
      btn.click()
    }
  })
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const ThesisProcessNav = ({ steps }: IThesisProcessNavProps) => {
  const theme = useMantineTheme()
  const headerOffset = getAppShellHeaderOffset()
  const [activeId, setActiveSection] = useActiveSection(
    steps.map((s) => s.id),
    headerOffset + 60,
  )

  if (steps.length === 0) {
    return null
  }

  const containerStyle: CSSProperties = {
    position: 'sticky',
    top: headerOffset,
    zIndex: 50,
    backgroundColor: 'var(--mantine-color-body)',
    borderBottom: `1px solid var(--mantine-color-default-border)`,
    marginLeft: 'calc(-1 * var(--mantine-spacing-md))',
    marginRight: 'calc(-1 * var(--mantine-spacing-md))',
    paddingLeft: 'var(--mantine-spacing-md)',
    paddingRight: 'var(--mantine-spacing-md)',
    paddingTop: 8,
    paddingBottom: 8,
  }

  const primaryColor = theme.colors[theme.primaryColor][6]
  let lifecycleIndex = 0

  return (
    <Box style={containerStyle}>
      <ScrollArea scrollbarSize={4} type='auto' offsetScrollbars={false}>
        <Group gap='xs' wrap='nowrap'>
          {steps.map((step, idx) => {
            const active = activeId === step.id
            const done = step.isCompleted
            const current = step.isCurrent

            const stepNumber = step.isOverview ? null : ++lifecycleIndex
            const prevStep = idx > 0 ? steps[idx - 1] : undefined
            const showArrow = idx > 0 && !prevStep?.isOverview && !step.isOverview

            const pillStyle: CSSProperties = {
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              padding: '6px 12px',
              borderRadius: 999,
              fontSize: 14,
              fontWeight: active || current ? 600 : 500,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              backgroundColor: active
                ? primaryColor
                : done
                  ? 'var(--mantine-color-default-hover)'
                  : 'transparent',
              color: active ? theme.white : done ? undefined : 'var(--mantine-color-dimmed)',
              border: active
                ? '1px solid transparent'
                : `1px solid var(--mantine-color-default-border)`,
              transition: 'background-color 120ms ease, color 120ms ease',
            }

            const indicatorStyle: CSSProperties = {
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 20,
              height: 20,
              borderRadius: 999,
              fontSize: 11,
              fontWeight: 700,
              backgroundColor: active ? 'rgba(255,255,255,0.25)' : primaryColor,
              color: theme.white,
            }

            return (
              <Fragment key={step.id}>
                {showArrow && (
                  <CaretRight
                    size={14}
                    weight='bold'
                    color='var(--mantine-color-dimmed)'
                    aria-hidden='true'
                    style={{ flexShrink: 0 }}
                  />
                )}
                <UnstyledButton
                  onClick={() => {
                    setActiveSection(step.id)
                    scrollToSection(step.id)
                  }}
                  style={pillStyle}
                  aria-current={active ? 'step' : undefined}
                >
                  {stepNumber !== null && <span style={indicatorStyle}>{stepNumber}</span>}
                  <span>{step.label}</span>
                </UnstyledButton>
              </Fragment>
            )
          })}
        </Group>
      </ScrollArea>
    </Box>
  )
}

export default ThesisProcessNav
