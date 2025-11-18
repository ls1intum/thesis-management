import {
  Button,
  Center,
  Group,
  Loader,
  SegmentedControl,
  Stack,
  TextInput,
  Title,
  Text,
} from '@mantine/core'
import {
  MagnifyingGlassIcon,
  PaperPlaneTiltIcon,
  PlusIcon,
  UsersFourIcon,
} from '@phosphor-icons/react'
import { useEffect, useState } from 'react'
import {
  IIntervieweeLightWithNextSlot,
  InterviewState,
} from '../../../requests/responses/interview'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { doRequest } from '../../../requests/request'
import { PaginationResponse } from '../../../requests/responses/pagination'
import { useNavigate, useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useDebouncedValue } from '@mantine/hooks'
import IntervieweeCard from './IntervieweeCard'

const IntervieweesList = () => {
  const { processId } = useParams<{ processId: string }>()
  const [searchIntervieweeKey, setSearchIntervieweeKey] = useState('')
  const [debouncedSearch] = useDebouncedValue(searchIntervieweeKey, 500)

  const [state, setState] = useState<string>('ALL')

  const isSmaller = useIsSmallerBreakpoint('md')

  const [interviewees, setInterviewees] = useState<IIntervieweeLightWithNextSlot[]>([])
  const [intervieweesLoading, setIntervieweesLoading] = useState(false)

  const navigate = useNavigate()

  const fetchMyInterviewProcesses = async () => {
    setIntervieweesLoading(true)
    doRequest<PaginationResponse<IIntervieweeLightWithNextSlot>>(
      `/v2/interview-process/${processId}/interviewees`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          searchQuery: searchIntervieweeKey,
          limit: 50,
          state: state !== 'ALL' ? state : '',
        },
      },
      (res) => {
        if (res.ok) {
          setInterviewees(res.data.content)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setIntervieweesLoading(false)
      },
    )

    {
      /*TODO: In UI show the pagination if it is more than one page*/
    }
  }

  useEffect(() => {
    fetchMyInterviewProcesses()
  }, [state, debouncedSearch])

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
        <TextInput
          placeholder='Search name...'
          leftSection={<MagnifyingGlassIcon size={16} />}
          value={searchIntervieweeKey}
          onChange={(x) => setSearchIntervieweeKey(x.target.value || '')}
          w={isSmaller ? '100%' : 300}
        />
      </Group>
      {intervieweesLoading ? (
        <Center h={'100%'} mih={'30vh'}>
          <Loader />
        </Center>
      ) : interviewees.length === 0 ? (
        <Center h={'100%'} mih={'30vh'}>
          <Stack justify='center' align='center' h={'100%'}>
            <UsersFourIcon size={60} />
            <Stack gap={'0.5rem'} justify='center' align='center'>
              <Title order={5}>No Interviewees Found</Title>
              <Text c='dimmed' ta={'center'}>
                Add a interviewee while reviewing application or just add them here
              </Text>
            </Stack>
          </Stack>
        </Center>
      ) : null}
      <Stack>
        {interviewees.map((interviewee) => (
          <IntervieweeCard
            key={interviewee.intervieweeId}
            interviewee={interviewee}
            onClick={() =>
              navigate(`interviewee/${interviewee.intervieweeId}`, { relative: 'path' })
            }
          />
        ))}
      </Stack>
    </Stack>
  )
}

export default IntervieweesList
