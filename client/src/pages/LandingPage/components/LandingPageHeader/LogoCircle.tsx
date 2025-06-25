import { Center, useComputedColorScheme, useMantineTheme } from '@mantine/core'
import Logo from '../../../../components/Logo/Logo'

const LogoCircle = ({
  size,
  logoSize,
  visibleFrom,
  hiddenFrom,
}: {
  size: number
  logoSize: number
  visibleFrom?: string
  hiddenFrom?: string
}) => {
  const computedColorScheme = useComputedColorScheme()
  const theme = useMantineTheme()

  return (
    <Center
      w={size}
      h={size}
      bg='var(--mantine-color-primary-3)'
      visibleFrom={visibleFrom}
      hiddenFrom={hiddenFrom}
      style={{
        borderRadius: '50%',
        flexShrink: 0,
      }}
    >
      <Logo
        size={logoSize}
        color={computedColorScheme === 'dark' ? theme.colors.dark[7] : theme.white}
      />
    </Center>
  )
}
export default LogoCircle
