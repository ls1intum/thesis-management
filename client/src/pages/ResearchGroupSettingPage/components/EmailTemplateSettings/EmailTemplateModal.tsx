import { Modal, Stack, Text } from '@mantine/core'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'

interface IEmailTemplateModalProps {
  opened: boolean
  onClose: () => void
  mode: 'preview' | 'edit'
  defaultTemplate?: IEmailTemplate
  researchGroupTemplate?: IEmailTemplate
  editingTemplate?: IEmailTemplate | null
  setEditingTemplate?: (template: IEmailTemplate | null) => void
}

const EmailTemplateModal = ({
  opened,
  onClose,
  mode,
  defaultTemplate,
  researchGroupTemplate,
  editingTemplate,
  setEditingTemplate,
}: IEmailTemplateModalProps) => {
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={mode === 'edit' ? 'Edit Email Template' : 'Preview Email Template'}
      centered
      size='xl'
    >
      <Stack>
        <Text size='sm' c='dimmed'>
          Customize the content of your email template.
        </Text>
      </Stack>
    </Modal>
  )
}

export default EmailTemplateModal
