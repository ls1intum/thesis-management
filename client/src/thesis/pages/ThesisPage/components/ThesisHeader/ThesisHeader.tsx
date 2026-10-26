import { Alert, Button, Group, Stack, Title } from '@mantine/core'
import { ThesisState } from '@/thesis/requests/responses/thesis'
import { GearSix, Info } from '@phosphor-icons/react'
import React from 'react'
import { Link } from 'react-router'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import { usePageTitle } from '@/core/hooks/theme'

const ThesisHeader = () => {
  const { thesis, access } = useLoadedThesisContext()

  usePageTitle(thesis.title)

  return (
    <Stack>
      <Group justify='space-between' align='flex-start' wrap='nowrap'>
        <Title order={2} style={{ flex: 1 }}>
          {thesis.title}
        </Title>
        {access.supervisor && (
          <Button
            component={Link}
            to={`/theses/${thesis.thesisId}/configuration`}
            variant='light'
            leftSection={<GearSix />}
          >
            Configuration
          </Button>
        )}
      </Group>
      {thesis.state === ThesisState.DROPPED_OUT && (
        <Alert variant='light' color='red' title='This thesis is closed' icon={<Info />} mb='md' />
      )}
    </Stack>
  )
}

export default ThesisHeader
