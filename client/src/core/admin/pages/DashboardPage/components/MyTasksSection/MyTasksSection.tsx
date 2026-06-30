import { ActionIcon, Center, Group, Skeleton, Stack, Title } from '@mantine/core'
import React, { useEffect, useState } from 'react'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { ITask } from '@/core/admin/requests/responses/dashboard'
import { DataTable } from 'mantine-datatable'
import { useNavigate } from 'react-router'
import { Link as LinkIcon } from '@phosphor-icons/react'

const MyTasksSection = () => {
  const navigate = useNavigate()

  const [tasks, setTasks] = useState<ITask[]>()

  useEffect(() => {
    setTasks(undefined)

    return doRequest<ITask[]>(
      `/v2/dashboard/tasks`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setTasks(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [])

  if (!tasks) {
    return <Skeleton height={200} />
  }

  if (!tasks.length) {
    return null
  }

  const redirectTask = (task: ITask, openInNewTab = false) => {
    if (openInNewTab) {
      window.open(task.link, '_blank', 'noopener,noreferrer')
    } else if (task.link.startsWith('http')) {
      window.location.replace(task.link)
    } else {
      void navigate(task.link)
    }
  }

  return (
    <Stack gap='xs'>
      <Title order={2}>My Tasks</Title>
      <DataTable
        withTableBorder
        striped
        noHeader
        borderRadius='sm'
        verticalSpacing='md'
        highlightOnHover
        records={tasks}
        idAccessor='message'
        columns={[
          {
            accessor: 'message',
          },
          {
            accessor: 'actions',
            textAlign: 'center',
            width: 80,
            render: (record) => (
              <Center>
                <Group gap='xs' onClick={(e) => e.stopPropagation()} wrap='nowrap'>
                  <ActionIcon onClick={() => redirectTask(record)}>
                    <LinkIcon />
                  </ActionIcon>
                </Group>
              </Center>
            ),
          },
        ]}
        onRowClick={({ record, event }) =>
          redirectTask(record, event.metaKey || event.ctrlKey || event.shiftKey)
        }
        customRowAttributes={(record) => ({
          onAuxClick: (event: React.MouseEvent) => {
            if (event.button === 1) {
              event.preventDefault()
              redirectTask(record, true)
            }
          },
        })}
      />
    </Stack>
  )
}

export default MyTasksSection
