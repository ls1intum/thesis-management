import {
  ActionIcon,
  Box,
  Button,
  Card,
  Divider,
  Flex,
  Loader,
  Select,
  Stack,
  Table,
  TextInput,
  Tooltip,
} from '@mantine/core'
import { IResearchGroup } from '../../../requests/responses/researchGroup'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { MagnifyingGlass, Plus, Trash, User } from 'phosphor-react'
import AddResearchGroupMemberModal from './AddResearchGroupMemberModal'
import { useEffect, useState } from 'react'
import { ILightUser } from '../../../requests/responses/user'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useParams } from 'react-router'
import { showNotification } from '@mantine/notifications'
import UserInformationRow from '../../../components/UserInformationRow/UserInformationRow'
import DeleteButton from '../../../components/DeleteButton/DeleteButton'

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

      {membersLoading ? (
        <Loader />
      ) : (
        <Table verticalSpacing='sm'>
          <Table.Tbody>
            {members.map((member) => (
              <Table.Tr key={member.userId}>
                <Table.Td style={{ width: '100%' }}>
                  <UserInformationRow
                    firstName={member.firstName ?? ''}
                    lastName={member.lastName ?? ''}
                    username={member.universityId ?? ''}
                    email={member.email ?? ''}
                    user={member}
                  />
                </Table.Td>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>
                  <Box style={{ minWidth: 140, maxWidth: 200 }}>
                    <Select
                      data={[
                        { value: 'advisor', label: 'Advisor' },
                        { value: 'supervisor', label: 'Supervisor' },
                      ]}
                      value={''}
                      onChange={(val) => {}}
                      placeholder='Select role'
                      variant='filled'
                      size='xs'
                    />
                  </Box>
                </Table.Td>
                <Table.Td style={{ width: '1%', whiteSpace: 'nowrap', textAlign: 'right' }}>
                  <DeleteButton
                    onClick={() => {}}
                    disabled={member.userId === researchGroupData?.head.userId}
                  />
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}

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
