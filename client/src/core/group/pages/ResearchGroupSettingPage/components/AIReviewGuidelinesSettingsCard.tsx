import { useEffect, useState } from 'react'
import {
  ActionIcon,
  Alert,
  Anchor,
  Badge,
  Button,
  Group,
  Stack,
  Tabs,
  Text,
  Textarea,
} from '@mantine/core'
import { CheckCircle, FloppyDisk, Info, Plus, Robot, Trash, Warning } from '@phosphor-icons/react'
import { useParams } from 'react-router'
import { ResearchGroupSettingsCard } from '@/core/group/pages/ResearchGroupSettingPage/components/ResearchGroupSettingsCard'
import { doRequest } from '@/core/requests/request'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import type { IResearchGroupGuidelines } from '@/core/group/requests/responses/researchGroupGuidelines'

/** A single editable rule, carrying a stable id so React keeps input focus across edits/removals. */
interface IEditableRule {
  id: string
  text: string
}

/** Local, editable representation of the structured rules keyed by review category slug. */
interface IRuleDraft {
  overview: string
  rules: Record<string, IEditableRule[]>
}

/**
 * Guide for group leads: what the processing step expects, which review categories exist, and a
 * copy-paste example set of guidelines. Linked from the editor because a first-time lead has no
 * other way to know how specific the input has to be before it is accepted.
 */
const GUIDELINES_DOCS_URL = 'https://docs.thesis.aet.cit.tum.de/supervisors/ai-review-guidelines'

const newRule = (text: string): IEditableRule => ({ id: crypto.randomUUID(), text })

/** Builds the editable draft from the persisted guidelines, preserving the fixed category order. */
const buildRuleDraft = (guidelines: IResearchGroupGuidelines): IRuleDraft => ({
  overview: guidelines.overview ?? '',
  rules: Object.fromEntries(
    (guidelines.categories ?? []).map((category) => [
      category.category,
      (category.rules ?? []).map((rule) => newRule(rule)),
    ]),
  ),
})

/** Drops blank rules and empty categories, producing the shape the `/rules` endpoint expects. */
const normalizeRules = (rules: Record<string, IEditableRule[]>) =>
  Object.entries(rules)
    .map(([category, list]) => ({
      category,
      rules: list.map((rule) => rule.text.trim()).filter(Boolean),
    }))
    .filter((category) => category.rules.length > 0)

