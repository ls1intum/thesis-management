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
  Center,
  Text,
  Checkbox,
  Drawer,
} from '@mantine/core'
import { Link, useParams, useSearchParams } from 'react-router'
import PublishedTheses from './components/PublishedTheses/PublishedTheses'
import { usePageTitle } from '../../hooks/theme'
import LandingPageHeader from './components/LandingPageHeader/LandingPageHeader'
import { FadersHorizontal, List, MagnifyingGlass, SquaresFour } from 'phosphor-react'
import { useEffect, useState } from 'react'
import { useDebouncedValue, useMediaQuery } from '@mantine/hooks'
import { GLOBAL_CONFIG } from '../../config/global'
import TopicCardGrid from './components/TopicCardGrid/TopicCardGrid'
import DropDownMultiSelect from '../../components/DropDownMultiSelect/DropDownMultiSelect'
import ThesisTypeBadge from './components/ThesisTypBadge/ThesisTypBadge'
import { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'

const LandingPage = () => {
  usePageTitle('Find a Thesis Topic')

  const { researchGroupId } = useParams<{ researchGroupId: string }>()

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
  const [selectedGroups, setSelectedGroups] = useState<string[]>(
    searchParams.get('groups')?.split(',') ?? [],
  )

  let pageItemLimit = 12

  useEffect(() => {
    const params = new URLSearchParams(searchParams)

    if (selectedThesisTypes.length > 0) {
      params.set('types', selectedThesisTypes.join(','))
    } else {
      params.delete('types')
    }

    if (selectedGroups.length > 0) {
      params.set('groups', selectedGroups.join(','))
    } else {
      params.delete('groups')
    }

    setSearchParams(params, { replace: true })
  }, [selectedThesisTypes, selectedGroups])

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

  const isMobile = useMediaQuery('(max-width: 768px)')
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false)

  const segmentedControls = () => (
    <>
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
    </>
  )

  const multiSelectDropdowns = () => (
    <>
      {!researchGroupId && (
        <DropDownMultiSelect
          data={researchGroups.map((group) => group.id)}
          searchPlaceholder='Search Research Groups'
          dropdownLable='Research Groups'
          selectedItems={selectedGroups}
          setSelectedItem={(groupId: string) => {
            setSelectedGroups((prev) =>
              prev.includes(groupId) ? prev.filter((id) => id !== groupId) : [...prev, groupId],
            )
          }}
          renderOption={(groupId) => {
            const group = researchGroups.find((g) => g.id === groupId)
            return group ? (
              <Flex align='center' gap='xs'>
                <Checkbox checked={selectedGroups.includes(group.id)} readOnly />
                <Text size='sm'>{group.name}</Text>{' '}
              </Flex>
            ) : null
          }}
          withoutDropdown={isMobile}
        ></DropDownMultiSelect>
      )}
      <DropDownMultiSelect
        data={Object.keys(GLOBAL_CONFIG.thesis_types)}
        searchPlaceholder='Search Thesis Type'
        dropdownLable='Thesis Types'
        renderOption={(type) => (
          <Group gap='xs'>
            <Checkbox checked={selectedThesisTypes.includes(type)} readOnly />
            <ThesisTypeBadge type={type} />
          </Group>
        )}
        selectedItems={selectedThesisTypes}
        setSelectedItem={(type) => {
          setSelectedThesisTypes((prev) => {
            if (prev.includes(type)) {
              return prev.filter((t) => t !== type)
            } else {
              return [...prev, type]
            }
          })
        }}
        withSearch={false}
        withoutDropdown={isMobile}
      ></DropDownMultiSelect>
    </>
  )

  return (
    <Stack h={'100%'}>
      <LandingPageHeader />
      <Flex direction={'column'} gap={'xs'}>
        <Flex justify='space-between' align='stretch' gap={5} direction='row'>
          <Box flex={1}>
            <TextInput
              w='100%'
              placeholder='Search Thesis Topics...'
              leftSection={<MagnifyingGlass size={16} />}
              value={searchKey}
              onChange={(x) => setSearchKey(x.currentTarget.value)}
            />
          </Box>
          {isMobile ? (
            <>
              <Button
                variant='default'
                onClick={() => setFilterDrawerOpen(true)}
                style={{ flexShrink: 0 }}
                leftSection={<FadersHorizontal size={16} />}
              >
                Filter
              </Button>
              <Drawer
                opened={filterDrawerOpen}
                onClose={() => setFilterDrawerOpen(false)}
                title='Filter Topics'
                position={'right'}
                size='xs'
              >
                <Stack gap='xs'>
                  {segmentedControls()}
                  {multiSelectDropdowns()}
                </Stack>
              </Drawer>
            </>
          ) : (
            <Group gap={5}>{multiSelectDropdowns()}</Group>
          )}
        </Flex>

        {!isMobile && (
          <Flex
            justify='space-between'
            gap={{ base: 'xs', sm: 'xl' }}
            direction={{ base: 'column', sm: 'row' }}
          >
            {segmentedControls()}
          </Flex>
        )}
      </Flex>
      <TopicsProvider
        limit={pageItemLimit}
        researchSpecific={false}
        initialFilters={{
          researchGroupIds: researchGroupId ? [researchGroupId] : selectedGroups,
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
                researchGroupIds: researchGroupId ? [researchGroupId] : selectedGroups,
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
