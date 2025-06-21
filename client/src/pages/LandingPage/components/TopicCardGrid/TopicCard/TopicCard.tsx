import { Card, Flex, Text, Badge, Group, Box, Stack, Button, Tooltip } from '@mantine/core'
import { ITopic } from '../../../../../requests/responses/topic'
import { useHover } from '@mantine/hooks'
import { formatThesisType } from '../../../../../utils/format'
import { Users } from 'phosphor-react'
import CustomAvatar from '../../../../../components/CustomAvatar/CustomAvatar'
import AvatarUserList from '../../../../../components/AvatarUserList/AvatarUserList'

interface ITopicCardProps {
  topic: ITopic
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
      <Card.Section withBorder inheritPadding py={'xs'}>
        <Flex
          justify={'space-between'}
          align={'center'}
          gap={5}
          style={{
            minHeight: '3rem',
          }}
        >
          <Flex wrap='wrap' gap={8}>
            {topic.thesisTypes?.length ? (
              topic.thesisTypes.map((type) => (
                <Group key={type} gap={5} wrap='nowrap'>
                  <Box w={15} h={15} style={{ borderRadius: '50%' }} bg={getTypeColor(type)} />
                  <Text size='sm'>
                    {type.toLowerCase() === 'interdisciplinary_project'
                      ? formatThesisType(type, true)
                      : formatThesisType(type)}
                  </Text>
                </Group>
              ))
            ) : (
              <Group key={'any'} gap={5} wrap='nowrap'>
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

        <Stack gap={0} pt={10}>
          <Tooltip openDelay={500} label={topic.title} withArrow>
            <Flex
              style={{
                minHeight: '4rem',
              }}
              align={'center'}
            >
              <Text fw={500} lineClamp={2} size='xl'>
                {topic.title}
              </Text>
            </Flex>
          </Tooltip>

          <Text c='dimmed'>{topic.researchGroup.name}</Text>
        </Stack>
      </Card.Section>

      <Stack gap={5} pt={15} flex={1}>
        <Group gap={3} c='dimmed'>
          <Users></Users>
          <Text size='sm'>Advisors</Text>
        </Group>
        <AvatarUserList users={topic.advisors} />
      </Stack>

      <Button type='submit' fullWidth mt='md'>
        Apply
      </Button>
    </Card>
  )
}

export default TopicCard
