import type { IThesis } from '@/thesis/requests/responses/thesis'
import React, { useEffect, useState } from 'react'
import { Button, Flex, Grid, Group, Stack, TextInput } from '@mantine/core'
import { Link } from 'react-router'
import DocumentEditor from '@/core/components/DocumentEditor/DocumentEditor'
import { doRequest } from '@/core/requests/request'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import { ApiError } from '@/core/requests/handler'
import DownloadAllFilesButton from '@/thesis/pages/ThesisPage/components/ThesisOverviewSection/components/DownloadAllFilesButton/DownloadAllFilesButton'
import AbstractSuggestionModal from '@/thesis/pages/ThesisPage/components/ThesisOverviewSection/components/AbstractSuggestionModal/AbstractSuggestionModal'
import { isThesisClosed } from '@/thesis/utils/thesis'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { formatLanguage } from '@/core/utils/format'
import LabeledItem from '@/core/components/LabeledItem/LabeledItem'

const InfoContent = () => {
  const { thesis, access } = useLoadedThesisContext()

  const [editMode, setEditMode] = useState(false)

  const [infoText, setInfoText] = useState(thesis.infoText)
  const [abstractText, setAbstractText] = useState(thesis.abstractText)
  const [titles, setTitles] = useState<Record<string, string>>({})

  useEffect(() => {
    // Skip resync while the user is editing so an unrelated thesis update from
    // another section can't wipe their in-progress edits.
    if (editMode) return
    setInfoText(thesis.infoText)
    setAbstractText(thesis.abstractText)
    setTitles({
      ...thesis.metadata.titles,
      [thesis.language]: thesis.title,
    })
  }, [
    editMode,
    thesis.infoText,
    thesis.abstractText,
    thesis.metadata,
    thesis.title,
    thesis.language,
  ])

  const [saving, onSave] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/info`, {
      method: 'PUT',
      requiresAuth: true,
      data: {
        abstractText,
        infoText,
        primaryTitle: titles[thesis.language],
        secondaryTitles: titles,
      },
    })

    if (response.ok) {
      setEditMode(false)

      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Thesis info updated successfully')

  const [accepting, onUseSuggestion] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(
      `/v2/theses/${thesis.thesisId}/abstract-suggestion/accept`,
      { method: 'POST', requiresAuth: true },
    )

    if (response.ok) {
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Abstract updated from the suggestion')

  const [dismissing, onDismissSuggestion] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(
      `/v2/theses/${thesis.thesisId}/abstract-suggestion/dismiss`,
      { method: 'POST', requiresAuth: true },
    )

    if (response.ok) {
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Abstract suggestion dismissed')

  const showSuggestion =
    access.student && !editMode && !isThesisClosed(thesis) && !!thesis.abstractSuggestion

  return (
    <Stack>
      {editMode ? (
        <Stack>
          {Object.keys(GLOBAL_CONFIG.languages).map((language) => (
            <TextInput
              key={`input-${language}`}
              label={`${formatLanguage(language)} Title`}
              value={titles[language] || ''}
              onChange={(e) => setTitles((prev) => ({ ...prev, [language]: e.target.value }))}
            />
          ))}
        </Stack>
      ) : (
        <Stack>
          {Object.keys(GLOBAL_CONFIG.languages).map((language) => (
            <LabeledItem
              key={`label-${language}`}
              label={`${formatLanguage(language)} Title`}
              value={titles[language] || 'No Title'}
              copyText={titles[language]}
            />
          ))}
        </Stack>
      )}
      <AbstractSuggestionModal
        opened={showSuggestion}
        currentAbstract={thesis.abstractText ?? ''}
        suggestion={thesis.abstractSuggestion ?? ''}
        loading={accepting || dismissing}
        onConfirm={onUseSuggestion}
        onDeny={onDismissSuggestion}
      />
      <DocumentEditor
        label='Abstract'
        value={abstractText}
        editMode={editMode}
        onChange={(e) => setAbstractText(e.target.value)}
        maxLength={2000}
      />
      <DocumentEditor
        label='Additional Information (Important links, repositories etc.)'
        value={infoText}
        editMode={editMode}
        onChange={(e) => setInfoText(e.target.value)}
        maxLength={2000}
      />
      <Grid>
        <Grid.Col span={{ md: 6 }}>
          {access.supervisor && thesis.applicationId && (
            <Button component={Link} variant='outline' to={`/applications/${thesis.applicationId}`}>
              View Student Application
            </Button>
          )}
        </Grid.Col>
        <Grid.Col span={{ md: 6 }}>
          <Flex justify='flex-end'>
            <Group>
              {!editMode && <DownloadAllFilesButton />}
              {access.student && !editMode && !isThesisClosed(thesis) && (
                <Button ml='auto' onClick={() => setEditMode(true)}>
                  Edit
                </Button>
              )}
            </Group>
            {editMode && (
              <Group>
                <Button
                  loading={saving}
                  variant='danger'
                  onClick={() => {
                    setInfoText(thesis.infoText)
                    setAbstractText(thesis.abstractText)
                    setTitles({
                      ...thesis.metadata.titles,
                      [thesis.language]: thesis.title,
                    })
                    setEditMode(false)
                  }}
                >
                  Cancel
                </Button>
                <Button loading={saving} onClick={onSave}>
                  Save
                </Button>
              </Group>
            )}
          </Flex>
        </Grid.Col>
      </Grid>
    </Stack>
  )
}

export default InfoContent
