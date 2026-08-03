import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Group, List, Stack, Tabs, Text, Textarea } from '@mantine/core'
import { CheckCircle, Info, Robot, Warning } from '@phosphor-icons/react'
import { useParams } from 'react-router'
import { ResearchGroupSettingsCard } from '@/core/group/pages/ResearchGroupSettingPage/components/ResearchGroupSettingsCard'
import { doRequest } from '@/core/requests/request'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import type { IResearchGroupGuidelines } from '@/core/group/requests/responses/researchGroupGuidelines'

/**
 * Lets a research group lead upload and manage the custom guidelines that unlock the AI review
 * features for the group. The raw text is preprocessed server-side into the fixed review
 * categories; until specific guidelines are stored, the group's members cannot use AI features.
 *
 * The card fetches its own state and hides itself entirely when the AI features are disabled
 * server-side (the endpoint 404s in that case).
 */
const AIReviewGuidelinesSettingsCard = () => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const [loading, setLoading] = useState(true)
  const [aiDisabled, setAiDisabled] = useState(false)
  const [guidelines, setGuidelines] = useState<IResearchGroupGuidelines | undefined>(undefined)
  const [draft, setDraft] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!researchGroupId) return

    setLoading(true)
    return doRequest<IResearchGroupGuidelines>(
      `/v2/ai-review/guidelines/${researchGroupId}`,
      { method: 'GET', requiresAuth: true },
      (res) => {
        setLoading(false)
        if (res.ok) {
          // The server returns an empty object ({}) when no guidelines are set yet.
          const configured = Boolean(res.data?.status)
          setGuidelines(configured ? res.data : undefined)
          setDraft(res.data?.rawGuidelines ?? '')
        } else if (res.status === 404) {
          // AI features are disabled server-side — hide the card entirely.
          setAiDisabled(true)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [researchGroupId])

  const save = () => {
    setSaving(true)
    doRequest<IResearchGroupGuidelines>(
      `/v2/ai-review/guidelines/${researchGroupId}`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: { rawGuidelines: draft.trim() },
      },
      (res) => {
        setSaving(false)
        if (res.ok) {
          setGuidelines(res.data)
          setDraft(res.data.rawGuidelines ?? draft)
          if (res.data.status === 'ready') {
            showSimpleSuccess(
              'Guidelines processed. AI review features are now enabled for your group.',
            )
          } else {
            showSimpleError(
              res.data.failureReason ??
                'The guidelines were too vague to process. Please make them more specific.',
            )
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  if (aiDisabled) {
    return null
  }

  const hasChanges = draft.trim() !== (guidelines?.rawGuidelines ?? '')
  const categoriesWithRules = (guidelines?.categories ?? []).filter(
    (category) => (category.rules ?? []).length > 0,
  )

  return (
    <ResearchGroupSettingsCard
      title='AI Review Guidelines'
      subtle='Upload your group’s writing guidelines, processed into the review categories. Must be specific — until set, members cannot use AI review.'
    >
      <Stack>
        {!loading && !guidelines && (
          <Alert
            icon={<Info size={16} />}
            color='blue'
            title='AI features are disabled for your group'
          >
            No guidelines have been uploaded yet. Add specific, actionable guidelines below to
            enable the AI review features for your members.
          </Alert>
        )}

        {guidelines?.status === 'ready' && (
          <Alert icon={<CheckCircle size={16} />} color='green' title='AI features are enabled'>
            Your guidelines are active. The AI reviewer applies them across all review categories.
          </Alert>
        )}

        {guidelines?.status === 'failed' && (
          <Alert icon={<Warning size={16} />} color='red' title='Guidelines could not be processed'>
            {guidelines.failureReason ??
              'The guidelines were too vague to build specific review rules. Please provide concrete, actionable guidance.'}
          </Alert>
        )}

        <Textarea
          label='Guidelines'
          description='Paste your research group’s thesis writing guidelines as free text (Markdown is fine).'
          placeholder='e.g. The bibliography must contain at least 6 peer-reviewed publications. Every figure must be referenced in the text…'
          autosize
          minRows={8}
          maxRows={24}
          value={draft}
          onChange={(event) => setDraft(event.currentTarget.value)}
          disabled={saving || loading}
        />

        <Group justify='space-between'>
          {guidelines?.status === 'ready' ? (
            <Badge color='green' variant='light'>
              Active
            </Badge>
          ) : (
            <Badge color='gray' variant='light'>
              Not active
            </Badge>
          )}
          <Button
            leftSection={<Robot size={16} />}
            loading={saving}
            disabled={loading || saving || !draft.trim() || !hasChanges}
            onClick={save}
          >
            Save & process
          </Button>
        </Group>

        {guidelines?.status === 'ready' && categoriesWithRules.length > 0 && (
          <Stack gap={5}>
            <Text size='sm' fw={500}>
              Processed rules by category
            </Text>
            {guidelines.overview && (
              <Text size='sm' c='dimmed'>
                {guidelines.overview}
              </Text>
            )}
            <Tabs defaultValue={categoriesWithRules[0].category} orientation='horizontal'>
              <Tabs.List>
                {categoriesWithRules.map((category) => (
                  <Tabs.Tab key={category.category} value={category.category}>
                    {category.displayName}
                  </Tabs.Tab>
                ))}
              </Tabs.List>
              {categoriesWithRules.map((category) => (
                <Tabs.Panel key={category.category} value={category.category} pt='sm'>
                  <List size='sm' spacing='xs'>
                    {(category.rules ?? []).map((rule) => (
                      <List.Item key={rule}>{rule}</List.Item>
                    ))}
                  </List>
                </Tabs.Panel>
              ))}
            </Tabs>
          </Stack>
        )}
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default AIReviewGuidelinesSettingsCard
