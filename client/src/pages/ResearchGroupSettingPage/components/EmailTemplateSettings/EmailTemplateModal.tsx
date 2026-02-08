import {
  Button,
  Flex,
  Group,
  Modal,
  Paper,
  Stack,
  TextInput,
  Title,
  Text,
  Divider,
  Box,
} from '@mantine/core'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import EmailTextEditor from './EmailTextEditor/EmailTextEditor'

interface IEmailTemplateModalProps {
  opened: boolean
  onClose: () => void
  defaultTemplate?: IEmailTemplate
  researchGroupTemplate?: IEmailTemplate
  editingTemplate?: IEmailTemplate | null
  setEditingTemplate?: (template: IEmailTemplate | null) => void
}

const EmailTemplateModal = ({
  opened,
  onClose,
  defaultTemplate,
  researchGroupTemplate,
  editingTemplate,
  setEditingTemplate,
}: IEmailTemplateModalProps) => {
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={
        <Stack gap={'0.25rem'}>
          <Title>Edit Email Template</Title>
          <Title c='dimmed' order={4}>
            {defaultTemplate?.subject || researchGroupTemplate?.subject || ''}
          </Title>
        </Stack>
      }
      centered
      fullScreen
    >
      <Stack pt={'md'}>
        <Flex flex={1} w={'100%'} direction={{ base: 'column-reverse', md: 'row' }} gap={'1rem'}>
          <Stack flex={1}>
            <Title order={2}>Edit Template</Title>
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
              stickyOffset={50}
            />
          </Stack>

          <Divider orientation='vertical' />

          <Stack flex={1}>
            <Title order={2}>Preview Template</Title>
            <Text c={'dimmed'} size='sm'>
              This Preview uses example data and contact. The system will fill in the data when
              sending out the emails.
            </Text>
            <Paper withBorder radius='sm' flex={1}>
              <Box p={'1rem'}>
                <div dangerouslySetInnerHTML={{ __html: editingTemplate?.bodyHtml ?? '' }} />
              </Box>
            </Paper>
          </Stack>
        </Flex>
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
        </Group>
      </Stack>
    </Modal>
  )
}

export default EmailTemplateModal
