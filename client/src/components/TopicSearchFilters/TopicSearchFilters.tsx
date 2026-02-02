import {
  Box,
  Button,
  Checkbox,
  Drawer,
  Flex,
  Group,
  SegmentedControl,
  Stack,
  TextInput,
  Text,
} from '@mantine/core'
import { FadersHorizontalIcon, MagnifyingGlassIcon } from '@phosphor-icons/react'
import { JSX, useEffect, useState, Dispatch, SetStateAction } from 'react'
import { useParams } from 'react-router'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import DropDownMultiSelect from '../DropDownMultiSelect/DropDownMultiSelect'
import { GLOBAL_CONFIG } from '../../config/global'
import { ILightResearchGroup } from '../../requests/responses/researchGroup'
import ThesisTypeBadge from '../../pages/LandingPage/components/ThesisTypBadge/ThesisTypBadge'

interface ITopicSearchFiltersProps {
  searchKey: string
  setSearchKey: (key: string) => void
  researchGroups: ILightResearchGroup[]
  selectedGroups: string[]
  setSelectedGroups: Dispatch<SetStateAction<string[]>>
  selectedThesisTypes: string[]
  setSelectedThesisTypes: Dispatch<SetStateAction<string[]>>
  searchParams: URLSearchParams
  setSearchParams: (params: URLSearchParams, options?: { replace: boolean | undefined }) => void
  topicView?: string
  setTopicView?: (view: string) => void
  listRepresentation?: string
  setListRepresentation?: (representation: string) => void
  listRepresentationOptions?: {
    label: JSX.Element
    value: string
  }[]
}

const TopicSearchFilters = ({
  searchKey,
  setSearchKey,
  researchGroups,
  selectedGroups,
  setSelectedGroups,
  selectedThesisTypes,
  setSelectedThesisTypes,
  searchParams,
  setSearchParams,
  topicView,
  setTopicView,
  listRepresentation,
  setListRepresentation,
  listRepresentationOptions,
}: ITopicSearchFiltersProps) => {
  const isMobile = useIsSmallerBreakpoint('sm')

  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false)

  const { researchGroupAbbreviation } = useParams<{ researchGroupAbbreviation: string }>()

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

  const segmentedControls = () => (
    <>
      {topicView && setTopicView && (
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
      )}
      {listRepresentation && setListRepresentation && listRepresentationOptions && (
        <SegmentedControl
          value={listRepresentation}
          onChange={setListRepresentation}
          data={listRepresentationOptions}
        />
      )}
    </>
  )

  const multiSelectDropdowns = () => (
    <>
      {!researchGroupAbbreviation && (
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
          searchFunction={(search) =>
            researchGroups
              .filter((group) => group.name.toLowerCase().includes(search.toLowerCase()))
              .map((group) => group.id)
          }
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
    <Flex direction={'column'} gap={'xs'}>
      <Flex justify='space-between' align='stretch' gap={5} direction='row'>
        <Box flex={1}>
          <TextInput
            w='100%'
            placeholder='Search Thesis Topics...'
            leftSection={<MagnifyingGlassIcon size={16} />}
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
              leftSection={<FadersHorizontalIcon size={16} />}
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
  )
}

export default TopicSearchFilters
