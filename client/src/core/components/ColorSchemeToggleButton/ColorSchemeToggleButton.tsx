import { Moon, Sun } from '@phosphor-icons/react'
import { ActionIcon, useMantineColorScheme, type BoxProps } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'

interface ColorSchemeToggleButtonProps extends BoxProps {
  iconSize?: number | string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | number
}

export const ColorSchemeToggleButton = ({
  iconSize = '1.1rem',
  size = 'md',
  ...props
}: ColorSchemeToggleButtonProps) => {
  const { colorScheme, toggleColorScheme, clearColorScheme } = useMantineColorScheme()
  const prefersDarkColorScheme = useMediaQuery('(prefers-color-scheme: dark)', undefined, {
    getInitialValueInEffect: false,
  })

  const showSunIcon = (colorScheme === 'auto' && prefersDarkColorScheme) || colorScheme === 'dark'

  return (
    <ActionIcon
      variant='outline'
      color={showSunIcon ? 'gray.4' : 'dark.2'}
      onClick={() => {
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
      size={size}
      {...props}
    >
      {showSunIcon ? <Sun size={iconSize} /> : <Moon size={iconSize} />}
    </ActionIcon>
  )
}
