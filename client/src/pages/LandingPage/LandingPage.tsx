import TopicsProvider from '../../providers/TopicsProvider/TopicsProvider'
import TopicsTable from '../../components/TopicsTable/TopicsTable'
import {
  Box,
  Button,
  Flex,
  Group,
  SegmentedControl,
  Stack,
  TextInput,
  Title,
  Text,
  Center,
} from '@mantine/core'
import { Link, useSearchParams } from 'react-router'
import PublishedTheses from './components/PublishedTheses/PublishedTheses'
import { usePageTitle } from '../../hooks/theme'
import LandingPageHeader from './components/LandingPageHeader/LandingPageHeader'
import { List, MagnifyingGlass, SquaresFour } from 'phosphor-react'
import { useEffect, useState } from 'react'
import { useDebouncedValue } from '@mantine/hooks'
import { GLOBAL_CONFIG } from '../../config/global'

const LandingPage = () => {
  usePageTitle('Find a Thesis Topic')

  const [searchParams, setSearchParams] = useSearchParams()
  const [searchKey, setSearchKey] = useState(searchParams.get('search') ?? '')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)

  const [topicView, setTopicView] = useState<string>(
    searchParams.get('view') ?? GLOBAL_CONFIG.topic_views_options.OPEN,
  )

  const listRepresentationOptions = [
    {
      label: (
        <Center style={{ gap: 10 }}>
          <List size={18} />
          <span>List</span>
        </Center>
      ),
      value: 'list',
    },
    {
      label: (
        <Center style={{ gap: 10 }}>
          <SquaresFour size={18} />
          <span>Grid</span>
        </Center>
      ),
      value: 'grid',
    },
  ]

  const [listRepresentation, setListRepresentation] = useState<string>(
    listRepresentationOptions[0].value,
  )

  useEffect(() => {
    const params = new URLSearchParams(searchParams)
    if (debouncedSearch) {
      params.set('search', debouncedSearch)
    } else {
      params.delete('search')
    }
    setSearchParams(params, { replace: true })
  }, [debouncedSearch])

  return (
    <Stack h={'100%'}>
      <LandingPageHeader />
      <Flex direction={'column'} gap={'xs'}>
        <Flex
          justify='space-between'
          align='stretch'
          gap='md'
          direction={{ base: 'column', sm: 'row' }}
        >
          <Box style={{ flex: 1 }}>
            <TextInput
              w='100%'
              placeholder='Search Thesis Topics...'
              leftSection={<MagnifyingGlass size={16} />}
              value={searchKey}
              onChange={(x) => setSearchKey(x.currentTarget.value)}
            />
          </Box>
        </Flex>

        <Flex justify='space-between' gap='md' direction={{ base: 'column', sm: 'row' }}>
          <SegmentedControl
            value={topicView}
            onChange={(newVal) => {
              setTopicView(newVal)
              const params = new URLSearchParams(searchParams)
              if (newVal === GLOBAL_CONFIG.topic_views_options.PUBLISHED) {
                params.set('view', newVal)
              } else {
                params.delete('view')
              }
              setSearchParams(params, { replace: true })
            }}
            data={Object.values(GLOBAL_CONFIG.topic_views_options)}
          />

          <SegmentedControl
            value={listRepresentation}
            onChange={setListRepresentation}
            data={listRepresentationOptions}
          />
        </Flex>
      </Flex>
      <TopicsProvider
        limit={10}
        researchSpecific={false}
        initialFilters={{ search: debouncedSearch }}
      >
        <Stack gap='xs' h={'100%'}>
          {topicView === GLOBAL_CONFIG.topic_views_options.OPEN ? (
            <TopicsTable
              columns={['title', 'types', 'advisor', 'researchGroup', 'actions']}
              noBorder
              extraColumns={{
                actions: {
                  accessor: 'actions',
                  title: 'Actions',
                  textAlign: 'center',
                  noWrap: true,
                  width: 120,
                  render: (topic) => (
                    <Group
                      preventGrowOverflow={false}
                      justify='center'
                      onClick={(e) => e.stopPropagation()}
                    >
                      {!topic.closedAt && (
                        <Button
                          component={Link}
                          to={`/submit-application/${topic.topicId}`}
                          size='xs'
                        >
                          Apply
                        </Button>
                      )}
                    </Group>
                  ),
                },
              }}
            />
          ) : (
            <PublishedTheses search={debouncedSearch} />
          )}
        </Stack>
      </TopicsProvider>
    </Stack>
  )
}

export default LandingPage
