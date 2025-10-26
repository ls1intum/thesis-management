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
import { useTopicsContext } from '../../../../providers/TopicsProvider/hooks'
import React, { useEffect, useState } from 'react'
import TopicAccordionItem from '../../../../components/TopicAccordionItem/TopicAccordionItem'
import { GLOBAL_CONFIG } from '../../../../config/global'
import { DatabaseIcon } from '@phosphor-icons/react'
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
  const { onComplete } = props

  const { topics, page, setPage, limit, isLoading } = useTopicsContext()

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

  if (
    !GLOBAL_CONFIG.allow_suggested_topics &&
    topics?.content.length === 0 &&
    topics?.pageNumber === 0
  ) {
    return (
      <Center h='100%'>
        <Stack align='center' gap='xs'>
          <ThemeIcon radius='xl' size={50} color='gray' variant='light'>
            <DatabaseIcon size={24} weight='duotone' />
          </ThemeIcon>
          <Text size='sm' color='dimmed'>
            No topics found
          </Text>
        </Stack>
      </Center>
    )
  }

  return (
    <Stack>
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
        <TopicCardGrid collapsibleTopics={true} />
      </TopicsProvider>
    </Stack>
  )
}

export default SelectTopicStep
function setResearchGroups(data: ILightResearchGroup[]) {
  throw new Error('Function not implemented.')
}
