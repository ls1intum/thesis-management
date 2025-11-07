import {
  Badge,
  Button,
  Divider,
  Group,
  ScrollArea,
  Stack,
  Text,
  useMantineColorScheme,
} from '@mantine/core'
import { CheckCircleIcon, CircleIcon } from '@phosphor-icons/react'
import { ApplicationState } from '../../../requests/responses/application'
import { IApplicationInterviewProcess } from '../../../requests/responses/interview'

interface ISelectApplicantsListProps {
  possibleInterviewApplicants: IApplicationInterviewProcess[]
  selectedApplicants: string[]
  setSelectedApplicants: (applicantIds: string[]) => void
}

const SelectApplicantsList = ({
  possibleInterviewApplicants,
  selectedApplicants,
  setSelectedApplicants,
}: ISelectApplicantsListProps) => {
  const colorScheme = useMantineColorScheme()
  return (
    <Stack
      bg={colorScheme.colorScheme === 'dark' ? 'dark.8' : 'gray.0'}
      bdrs={'md'}
      gap={0}
      style={{ overflow: 'hidden' }}
    >
      <Group
        w={'100%'}
        justify='space-between'
        px={'1rem'}
        py={'0.5rem'}
        bg={
          selectedApplicants.length > 0
            ? colorScheme.colorScheme === 'dark'
              ? 'primary.11'
              : 'primary.2'
            : colorScheme.colorScheme === 'dark'
              ? 'dark.9'
              : 'gray.2'
        }
      >
        <Text fw={500}>{`${selectedApplicants.length} selected`}</Text>
        <Button
          variant={'subtle'}
          onClick={() => {
            if (selectedApplicants.length === possibleInterviewApplicants.length) {
              setSelectedApplicants([])
            } else {
              setSelectedApplicants(
                possibleInterviewApplicants.map((applicant) => applicant.applicationId),
              )
            }
          }}
          style={{ flexShrink: 0 }}
          c={colorScheme.colorScheme === 'dark' ? 'primary.3' : 'primary.8'}
          size='xs'
        >
          {selectedApplicants.length === possibleInterviewApplicants.length
            ? 'Deselect All'
            : 'Select All'}
        </Button>
      </Group>
      <ScrollArea.Autosize w={'100%'} type='hover' mih={'50px'} mah={'30vh'}>
        {possibleInterviewApplicants.map((applicant, index) => (
          <Stack
            key={applicant.applicationId}
            gap={0}
            style={{ cursor: 'pointer' }}
            onClick={() => {
              if (selectedApplicants.includes(applicant.applicationId)) {
                setSelectedApplicants(
                  selectedApplicants.filter((id) => id !== applicant.applicationId),
                )
              } else {
                setSelectedApplicants([...selectedApplicants, applicant.applicationId])
              }
            }}
            bg={
              selectedApplicants.includes(applicant.applicationId)
                ? colorScheme.colorScheme === 'dark'
                  ? 'primary.3'
                  : 'primary.0'
                : undefined
            }
            c={
              selectedApplicants.includes(applicant.applicationId)
                ? colorScheme.colorScheme === 'dark'
                  ? 'primary.10'
                  : 'primary'
                : undefined
            }
          >
            <Group justify='space-between' align='center' p={'1rem'}>
              <Group wrap='nowrap' justify='center' align='center' gap={'0.5rem'}>
                {selectedApplicants.includes(applicant.applicationId) ? (
                  <CheckCircleIcon size={24} style={{ flexShrink: 0 }} weight={'bold'} />
                ) : (
                  <CircleIcon size={24} style={{ flexShrink: 0 }} weight={'bold'} />
                )}
                <Text fw={500}>{applicant.applicantName}</Text>
              </Group>
              {applicant.state === ApplicationState.INTERVIEWING && (
                <Badge color='gray' radius='sm'>
                  Already Invited
                </Badge>
              )}
            </Group>
            {index < possibleInterviewApplicants.length - 1 && <Divider />}
          </Stack>
        ))}
      </ScrollArea.Autosize>
    </Stack>
  )
}
export default SelectApplicantsList
