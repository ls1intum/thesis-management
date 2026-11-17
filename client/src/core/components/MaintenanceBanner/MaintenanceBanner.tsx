import { Group, Text } from '@mantine/core'
import { Wrench } from '@phosphor-icons/react'
import { GLOBAL_CONFIG } from '@/core/config/global'

export const MAINTENANCE_BANNER_HEIGHT = 28

// Shown on every page when the MAINTENANCE_MESSAGE runtime environment
// variable is set, e.g. while a security update is being rolled out.
const getMaintenanceMessage = (): string => GLOBAL_CONFIG.maintenance_message?.trim() ?? ''

export const isMaintenanceBannerVisible = (): boolean => getMaintenanceMessage().length > 0

const MaintenanceBanner = () => {
  const message = getMaintenanceMessage()

  if (!message) {
    return null
  }

  return (
    <Group
      role='alert'
      gap='xs'
      justify='center'
      align='center'
      wrap='nowrap'
      bg='yellow.1'
      c='yellow.9'
      h={MAINTENANCE_BANNER_HEIGHT}
      py={4}
      px='md'
      style={{
        borderBottom: '2px solid var(--mantine-color-yellow-5)',
        overflow: 'hidden',
      }}
    >
      <Wrench size={16} weight='bold' style={{ flexShrink: 0 }} />
      <Text
        size='sm'
        fw={600}
        ta='center'
        title={message}
        style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
      >
        {message}
      </Text>
    </Group>
  )
}

export default MaintenanceBanner
