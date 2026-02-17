import { Button, Divider, Flex, Group, Paper, Stack, Text, TextInput, Title } from '@mantine/core'
import { ArrowLeftIcon } from '@phosphor-icons/react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useUser } from '../../../../hooks/authentication'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { doRequest } from '../../../../requests/request'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import { PaginationResponse } from '../../../../requests/responses/pagination'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import EmailTextEditor from './EmailTextEditor/EmailTextEditor'
import DOMPurify from 'dompurify'

const EmailTemplateEditPage = () => {
  const navigate = useNavigate()
  const user = useUser()
  const { researchGroupId, templateCase } = useParams<{
    researchGroupId: string
    templateCase: string
  }>()

  const [defaultTemplate, setDefaultTemplate] = useState<IEmailTemplate | null>(null)
  const [researchGroupTemplate, setResearchGroupTemplate] = useState<IEmailTemplate | null>(null)
  const [editingTemplate, setEditingTemplate] = useState<IEmailTemplate | null>(null)
  const [exampleText, setExampleText] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(true)
  const [saving, setSaving] = useState<boolean>(false)

  useEffect(() => {
    if (!templateCase) return

    setLoading(true)
    doRequest<PaginationResponse<IEmailTemplate>>(
      '/v2/email-templates',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          templateCases: templateCase,
          page: 0,
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          const currentDefault =
            res.data.content.find((template) => !template.researchGroup) ?? null
          const currentResearchGroup =
            res.data.content.find((template) => Boolean(template.researchGroup)) ?? null

          setDefaultTemplate(currentDefault)
          setResearchGroupTemplate(currentResearchGroup)
          setEditingTemplate(currentResearchGroup ?? currentDefault)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setLoading(false)
      },
    )
  }, [templateCase])

  const saveChanges = async () => {
    if (!editingTemplate) return

    setSaving(true)
    const url = editingTemplate.researchGroup
      ? `/v2/email-templates/${editingTemplate.id}`
      : `/v2/email-templates`
    const method = editingTemplate.researchGroup ? 'PUT' : 'POST'
    const currentResearchGroupId = editingTemplate.researchGroup
      ? editingTemplate.researchGroup.id
      : user?.researchGroupId

    await doRequest<IEmailTemplate>(
      url,
      {
        method,
        requiresAuth: true,
        data: {
          researchGroupId: currentResearchGroupId,
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
          setResearchGroupTemplate(res.data.researchGroup ? res.data : null)
          setEditingTemplate(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setSaving(false)
      },
    )
  }

  if (loading) {
    return <Text>Loading template...</Text>
  }

  if (!editingTemplate) {
    return <Text>Template not found.</Text>
  }

  return (
    <Stack>
      <Stack
        gap='0.5rem'
        styles={{
          root: {
            cursor: 'pointer',
          },
        }}
        onClick={() => navigate(`/research-groups/${researchGroupId}?setting=email-templates`)}
      >
        <Group align='center'>
          <ArrowLeftIcon size={32} weight='bold' />
          <Title>Edit Email Template</Title>
        </Group>
        <Title order={4} c='dimmed'>
          {editingTemplate.subject}
        </Title>
      </Stack>

      <Flex flex={1} w='100%' direction={{ base: 'column-reverse', md: 'row' }} gap='1rem'>
        <Stack flex={1}>
          <Title order={3}>Edit Template</Title>
          <TextInput
            label='Subject'
            placeholder='Enter subject of your email...'
            value={editingTemplate.subject ?? ''}
            onChange={(event) => {
              setEditingTemplate({
                ...editingTemplate,
                subject: event.currentTarget.value,
              })
            }}
          />
          <EmailTextEditor
            editingTemplate={editingTemplate}
            setEditingTemplate={setEditingTemplate}
            stickyOffset={50}
            setExampleText={setExampleText}
          />
        </Stack>

        <Divider orientation='vertical' />

        <Stack flex={1}>
          <Title order={3}>Preview Template</Title>
          <Text c='dimmed' size='sm' pt='1rem'>
            This preview uses example data. Real values are filled in when emails are sent.
          </Text>
          <Paper withBorder radius='sm' flex={1}>
            <div
              style={{ padding: '1rem' }}
              dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(exampleText) }}
            />
          </Paper>
        </Stack>
      </Flex>

      <Group justify='space-between'>
        <Button
          variant='default'
          onClick={() => {
            setEditingTemplate(defaultTemplate)
          }}
          disabled={!defaultTemplate || editingTemplate.id === defaultTemplate.id}
        >
          Reset to default
        </Button>
        <Group>
          <Button
            variant='default'
            onClick={() => setEditingTemplate(researchGroupTemplate ?? defaultTemplate)}
            disabled={!researchGroupTemplate && !defaultTemplate}
          >
            Discard changes
          </Button>
          <Button onClick={saveChanges} loading={saving}>
            Save changes
          </Button>
        </Group>
      </Group>
    </Stack>
  )
}

export default EmailTemplateEditPage
