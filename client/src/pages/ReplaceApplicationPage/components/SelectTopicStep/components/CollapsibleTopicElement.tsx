import { Accordion, Card, Group, Stack, Title, Text, Button, Divider, Grid } from '@mantine/core'
import { ITopic } from '../../../../../requests/responses/topic'
import ThesisTypeBadge from '../../../../LandingPage/components/ThesisTypBadge/ThesisTypBadge'
import { IPublishedThesis } from '../../../../../requests/responses/thesis'
import { useHover } from '@mantine/hooks'
import AvatarUserList from '../../../../../components/AvatarUserList/AvatarUserList'
import DocumentEditor from '../../../../../components/DocumentEditor/DocumentEditor'
import LabeledItem from '../../../../../components/LabeledItem/LabeledItem'
import { pluralize } from '../../../../../utils/format'

interface ICollapsibleTopicElementProps {
  topic: IPublishedThesis | ITopic
  onApply?: (topic: ITopic | undefined) => unknown
}

const CollapsibleTopicElement = ({ topic, onApply }: ICollapsibleTopicElementProps) => {
  const { hovered, ref } = useHover()
  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      my='sm'
      p={0}
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
                    <ThesisTypeBadge
                      type={type}
                      textColor='gray'
                      textSize='xs'
                      key={`${topic.topicId}-${type}`}
                    />
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
          <Stack>
            <Divider />
            {'topicId' in topic ? (
              <>
                <Grid>
                  <Grid.Col span={4}>
                    {topic.supervisors.length > 0 && (
                      <LabeledItem
                        label={pluralize('Supervisor', topic.supervisors.length)}
                        value={<AvatarUserList users={topic.supervisors} size='xs' />}
                      />
                    )}
                  </Grid.Col>
                  <Grid.Col span={8}>
                    {topic.advisors.length > 0 && (
                      <LabeledItem
                        label={pluralize('Advisor', topic.advisors.length)}
                        value={<AvatarUserList users={topic.advisors} size='xs' />}
                      />
                    )}
                  </Grid.Col>
                  <Grid.Col span={4}>
                    <LabeledItem
                      label={'Published At'}
                      value={new Date(topic.createdAt).toLocaleDateString()}
                    />
                  </Grid.Col>
                  {topic.applicationDeadline && (
                    <Grid.Col span={4}>
                      <LabeledItem
                        label={'Application Deadline'}
                        value={new Date(topic.applicationDeadline).toLocaleDateString()}
                      />
                    </Grid.Col>
                  )}
                  {topic.intendedStart && (
                    <Grid.Col span={4}>
                      <LabeledItem
                        label={'Intended Start'}
                        value={new Date(topic.intendedStart).toLocaleDateString()}
                      />
                    </Grid.Col>
                  )}
                </Grid>
                <DocumentEditor label='Problem Statement' value={topic.problemStatement} />
                {topic.requirements && (
                  <DocumentEditor label='Requirements' value={topic.requirements} />
                )}
                {topic.goals && <DocumentEditor label='Goals' value={topic.goals} />}
                {topic.references && <DocumentEditor label='References' value={topic.references} />}
              </>
            ) : (
              <></>
            )}
            {onApply && 'topicId' in topic && (
              <Button onClick={() => onApply(topic)} fullWidth>
                Apply
              </Button>
            )}
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Card>
  )
}
export default CollapsibleTopicElement