/**
 * Lets a research group lead upload and manage the custom guidelines that unlock the AI review
 * features for the group. The raw text is preprocessed server-side into the fixed review
 * categories; until specific guidelines are stored, the group's members cannot use AI features.
 *
 * Once processed, the lead can also refine the generated per-category rules by hand — tweaking
 * wording, adding a new convention, or removing a rule — without regenerating from the raw text.
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
  const [ruleDraft, setRuleDraft] = useState<IRuleDraft>({ overview: '', rules: {} })
  const [savingRules, setSavingRules] = useState(false)

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

  // Re-seed the editable rule draft whenever a new set of ready guidelines arrives (load or save).
  useEffect(() => {
    if (guidelines?.status === 'ready') {
      setRuleDraft(buildRuleDraft(guidelines))
    }
  }, [guidelines])

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

  const saveRules = () => {
    setSavingRules(true)
    doRequest<IResearchGroupGuidelines>(
      `/v2/ai-review/guidelines/${researchGroupId}/rules`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: { overview: ruleDraft.overview.trim(), categories: normalizeRules(ruleDraft.rules) },
      },
      (res) => {
        setSavingRules(false)
        if (res.ok) {
          setGuidelines(res.data)
          showSimpleSuccess('Guidelines updated.')
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const setRule = (category: string, id: string, value: string) =>
    setRuleDraft((current) => ({
      ...current,
      rules: {
        ...current.rules,
        [category]: (current.rules[category] ?? []).map((rule) =>
          rule.id === id ? { ...rule, text: value } : rule,
        ),
      },
    }))

  const addRule = (category: string) =>
    setRuleDraft((current) => ({
      ...current,
      rules: { ...current.rules, [category]: [...(current.rules[category] ?? []), newRule('')] },
    }))

  const removeRule = (category: string, id: string) =>
    setRuleDraft((current) => ({
      ...current,
      rules: {
        ...current.rules,
        [category]: (current.rules[category] ?? []).filter((rule) => rule.id !== id),
      },
    }))

  if (aiDisabled) {
    return null
  }

  const hasChanges = draft.trim() !== (guidelines?.rawGuidelines ?? '')
  const categories = guidelines?.categories ?? []
  const normalizedRules = normalizeRules(ruleDraft.rules)
  const rulesUsable = normalizedRules.length > 0
  const rulesDirty =
    guidelines?.status === 'ready' &&
    JSON.stringify({ o: ruleDraft.overview.trim(), r: normalizedRules }) !==
      JSON.stringify({
        o: (guidelines.overview ?? '').trim(),
        r: normalizeRules(buildRuleDraft(guidelines).rules),
      })

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

        <Text size='sm' c='dimmed'>
          Not sure how specific the guidelines have to be?{' '}
          <Anchor href={GUIDELINES_DOCS_URL} target='_blank' rel='noopener noreferrer'>
            Read the guide
          </Anchor>{' '}
          for the review categories, what makes a rule checkable, and an example you can copy.
        </Text>

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

        {guidelines?.status === 'ready' && categories.length > 0 && (
          <Stack gap='sm'>
            <div>
              <Text size='sm' fw={500}>
                Refine rules by category
              </Text>
              <Text size='xs' c='dimmed'>
                Fine-tune the generated rules — edit wording, add a new convention, or remove one —
                without regenerating. Re-running “Save &amp; process” above replaces these edits.
              </Text>
            </div>

            <Textarea
              label='Overview'
              description='Short, category-independent summary applied across all categories.'
              autosize
              minRows={2}
              maxRows={6}
              value={ruleDraft.overview}
              onChange={(event) =>
                setRuleDraft((current) => ({ ...current, overview: event.currentTarget.value }))
              }
              disabled={savingRules}
            />

            <Tabs defaultValue={categories[0]?.category} orientation='horizontal'>
              <Tabs.List>
                {categories.map((category) => {
                  const count = (ruleDraft.rules[category.category] ?? []).filter((rule) =>
                    rule.text.trim(),
                  ).length
                  return (
                    <Tabs.Tab
                      key={category.category}
                      value={category.category}
                      rightSection={
                        count > 0 ? (
                          <Badge size='xs' variant='light' circle>
                            {count}
                          </Badge>
                        ) : undefined
                      }
                    >
                      {category.displayName}
                    </Tabs.Tab>
                  )
                })}
              </Tabs.List>
              {categories.map((category) => {
                const rules = ruleDraft.rules[category.category] ?? []
                return (
                  <Tabs.Panel key={category.category} value={category.category} pt='sm'>
                    <Stack gap='xs'>
                      {rules.length === 0 && (
                        <Text size='sm' c='dimmed'>
                          No rules for this category yet. Add one to include it in the review.
                        </Text>
                      )}
                      {rules.map((rule) => (
                        <Group key={rule.id} align='flex-start' wrap='nowrap'>
                          <Textarea
                            autosize
                            minRows={1}
                            style={{ flex: 1 }}
                            value={rule.text}
                            onChange={(event) =>
                              setRule(category.category, rule.id, event.currentTarget.value)
                            }
                            disabled={savingRules}
                          />
                          <ActionIcon
                            variant='subtle'
                            color='red'
                            mt={4}
                            onClick={() => removeRule(category.category, rule.id)}
                            disabled={savingRules}
                            aria-label='Remove rule'
                          >
                            <Trash size={16} />
                          </ActionIcon>
                        </Group>
                      ))}
                      <Button
                        variant='light'
                        size='xs'
                        leftSection={<Plus size={14} />}
                        onClick={() => addRule(category.category)}
                        disabled={savingRules}
                        style={{ alignSelf: 'flex-start' }}
                      >
                        Add rule
                      </Button>
                    </Stack>
                  </Tabs.Panel>
                )
              })}
            </Tabs>

            <Group justify='flex-end'>
              <Button
                variant='default'
                onClick={() => setRuleDraft(buildRuleDraft(guidelines))}
                disabled={savingRules || !rulesDirty}
              >
                Reset
              </Button>
              <Button
                leftSection={<FloppyDisk size={16} />}
                loading={savingRules}
                disabled={savingRules || !rulesDirty || !rulesUsable}
                onClick={saveRules}
              >
                Save changes
              </Button>
            </Group>
          </Stack>
        )}
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default AIReviewGuidelinesSettingsCard
