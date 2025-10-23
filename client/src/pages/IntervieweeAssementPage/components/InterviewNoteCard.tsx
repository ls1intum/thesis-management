import { Card, Stack, Title } from '@mantine/core'
import DocumentEditor from '../../../components/DocumentEditor/DocumentEditor'

interface IInterviewNoteCardProps {
  interviewNote: string | null
  onInterviewNoteChange?: (newNote: string) => void
}

const InterviewNoteCard = ({ interviewNote, onInterviewNoteChange }: IInterviewNoteCardProps) => {
  return (
    <Card withBorder radius='md' h={'100%'}>
      <Stack h={'100%'}>
        <Title order={4}>Interview Note</Title>
        <DocumentEditor value={interviewNote || ''} editMode /> {/* TODO: Add onchange*/}
      </Stack>
    </Card>
  )
}

export default InterviewNoteCard
