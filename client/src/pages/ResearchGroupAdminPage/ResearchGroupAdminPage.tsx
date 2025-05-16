import React, { useEffect, useState } from 'react'
import { usePageTitle } from '../../hooks/theme'
import { Box, Button, Flex, Grid, Loader, Stack, TextInput, Title } from '@mantine/core'
import { MagnifyingGlass, Plus } from 'phosphor-react'
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

const ResearchGroupAdminPage = () => {
  usePageTitle('Theses Overview')

  const [searchKey, setSearchKey] = React.useState('')
  const [debouncedSearch] = useDebouncedValue(searchKey, 300)
  const [researchGroupsLoading, setResearchGroupsLoading] = useState(false)

  const [researchGroups, setResearchGroups] = useState<PaginationResponse<IResearchGroup>>()

  const [createResearchGroupModalOpened, setCreateResearchGroupModalOpened] = useState(false)

  useEffect(() => {
    setResearchGroupsLoading(true)
    return doRequest<PaginationResponse<IResearchGroup>>(
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
          //TODO: Refresh the list of research groups
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
      ) : (
        <Grid>
          {researchGroups?.content.map((group) => (
            <Grid.Col span={{ base: 12, sm: 6, lg: 4 }} key={group.id} style={{ display: 'flex' }}>
              <ResearchGroupCard {...group} />
            </Grid.Col>
          ))}
        </Grid>
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
