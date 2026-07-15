import type { CSSProperties } from 'react'
import { Box, Group, ScrollArea, UnstyledButton, useMantineTheme } from '@mantine/core'
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
}

interface IThesisProcessNavProps {
  steps: IThesisProcessNavStep[]
}

const scrollToSection = (id: string) => {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const ThesisProcessNav = ({ steps }: IThesisProcessNavProps) => {
  const theme = useMantineTheme()
  const headerOffset = getAppShellHeaderOffset()
  const activeId = useActiveSection(
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

  return (
    <Box style={containerStyle}>
      <ScrollArea scrollbarSize={4} type='auto' offsetScrollbars={false}>
        <Group gap='xs' wrap='nowrap'>
          {steps.map((step, idx) => {
            const active = activeId === step.id
            const done = step.isCompleted
            const current = step.isCurrent

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
                ? theme.colors[theme.primaryColor][6]
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
              backgroundColor: active
                ? 'rgba(255,255,255,0.25)'
                : done
                  ? theme.colors[theme.primaryColor][6]
                  : 'var(--mantine-color-default-border)',
              color: active ? theme.white : done ? theme.white : 'var(--mantine-color-dimmed)',
            }

            return (
              <UnstyledButton
                key={step.id}
                onClick={() => scrollToSection(step.id)}
                style={pillStyle}
                aria-current={active ? 'step' : undefined}
              >
                <span style={indicatorStyle}>{idx + 1}</span>
                <span>{step.label}</span>
              </UnstyledButton>
            )
          })}
        </Group>
      </ScrollArea>
    </Box>
  )
}

export default ThesisProcessNav
