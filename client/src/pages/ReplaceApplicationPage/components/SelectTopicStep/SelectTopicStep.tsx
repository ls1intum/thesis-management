import { ITopic } from '../../../../requests/responses/topic'
import {
  Accordion,
  Button,
  Center,
  Flex,
  Pagination,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
} from '@mantine/core'
import React, { useEffect, useState } from 'react'
import { GLOBAL_CONFIG } from '../../../../config/global'
import { useSearchParams } from 'react-router'
import { useDebouncedValue } from '@mantine/hooks'
import TopicsProvider from '../../../../providers/TopicsProvider/TopicsProvider'
import TopicSearchFilters from '../../../../components/TopicSearchFilters/TopicSearchFilters'
import { doRequest } from '../../../../requests/request'
import { ILightResearchGroup } from '../../../../requests/responses/researchGroup'
import { showSimpleError } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import TopicCardGrid from '../../../LandingPage/components/TopicCardGrid/TopicCardGrid'

interface ISelectTopicStepProps {
  onComplete: (topic: ITopic | undefined) => unknown
}

const SelectTopicStep = (props: ISelectTopicStepProps) => {
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchKey, setSearchKey] = useState(searchParams.get('search') ?? '')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)

  const [selectedThesisTypes, setSelectedThesisTypes] = useState<string[]>(
    searchParams.get('types')?.split(',') ?? [],
  )

  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const [researchGroupsLoaded, setResearchGroupsLoaded] = useState(false)
  const [selectedGroups, setSelectedGroups] = useState<string[]>(
    searchParams.get('groups')?.split(',') ?? [],
  )

  const pageItemLimit = 10

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
    <Stack gap={'0rem'} pt={'1rem'}>
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
      />
      <TopicsProvider
        limit={pageItemLimit}
        researchSpecific={false}
        initialFilters={{
          researchGroupIds: selectedGroups,
          search: debouncedSearch,
          types: selectedThesisTypes,
        }}
      >
        <TopicCardGrid
          collapsibleTopics={true}
          showSuggestedTopic={GLOBAL_CONFIG.allow_suggested_topics}
          onApply={props.onComplete}
        />
      </TopicsProvider>
    </Stack>
  )
}

export default SelectTopicStep
function setResearchGroups(data: ILightResearchGroup[]) {
  throw new Error('Function not implemented.')
}
