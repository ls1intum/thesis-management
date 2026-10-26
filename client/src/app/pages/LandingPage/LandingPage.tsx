import TopicsProvider from '@/core/topic/providers/TopicsProvider/TopicsProvider'
import TopicsTable from '@/core/topic/components/TopicsTable/TopicsTable'
import { TopicState } from '@/core/topic/requests/responses/topic'
import { Alert, Button, Group, Stack, Center } from '@mantine/core'
import { Link, useParams, useSearchParams } from 'react-router'
import PublishedTheses from '@/app/pages/LandingPage/components/PublishedTheses/PublishedTheses'
import { usePageTitle } from '@/core/hooks/theme'
import LandingPageHeader from '@/app/pages/LandingPage/components/LandingPageHeader/LandingPageHeader'
import { InfoIcon, ListIcon, SquaresFourIcon } from '@phosphor-icons/react'
import { useEffect, useMemo, useState } from 'react'
import { useDebouncedValue } from '@mantine/hooks'
import { GLOBAL_CONFIG } from '@/core/config/global'
import TopicCardGrid from '@/app/pages/LandingPage/components/TopicCardGrid/TopicCardGrid'
import type { ILightResearchGroup } from '@/core/group/requests/responses/researchGroup'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import TopicSearchFilters from '@/core/topic/components/TopicSearchFilters/TopicSearchFilters'
import { TOPIC_DISCLAIMER_TEXT } from '@/core/topic/components/TopicDisclaimerAlert/TopicDisclaimerAlert'

const LandingPage = () => {
  usePageTitle('Find a Thesis Topic')

  const { researchGroupAbbreviation, supervisorName: supervisorNameParam } = useParams<{
    researchGroupAbbreviation: string
    supervisorName: string
  }>()

  const [searchParams, setSearchParams] = useSearchParams()
  const [searchKey, setSearchKey] = useState(searchParams.get('search') ?? '')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)

  const supervisorFilter = supervisorNameParam ?? searchParams.get('supervisor') ?? undefined

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

  const researchGroupFilter = useMemo(() => {
    if (researchGroupAbbreviation) {
      const group = researchGroups.find((g) => g.abbreviation === researchGroupAbbreviation)
      if (group) {
        return [group.id]
      }
    }
    return selectedGroups
  }, [researchGroupAbbreviation, researchGroups, selectedGroups])

  useEffect(() => {
    if (researchGroupAbbreviation && researchGroupsLoaded) {
      const group = researchGroups.find((g) => g.abbreviation === researchGroupAbbreviation)
      if (!group) {
        showSimpleError(
          `Research group ${researchGroupAbbreviation} not found - showing all topics`,
        )
      }
    }
  }, [researchGroupAbbreviation, researchGroupsLoaded, researchGroups])

  const initialFilters = useMemo(
    () => ({
      researchGroupIds: researchGroupFilter,
      search: debouncedSearch,
      types: selectedThesisTypes,
      supervisorName: supervisorFilter,
    }),
    [researchGroupFilter, debouncedSearch, selectedThesisTypes, supervisorFilter],
  )

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
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- searchParams/setSearchParams change on every navigation; only sync URL when the debounced search input changes
  }, [debouncedSearch])

  return (
    <Stack h={'100%'}>
      <LandingPageHeader
        researchGroupId={
          researchGroups.find((group) => group.abbreviation === researchGroupAbbreviation)?.id
        }
        supervisorName={supervisorFilter}
      />

      <Alert variant='light' color='blue' icon={<InfoIcon />} style={{ flexShrink: 0 }}>
        {TOPIC_DISCLAIMER_TEXT}
      </Alert>

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
        initialFilters={initialFilters}
      >
        <Stack gap='xs' h={'100%'}>
          {topicView === GLOBAL_CONFIG.topic_views_options.OPEN ? (
            listRepresentation === 'list' ? (
              <TopicsTable
                columns={['title', 'types', 'supervisor', 'researchGroup', 'actions']}
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
                        {topic.state === TopicState.OPEN && (
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
                researchGroupIds: researchGroupFilter,
                types: selectedThesisTypes,
                supervisorName: supervisorFilter,
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
