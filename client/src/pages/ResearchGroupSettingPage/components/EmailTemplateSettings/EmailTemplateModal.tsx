import { Modal, SegmentedControl, Stack, Text } from '@mantine/core'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import EmailTextEditor from './EmailTextEditor/EmailTextEditor'

interface IEmailTemplateModalProps {
  opened: boolean
  onClose: () => void
  mode: 'preview' | 'edit'
  setMode?: (mode: 'preview' | 'edit') => void
  defaultTemplate?: IEmailTemplate
  researchGroupTemplate?: IEmailTemplate
  editingTemplate?: IEmailTemplate | null
  setEditingTemplate?: (template: IEmailTemplate | null) => void
}

const EmailTemplateModal = ({
  opened,
  onClose,
  mode,
  setMode,
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
        <SegmentedControl
          value={mode}
          onChange={(value) => {
            if (value === 'preview' || value === 'edit') {
              setMode && setMode(value)
            }
          }}
          data={[
            { value: 'preview', label: 'Preview' },
            { value: 'edit', label: 'Edit' },
          ]}
          size='xs'
        ></SegmentedControl>
        {mode === 'edit' ? (
          <EmailTextEditor
            editingTemplate={editingTemplate}
            setEditingTemplate={setEditingTemplate}
          />
        ) : (
          <div dangerouslySetInnerHTML={{ __html: editingTemplate?.bodyHtml ?? '' }} />
        )}
      </Stack>
    </Modal>
  )
}

export default EmailTemplateModal
