import { Group, Stack, Text } from '@mantine/core'
import NotificationToggleSwitch from '../NotificationToggleSwitch/NotificationToggleSwitch'
import { NotificationSelect } from '../NotificationSelect/NotificationSelect'

interface INotificationOptionProps {
  name: string
  settings: Array<{ name: string; email: string }>
  setSettings: React.Dispatch<
    React.SetStateAction<Array<{ name: string; email: string }> | undefined>
  >
  heading: string
  description: string
  options?: Array<{ value: string; label: string }>
  defaultValue?: string
  flex?: number | string
}

export function NotificationOption({
  name,
  settings,
  setSettings,
  heading,
  description,
  options,
  defaultValue,
  flex,
  ...other
}: INotificationOptionProps) {
  return (
    <Group justify='space-between' flex={flex}>
      <Stack gap={2}>
        <Text size='sm'>{heading}</Text>
        <Text size='xs' c='dimmed'>
          {description}
        </Text>
      </Stack>
      {options ? (
        <NotificationSelect
          name={name}
          settings={settings}
          setSettings={setSettings}
          options={options}
          defaultValue={defaultValue}
          {...other}
        />
      ) : (
        <NotificationToggleSwitch name={name} settings={settings} setSettings={setSettings} />
      )}
    </Group>
  )
}
