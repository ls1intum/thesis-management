import { Box, Button, Flex, Loader, TextInput } from '@mantine/core'
import { IResearchGroup } from '../../../requests/responses/researchGroup'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { MagnifyingGlass, Plus } from 'phosphor-react'
import AddResearchGroupMemberModal from './AddResearchGroupMemberModal'
import { useEffect, useState } from 'react'
import { ILightUser } from '../../../requests/responses/user'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useParams } from 'react-router'
import { showNotification } from '@mantine/notifications'

interface IAddResearchGroupMemberProps {
  researchGroupData: IResearchGroup | undefined
}

const AddResearchGroupMember = ({ researchGroupData }: IAddResearchGroupMemberProps) => {
  const [addResearchGroupMemberModalOpened, setAddResearchGroupMemberModalOpened] = useState(false)
  const [searchKey, setSearchKey] = useState('')

  const [members, setMembers] = useState<ILightUser[]>([])
  const [membersLoading, setMembersLoading] = useState(false)

  const fetchMembers = () => {
    if (!researchGroupData?.id) return

    setMembersLoading(true)

    doRequest<{ content: ILightUser[] }>(
      `/v2/research-groups/${researchGroupData.id}/members`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setMembers(res.data.content)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setMembersLoading(false)
      },
    )
  }

  useEffect(() => {
    fetchMembers()
  }, [researchGroupData])

  const handleAddMember = async (username: string) => {
    if (!researchGroupData?.id) return

    doRequest<ILightUser>(
      `/v2/research-groups/${researchGroupData.id}/assign/${username}`,
      {
        method: 'PUT',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: `${username} was successfully added to the group.`,
            color: 'green',
          })
          setAddResearchGroupMemberModalOpened(false)
          members.push(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard
      title='Group Members'
      subtle='Manage the members of your research group.'
    >
      <Flex
        justify='space-between'
        align='stretch'
        gap='md'
        direction={{ base: 'column', sm: 'row' }}
      >
        <Box style={{ flex: 1 }}>
          <TextInput
            w='100%'
            placeholder='Search Research Group Member...'
            leftSection={<MagnifyingGlass size={16} />}
            value={searchKey}
            onChange={(x) => setSearchKey(x.target.value || '')}
          />
        </Box>
        <Button
          w={{ base: '100%', sm: 'auto' }}
          leftSection={<Plus />}
          onClick={() => setAddResearchGroupMemberModalOpened(true)}
        >
          Add Member
        </Button>
      </Flex>

      <Flex
        justify='center'
        align='center'
        style={{ height: '100%', width: '100%' }}
        direction='column'
      >
        {membersLoading ? (
          <Loader />
        ) : (
          members.map((member) => (
            <div key={member.firstName}>{`${member.lastName} ${member.groups}`}</div>
          ))
        )}
      </Flex>

      <AddResearchGroupMemberModal
        opened={addResearchGroupMemberModalOpened}
        onClose={() => setAddResearchGroupMemberModalOpened(false)}
        researchGroupName={researchGroupData?.name ?? ''}
        handleAddMember={handleAddMember}
      ></AddResearchGroupMemberModal>
    </ResearchGroupSettingsCard>
  )
}

export default AddResearchGroupMember
