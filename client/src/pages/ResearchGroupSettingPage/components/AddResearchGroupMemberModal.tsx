import { Modal, Text } from '@mantine/core'
import KeycloakUserAutocomplete from '../../../components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { useState } from 'react'

interface IAddResearchGroupMemberModalProps {
  opened: boolean
  onClose: () => void
  researchGroupName: string
}

const AddResearchGroupMemberModal = ({
  opened,
  onClose,
  researchGroupName,
}: IAddResearchGroupMemberModalProps) => {
  const [userDisplayLable, setUserDisplayLabel] = useState('')

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={`Add member to ${researchGroupName}`}
      centered
      size='xl'
    >
      <Text>Search by username, full name, or email</Text>

      <KeycloakUserAutocomplete
        selectedLabel={userDisplayLable}
        onSelect={(username, label) => {
          setUserDisplayLabel(label)
        }}
      ></KeycloakUserAutocomplete>
    </Modal>
  )
}

export default AddResearchGroupMemberModal
