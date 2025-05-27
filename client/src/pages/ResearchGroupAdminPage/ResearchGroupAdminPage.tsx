import { useEffect, useState } from 'react'
import { usePageTitle } from '../../hooks/theme'
import { Box, Button, Flex, Loader, SimpleGrid, Stack, TextInput, Title } from '@mantine/core'
import { MagnifyingGlass, Plus, UsersThree } from 'phosphor-react'
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

  const fetchResearchGroups = () => {
    setResearchGroupsLoading(true)
    doRequest<PaginationResponse<IResearchGroup>>(
      '/v2/research-groups',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          page: 0,
          limit: -1,
          search: debouncedSearch.trim(),
        },
      },
      (res) => {
        if (res.ok) {
          setResearchGroups({
            ...res.data,
            content: res.data.content,
          })
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
    fetchResearchGroups()
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
          fetchResearchGroups()
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <Stack>
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
      {researchGroupsLoading ? (
        <Flex justify='center' align='center'>
          <Loader color='blue' />
        </Flex>
      ) : researchGroups && researchGroups.content.length === 0 ? (
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
          {researchGroups?.content.map((group) => <ResearchGroupCard {...group} />)}
        </SimpleGrid>
      )}

      <CreateResearchGroupModal
        opened={createResearchGroupModalOpened}
        onClose={() => setCreateResearchGroupModalOpened(false)}
        onSubmit={handleCreateResearchGroup}
      ></CreateResearchGroupModal>
    </Stack>
  )
}

export default ResearchGroupAdminPage
