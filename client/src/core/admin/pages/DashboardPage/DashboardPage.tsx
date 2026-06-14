import React, { useState } from 'react'
import { usePageTitle } from '@/core/hooks/theme'
import ApplicationsTable from '@/core/application/components/ApplicationsTable/ApplicationsTable'
import ThesesTable from '@/thesis/components/ThesesTable/ThesesTable'
import ApplicationsProvider from '@/core/application/providers/ApplicationsProvider/ApplicationsProvider'
import ThesesProvider from '@/thesis/providers/ThesesProvider/ThesesProvider'
import { Button, Center, Group, Stack, Title } from '@mantine/core'
import type { IApplication } from '@/core/application/requests/responses/application'
import { ApplicationState } from '@/core/application/requests/responses/application'
import ThesesGanttChart from '@/thesis/components/ThesesGanttChart/ThesesGanttChart'
import { useManagementAccess } from '@/core/hooks/authentication'
import { Link } from 'react-router'
import ApplicationModal from '@/core/application/components/ApplicationModal/ApplicationModal'
import MyTasksSection from '@/core/admin/pages/DashboardPage/components/MyTasksSection/MyTasksSection'
import { Pencil } from '@phosphor-icons/react'
import { ThesisState } from '@/thesis/requests/responses/thesis'

const DashboardPage = () => {
  usePageTitle('Dashboard')

  const [application, setApplication] = useState<IApplication>()

  const managementAccess = useManagementAccess()

  return (
    <Stack gap='md'>
      <Title order={1}>Dashboard</Title>
      <MyTasksSection />
      <ThesesProvider
        hideIfEmpty
        defaultStates={
          managementAccess
            ? [
                ThesisState.PROPOSAL,
                ThesisState.WRITING,
                ThesisState.SUBMITTED,
                ThesisState.ASSESSED,
                ThesisState.GRADED,
              ]
            : undefined
        }
        limit={200}
      >
        <Stack gap='xs'>
          <Title order={2}>My Theses</Title>
          {managementAccess ? <ThesesGanttChart /> : <ThesesTable />}
        </Stack>
      </ThesesProvider>
      <ApplicationsProvider
        hideIfEmpty={true}
        limit={10}
        defaultTopics={[]}
        defaultStates={[]}
        emptyComponent={
          !managementAccess ? (
            <Stack>
              <Title order={2} mb='sm'>
                My Applications
              </Title>
              <Center>
                <Button mb='md' component={Link} to='/applications/thesis'>
                  New Application
                </Button>
              </Center>
            </Stack>
          ) : undefined
        }
      >
        <Stack gap='xs'>
          <Title order={2}>My Applications</Title>
          <ApplicationsTable
            onApplicationClick={setApplication}
            columns={[
              'state',
              'thesis_title',
              'research_group',
              'thesis_type',
              'user',
              'reviewed_at',
              'created_at',
              'actions',
            ]}
            extraColumns={{
              actions: {
                accessor: 'actions',
                title: 'Actions',
                textAlign: 'center',
                noWrap: true,
                width: 80,
                render: (row) => (
                  <Group
                    preventGrowOverflow={false}
                    justify='center'
                    onClick={(e) => e.stopPropagation()}
                  >
                    {row.state === ApplicationState.NOT_ASSESSED && (
                      <Button
                        size='xs'
                        component={Link}
                        to={`/edit-application/${row.applicationId}`}
                      >
                        <Pencil />
                      </Button>
                    )}
                  </Group>
                ),
              },
            }}
          />
          <ApplicationModal
            application={application}
            onClose={() => setApplication(undefined)}
            allowReviews={false}
            allowEdit={true}
          />
        </Stack>
      </ApplicationsProvider>
    </Stack>
  )
}

export default DashboardPage
