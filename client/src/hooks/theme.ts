import { useMediaQuery } from '@mantine/hooks'
import { useMantineColorScheme, useMantineTheme } from '@mantine/core'
import { GLOBAL_CONFIG } from '../config/global'
import { useEffect } from 'react'

export function useIsSmallerBreakpoint(breakpoint: string) {
  const theme = useMantineTheme()

  return useMediaQuery(`(max-width: ${theme.breakpoints[breakpoint]})`) || false
}

export function useIsBiggerThanBreakpoint(breakpoint: string) {
  const theme = useMantineTheme()

  return useMediaQuery(`(min-width: ${theme.breakpoints[breakpoint]})`)
}

export function usePageTitle(title: string) {
  useEffect(() => {
    const previousTitle = document.title

    document.title = `${title} - ${GLOBAL_CONFIG.title}`

    return () => {
      document.title = previousTitle
    }
  }, [title])
}

export function useHighlightedBackgroundColor(selected: boolean) {
  const theme = useMantineTheme()
  const { colorScheme } = useMantineColorScheme()

  return selected
    ? colorScheme === 'dark'
      ? theme.colors[theme.primaryColor][6]
      : theme.colors[theme.primaryColor][4]
    : colorScheme === 'dark'
      ? theme.colors.dark[6]
      : theme.colors.gray[1]
}
