import { Card, Flex, Text, Group, Stack, Button, Tooltip, Anchor } from '@mantine/core'
import { ITopic } from '../../../../../requests/responses/topic'
import { useHover, useMediaQuery } from '@mantine/hooks'
import { DownloadSimple, Users } from 'phosphor-react'
import AvatarUserList from '../../../../../components/AvatarUserList/AvatarUserList'
import { IPublishedThesis } from '../../../../../requests/responses/thesis'
import { Dispatch, useEffect, useState } from 'react'
import { ILightUser } from '../../../../../requests/responses/user'
import { Link, useNavigate } from 'react-router'
import { GLOBAL_CONFIG } from '../../../../../config/global'
import ThesisTypeBadge from '../../ThesisTypBadge/ThesisTypBadge'

interface ITopicCardProps {
  topic: ITopic | IPublishedThesis
  setOpenTopic?: Dispatch<React.SetStateAction<IPublishedThesis | undefined>>
}

const TopicCard = ({ topic, setOpenTopic }: ITopicCardProps) => {
  const { hovered, ref } = useHover()

  const navigate = useNavigate()

  const [thesisTypes, setThesisTypes] = useState<string[]>([])
  const [students, setStudents] = useState<ILightUser[]>([])
  const [isPublished, setIsPublished] = useState<boolean>(false)
  const [topicId, setTopicId] = useState<string | undefined>(undefined)

  useEffect(() => {
    if ('type' in topic) {
      // It's a published thesis
      setThesisTypes([topic.type])
      setStudents(topic.students)
      setIsPublished(true)
      setTopicId(topic.thesisId)
    } else {
      setThesisTypes(topic.thesisTypes ?? [])
      setIsPublished(false)
      setTopicId(topic.topicId)
    }
  }, [topic])

  const largerThanXs = useMediaQuery('(min-width: 36em)')

  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      h='100%'
      w='100%'
      style={{ cursor: 'pointer' }}
      ref={ref}
      onClick={() => {
        if (!isPublished) {
          navigate(`/topics/${topicId}`)
        } else if (setOpenTopic) {
          setOpenTopic(topic as IPublishedThesis)
        }
      }}
    >
      <Card.Section withBorder inheritPadding p='1rem'>
        <Stack gap={'0.25rem'}>
          <Flex
            justify={'space-between'}
            align={'center'}
            gap={5}
            style={{
              minHeight: '3.5rem',
            }}
            pb={'0.25rem'}
          >
            <Flex wrap='wrap' gap={5}>
              {thesisTypes?.length ? (
                thesisTypes.map((type) => <ThesisTypeBadge type={type}></ThesisTypeBadge>)
              ) : (
                <ThesisTypeBadge type='Any' key={'any'} />
              )}
            </Flex>
          </Flex>
          <Tooltip openDelay={500} label={topic.title} withArrow>
            <Flex
              style={{
                minHeight: largerThanXs ? '4rem' : undefined,
              }}
              align={'flex-start'}
            >
              <Text fw={500} lineClamp={2} size='lg'>
                {topic.title}
              </Text>
            </Flex>
          </Tooltip>

          <Text c='dimmed'>{topic.researchGroup.name}</Text>
        </Stack>
      </Card.Section>

      <Stack gap='xs' pt='1rem' flex={1}>
        <Group gap={'xs'} c='dimmed'>
          <Users></Users>
          <Text size='sm'>Advisor(s)</Text>
        </Group>
        <AvatarUserList users={topic.advisors} />
      </Stack>

      {students.length > 0 && (
        <Stack gap={5} pt={15} flex={1}>
          <Group gap={3} c='dimmed'>
            <Users></Users>
            <Text size='sm'>Student(s)</Text>
          </Group>
          <AvatarUserList users={students} />
        </Stack>
      )}

      {isPublished ? (
        <Group gap={5} mt='md' w={'100%'} align='center'>
          <Button flex={1}>More Information</Button>
          <Button
            component={Anchor<'a'>}
            href={`${GLOBAL_CONFIG.server_host}/api/v2/published-theses/${topicId}/thesis`}
            target='_blank'
          >
            <DownloadSimple />
          </Button>
        </Group>
      ) : (
        <Button fullWidth mt='md' component={Link} to={`/submit-application/${topicId}`}>
          Apply
        </Button>
      )}
    </Card>
  )
}

export default TopicCard
