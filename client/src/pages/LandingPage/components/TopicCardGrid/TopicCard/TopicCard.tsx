import { Card, Flex, Text, Badge, Group, Box, Stack, Button, Tooltip } from '@mantine/core'
import { ITopic } from '../../../../../requests/responses/topic'
import { useHover } from '@mantine/hooks'
import { formatThesisType } from '../../../../../utils/format'
import { Users } from 'phosphor-react'
import CustomAvatar from '../../../../../components/CustomAvatar/CustomAvatar'
import AvatarUserList from '../../../../../components/AvatarUserList/AvatarUserList'
import { IPublishedThesis } from '../../../../../requests/responses/thesis'
import { useEffect, useState } from 'react'
import { ILightUser } from '../../../../../requests/responses/user'
import { Link } from 'react-router'

interface ITopicCardProps {
  topic: ITopic | IPublishedThesis
}

const TopicCard = ({ topic }: ITopicCardProps) => {
  const { hovered, ref } = useHover()

  const getTypeColor = (type: string): string => {
    switch (type.toLowerCase()) {
      case 'bachelor':
        return 'indigo.3'
      case 'master':
        return 'pink.3'
      case 'guided_research':
        return 'yellow.3'
      case 'interdisciplinary_project':
        return 'lime.3'
      default:
        return 'gray.3'
    }
  }

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
    } else {
      setThesisTypes(topic.thesisTypes ?? [])
      setIsPublished(false)
      setTopicId(topic.topicId)
    }
  }, [topic])

  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      h='100%'
      w='100%'
      style={{ cursor: 'pointer' }}
      ref={ref}
    >
      <Card.Section withBorder inheritPadding p='1rem'>
        <Stack gap='0.5rem'>
          <Flex
            justify={'space-between'}
            align={'center'}
            gap={5}
            style={{
              minHeight: '3.5rem',
            }}
          >
            <Flex wrap='wrap' gap={5}>
              {thesisTypes?.length ? (
                thesisTypes.map((type) => (
                  <Group key={type} gap={3} wrap='nowrap'>
                    <Box w={15} h={15} style={{ borderRadius: '50%' }} bg={getTypeColor(type)} />
                    <Text size='sm'>
                      {type.toLowerCase() === 'interdisciplinary_project'
                        ? formatThesisType(type, true)
                        : formatThesisType(type)}
                    </Text>
                  </Group>
                ))
              ) : (
                <Group key={'any'} gap={3} wrap='nowrap'>
                  <Box w={15} h={15} style={{ borderRadius: '50%' }} bg={'gray.3'} />
                  <Text size='sm'>Any</Text>
                </Group>
              )}
            </Flex>
            <Badge variant='outline' style={{ flexShrink: 0 }}>
              {/*TODO: SEND ACTUALL abbriviation*/}
              {'AET'}
            </Badge>
          </Flex>
          <Tooltip openDelay={500} label={topic.title} withArrow>
            <Flex
              style={{
                minHeight: '4rem',
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

      <Button fullWidth mt='md' component={Link} to={`/submit-application/${topicId}`}>
        Apply
      </Button>
    </Card>
  )
}

export default TopicCard
