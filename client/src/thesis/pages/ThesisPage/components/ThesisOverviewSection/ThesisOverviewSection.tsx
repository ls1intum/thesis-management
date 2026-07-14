import { Accordion, Badge, Group, Text } from '@mantine/core'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import InfoContent from '@/thesis/pages/ThesisPage/components/ThesisOverviewSection/InfoContent'
import InvolvedPersonsContent from '@/thesis/pages/ThesisPage/components/ThesisOverviewSection/InvolvedPersonsContent'
import CommentsContent from '@/thesis/pages/ThesisPage/components/ThesisOverviewSection/CommentsContent'

const ThesisOverviewSection = () => {
  const { access } = useLoadedThesisContext()

  const defaultOpen: string[] = ['info']

  return (
    <Accordion variant='separated' multiple defaultValue={defaultOpen}>
      <Accordion.Item value='info'>
        <Accordion.Control>Info</Accordion.Control>
        <Accordion.Panel>
          <InfoContent />
        </Accordion.Panel>
      </Accordion.Item>
      {access.supervisor && (
        <Accordion.Item value='persons'>
          <Accordion.Control>Involved Persons</Accordion.Control>
          <Accordion.Panel>
            <InvolvedPersonsContent />
          </Accordion.Panel>
        </Accordion.Item>
      )}
      {access.supervisor && (
        <Accordion.Item value='comments'>
          <Accordion.Control>
            <Group gap='xs'>
              <Text>Comments</Text>
              <Badge color='grey'>Not visible to student</Badge>
            </Group>
          </Accordion.Control>
          <Accordion.Panel>
            <CommentsContent />
          </Accordion.Panel>
        </Accordion.Item>
      )}
    </Accordion>
  )
}

export default ThesisOverviewSection
