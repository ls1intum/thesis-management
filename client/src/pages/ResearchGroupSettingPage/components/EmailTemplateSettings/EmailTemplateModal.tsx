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
import { useState } from 'react'
import { doRequest } from '../../../../requests/request'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { useUser } from '../../../../hooks/authentication'

interface IEmailTemplateModalProps {
  opened: boolean
  onClose: () => void
  defaultTemplate?: IEmailTemplate
  researchGroupTemplate?: IEmailTemplate
  editingTemplate?: IEmailTemplate | null
  setEditingTemplate?: (template: IEmailTemplate | null) => void
  updateTemplate?: (template: IEmailTemplate) => void
}

const EmailTemplateModal = ({
  opened,
  onClose,
  defaultTemplate,
  researchGroupTemplate,
  editingTemplate,
  setEditingTemplate,
  updateTemplate,
}: IEmailTemplateModalProps) => {
  const [exampleText, setExampleText] = useState<string>(editingTemplate?.bodyHtml || '')

  const user = useUser()

  const saveChanges = async () => {
    if (!editingTemplate) return

    const url = editingTemplate.researchGroup
      ? `/v2/email-templates/${editingTemplate.id}`
      : `/v2/email-templates`
    const method = editingTemplate.researchGroup ? 'PUT' : 'POST'
    const researchGroupId = editingTemplate.researchGroup
      ? editingTemplate.researchGroup.id
      : user?.researchGroupId

    return doRequest<IEmailTemplate>(
      url,
      {
        method: method,
        requiresAuth: true,
        data: {
          researchGroupId: researchGroupId,
          templateCase: editingTemplate.templateCase,
          description: editingTemplate.description,
          subject: editingTemplate.subject,
          bodyHtml: editingTemplate.bodyHtml,
          language: editingTemplate.language,
        },
      },
      (res) => {
        if (res.ok) {
          showSimpleSuccess('Email template updated successfully')
          if (updateTemplate) {
            updateTemplate(res.data)
          }
          onClose()
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

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
              stickyOffset={100}
              setExampleText={setExampleText}
            />
          </Stack>

          <Divider orientation='vertical' />

          <Stack flex={1}>
            <Title order={2}>Preview Template</Title>
            <Text c={'dimmed'} size='sm' pt={'1rem'}>
              This Preview uses example data and contact. The system will fill in the data when
              sending out the emails.
            </Text>
            <Paper withBorder radius='sm' flex={1}>
              <Box p={'1rem'}>
                <div dangerouslySetInnerHTML={{ __html: exampleText }} />
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
            <Button
              onClick={() => {
                saveChanges()
              }}
            >
              Save changes
            </Button>
          </Group>
        </Group>
      </Stack>
    </Modal>
  )
}

export default EmailTemplateModal
