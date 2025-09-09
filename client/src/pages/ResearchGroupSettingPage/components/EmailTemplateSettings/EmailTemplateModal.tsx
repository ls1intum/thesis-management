import { Button, Group, Modal, SegmentedControl, Stack, Text, TextInput } from '@mantine/core'
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
          <Stack>
            <TextInput
              label='Subject'
              placeholder='Enter subject of your email...'
              value={editingTemplate?.subject ?? ''}
              onChange={(event) => {
                setEditingTemplate &&
                  setEditingTemplate({
                    ...editingTemplate!,
                    subject: event.currentTarget.value,
                  })
              }}
            />
            <EmailTextEditor
              editingTemplate={editingTemplate}
              setEditingTemplate={setEditingTemplate}
            />
          </Stack>
        ) : (
          <div dangerouslySetInnerHTML={{ __html: editingTemplate?.bodyHtml ?? '' }} />
        )}
        <Group justify='space-between'>
          <Button
            variant='default'
            onClick={() => {
              setEditingTemplate && setEditingTemplate(defaultTemplate ?? null)
            }}
            disabled={!defaultTemplate || editingTemplate === defaultTemplate}
          >
            Reset to default
          </Button>
          {mode === 'edit' && (
            <Group>
              {!(
                researchGroupTemplate === editingTemplate || defaultTemplate === editingTemplate
              ) && (
                <Button
                  variant='default'
                  onClick={() =>
                    setEditingTemplate &&
                    setEditingTemplate(researchGroupTemplate ?? defaultTemplate ?? null)
                  }
                >
                  Discard changes
                </Button>
              )}
              <Button>Save changes</Button>
            </Group>
          )}
        </Group>
      </Stack>
    </Modal>
  )
}

export default EmailTemplateModal
