import {
  Accordion,
  Box,
  Button,
  Card,
  Center,
  Flex,
  Loader,
  Pagination,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import TopicCard from './TopicCard/TopicCard'
import { TopicsContext } from '../../../../providers/TopicsProvider/context'
import { Database } from '@phosphor-icons/react'
import type { Dispatch } from 'react'
import { use, useEffect, useState } from 'react'
import type { ITopic, ITopicOverview } from '../../../../requests/responses/topic'
import type { IPublishedThesis } from '../../../../requests/responses/thesis'
import type { PaginationResponse } from '../../../../requests/responses/pagination'
import CollapsibleTopicElement from '../../../ReplaceApplicationPage/components/SelectTopicStep/components/CollapsibleTopicElement'
import { GLOBAL_CONFIG } from '../../../../config/global'

interface ITopicCardGridProps {
  topics: PaginationResponse<ITopicOverview> | PaginationResponse<IPublishedThesis>
  page: number
  setPage: (page: number) => void
  limit: number
  isLoading?: boolean
}

interface ITopicCardGridContentProps {
  gridContent?: ITopicCardGridProps
  setOpenTopic?: Dispatch<React.SetStateAction<IPublishedThesis | undefined>>
  collapsibleTopics?: boolean
  showSuggestedTopic?: boolean
  onApply?: (topic: ITopic | undefined) => unknown
}

const TopicCardGrid = ({
  gridContent,
  setOpenTopic,
  collapsibleTopics = false,
  showSuggestedTopic = false,
  onApply,
}: ITopicCardGridContentProps) => {
  const contextData = use(TopicsContext)
  const source = gridContent ?? contextData
  if (!source) {
    throw new Error('TopicCardGrid requires either a gridContent prop or a TopicsProvider ancestor')
  }
  const { topics, page, setPage, limit, isLoading } = source

  //Prevent flickering spinner for short loading times
  const [showSpinner, setShowSpinner] = useState(false)
  useEffect(() => {
    if (isLoading) {
      const timeout = setTimeout(() => setShowSpinner(true), 200)
      return () => clearTimeout(timeout) // cancel if loading ends early
    } else {
      setShowSpinner(false)
    }
  }, [isLoading])

  return (
    <Flex direction={'column'} gap='md' w='100%' h='100%'>
      {(topics?.content ?? []).length === 0 && !showSuggestedTopic && (
        <Center h='100%'>
          <Stack align='center' gap='xs'>
            <ThemeIcon radius='xl' size={50} color='gray' variant='light'>
              <Database size={24} weight='duotone' />
            </ThemeIcon>
            <Text size='sm' c='dimmed'>
              No topics found
            </Text>
          </Stack>
        </Center>
      )}
      <Box flex={1}>
        {showSpinner ? (
          <Center>
            <Loader size={32} />
          </Center>
        ) : collapsibleTopics ? (
          <Accordion
            chevronPosition={'right'}
            variant={'unstyled'}
            radius='md'
            chevronIconSize={20}
          >
            {(topics?.content ?? []).map((topic) => (
              <CollapsibleTopicElement
                key={'topicId' in topic ? topic.topicId : topic.thesisId}
                topic={topic}
                onApply={'topicId' in topic ? onApply : undefined}
              />
            ))}

            {GLOBAL_CONFIG.allow_suggested_topics && topics?.last && (
              <Card withBorder shadow='xs' radius='md' my='sm' p={0} style={{ cursor: 'pointer' }}>
                <Accordion.Item value='custom'>
                  <Accordion.Control>
                    <Stack gap={'0.5rem'}>
                      <Title order={5}>Suggest your own topic</Title>
                      <Title c={'dimmed'} order={6}>
                        Can&apos;t find a suitable topic? Suggest your own thesis topic to a group.
                      </Title>
                    </Stack>
                  </Accordion.Control>
                  <Accordion.Panel>
                    <Center>
                      <Button onClick={() => (onApply ? onApply(undefined) : null)} fullWidth>
                        Suggest topic
                      </Button>
                    </Center>
                  </Accordion.Panel>
                </Accordion.Item>
              </Card>
            )}
          </Accordion>
        ) : (
          <SimpleGrid
            cols={{ base: 1, sm: 2, xl: 3 }}
            spacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
            verticalSpacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
          >
            {(topics?.content ?? []).map((topic) => (
              <TopicCard
                key={'topicId' in topic ? topic.topicId : topic.thesisId}
                topic={topic}
                setOpenTopic={setOpenTopic}
              />
            ))}
          </SimpleGrid>
        )}
      </Box>
      <Flex justify={'space-between'} align={'center'} gap='md'>
        <Text size='sm'>
          {topics && topics.totalElements > 0 ? (
            <>
              {page * limit + 1}–{Math.min((page + 1) * limit, topics.totalElements)} /{' '}
              {topics.totalElements}
            </>
          ) : (
            '0 results'
          )}
        </Text>

        <Pagination
          value={page + 1}
          onChange={(p) => setPage(p - 1)}
          total={topics ? topics.totalPages : 1}
          size='sm'
        />
      </Flex>
    </Flex>
  )
}

export default TopicCardGrid
