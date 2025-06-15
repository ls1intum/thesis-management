import { ActionIcon, useMantineColorScheme } from '@mantine/core'
import { BoxProps } from '@mantine/core/lib/core'
import { useMediaQuery } from '@mantine/hooks'
import { Moon, Sun } from 'phosphor-react'

const ColorSchemeToggleButton = (props: BoxProps) => {
  const { colorScheme, toggleColorScheme, clearColorScheme } = useMantineColorScheme()
  const prefersDarkColorScheme = useMediaQuery('(prefers-color-scheme: dark)')

  const showSunIcon = (colorScheme === 'auto' && prefersDarkColorScheme) || colorScheme === 'dark'

  return (
    <ActionIcon
      variant='outline'
      color={colorScheme === 'dark' ? 'yellow' : 'pale-purple'}
      onClick={() => {
        // Reset to "auto" if the preferred value is reached again
        if (
          (colorScheme === 'dark' && !prefersDarkColorScheme) ||
          (colorScheme === 'light' && prefersDarkColorScheme)
        ) {
          clearColorScheme()
        } else {
          toggleColorScheme()
        }
      }}
      title='Toggle color scheme'
      {...props}
    >
      {showSunIcon ? <Sun size='1.1rem' /> : <Moon size='1.1rem' />}
    </ActionIcon>
  )
}

export default ColorSchemeToggleButton
