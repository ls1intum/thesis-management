import { Select } from '@mantine/core'
import { useNotificationSetting } from '../../../../../../hooks/notification'

interface INotificationSelectProps {
  name: string
  settings: Array<{ name: string; email: string }>
  setSettings: React.Dispatch<
    React.SetStateAction<Array<{ name: string; email: string }> | undefined>
  >
  options: Array<{ value: string; label: string }>
  defaultValue?: string
}

export function NotificationSelect({
  name,
  settings,
  setSettings,
  options,
  defaultValue,
  ...other
}: INotificationSelectProps) {
  const { loading, currentEmail, updateSetting } = useNotificationSetting(
    name,
    settings,
    setSettings,
    defaultValue,
  )

  const handleChange = (value: string | null) => {
    if (value) {
      updateSetting(value)
    }
  }

  return (
    <Select
      data={options}
      value={currentEmail}
      onChange={handleChange}
      disabled={loading}
      allowDeselect={false}
      {...other}
    />
  )
}
