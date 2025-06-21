import { Box, Flex, Pagination, SimpleGrid, Text } from '@mantine/core'
import TopicCard from './TopicCard/TopicCard'
import { useTopicsContext } from '../../../../providers/TopicsProvider/hooks'

const TopicCardGrid = () => {
  const { topics, page, setPage, limit, isLoading } = useTopicsContext()

  return (
    <Flex direction={'column'} gap='md' w='100%' h='100%'>
      <Box flex={1}>
        <SimpleGrid
          cols={{ base: 1, sm: 2, xl: 3 }}
          spacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
          verticalSpacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
        >
          {topics?.content.map((topic) => <TopicCard key={topic.topicId} topic={topic} />)}
        </SimpleGrid>
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
