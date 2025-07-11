import { useMantineTheme, useComputedColorScheme } from '@mantine/core'

interface ILogoProps {
  size?: number | string
  color?: string
}

const Logo = ({ size = 44, color }: ILogoProps) => {
  const theme = useMantineTheme()
  const computedColorScheme = useComputedColorScheme()

  const strokeColor =
    color ?? (computedColorScheme === 'dark' ? theme.colors.dark[2] : theme.colors.gray[8])

  return (
    <svg
      xmlns='http://www.w3.org/2000/svg'
      width={size}
      height={size}
      viewBox='0 0 24 24'
      strokeWidth='1.5'
      stroke={strokeColor}
      fill='none'
      strokeLinecap='round'
      strokeLinejoin='round'
    >
      <path stroke='none' d='M0 0h24v24H0z' fill='none' />
      <path d='M22 9l-10 -4l-10 4l10 4l10 -4v6' />
      <path d='M6 10.6v5.4a6 3 0 0 0 12 0v-5.4' />
    </svg>
  )
}

export default Logo
