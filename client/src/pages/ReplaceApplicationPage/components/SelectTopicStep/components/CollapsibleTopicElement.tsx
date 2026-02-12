import {
  Accordion,
  Card,
  Group,
  Stack,
  Title,
  Text,
  Button,
  Divider,
  Grid,
  Center,
  Loader,
} from '@mantine/core'
import { ITopicOverview, ITopic } from '../../../../../requests/responses/topic'
import ThesisTypeBadge from '../../../../LandingPage/components/ThesisTypBadge/ThesisTypBadge'
import { IPublishedThesis } from '../../../../../requests/responses/thesis'
import { useHover } from '@mantine/hooks'
import AvatarUserList from '../../../../../components/AvatarUserList/AvatarUserList'
import DocumentEditor from '../../../../../components/DocumentEditor/DocumentEditor'
import LabeledItem from '../../../../../components/LabeledItem/LabeledItem'
import { pluralize } from '../../../../../utils/format'
import { useTopic } from '../../../../../hooks/fetcher'
import { useState } from 'react'

interface ICollapsibleTopicElementProps {
  topic: IPublishedThesis | ITopicOverview
  onApply?: (topic: ITopic | undefined) => unknown
}

const CollapsibleTopicElement = ({ topic, onApply }: ICollapsibleTopicElementProps) => {
  const { hovered, ref } = useHover()
  const [expanded, setExpanded] = useState(false)

  const isTopicOverview = 'topicId' in topic
  const fullTopic = useTopic(isTopicOverview && expanded ? topic.topicId : undefined)

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
      <Accordion.Item value={`topic-card-${isTopicOverview ? topic.topicId : topic.thesisId}`}>
        <Accordion.Control onClick={() => setExpanded(true)}>
          <Stack gap={'0.5rem'}>
            <Group gap={'0.75rem'}>
              {isTopicOverview ? (
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
            {isTopicOverview ? (
              fullTopic === undefined ? (
                <Center py='md'>
                  <Loader size='sm' />
                </Center>
              ) : fullTopic === false ? (
                <Center py='md'>
                  <Text c='dimmed' size='sm'>
                    Failed to load topic details.
                  </Text>
                </Center>
              ) : fullTopic ? (
                <>
                  <Grid>
                    <Grid.Col span={4}>
                      {fullTopic.supervisors.length > 0 && (
                        <LabeledItem
                          label={pluralize('Examiner', fullTopic.supervisors.length)}
                          value={<AvatarUserList users={fullTopic.supervisors} size='xs' />}
                        />
                      )}
                    </Grid.Col>
                    <Grid.Col span={8}>
                      {fullTopic.advisors.length > 0 && (
                        <LabeledItem
                          label={pluralize('Supervisor', fullTopic.advisors.length)}
                          value={<AvatarUserList users={fullTopic.advisors} size='xs' />}
                        />
                      )}
                    </Grid.Col>
                    <Grid.Col span={4}>
                      <LabeledItem
                        label={'Published At'}
                        value={
                          fullTopic.publishedAt
                            ? new Date(fullTopic.publishedAt).toLocaleDateString()
                            : '-'
                        }
                      />
                    </Grid.Col>
                    {fullTopic.applicationDeadline && (
                      <Grid.Col span={4}>
                        <LabeledItem
                          label={'Application Deadline'}
                          value={new Date(fullTopic.applicationDeadline).toLocaleDateString()}
                        />
                      </Grid.Col>
                    )}
                    {fullTopic.intendedStart && (
                      <Grid.Col span={4}>
                        <LabeledItem
                          label={'Intended Start'}
                          value={new Date(fullTopic.intendedStart).toLocaleDateString()}
                        />
                      </Grid.Col>
                    )}
                  </Grid>
                  <DocumentEditor label='Problem Statement' value={fullTopic.problemStatement} />
                  {fullTopic.requirements && (
                    <DocumentEditor label='Requirements' value={fullTopic.requirements} />
                  )}
                  {fullTopic.goals && <DocumentEditor label='Goals' value={fullTopic.goals} />}
                  {fullTopic.references && (
                    <DocumentEditor label='References' value={fullTopic.references} />
                  )}
                </>
              ) : null
            ) : (
              <></>
            )}
            {onApply && isTopicOverview && (
              <Button
                onClick={() => onApply(fullTopic || undefined)}
                fullWidth
                disabled={!fullTopic}
              >
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
