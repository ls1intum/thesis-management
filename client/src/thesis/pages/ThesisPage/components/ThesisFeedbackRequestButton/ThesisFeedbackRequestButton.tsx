import { useEffect, useMemo, useState } from 'react'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Checkbox,
  Divider,
  Group,
  Modal,
  Select,
  Stack,
  Text,
  Textarea,
  Title,
  Tooltip,
} from '@mantine/core'
import { doRequest } from '@/core/requests/request'
import type { IThesis } from '@/thesis/requests/responses/thesis'
import type {
  ThesisFeedbackCategory,
  ThesisFeedbackSeverity,
} from '@/thesis/requests/responses/thesis'
import { ThesisFeedbackSource } from '@/thesis/requests/responses/thesis'
import { ApiError, getApiResponseErrorMessage } from '@/core/requests/handler'
import { ListBullets, MagicWand, Plus, Robot, Trash } from '@phosphor-icons/react'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import { GLOBAL_CONFIG } from '@/core/config/global'
import FeedbackCategoryCounts from '@/thesis/components/FeedbackCategoryCounts/FeedbackCategoryCounts'
import {
  ASSESSMENT_LABEL,
  countByCategory,
  FEEDBACK_CATEGORY_OPTIONS,
  FEEDBACK_SEVERITY_OPTIONS,
  SEVERITY_COLOR,
  SEVERITY_DESCRIPTION,
} from '@/thesis/utils/feedbackLabels'
import type { AIAssessment } from '@/thesis/utils/feedbackLabels'

interface IThesisFeedbackRequestButtonProps {
  type: string
}

interface INewEntry {
  key: string
  feedback: string
  category: ThesisFeedbackCategory | ''
  severity: ThesisFeedbackSeverity | ''
  // Provenance of this row. Manual rows are HUMAN; rows produced by "Generate with AI" are
  // AI_REVIEWED_BY_HUMAN (an AI draft the instructor reviews before saving). Persisted so the
  // feedback overview can badge AI-assisted entries correctly.
  source: ThesisFeedbackSource
}

interface IAIDraft {
  feedback: string
  category?: ThesisFeedbackCategory | null
  severity?: ThesisFeedbackSeverity | null
}

interface IFeedbackClassification {
  category?: ThesisFeedbackCategory | null
  severity?: ThesisFeedbackSeverity | null
}

interface IImportedNotes {
  entries?: IAIDraft[]
}

/** Why an entry came back unlabelled, or nothing when it was classified. */
type IClassifyOutcome = { classified: true } | { classified: false; reason: string }

interface IAIPreviewResponse {
  assessment?: AIAssessment
  score?: number | null
  summary?: string
  drafts?: IAIDraft[]
}

const NO_SUGGESTION_MESSAGE =
  'The AI could not classify this entry. Please select the values manually.'

const emptyEntry = (): INewEntry => ({
  key: crypto.randomUUID(),
  feedback: '',
  category: '',
  severity: '',
  source: ThesisFeedbackSource.HUMAN,
})

