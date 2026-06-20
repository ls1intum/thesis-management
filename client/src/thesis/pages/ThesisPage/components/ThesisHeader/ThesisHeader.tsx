import { Alert, Stack, Title } from '@mantine/core'
import { ThesisState } from '@/thesis/requests/responses/thesis'
import { Info } from '@phosphor-icons/react'
import React from 'react'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import { usePageTitle } from '@/core/hooks/theme'

const ThesisHeader = () => {
  const { thesis } = useLoadedThesisContext()

  usePageTitle(thesis.title)

  return (
    <Stack>
      <Title>{thesis.title}</Title>
      {thesis.state === ThesisState.DROPPED_OUT && (
        <Alert variant='light' color='red' title='This thesis is closed' icon={<Info />} mb='md' />
      )}
    </Stack>
  )
}

export default ThesisHeader
