import { Moon, Sun } from '@phosphor-icons/react'
import { ActionIcon, useMantineColorScheme } from '@mantine/core'
import { BoxProps } from '@mantine/core/lib/core'

interface ColorSchemeToggleButtonProps extends BoxProps {
  iconSize?: number | string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | number
}

const ColorSchemeToggleButton = ({
  iconSize = '1.1rem',
  size = 'md',
  ...props
}: ColorSchemeToggleButtonProps) => {
  const { colorScheme, toggleColorScheme } = useMantineColorScheme()

  return (
    <ActionIcon
      variant='outline'
      color={colorScheme === 'dark' ? 'gray.4' : 'dark.2'}
      onClick={() => toggleColorScheme()}
      title='Toggle color scheme'
      size={size}
      {...props}
    >
      {colorScheme === 'dark' ? <Sun size={iconSize} /> : <Moon size={iconSize} />}
    </ActionIcon>
  )
}

export default ColorSchemeToggleButton
