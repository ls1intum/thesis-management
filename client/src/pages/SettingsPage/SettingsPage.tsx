import React from 'react'
import { Divider, Space, Tabs } from '@mantine/core'
import { EnvelopeOpen, User, UserMinus } from '@phosphor-icons/react'
import MyInformation from './components/MyInformation/MyInformation'
import NotificationSettings from './components/NotificationSettings/NotificationSettings'
import AccountDeletion from './components/AccountDeletion/AccountDeletion'
import DataExport from './components/DataExport/DataExport'
import { useNavigate, useParams } from 'react-router'

const SettingsPage = () => {
  const { tab } = useParams<{ tab: string }>()

  const navigate = useNavigate()

  const value = tab || 'my-information'

  return (
    <Tabs value={value} onChange={(newValue) => navigate(`/settings/${newValue}`)}>
      <Tabs.List>
        <Tabs.Tab value='my-information' leftSection={<User />}>
          My Information
        </Tabs.Tab>
        <Tabs.Tab value='notifications' leftSection={<EnvelopeOpen />}>
          Notification Settings
        </Tabs.Tab>
        <Tabs.Tab value='account' leftSection={<UserMinus />}>
          Account
        </Tabs.Tab>
      </Tabs.List>
      <Space my='md' />
      <Tabs.Panel value='my-information'>
        {value === 'my-information' && <MyInformation />}
      </Tabs.Panel>
      <Tabs.Panel value='notifications'>
        {value === 'notifications' && <NotificationSettings />}
      </Tabs.Panel>
      <Tabs.Panel value='account'>
        {value === 'account' && (
          <>
            <DataExport />
            <Divider my='xl' />
            <AccountDeletion />
          </>
        )}
      </Tabs.Panel>
    </Tabs>
  )
}

export default SettingsPage
