import { Button, Flex, Modal, Stack, Text } from '@mantine/core'
import KeycloakUserAutocomplete from '../../../components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { useState } from 'react'

interface IAddResearchGroupMemberModalProps {
  opened: boolean
  onClose: () => void
  researchGroupName: string
  handleAddMember: (username: string) => void
}

const AddResearchGroupMemberModal = ({
  opened,
  onClose,
  researchGroupName,
  handleAddMember,
}: IAddResearchGroupMemberModalProps) => {
  const [userDisplayLabel, setUserDisplayLabel] = useState('')
  const [selectedUsername, setSelectedUsername] = useState('')

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={<Text fw={500}>{`Add member to ${researchGroupName}`}</Text>}
      centered
      size='xl'
    >
      <Stack>
        <Text size='sm' c='dimmed'>
          Search by username, full name, or email
        </Text>

        <KeycloakUserAutocomplete
          selectedLabel={userDisplayLabel}
          onSelect={(username, label) => {
            setUserDisplayLabel(label)
            setSelectedUsername(username)
          }}
          placeholder='Find members...'
        ></KeycloakUserAutocomplete>

        <Flex justify='flex-end'>
          <Button
            ml='sm'
            onClick={() => handleAddMember(selectedUsername)}
            disabled={!selectedUsername}
          >
            Add Member
          </Button>
        </Flex>
      </Stack>
    </Modal>
  )
}

export default AddResearchGroupMemberModal
