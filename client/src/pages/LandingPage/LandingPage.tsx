import TopicsProvider from '../../providers/TopicsProvider/TopicsProvider'
import TopicsTable from '../../components/TopicsTable/TopicsTable'
import { Button, Group, Stack, Center } from '@mantine/core'
import { Link, useParams, useSearchParams } from 'react-router'
import PublishedTheses from './components/PublishedTheses/PublishedTheses'
import { usePageTitle } from '../../hooks/theme'
import LandingPageHeader from './components/LandingPageHeader/LandingPageHeader'
import { ListIcon, SquaresFourIcon } from '@phosphor-icons/react'
import { useEffect, useState } from 'react'
import { useDebouncedValue } from '@mantine/hooks'
import { GLOBAL_CONFIG } from '../../config/global'
import TopicCardGrid from './components/TopicCardGrid/TopicCardGrid'
import { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import TopicSearchFilters from '../../components/TopicSearchFilters/TopicSearchFilters'

const LandingPage = () => {
  usePageTitle('Find a Thesis Topic')

  const { researchGroupAbbreviation } = useParams<{ researchGroupAbbreviation: string }>()

  const [searchParams, setSearchParams] = useSearchParams()
  const [searchKey, setSearchKey] = useState(searchParams.get('search') ?? '')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)

  const [topicView, setTopicView] = useState<string>(
    searchParams.get('view') ?? GLOBAL_CONFIG.topic_views_options.OPEN,
  )

  const [selectedThesisTypes, setSelectedThesisTypes] = useState<string[]>(
    searchParams.get('types')?.split(',') ?? [],
  )

  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const [researchGroupsLoaded, setResearchGroupsLoaded] = useState(false)
  const [selectedGroups, setSelectedGroups] = useState<string[]>(
    searchParams.get('groups')?.split(',') ?? [],
  )

  const pageItemLimit = 12

  const createResearchGroupFilter = () => {
    if (researchGroupAbbreviation) {
      const group = researchGroups.find((g) => g.abbreviation === researchGroupAbbreviation)
      if (group) {
        return [group.id]
      } else if (researchGroupsLoaded) {
        showSimpleError(
          `Research group ${researchGroupAbbreviation} not found - showing all topics`,
        )
      }
    }
    return selectedGroups
  }

  useEffect(() => {
    return doRequest<ILightResearchGroup[]>(
      '/v2/research-groups/light',
      {
        method: 'GET',
        requiresAuth: false,
        params: {
          search: '',
        },
      },
      (response) => {
        if (response.ok) {
          setResearchGroups(response.data)
          setResearchGroupsLoaded(true)
        } else {
          showSimpleError(getApiResponseErrorMessage(response))
        }
      },
    )
  }, [])

  const listRepresentationOptions = [
    {
      label: (
        <Center style={{ gap: 10 }}>
          <ListIcon size={18} />
          <span>List</span>
        </Center>
      ),
      value: 'list',
    },
    {
      label: (
        <Center style={{ gap: 10 }}>
          <SquaresFourIcon size={18} />
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
      <LandingPageHeader
        researchGroupId={
          researchGroups.find((group) => group.abbreviation === researchGroupAbbreviation)?.id
        }
      />

      <TopicSearchFilters
        searchKey={searchKey}
        setSearchKey={setSearchKey}
        researchGroups={researchGroups}
        selectedGroups={selectedGroups}
        setSelectedGroups={setSelectedGroups}
        selectedThesisTypes={selectedThesisTypes}
        setSelectedThesisTypes={setSelectedThesisTypes}
        searchParams={searchParams}
        setSearchParams={setSearchParams}
        topicView={topicView}
        setTopicView={setTopicView}
        listRepresentation={listRepresentation}
        setListRepresentation={setListRepresentation}
        listRepresentationOptions={listRepresentationOptions}
      />

      <TopicsProvider
        limit={pageItemLimit}
        researchSpecific={false}
        initialFilters={{
          researchGroupIds: createResearchGroupFilter(),
          search: debouncedSearch,
          types: selectedThesisTypes,
        }}
      >
        <Stack gap='xs' h={'100%'}>
          {topicView === GLOBAL_CONFIG.topic_views_options.OPEN ? (
            listRepresentation === 'list' ? (
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
              <TopicCardGrid></TopicCardGrid>
            )
          ) : (
            <PublishedTheses
              search={debouncedSearch}
              representationType={listRepresentation}
              filters={{
                researchGroupIds: createResearchGroupFilter(),
                types: selectedThesisTypes,
              }}
              limit={pageItemLimit}
            />
          )}
        </Stack>
      </TopicsProvider>
    </Stack>
  )
}

export default LandingPage
