import {
  Box,
  Button,
  Flex,
  Group,
  Loader,
  Menu,
  Select,
  Table,
  TextInput,
  UnstyledButton,
  Text,
} from '@mantine/core'
import { IResearchGroup } from '../../../requests/responses/researchGroup'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import {
  DotsThreeVertical,
  MagnifyingGlass,
  Plus,
  Trash,
  UserMinus,
  UserPlus,
} from '@phosphor-icons/react'
import AddResearchGroupMemberModal from './AddResearchGroupMemberModal'
import { useEffect, useState } from 'react'
import { ILightUser } from '../../../requests/responses/user'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { showNotification } from '@mantine/notifications'
import UserInformationRow from '../../../components/UserInformationRow/UserInformationRow'
import DeleteMemberModal from './DeleteMemberModal'

interface IResearchGroupMembersProps {
  researchGroupData: IResearchGroup | undefined
}

const ResearchGroupMembers = ({ researchGroupData }: IResearchGroupMembersProps) => {
  const [addResearchGroupMemberModalOpened, setAddResearchGroupMemberModalOpened] = useState(false)
  const [searchKey, setSearchKey] = useState('')

  const [members, setMembers] = useState<ILightUser[]>([])
  const [membersLoading, setMembersLoading] = useState(false)

  const getPrimaryRoleFromGroups = (groups: string[]): '' | 'advisor' | 'supervisor' => {
    if (groups.includes('supervisor')) return 'supervisor'
    if (groups.includes('advisor')) return 'advisor'
    return ''
  }

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

  const handleRemoveMember = async (userId: string) => {
    if (!researchGroupData?.id) return

    doRequest<ILightUser>(
      `/v2/research-groups/${researchGroupData.id}/remove/${userId}`,
      {
        method: 'PUT',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: `User was successfully removed from the group.`,
            color: 'green',
          })

          setMembers((prev) => prev.filter((m) => m.userId !== userId))
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const updateMemberRole = async (userId: string, newRole: string) => {
    if (!researchGroupData?.id) return

    doRequest<ILightUser>(
      `/v2/research-groups/${researchGroupData.id}/member/${userId}/role`,
      {
        method: 'PUT',
        requiresAuth: true,
        params: {
          role: newRole,
        },
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: `Role updated to ${newRole}.`,
            color: 'green',
          })

          setMembers((prev) =>
            prev.map((member) => {
              if (member.userId === userId) {
                return { ...member, groups: res.data.groups }
              }
              return member
            }),
          )
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const updateGroupAdminForUser = async (userId: string, userName: string) => {
    if (!researchGroupData?.id) return

    doRequest<ILightUser>(
      `/v2/research-groups/${researchGroupData.id}/member/${userId}/group-admin`,
      {
        method: 'PUT',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: res.data.groups.includes('group-admin')
              ? `${userName} is now a group admin.`
              : `${userName} is no longer a group admin.`,
            color: 'green',
          })

          setMembers((prev) =>
            prev.map((member) => {
              if (member.userId === userId) {
                return { ...member, groups: res.data.groups }
              }
              return member
            }),
          )
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const [deleteMemberModalOpened, setDeleteMemberModalOpened] = useState(false)
  const [memberForDelete, setMemberForDelete] = useState<ILightUser>({} as ILightUser)

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
            {members
              .slice()
              .sort((a, b) => {
                // Sort head to first position
                const headId = researchGroupData?.head?.userId
                const isAHead = a.userId === headId
                const isBHead = b.userId === headId

                if (isAHead && !isBHead) return -1
                if (!isAHead && isBHead) return 1
                return 0
              })
              .filter((member) => {
                const key = searchKey.toLowerCase().replaceAll(' ', '')

                const searchTarget =
                  `${member.firstName ?? ''}${member.lastName ?? ''}${member.universityId ?? ''}${member.email ?? ''}${member.lastName ?? ''}${member.firstName ?? ''}`
                    .toLowerCase()
                    .replaceAll(' ', '')

                return searchTarget.includes(key)
              })
              .map((member) => (
                <Table.Tr key={member.userId}>
                  <Table.Td style={{ width: '100%' }}>
                    <UserInformationRow
                      firstName={member.firstName ?? ''}
                      lastName={member.lastName ?? ''}
                      username={member.universityId ?? ''}
                      email={member.email ?? ''}
                      user={member}
                      researchGroupAdmin={member.groups.includes('group-admin')}
                    />
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>
                    <Box style={{ minWidth: 140, maxWidth: 200 }}>
                      <Select
                        data={[
                          { value: 'advisor', label: 'Supervisor' },
                          { value: 'supervisor', label: 'Examiner' },
                        ]}
                        value={getPrimaryRoleFromGroups(member.groups)}
                        onChange={(val) => {
                          updateMemberRole(member.userId, val ?? '')
                        }}
                        placeholder='Select role'
                        size='xs'
                      />
                    </Box>
                  </Table.Td>
                  <Table.Td
                    style={{
                      width: '1%',
                      whiteSpace: 'nowrap',
                      textAlign: 'right',
                    }}
                  >
                    <Menu
                      shadow='md'
                      width={200}
                      position='bottom-end'
                      withArrow
                      transitionProps={{ transition: 'scale-y', duration: 200 }}
                    >
                      <Menu.Target>
                        <UnstyledButton
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                          }}
                        >
                          <DotsThreeVertical size={24} />
                        </UnstyledButton>
                      </Menu.Target>

                      <Menu.Dropdown>
                        <Menu.Item disabled={member.userId === researchGroupData?.head.userId}>
                          <Group
                            justify='flex-start'
                            align='center'
                            gap='xs'
                            onClick={() => {
                              updateGroupAdminForUser(
                                member.userId,
                                `${member.firstName ?? ''} ${member.lastName ?? ''}`,
                              )
                            }}
                          >
                            {member.groups.includes('group-admin') ? (
                              <>
                                <UserMinus size={16} />
                                <Text size='xs'>Remove Group Admin</Text>
                              </>
                            ) : (
                              <>
                                <UserPlus size={16} />
                                <Text size='xs'>Make Group Admin</Text>
                              </>
                            )}
                          </Group>
                        </Menu.Item>
                        <Menu.Item disabled={member.userId === researchGroupData?.head.userId}>
                          <Group
                            justify='flex-start'
                            align='center'
                            gap='xs'
                            onClick={() => {
                              setDeleteMemberModalOpened(true)
                              setMemberForDelete(member)
                            }}
                          >
                            <Trash size={16} color='red' />
                            <Text size='xs'>Remove from Group</Text>
                          </Group>
                        </Menu.Item>
                      </Menu.Dropdown>
                    </Menu>
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
      <DeleteMemberModal
        onConfirm={() => {
          handleRemoveMember(memberForDelete.userId)
          setMemberForDelete({} as ILightUser)
        }}
        member={memberForDelete}
        disabled={memberForDelete.userId === researchGroupData?.head.userId}
        deleteMemberModalOpened={deleteMemberModalOpened}
        setDeleteMemberModalOpened={setDeleteMemberModalOpened}
      />
    </ResearchGroupSettingsCard>
  )
}

export default ResearchGroupMembers
