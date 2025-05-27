import { Button, Modal, Select, Stack, Text, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import KeycloakUserAutocomplete from '../../../components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { GLOBAL_CONFIG } from '../../../config/global'
import ResearchGroupForm from '../../../components/ResearchGroupForm/ResearchGroupForm'

interface ICreateResearchGroupModalProps {
  opened: boolean
  onClose: () => void
  onSubmit: (values: ResearchGroupFormValues) => void
}

export interface ResearchGroupFormValues {
  name: string
  abbreviation: string
  campus: string
  description: string
  websiteUrl: string
  headUsername: string
}

const CreateResearchGroupModal = ({
  opened,
  onClose,
  onSubmit,
}: ICreateResearchGroupModalProps) => {
  return (
    <Modal opened={opened} onClose={onClose} title='Create Research Group' centered>
      <ResearchGroupForm
        onSubmit={(values) => onSubmit(values)}
        submitLabel='Create Research Group'
      />
    </Modal>
  )
}

export default CreateResearchGroupModal
