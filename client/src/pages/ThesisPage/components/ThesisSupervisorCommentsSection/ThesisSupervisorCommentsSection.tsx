import { Accordion, Badge, Group, Stack, Text } from '@mantine/core'
import { useLoadedThesisContext } from '../../../../providers/ThesisProvider/hooks'
import ThesisCommentsProvider from '../../../../providers/ThesisCommentsProvider/ThesisCommentsProvider'
import ThesisCommentsList from '../../../../components/ThesisCommentsList/ThesisCommentsList'
import ThesisCommentsForm from '../../../../components/ThesisCommentsForm/ThesisCommentsForm'

const ThesisSupervisorCommentsSection = () => {
  const { thesis, access } = useLoadedThesisContext()

  if (!access.supervisor) {
    return null
  }

  return (
    <Accordion variant='separated' defaultValue=''>
      <Accordion.Item value='open'>
        <Accordion.Control>
          <Group gap='xs'>
            <Text>Supervisor Comments</Text>
            <Badge color='grey'>Not visible to student</Badge>
          </Group>
        </Accordion.Control>
        <Accordion.Panel>
          <Stack>
            <ThesisCommentsProvider limit={10} thesis={thesis} commentType='SUPERVISOR'>
              <ThesisCommentsList />
              <ThesisCommentsForm />
            </ThesisCommentsProvider>
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}

export default ThesisSupervisorCommentsSection
