import { Card, Group, Stack, Title, Text } from '@mantine/core'
import DocumentEditor from '../../../components/DocumentEditor/DocumentEditor'
import { useEffect } from 'react'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { useForm } from '@mantine/form'
import { useDebouncedValue } from '@mantine/hooks'
import { CheckCircleIcon, WarningCircleIcon } from '@phosphor-icons/react/dist/ssr'

interface IInterviewNoteCardProps {
  interviewNote: string | null
  onInterviewNoteChange?: (newNote: string) => void
}

interface IInterviewNoteForm {
  interviewNote: string
}

const InterviewNoteCard = ({ interviewNote, onInterviewNoteChange }: IInterviewNoteCardProps) => {
  const isSmaller = useIsSmallerBreakpoint('sm')

  const form = useForm<IInterviewNoteForm>({
    mode: 'controlled',
    initialValues: {
      interviewNote: interviewNote ?? '',
    },
  })

  useEffect(() => {
    form.setValues({ interviewNote: interviewNote ?? '' })
  }, [interviewNote])

  const [debouncedInterviewNote] = useDebouncedValue(form.values.interviewNote, 1000)

  useEffect(() => {
    if (onInterviewNoteChange) {
      onInterviewNoteChange(debouncedInterviewNote)
    }
  }, [debouncedInterviewNote])

  const changesSaved = debouncedInterviewNote === form.values.interviewNote

  return (
    <Card withBorder radius='md' h={'100%'} flex={1}>
      <Stack h={'100%'}>
        <Group align='center'>
          <Title order={4} flex={1}>
            Interview Note
          </Title>
          <Group gap={'0.25rem'} align='center' justify='center'>
            {changesSaved ? (
              <CheckCircleIcon color='green' size={20} />
            ) : (
              <WarningCircleIcon color='orange' size={20} />
            )}
            <Text size='sm' c={changesSaved ? 'green' : 'orange'} pt={'1px'}>
              {changesSaved ? 'Saved' : 'Unsaved Changes'}
            </Text>
          </Group>
        </Group>
        <DocumentEditor
          value={interviewNote || ''}
          editMode
          minHeight={isSmaller ? '30vh' : '50vh'}
          {...form.getInputProps('interviewNote')}
        />
      </Stack>
    </Card>
  )
}

export default InterviewNoteCard
