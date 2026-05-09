import { Group, Text, MantineColor } from '@mantine/core'
import { Warning } from '@phosphor-icons/react'
import { Environment } from '../../config/types'
import { GLOBAL_CONFIG } from '../../config/global'

export const ENVIRONMENT_BANNER_HEIGHT = 28

type NonProdEnvironment = Exclude<Environment, 'production'>

const colorByEnvironment: Record<NonProdEnvironment, MantineColor> = {
  staging: 'blue',
  test: 'orange',
  dev: 'red',
}

export const labelByEnvironment: Record<NonProdEnvironment, string> = {
  staging: 'Staging',
  test: 'Test',
  dev: 'Development',
}

export const isEnvironmentBannerVisible = (): boolean => {
  const environment = GLOBAL_CONFIG.environment
  return !!environment && environment !== 'production'
}

const EnvironmentBanner = () => {
  if (!isEnvironmentBannerVisible()) {
    return null
  }

  const environment = GLOBAL_CONFIG.environment as NonProdEnvironment
  const color = colorByEnvironment[environment]
  const label = labelByEnvironment[environment]

  return (
    <Group
      role='status'
      aria-live='polite'
      gap='xs'
      justify='center'
      align='center'
      wrap='nowrap'
      bg={`${color}.1`}
      c={`${color}.9`}
      h={ENVIRONMENT_BANNER_HEIGHT}
      py={4}
      px='md'
      style={{
        borderBottom: `2px solid var(--mantine-color-${color}-5)`,
        overflow: 'hidden',
      }}
    >
      <Warning size={16} weight='bold' style={{ flexShrink: 0 }} />
      <Text
        size='sm'
        fw={600}
        ta='center'
        style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
      >
        {label} environment — not for production use
      </Text>
    </Group>
  )
}

export default EnvironmentBanner
