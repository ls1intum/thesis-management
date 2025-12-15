import { Card, Stack, Title } from '@mantine/core'
import DocumentEditor from '../../../components/DocumentEditor/DocumentEditor'
import { use } from 'react'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'

interface IInterviewNoteCardProps {
  interviewNote: string | null
  onInterviewNoteChange?: (newNote: string) => void
}

const InterviewNoteCard = ({ interviewNote, onInterviewNoteChange }: IInterviewNoteCardProps) => {
  const isSmaller = useIsSmallerBreakpoint('sm')
  return (
    <Card withBorder radius='md' h={'100%'} flex={1}>
      <Stack h={'100%'}>
        <Title order={4}>Interview Note</Title>
        <DocumentEditor
          value={interviewNote || ''}
          editMode
          minHeight={isSmaller ? '30vh' : '50vh'}
        />{' '}
        {/* TODO: Add onchange*/}
      </Stack>
    </Card>
  )
}

export default InterviewNoteCard