const ThesisFeedbackRequestButton = (props: IThesisFeedbackRequestButtonProps) => {
  const { type } = props

  const { thesis } = useLoadedThesisContext()

  const [opened, setOpened] = useState(false)
  const [entries, setEntries] = useState<INewEntry[]>([])
  const [editChanges, setEditChanges] = useState<
    Array<{
      feedbackId: string
      completed: boolean
    }>
  >([])
  const [aiAssessment, setAiAssessment] = useState<IAIPreviewResponse | null>(null)
  const [aiLoading, setAiLoading] = useState(false)
  // Keys of the entries currently being classified. A set rather than a single key so two rows
  // classified back to back each keep their own spinner until their own request returns.
  const [classifyingKeys, setClassifyingKeys] = useState<ReadonlySet<string>>(() => new Set())
  const [bulkClassifying, setBulkClassifying] = useState(false)
  const [showDisregardChanges, setShowDisregardChanges] = useState(false)
  // The offline-notes paste panel. It replaces the modal's content while open, the way the
  // unsaved-changes prompt does, rather than stacking a second modal on top of the first.
  const [showImportNotes, setShowImportNotes] = useState(false)
  const [notes, setNotes] = useState('')
  const [importing, setImporting] = useState(false)

  useEffect(() => {
    if (opened) {
      setEntries([emptyEntry()])
      setEditChanges([])
      setAiAssessment(null)
      setShowImportNotes(false)
      setNotes('')
    }
  }, [opened])

  const validEntries = useMemo(
    () => entries.filter((entry) => entry.feedback.trim().length > 0),
    [entries],
  )

  const categoryCounts = useMemo(() => countByCategory(aiAssessment?.drafts ?? []), [aiAssessment])

  // Entries worth spending a classification call on: written, but still missing at least one of
  // the two labels. Imported notes are the usual source of these.
  const unclassifiedEntries = useMemo(
    () =>
      entries.filter(
        (entry) => entry.feedback.trim().length > 0 && (!entry.category || !entry.severity),
      ),
    [entries],
  )

  const hasUnsavedWork =
    validEntries.length > 0 || editChanges.length > 0 || notes.trim().length > 0

  const updateEntry = (key: string, patch: Partial<INewEntry>) => {
    setEntries((prev) => prev.map((entry) => (entry.key === key ? { ...entry, ...patch } : entry)))
  }

  const removeEntry = (key: string) => {
    setEntries((prev) => {
      const next = prev.filter((entry) => entry.key !== key)
      return next.length === 0 ? [emptyEntry()] : next
    })
  }

  /**
   * Appends rows for drafts the server produced, dropping the empty placeholder row they would
   * otherwise sit below. The source is the caller's to decide: a finding the AI made is
   * AI-authored, whereas an imported note is the instructor's own — the AI only split it up.
   */
  const appendEntries = (drafts: IAIDraft[], source: ThesisFeedbackSource) => {
    setEntries((prev) => {
      const cleaned = prev.filter((entry) => entry.feedback.trim().length > 0)
      const newRows: INewEntry[] = drafts.map((draft) => ({
        key: crypto.randomUUID(),
        feedback: draft.feedback ?? '',
        category: draft.category ?? '',
        severity: draft.severity ?? '',
        source,
      }))
      const merged = [...cleaned, ...newRows]
      return merged.length === 0 ? [emptyEntry()] : merged
    })
  }

  /**
   * Asks the server to classify one entry and fills in whichever of category and severity the AI
   * committed to. The text itself is never touched — only the two labels the instructor would
   * otherwise pick by hand — so the entry stays a human-authored one.
   *
   * Reports the outcome instead of notifying: a single wand click surfaces its own failure right
   * away, while "Classify all" collects them into one message rather than stacking a notification
   * per entry.
   */
  const classifyEntry = async (entry: INewEntry): Promise<IClassifyOutcome> => {
    const feedback = entry.feedback.trim()
    if (!feedback) {
      return { classified: false, reason: NO_SUGGESTION_MESSAGE }
    }

    setClassifyingKeys((prev) => new Set(prev).add(entry.key))
    try {
      const response = await doRequest<IFeedbackClassification>('/v2/ai-review/classify-feedback', {
        method: 'POST',
        requiresAuth: true,
        data: {
          thesisId: thesis.thesisId,
          feedback,
        },
      })

      if (!response.ok) {
        return { classified: false, reason: getApiResponseErrorMessage(response) }
      }

      const { category, severity } = response.data
      if (!category && !severity) {
        return { classified: false, reason: NO_SUGGESTION_MESSAGE }
      }

      // NON_EMPTY serialization drops a field the AI left open; keep whatever is already selected
      // for that dropdown rather than clearing it.
      updateEntry(entry.key, {
        ...(category ? { category } : {}),
        ...(severity ? { severity } : {}),
      })
      return { classified: true }
    } finally {
      setClassifyingKeys((prev) => {
        const next = new Set(prev)
        next.delete(entry.key)
        return next
      })
    }
  }

  const onSuggestClassification = async (entry: INewEntry) => {
    const outcome = await classifyEntry(entry)
    if (!outcome.classified) {
      showSimpleError(outcome.reason)
    }
  }

  /**
   * Classifies every entry that still lacks a category or a severity, in one go. Imported notes
   * routinely arrive without labels — a terse note often does not say enough for the split to
   * judge it — and labelling a whole reading pass one wand click at a time is the tedious part.
   *
   * Entries are classified concurrently; each keeps its own spinner, and the failures are reported
   * once at the end rather than one notification per entry.
   */
  const onClassifyAll = async () => {
    const pending = unclassifiedEntries
    if (pending.length === 0) {
      return
    }

    setBulkClassifying(true)
    try {
      const outcomes = await Promise.all(pending.map((entry) => classifyEntry(entry)))
      const failed = outcomes.filter((outcome) => !outcome.classified).length
      if (failed === pending.length) {
        showSimpleError(
          pending.length === 1
            ? NO_SUGGESTION_MESSAGE
            : 'The AI could not classify any of these entries. Please select the values manually.',
        )
      } else if (failed > 0) {
        showSimpleError(
          `The AI could not classify ${failed} of ${pending.length} entries. Please select those values manually.`,
        )
      }
    } finally {
      setBulkClassifying(false)
    }
  }

  /**
   * Splits the notes the instructor took offline into one entry per issue and appends them to the
   * batch. A line raising two issues becomes two entries, several lines about one issue become
   * one, and a note too terse to label arrives with its dropdowns open — the wand, or "Classify
   * all", fills those in afterwards.
   */
  const onImportNotes = async () => {
    const trimmed = notes.trim()
    if (!trimmed) {
      return
    }

    setImporting(true)
    try {
      const response = await doRequest<IImportedNotes>('/v2/ai-review/import-notes', {
        method: 'POST',
        requiresAuth: true,
        data: {
          thesisId: thesis.thesisId,
          notes: trimmed,
        },
      })

      if (!response.ok) {
        showSimpleError(getApiResponseErrorMessage(response))
        return
      }

      // NON_EMPTY serialization drops the list entirely when the notes held no feedback at all.
      const imported = response.data.entries ?? []
      if (imported.length === 0) {
        showSimpleError('No feedback entries were found in these notes.')
        return
      }

      appendEntries(imported, ThesisFeedbackSource.HUMAN)
      showSimpleSuccess(
        imported.length === 1
          ? 'Added 1 entry from your notes.'
          : `Added ${imported.length} entries from your notes.`,
      )
      setNotes('')
      setShowImportNotes(false)
    } finally {
      setImporting(false)
    }
  }

  const onGenerateAi = async () => {
    setAiLoading(true)
    try {
      const response = await doRequest<IAIPreviewResponse>('/v2/ai-review/preview', {
        method: 'POST',
        requiresAuth: true,
        data: {
          thesisId: thesis.thesisId,
          reviewType: type === 'PROPOSAL' ? 'PROPOSAL' : 'THESIS',
        },
      })

      if (response.ok) {
        const drafts = response.data.drafts ?? []
        // AI-drafted rows the instructor reviews before saving are persisted as AI + Instructor.
        appendEntries(drafts, ThesisFeedbackSource.AI_REVIEWED_BY_HUMAN)
        setAiAssessment(response.data)
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setAiLoading(false)
    }
  }

  const [saving, onSave] = useThesisUpdateAction(async () => {
    for (const editChange of editChanges) {
      await doRequest<IThesis>(
        `/v2/theses/${thesis.thesisId}/feedback/${editChange.feedbackId}/${editChange.completed ? 'complete' : 'request'}`,
        {
          method: 'PUT',
          requiresAuth: true,
        },
      )
    }

    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/feedback`, {
      method: 'POST',
      requiresAuth: true,
      data: {
        type,
        requestedChanges: validEntries.map((entry) => ({
          feedback: entry.feedback.trim(),
          completed: false,
          category: entry.category || null,
          severity: entry.severity || null,
          source: entry.source,
        })),
      },
    })

    if (response.ok) {
      setOpened(false)
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Changes requested successfully')

  // Only offer AI drafting when the feature is enabled server-side; otherwise the preview
  // endpoint is unregistered and the button would 404.
  const supportsAi = GLOBAL_CONFIG.ai_enabled && (type === 'PROPOSAL' || type === 'THESIS')

  // Classification reads the feedback line alone, so unlike a document review it does not depend
  // on an uploaded proposal or thesis and is offered for every feedback type.
  const supportsClassification = GLOBAL_CONFIG.ai_enabled

  return (
    <Button variant='outline' color='red' onClick={() => setOpened(true)}>
      <Modal
        title='Request Changes'
        opened={opened}
        onClose={() => {
          if (!hasUnsavedWork) {
            setOpened(false)
          } else if (!showDisregardChanges) {
            setShowDisregardChanges(true)
          }
        }}
        onClick={(e) => e.stopPropagation()}
        size='xl'
        centered
      >
        {/* The unsaved-changes prompt wins over the import panel: it is what closing the modal
            raises, and it would otherwise be invisible behind the panel. */}
        {showDisregardChanges ? (
          <Stack align='center' gap={'2rem'} w={400} mx='auto'>
            <Stack gap={'0.25rem'} align='center'>
              <Title order={4}>Unsaved changes</Title>
              <Text c='dimmed' ta={'center'}>
                You have unsaved changes. Do you want to discard them or keep editing?
              </Text>
            </Stack>

            <Stack gap={'0.25rem'} align='center'>
              <Group gap={'0.5rem'}>
                <Button onClick={() => setShowDisregardChanges(false)} variant='outline'>
                  Keep editing
                </Button>
                <Button
                  loading={saving}
                  onClick={() => {
                    onSave()
                    setShowDisregardChanges(false)
                  }}
                >
                  Save & close
                </Button>
              </Group>
              <Button
                onClick={() => {
                  setOpened(false)
                  setEntries([emptyEntry()])
                  setEditChanges([])
                  setShowDisregardChanges(false)
                  setShowImportNotes(false)
                  setNotes('')
                }}
                variant='transparent'
                color='gray'
                size='sm'
              >
                Discard
              </Button>
            </Stack>
          </Stack>
        ) : showImportNotes ? (
          <Stack>
            <Stack gap={'0.25rem'}>
              <Title order={4}>Import notes</Title>
              <Text c='dimmed' size='sm'>
                Paste the notes you took while reading. They are split into one entry per issue — a
                line raising two problems becomes two entries — and each entry gets a category and
                severity wherever the note says enough to judge it. Nothing is saved until you
                request the changes.
              </Text>
            </Stack>

            <Textarea
              autosize
              minRows={8}
              maxRows={18}
              label='Your notes'
              placeholder={'p. 4 fig 3 unreadable, and Smith is not cited\nch 4 too thin\n…'}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />

            <Group justify='flex-end'>
              <Button
                variant='outline'
                color='gray'
                onClick={() => {
                  setShowImportNotes(false)
                  setNotes('')
                }}
              >
                Cancel
              </Button>
              <Button
                leftSection={<ListBullets size={16} />}
                loading={importing}
                disabled={!notes.trim()}
                onClick={() => {
                  void onImportNotes()
                }}
              >
                Import
              </Button>
            </Group>
          </Stack>
        ) : (
          <Stack>
            {(thesis.feedback ?? [])
              .filter((change) => change.type === type)
              .map((change) => (
                <Checkbox
                  key={change.feedbackId}
                  label={change.feedback}
                  checked={
                    editChanges.find((item) => item.feedbackId === change.feedbackId)?.completed ??
                    Boolean(change.completedAt)
                  }
                  onChange={(e) => {
                    if (e.target.checked === Boolean(change.completedAt)) {
                      setEditChanges((prev) =>
                        prev.filter((item) => item.feedbackId !== change.feedbackId),
                      )
                    } else {
                      setEditChanges((prev) => [
                        ...prev,
                        { feedbackId: change.feedbackId, completed: e.target.checked },
                      ])
                    }
                  }}
                />
              ))}

            <Divider label='New feedback entries' labelPosition='left' />

            {aiAssessment && (
              <Alert
                color='grape'
                variant='light'
                title={
                  typeof aiAssessment.score === 'number'
                    ? `AI review — Overall Score: ${aiAssessment.score}/100`
                    : 'AI review'
                }
              >
                <Stack gap={4}>
                  {aiAssessment.assessment && (
                    <Text size='sm' fw={500}>
                      {ASSESSMENT_LABEL[aiAssessment.assessment]}
                    </Text>
                  )}
                  {aiAssessment.summary && (
                    <Text size='sm' c='dimmed'>
                      {aiAssessment.summary}
                    </Text>
                  )}
                  <FeedbackCategoryCounts counts={categoryCounts} />
                  <Text size='xs' c='dimmed'>
                    Entries below are drafts — edit or delete them before saving.
                  </Text>
                </Stack>
              </Alert>
            )}

            <Stack gap='sm'>
              {entries.map((entry, index) => (
                <Stack
                  key={entry.key}
                  gap={6}
                  p='sm'
                  style={{ border: '1px solid var(--mantine-color-gray-3)', borderRadius: 6 }}
                >
                  <Group justify='space-between' align='center'>
                    <Group gap={6}>
                      <Text size='sm' fw={500}>
                        Entry {index + 1}
                      </Text>
                      {entry.severity && (
                        <Tooltip
                          label={SEVERITY_DESCRIPTION[entry.severity]}
                          withArrow
                          openDelay={300}
                        >
                          <Badge size='sm' color={SEVERITY_COLOR[entry.severity]} variant='light'>
                            {entry.severity}
                          </Badge>
                        </Tooltip>
                      )}
                    </Group>
                    <Tooltip label='Remove entry'>
                      <ActionIcon
                        variant='subtle'
                        color='red'
                        onClick={() => removeEntry(entry.key)}
                        aria-label='Remove entry'
                      >
                        <Trash />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                  <Textarea
                    autosize
                    minRows={2}
                    maxRows={8}
                    placeholder='Describe the change you want the student to make…'
                    value={entry.feedback}
                    onChange={(e) => updateEntry(entry.key, { feedback: e.target.value })}
                  />
                  <Group align='flex-end' gap='sm' wrap='nowrap'>
                    <Select
                      style={{ flex: 1 }}
                      label='Category'
                      placeholder='Uncategorized'
                      data={FEEDBACK_CATEGORY_OPTIONS}
                      value={entry.category || null}
                      clearable
                      onChange={(value) =>
                        updateEntry(entry.key, {
                          category: (value as ThesisFeedbackCategory) || '',
                        })
                      }
                    />
                    <Select
                      style={{ flex: 1 }}
                      label='Severity'
                      placeholder='Unspecified'
                      data={FEEDBACK_SEVERITY_OPTIONS}
                      value={entry.severity || null}
                      clearable
                      onChange={(value) =>
                        updateEntry(entry.key, {
                          severity: (value as ThesisFeedbackSeverity) || '',
                        })
                      }
                    />
                    {supportsClassification && (
                      <Tooltip
                        label={
                          entry.feedback.trim()
                            ? 'Suggest category and severity with AI'
                            : 'Write the feedback first, then let the AI classify it'
                        }
                        withArrow
                      >
                        <ActionIcon
                          variant='light'
                          color='grape'
                          size={36}
                          aria-label='Suggest category and severity with AI'
                          disabled={!entry.feedback.trim()}
                          loading={classifyingKeys.has(entry.key)}
                          onClick={() => {
                            void onSuggestClassification(entry)
                          }}
                        >
                          <MagicWand size={18} />
                        </ActionIcon>
                      </Tooltip>
                    )}
                  </Group>
                </Stack>
              ))}
            </Stack>

            <Group>
              <Button
                variant='outline'
                leftSection={<Plus size={16} />}
                onClick={() => setEntries((prev) => [...prev, emptyEntry()])}
              >
                Add Entry
              </Button>
              {supportsClassification && (
                <Button
                  variant='outline'
                  color='grape'
                  leftSection={<ListBullets size={16} />}
                  onClick={() => setShowImportNotes(true)}
                >
                  Import notes
                </Button>
              )}
              {supportsClassification && (
                <Tooltip
                  label={
                    unclassifiedEntries.length > 0
                      ? 'Suggest category and severity for every entry still missing one'
                      : 'Every entry already has a category and a severity'
                  }
                  withArrow
                >
                  <Button
                    variant='outline'
                    color='grape'
                    leftSection={<MagicWand size={16} />}
                    loading={bulkClassifying}
                    disabled={unclassifiedEntries.length === 0}
                    onClick={() => {
                      void onClassifyAll()
                    }}
                  >
                    Classify all
                  </Button>
                </Tooltip>
              )}
              {supportsAi && (
                <Button
                  variant='outline'
                  color='grape'
                  leftSection={<Robot size={16} />}
                  loading={aiLoading}
                  onClick={() => {
                    void onGenerateAi()
                  }}
                >
                  Generate with AI
                </Button>
              )}
            </Group>

            <Button
              fullWidth
              loading={saving}
              disabled={editChanges.length === 0 && validEntries.length === 0}
              onClick={onSave}
            >
              Request Changes
            </Button>
          </Stack>
        )}
      </Modal>
      Request Changes
    </Button>
  )
}

export default ThesisFeedbackRequestButton
