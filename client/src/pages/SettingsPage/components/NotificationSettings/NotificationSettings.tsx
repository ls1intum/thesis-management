import { Divider, Group, Stack, Switch, Text } from '@mantine/core'
import { usePageTitle } from '../../../../hooks/theme'
import { useLoggedInUser, useManagementAccess } from '../../../../hooks/authentication'
import ThesesTable from '../../../../components/ThesesTable/ThesesTable'
import ThesesProvider from '../../../../providers/ThesesProvider/ThesesProvider'
import React, { useEffect, useState } from 'react'
import { doRequest } from '../../../../requests/request'
import PageLoader from '../../../../components/PageLoader/PageLoader'
import NotificationToggleSwitch from './components/NotificationToggleSwitch/NotificationToggleSwitch'
import { NotificationSelect } from './components/NotificationSelect/NotificationSelect'
import { NotificationOption } from './components/NotificationOption/NotificationOption'

const NotificationSettings = () => {
  usePageTitle('Notification Settings')

  const user = useLoggedInUser()
  const managementAccess = useManagementAccess()

  const [settings, setSettings] = useState<Array<{ name: string; email: string }>>()

  useEffect(() => {
    setSettings(undefined)

    return doRequest<Array<{ name: string; email: string }>>(
      '/v2/user-info/notifications',
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setSettings(res.data)
        } else {
          setSettings([])
        }
      },
    )
  }, [user.userId])

  if (!settings) {
    return <PageLoader />
  }

  return (
    <Stack>
      {managementAccess && (
        <Stack>
          <NotificationOption
            name='new-applications'
            settings={settings}
            setSettings={setSettings}
            heading='New Applications'
            description='Receive a summary email on every new application'
            options={[
              { value: 'none', label: 'None' },
              { value: 'own', label: 'Own' },
              { value: 'all', label: 'All' },
            ]}
            defaultValue='own'
          />
          {settings.find((s) => s.name === 'new-applications')?.email !== 'none' && (
            <Group ml={'1rem'}>
              <Divider orientation='vertical' />
              <NotificationOption
                name='include-suggested-topics'
                settings={settings}
                setSettings={setSettings}
                heading='Include Suggested Topics'
                description='Receive notifications for student-suggested topics in your group'
                flex={1}
              />
            </Group>
          )}
          <NotificationOption
            name='application-review-reminder'
            settings={settings}
            setSettings={setSettings}
            heading='Application Review Reminder'
            description='Receive a weekly reminder email if you have unreviewed applications'
          />
        </Stack>
      )}
      <NotificationOption
        name='presentation-invitations'
        settings={settings}
        setSettings={setSettings}
        heading='Presentation Invitations'
        description='Get invitations to public thesis presentations when scheduled'
      />

      <NotificationOption
        name='thesis-comments'
        settings={settings}
        setSettings={setSettings}
        heading='Thesis Comments'
        description='Receive an email for every comment that is added to a thesis assigned to you'
      />

      <ThesesProvider limit={10}>
        <ThesesTable
          columns={[
            'title',
            'type',
            'students',
            'advisors',
            'supervisors',
            'researchGroup',
            'actions',
          ]}
          extraColumns={{
            actions: {
              accessor: 'actions',
              title: 'Notifications',
              textAlign: 'center',
              noWrap: true,
              width: 120,
              render: (thesis) => (
                <Group
                  preventGrowOverflow={false}
                  justify='center'
                  onClick={(e) => e.stopPropagation()}
                >
                  <NotificationToggleSwitch
                    name={`thesis-${thesis.thesisId}`}
                    settings={settings}
                    setSettings={setSettings}
                  />
                </Group>
              ),
            },
          }}
        />
      </ThesesProvider>
    </Stack>
  )
}

export default NotificationSettings
