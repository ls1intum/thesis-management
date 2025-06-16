import TopicsProvider from '../../providers/TopicsProvider/TopicsProvider'
import TopicsTable from '../../components/TopicsTable/TopicsTable'
import { Box, Button, Flex, Group, Stack, TextInput, Title } from '@mantine/core'
import { Link, useSearchParams } from 'react-router'
import PublishedTheses from './components/PublishedTheses/PublishedTheses'
import { usePageTitle } from '../../hooks/theme'
import LandingPageHeader from './components/LandingPageHeader/LandingPageHeader'
import { MagnifyingGlass } from 'phosphor-react'
import { useEffect, useState } from 'react'
import { useDebouncedValue } from '@mantine/hooks'

const LandingPage = () => {
  usePageTitle('Find a Thesis Topic')

  const [searchParams, setSearchParams] = useSearchParams()
  const [searchKey, setSearchKey] = useState(searchParams.get('search') ?? '')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)

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
    <Stack>
      <LandingPageHeader />
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
      <TopicsProvider
        limit={10}
        researchSpecific={false}
        initialFilters={{ search: debouncedSearch }}
      >
        <Stack gap='xs'>
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
        </Stack>
      </TopicsProvider>
      <PublishedTheses />
    </Stack>
  )
}

export default LandingPage
