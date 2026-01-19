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
  Checkbox,
  Tooltip,
  useMantineColorScheme,
  useMantineTheme,
} from '@mantine/core'
import {
  MagnifyingGlassIcon,
  PaperPlaneTiltIcon,
  PlusIcon,
  UsersFourIcon,
  XIcon,
} from '@phosphor-icons/react'
import { useEffect, useState } from 'react'
import {
  IIntervieweeLightWithNextSlot,
  InterviewState,
} from '../../../requests/responses/interview'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError, showSimpleSuccess } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useDebouncedValue } from '@mantine/hooks'
import IntervieweeCard from './IntervieweeCard'
import InviteConfirmationModal from './InviteConfirmationModal'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'
import AddIntervieweesModal from './AddIntervieweesModal'
import ApplicationsProvider from '../../../providers/ApplicationsProvider/ApplicationsProvider'

const IntervieweesList = () => {
  const { processId } = useParams<{ processId: string }>()
  const [searchIntervieweeKey, setSearchIntervieweeKey] = useState('')
  const [debouncedSearch] = useDebouncedValue(searchIntervieweeKey, 500)

  const [state, setState] = useState<string>('ALL')

  const isSmaller = useIsSmallerBreakpoint('md')

  const { interviewees, intervieweesLoading, fetchPossibleInterviewees } =
    useInterviewProcessContext()

  const [showLoader] = useDebouncedValue(intervieweesLoading, 1000)

  const [addIntervieweesModalOpen, setAddIntervieweesModalOpen] = useState(false)

  const inviteInterviewees = async (intervieweeIds: string[]) => {
    if (!intervieweeIds.length) return

    doRequest<IIntervieweeLightWithNextSlot[]>(
      `/v2/interview-process/${processId}/invite`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          intervieweeIds,
        },
      },
      (res) => {
        if (res.ok) {
          showSimpleSuccess('Invitations sent successfully')
          fetchPossibleInterviewees(debouncedSearch, state)
          setInviteModalOpen(false)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const cancelIntervieweeMode = () => {
    setSelectedIntervieweeIds([])
    setSelectIntervieweeMode(false)
  }

  useEffect(() => {
    fetchPossibleInterviewees(debouncedSearch, state)
  }, [state, debouncedSearch])

  const [selectedIntervieweeIds, setSelectedIntervieweeIds] = useState<string[]>([])
  const [selectIntervieweeMode, setSelectIntervieweeMode] = useState(false)

  const colorScheme = useMantineColorScheme()
  const theme = useMantineTheme()

  const numberOfSelectableInterviewees = interviewees.filter((interviewee) =>
    interviewee.score ? interviewee.score < 0 : true,
  ).length

  const [inviteModalOpen, setInviteModalOpen] = useState(false)

  return (
    <ApplicationsProvider fetchAll={false} limit={1}>
      <Stack gap={'1.5rem'}>
        <Group justify='space-between' align='center' gap={'0.5rem'}>
          <Title order={2}>Interviewees</Title>
          <Group gap={'0.5rem'}>
            <Button
              variant='outline'
              size='xs'
              leftSection={
                selectIntervieweeMode ? <XIcon size={16} /> : <PaperPlaneTiltIcon size={16} />
              }
              onClick={() => {
                if (selectIntervieweeMode) {
                  cancelIntervieweeMode()
                } else {
                  setSelectIntervieweeMode(true)
                }
              }}
            >
              {isSmaller
                ? selectIntervieweeMode
                  ? 'Cancel'
                  : 'Invites'
                : selectIntervieweeMode
                  ? 'Cancel Selection'
                  : 'Select for Invites'}
            </Button>
            <Button
              size='xs'
              leftSection={<PlusIcon size={16} />}
              onClick={() => setAddIntervieweesModalOpen(true)}
            >
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
        {showLoader ? (
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
        <Stack
          bdrs={'md'}
          style={
            selectIntervieweeMode
              ? {
                  overflow: 'hidden',
                  border: '1px solid',
                  borderColor:
                    colorScheme.colorScheme === 'dark'
                      ? theme.colors.dark[4]
                      : theme.colors.gray[3],
                }
              : undefined
          }
        >
          {selectIntervieweeMode && (
            <Group
              w={'100%'}
              justify='space-between'
              px={'1rem'}
              py={'0.5rem'}
              bg={
                selectedIntervieweeIds.length > 0
                  ? colorScheme.colorScheme === 'dark'
                    ? 'primary.11'
                    : 'primary.2'
                  : colorScheme.colorScheme === 'dark'
                    ? 'dark.9'
                    : 'gray.2'
              }
            >
              <Text fw={500} flex={1}>{`${selectedIntervieweeIds.length} selected`}</Text>
              <Button
                variant={'subtle'}
                style={{ flexShrink: 0 }}
                c={colorScheme.colorScheme === 'dark' ? 'primary.3' : 'primary.8'}
                size='xs'
                onClick={() => {
                  if (selectedIntervieweeIds.length === numberOfSelectableInterviewees) {
                    setSelectedIntervieweeIds([])
                  } else {
                    setSelectedIntervieweeIds(
                      interviewees
                        .filter((interviewee) => (interviewee.score ? interviewee.score < 0 : true))
                        .map((interviewee) => interviewee.intervieweeId),
                    )
                  }
                }}
              >
                {selectedIntervieweeIds.length === numberOfSelectableInterviewees
                  ? 'Deselect All'
                  : 'Select All'}
              </Button>
              <Button
                disabled={selectedIntervieweeIds.length === 0}
                size='xs'
                leftSection={<PaperPlaneTiltIcon size={16} />}
                onClick={() => {
                  setInviteModalOpen(true)
                }}
              >
                {`Send ${selectedIntervieweeIds.length > 0 ? selectedIntervieweeIds.length : ''} Invitation${selectedIntervieweeIds.length !== 1 && selectedIntervieweeIds.length > 0 ? 's' : ''}`}
              </Button>
            </Group>
          )}
          <Stack
            px={selectIntervieweeMode ? '1rem' : undefined}
            pt={selectIntervieweeMode ? '0.5rem' : undefined}
            pb={selectIntervieweeMode ? '1.5rem' : undefined}
          >
            {interviewees.map((interviewee) => (
              <Group key={interviewee.intervieweeId}>
                {selectIntervieweeMode && (
                  <>
                    {interviewee.score && interviewee.score >= 0 && (
                      <Tooltip
                        label={'Can not select completed interviewees'}
                        target={`#hover-me-${interviewee.intervieweeId}-completed`}
                      ></Tooltip>
                    )}
                    <Checkbox
                      checked={selectedIntervieweeIds.includes(interviewee.intervieweeId)}
                      onChange={(event) => {
                        const checked = event.currentTarget.checked
                        setSelectedIntervieweeIds((prev) => {
                          if (checked) {
                            return prev.includes(interviewee.intervieweeId)
                              ? prev
                              : [...prev, interviewee.intervieweeId]
                          }
                          return prev.filter((id) => id !== interviewee.intervieweeId)
                        })
                      }}
                      disabled={interviewee.score ? interviewee.score >= 0 : false}
                      id={`hover-me-${interviewee.intervieweeId}${interviewee.score && interviewee.score >= 0 ? '-completed' : ''}`}
                      radius={20}
                    />
                  </>
                )}
                <IntervieweeCard
                  interviewee={interviewee}
                  navigationLink={`interviewee/${interviewee.intervieweeId}`}
                  flex={1}
                  disableLink={selectIntervieweeMode}
                  inviteInterviewee={() => inviteInterviewees([interviewee.intervieweeId])}
                />
              </Group>
            ))}
          </Stack>
        </Stack>

        <InviteConfirmationModal
          inviteModalOpen={inviteModalOpen}
          setInviteModalOpen={setInviteModalOpen}
          interviewees={interviewees
            .filter((interviewee) => selectedIntervieweeIds.includes(interviewee.intervieweeId))
            .map((interviewee) => interviewee.user)}
          sendInvite={() => {
            inviteInterviewees(selectedIntervieweeIds)
            cancelIntervieweeMode()
          }}
          onCancel={cancelIntervieweeMode}
        />

        <AddIntervieweesModal
          opened={addIntervieweesModalOpen}
          closeModal={() => setAddIntervieweesModalOpen(false)}
        />
      </Stack>
    </ApplicationsProvider>
  )
}

export default IntervieweesList
