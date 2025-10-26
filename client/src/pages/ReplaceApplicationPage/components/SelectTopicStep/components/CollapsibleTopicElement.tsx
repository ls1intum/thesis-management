import { Accordion, Group, Stack, Title } from '@mantine/core'
import { ITopic } from '../../../../../requests/responses/topic'
import ThesisTypeBadge from '../../../../LandingPage/components/ThesisTypBadge/ThesisTypBadge'
import { IPublishedThesis } from '../../../../../requests/responses/thesis'

const CollapsibleTopicElement = ({ topic }: { topic: IPublishedThesis | ITopic }) => {
  return (
    <Accordion.Item value={`topic-card-${'topicId' in topic ? topic.topicId : topic.thesisId}`}>
      <Accordion.Control>
        <Stack>
          <Group gap={'0.5rem'}>
            {'topicId' in topic ? (
              topic.thesisTypes?.length ? (
                topic.thesisTypes.map((type) => <ThesisTypeBadge type={type}></ThesisTypeBadge>)
              ) : (
                <ThesisTypeBadge type='Any' key={'any'} />
              )
            ) : (
              <ThesisTypeBadge type={topic.type} />
            )}
          </Group>
          <Stack gap={'0.25rem'}>
            <Title order={5}>{topic.title}</Title>
            <Title order={6} c={'dimmed'}>
              {topic.researchGroup.name}
            </Title>
          </Stack>
        </Stack>
      </Accordion.Control>
      <Accordion.Panel>
        <Stack>TODO</Stack>
      </Accordion.Panel>
    </Accordion.Item>
  )
}
export default CollapsibleTopicElement
