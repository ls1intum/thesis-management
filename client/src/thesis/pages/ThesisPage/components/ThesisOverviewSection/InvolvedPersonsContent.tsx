import { Button, Grid, Group, NumberInput, Paper, Stack } from '@mantine/core'
import type { IThesis } from '@/thesis/requests/responses/thesis'
import LabeledItem from '@/core/components/LabeledItem/LabeledItem'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { useHighlightedBackgroundColor } from '@/core/hooks/theme'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import { formatUser } from '@/core/utils/format'
import { useEffect, useRef, useState } from 'react'
import { doRequest } from '@/core/requests/request'
import { ApiError } from '@/core/requests/handler'
import { renderCustomDataValue } from '@/core/utils/customDataLink'

const InvolvedPersonsContent = () => {
  const { thesis } = useLoadedThesisContext()

  const [credits, setCredits] = useState(thesis.metadata.credits)
  // Tracks unsaved edits so an unrelated thesis refresh doesn't stomp them.
  const dirtyRef = useRef(false)

  useEffect(() => {
    if (dirtyRef.current) return
    setCredits(thesis.metadata.credits)
  }, [thesis.metadata.credits])

  const studentBackgroundColor = useHighlightedBackgroundColor(false)

  const [updating, onUpdate] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/credits`, {
      method: 'PUT',
      data: {
        credits,
      },
      requiresAuth: true,
    })

    if (response.ok) {
      // Clear the dirty flag before the context update propagates so the
      // resync effect can pick up the just-saved credits.
      dirtyRef.current = false
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Credits updated successfully')

  const users = [
    ...(thesis.students ?? []).map((row) => ({
      type: 'student',
      data: row,
    })),
    ...(thesis.supervisors ?? []).map((row) => ({
      type: 'supervisor',
      data: row,
    })),
  ]

  return (
    <Stack gap='sm'>
      {users.map((user) => (
        <Paper
          key={`${user.type}-${user.data.userId}`}
          p='md'
          radius='sm'
          style={{ backgroundColor: studentBackgroundColor }}
        >
          <Grid>
            <Grid.Col span={{ md: 2 }}>
              <LabeledItem
                label={user.type === 'student' ? 'Student' : 'Supervisor'}
                value={formatUser(user.data)}
              />
            </Grid.Col>
            <Grid.Col span={{ md: 2 }}>
              <LabeledItem
                label='University ID'
                value={user.data.universityId}
                copyText={user.data.universityId}
              />
            </Grid.Col>
            {user.data.matriculationNumber && (
              <Grid.Col span={{ md: 2 }}>
                <LabeledItem
                  label='Matriculation Number'
                  value={user.data.matriculationNumber}
                  copyText={user.data.matriculationNumber || undefined}
                />
              </Grid.Col>
            )}
            {user.data.email && (
              <Grid.Col span={{ md: 2 }}>
                <LabeledItem
                  label='E-Mail'
                  value={user.data.email}
                  copyText={user.data.email || undefined}
                />
              </Grid.Col>
            )}
            {user.data.studyProgram && user.data.studyDegree && (
              <Grid.Col span={{ md: 2 }}>
                <LabeledItem
                  label='Study Degree'
                  value={`${
                    GLOBAL_CONFIG.study_programs[user.data.studyProgram || ''] ??
                    user.data.studyProgram
                  } ${
                    GLOBAL_CONFIG.study_degrees[user.data.studyDegree || ''] ??
                    user.data.studyDegree
                  } `}
                />
              </Grid.Col>
            )}
            {user.data.customData &&
              Object.entries(user.data.customData).map(([key, value]) => (
                <Grid.Col key={key} span={{ md: 6 }}>
                  <LabeledItem
                    label={GLOBAL_CONFIG.custom_data[key]?.label ?? key}
                    value={renderCustomDataValue(key, value)}
                  />
                </Grid.Col>
              ))}
            {user.type === 'student' && (
              <Grid.Col span={{ md: 6 }}>
                <NumberInput
                  label='Credits for Thesis'
                  min={1}
                  value={credits[user.data.userId]}
                  onChange={(value) => {
                    dirtyRef.current = true
                    setCredits((prev) => {
                      if (value) {
                        return { ...prev, [user.data.userId]: Number(value) }
                      } else {
                        // Copy first, then delete: prev may alias the shared
                        // thesis.metadata.credits object, which must not be mutated.
                        const next = { ...prev }
                        delete next[user.data.userId]

                        return next
                      }
                    })
                  }}
                  inputContainer={(children) => (
                    <Group>
                      {children}
                      <Button loading={updating} onClick={onUpdate}>
                        Save
                      </Button>
                    </Group>
                  )}
                />
              </Grid.Col>
            )}
          </Grid>
        </Paper>
      ))}
    </Stack>
  )
}

export default InvolvedPersonsContent
