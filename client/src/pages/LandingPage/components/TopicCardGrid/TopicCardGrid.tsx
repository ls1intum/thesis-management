import { Box, Center, Flex, Pagination, SimpleGrid, Text } from '@mantine/core'
import TopicCard from './TopicCard/TopicCard'
import { useTopicsContext } from '../../../../providers/TopicsProvider/hooks'
import { Spinner } from 'phosphor-react'
import { Dispatch, useEffect, useState } from 'react'
import { ITopic } from '../../../../requests/responses/topic'
import { IPublishedThesis } from '../../../../requests/responses/thesis'
import { PaginationResponse } from '../../../../requests/responses/pagination'

interface ITopicCardGridProps {
  topics: PaginationResponse<ITopic> | PaginationResponse<IPublishedThesis>
  page: number
  setPage: (page: number) => void
  limit: number
  isLoading?: boolean
}

interface ITopicCardGridContentProps {
  gridContent?: ITopicCardGridProps
  setOpenTopic?: Dispatch<React.SetStateAction<IPublishedThesis | undefined>>
}

const TopicCardGrid = ({ gridContent, setOpenTopic }: ITopicCardGridContentProps) => {
  const { topics, page, setPage, limit, isLoading } = gridContent ?? useTopicsContext()

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
      <Box flex={1}>
        {showSpinner ? (
          <Center>
            <Spinner size={32} weight='bold' />
          </Center>
        ) : (
          <SimpleGrid
            cols={{ base: 1, sm: 2, xl: 3 }}
            spacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
            verticalSpacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
          >
            {topics?.content.map((topic) => (
              <TopicCard key={topic.title} topic={topic} setOpenTopic={setOpenTopic} />
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
