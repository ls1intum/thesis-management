import React, { Dispatch, SetStateAction, useState } from 'react'
import { Switch } from '@mantine/core'
import { doRequest } from '../../../../../../requests/request'
import { showSimpleError } from '../../../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../../../requests/handler'
import { BoxProps } from '@mantine/core/lib/core'
import { useNotificationSetting } from '../../../../../../hooks/notification'

interface INotificationToggleSwitchProps extends BoxProps {
  name: string
  settings: Array<{ name: string; email: string }>
  setSettings: Dispatch<SetStateAction<Array<{ name: string; email: string }> | undefined>>
}

const NotificationToggleSwitch = (props: INotificationToggleSwitchProps) => {
  const { name, settings, setSettings, ...other } = props

  const { loading, currentEmail, updateSetting } = useNotificationSetting(
    name,
    settings,
    setSettings,
  )

  const isChecked = currentEmail !== 'none'

  const toggleSetting = async () => {
    updateSetting(isChecked ? 'none' : 'all')
  }

  return <Switch checked={isChecked} onChange={toggleSetting} disabled={loading} {...other} />
}

export default NotificationToggleSwitch
