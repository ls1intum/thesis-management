import { Accordion, Card, Group, Stack, Title, Text } from '@mantine/core'
import { ITopic } from '../../../../../requests/responses/topic'
import ThesisTypeBadge from '../../../../LandingPage/components/ThesisTypBadge/ThesisTypBadge'
import { IPublishedThesis } from '../../../../../requests/responses/thesis'
import { useHover } from '@mantine/hooks'

const CollapsibleTopicElement = ({ topic }: { topic: IPublishedThesis | ITopic }) => {
  const { hovered, ref } = useHover()
  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      my='sm'
      p={'0.25rem'}
      style={{ cursor: 'pointer' }}
      ref={ref}
    >
      <Accordion.Item value={`topic-card-${'topicId' in topic ? topic.topicId : topic.thesisId}`}>
        <Accordion.Control>
          <Stack gap={'0.5rem'}>
            <Group gap={'0.75rem'}>
              {'topicId' in topic ? (
                topic.thesisTypes?.length ? (
                  topic.thesisTypes.map((type) => (
                    <ThesisTypeBadge type={type} textColor='gray' textSize='xs' />
                  ))
                ) : (
                  <ThesisTypeBadge type='Any' key={'any'} textSize='xs' />
                )
              ) : (
                <ThesisTypeBadge type={topic.type} textColor='gray' textSize='xs' />
              )}
            </Group>
            <Stack gap={'0.25rem'}>
              <Title order={5}>{topic.title}</Title>
              <Title c={'dimmed'} order={6}>
                {topic.researchGroup.name}
              </Title>
            </Stack>
          </Stack>
        </Accordion.Control>
        <Accordion.Panel>
          {'topicId' in topic ? <Stack></Stack> : <Stack>{topic.abstractText}</Stack>}
        </Accordion.Panel>
      </Accordion.Item>
    </Card>
  )
}
export default CollapsibleTopicElement
