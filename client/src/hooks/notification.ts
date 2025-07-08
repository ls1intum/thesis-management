import { Dispatch, SetStateAction, useState } from 'react'
import { doRequest } from '../requests/request'
import { showSimpleError } from '../utils/notification'
import { getApiResponseErrorMessage } from '../requests/handler'

export const useNotificationSetting = (
  name: string,
  settings: Array<{ name: string; email: string }>,
  setSettings: Dispatch<SetStateAction<Array<{ name: string; email: string }> | undefined>>,
  defaultValue?: string,
) => {
  const [loading, setLoading] = useState(false)

  const currentSetting = settings.find((setting) => setting.name === name)
  const currentEmail = currentSetting?.email ?? defaultValue ?? 'all'

  const updateSetting = async (newEmail: string) => {
    setLoading(true)
    try {
      const response = await doRequest<Array<{ name: string; email: string }>>(
        '/v2/user-info/notifications',
        {
          method: 'PUT',
          requiresAuth: true,
          data: { name, email: newEmail },
        },
      )
      if (response.ok) {
        setSettings(response.data)
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoading(false)
    }
  }

  return { loading, currentEmail, updateSetting }
}
