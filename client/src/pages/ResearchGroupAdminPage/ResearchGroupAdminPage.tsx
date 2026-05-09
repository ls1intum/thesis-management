import { useEffect, useState } from 'react'
import { usePageTitle } from '../../hooks/theme'
import {
  Box,
  Button,
  Flex,
  Loader,
  SimpleGrid,
  Stack,
  TextInput,
  Title,
  Text,
  Pagination,
} from '@mantine/core'
import { MagnifyingGlass, Plus, UsersThree } from '@phosphor-icons/react'
import { doRequest } from '../../requests/request'
import { PaginationResponse } from '../../requests/responses/pagination'
import { IResearchGroup } from '../../requests/responses/researchGroup'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import ResearchGroupCard from './components/ResearchGroupCard'
import CreateResearchGroupModal, {
  ResearchGroupFormValues,
} from './components/CreateResearchGroupModal'
import { useDebouncedValue } from '@mantine/hooks'
import { showNotification } from '@mantine/notifications'
import NoContentFoundCard from '../../components/NoContentFoundCard/NoContentFoundCard'

const ResearchGroupAdminPage = () => {
  usePageTitle('Theses Overview')

  const [searchKey, setSearchKey] = useState('')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)
  const [researchGroupsLoading, setResearchGroupsLoading] = useState(false)

  const [researchGroups, setResearchGroups] = useState<PaginationResponse<IResearchGroup>>()

  const [createResearchGroupModalOpened, setCreateResearchGroupModalOpened] = useState(false)

  const [page, setPage] = useState(0)
  const limit = 30

  const fetchResearchGroups = (searchPage: number) => {
    setResearchGroupsLoading(true)
    doRequest<PaginationResponse<IResearchGroup>>(
      '/v2/research-groups',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          page: searchPage,
          limit: limit,
          search: debouncedSearch.trim(),
        },
      },
      (res) => {
        if (res.ok) {
          setResearchGroups({
            ...res.data,
            content: res.data.content,
          })
          if (searchPage !== page) {
            setPage(searchPage)
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
          setResearchGroups({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: -1,
          })
        }
        setResearchGroupsLoading(false)
      },
    )
  }

  useEffect(() => {
    fetchResearchGroups(0)
  }, [debouncedSearch])

  const handleCreateResearchGroup = async (values: ResearchGroupFormValues) => {
    const body = {
      headUsername: values.headUsername,
      name: values.name,
      abbreviation: values.abbreviation,
      campus: values.campus,
      description: values.description,
      websiteUrl: values.websiteUrl,
    }

    doRequest(
      '/v2/research-groups',
      {
        method: 'POST',
        requiresAuth: true,
        data: body,
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: 'Research group created.',
            color: 'green',
          })
          setCreateResearchGroupModalOpened(false)
          fetchResearchGroups(page)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <Stack h={'100%'}>
      <Title>Research Groups</Title>
      <Flex
        justify='space-between'
        align='stretch'
        gap='md'
        direction={{ base: 'column', sm: 'row' }}
      >
        <Box style={{ flex: 1 }}>
          <TextInput
            w='100%'
            placeholder='Search Research Groups...'
            leftSection={<MagnifyingGlass size={16} />}
            value={searchKey}
            onChange={(x) => setSearchKey(x.target.value || '')}
          />
        </Box>
        <Button
          w={{ base: '100%', sm: 'auto' }}
          leftSection={<Plus />}
          onClick={() => setCreateResearchGroupModalOpened(true)}
        >
          Create Research Group
        </Button>
      </Flex>
      <Box flex={1}>
        {researchGroupsLoading ? (
          <Flex justify='center' align='center'>
            <Loader color='blue' />
          </Flex>
        ) : researchGroups && (researchGroups.content ?? []).length === 0 ? (
          <NoContentFoundCard
            title={searchKey.length === 0 ? 'No Research Groups Found' : 'No Research Groups Found'}
            subtle={
              searchKey.length === 0
                ? 'There are no research groups yet. Create one to get started.'
                : 'Try changing the search term or create a new research group.'
            }
            icon={searchKey.length === 0 ? <UsersThree size={32} /> : <MagnifyingGlass size={32} />}
          />
        ) : (
          <SimpleGrid
            cols={{ base: 1, sm: 2, xl: 3 }}
            spacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
            verticalSpacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
          >
            {(researchGroups?.content ?? []).map((group) => (
              <ResearchGroupCard {...group} key={group.id} />
            ))}
          </SimpleGrid>
        )}
      </Box>

      <Flex justify={'space-between'} align={'center'} gap='md'>
        <Text size='sm'>
          {researchGroups && researchGroups.totalElements > 0 ? (
            <>
              {page * limit + 1}–{Math.min((page + 1) * limit, researchGroups.totalElements)} /{' '}
              {researchGroups.totalElements}
            </>
          ) : (
            '0 results'
          )}
        </Text>

        <Pagination
          value={page + 1}
          onChange={(p) => {
            setPage(p - 1)
            fetchResearchGroups(p - 1)
          }}
          total={researchGroups ? researchGroups.totalPages : 1}
          size='sm'
        />
      </Flex>

      <CreateResearchGroupModal
        opened={createResearchGroupModalOpened}
        onClose={() => setCreateResearchGroupModalOpened(false)}
        onSubmit={handleCreateResearchGroup}
      ></CreateResearchGroupModal>
    </Stack>
  )
}

export default ResearchGroupAdminPage
