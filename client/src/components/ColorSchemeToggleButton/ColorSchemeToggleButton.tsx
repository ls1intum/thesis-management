import { Moon, Sun } from 'phosphor-react'
import { ActionIcon, useMantineColorScheme } from '@mantine/core'
import { BoxProps } from '@mantine/core/lib/core'

interface ColorSchemeToggleButtonProps extends BoxProps {
  iconSize?: number | string
}

const ColorSchemeToggleButton = ({
  iconSize = '1.1rem',
  ...props
}: ColorSchemeToggleButtonProps) => {
  const { colorScheme, toggleColorScheme } = useMantineColorScheme()

  return (
    <ActionIcon
      variant='outline'
      color={colorScheme === 'dark' ? 'yellow' : 'pale-purple'}
      onClick={() => toggleColorScheme()}
      title='Toggle color scheme'
      {...props}
    >
      {colorScheme === 'dark' ? <Sun size={iconSize} /> : <Moon size={iconSize} />}
    </ActionIcon>
  )
}

export default ColorSchemeToggleButton
