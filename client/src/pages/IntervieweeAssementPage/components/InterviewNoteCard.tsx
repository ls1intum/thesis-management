import { Card, Stack, Title } from '@mantine/core'
import DocumentEditor from '../../../components/DocumentEditor/DocumentEditor'
import { use, useEffect } from 'react'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { useForm } from '@mantine/form'
import { useDebouncedValue } from '@mantine/hooks'

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

  return (
    <Card withBorder radius='md' h={'100%'} flex={1}>
      <Stack h={'100%'}>
        <Title order={4}>Interview Note</Title>
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
