import { Button, Group, SegmentedControl, Stack, TextInput, Title } from '@mantine/core'
import { MagnifyingGlassIcon, PaperPlaneTiltIcon, PlusIcon } from '@phosphor-icons/react'
import { useState } from 'react'
import { InterviewState } from '../../../requests/responses/interview'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'

const IntervieweesList = () => {
  const [searchIntervieweeKey, setSearchIntervieweeKey] = useState('')

  const [state, setState] = useState<string>('ALL')

  const isSmaller = useIsSmallerBreakpoint('md')

  return (
    <Stack gap={'1.5rem'}>
      <Group justify='space-between' align='center' gap={'0.5rem'}>
        <Title order={2}>Interviewees</Title>
        <Group gap={'0.5rem'}>
          <Button variant='outline' size='xs' leftSection={<PaperPlaneTiltIcon size={16} />}>
            {isSmaller ? 'Invites' : 'Send Invites'}
          </Button>
          <Button size='xs' leftSection={<PlusIcon size={16} />}>
            {isSmaller ? 'Add' : 'Add Interviewee'}
          </Button>
        </Group>
      </Group>

      <Group justify='space-between' align='center'>
        <SegmentedControl
          value={state}
          onChange={(value) => setState(value)}
          data={[
            { value: 'ALL', label: 'All' },
            ...Object.entries(InterviewState).map(([value, label]) => ({ value, label })),
          ]}
          radius={'md'}
        />
        <Group>
          <TextInput
            placeholder='Search name...'
            leftSection={<MagnifyingGlassIcon size={16} />}
            value={searchIntervieweeKey}
            onChange={(x) => setSearchIntervieweeKey(x.target.value || '')}
            w={300}
          />
        </Group>
      </Group>
      <Stack></Stack>
    </Stack>
  )
}

export default IntervieweesList
