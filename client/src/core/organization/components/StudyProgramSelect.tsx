import { useMemo } from 'react'
import { Select, type SelectProps } from '@mantine/core'
import type { IStudyProgram } from '@/core/organization/requests/responses/organization'

interface IStudyProgramSelectProps extends Omit<SelectProps, 'data' | 'value' | 'onChange'> {
  studyPrograms: IStudyProgram[]
  value?: string | null
  onChange: (studyProgramId: string | null) => void
}

const NO_SCHOOL_GROUP = 'Other'

/**
 * Select for a study program, grouped by school. Deactivated programs are hidden unless they are the
 * currently selected one, so users keep seeing what they already have.
 */
const StudyProgramSelect = ({
  studyPrograms,
  value,
  onChange,
  ...others
}: IStudyProgramSelectProps) => {
  const data = useMemo(() => {
    const groups = new Map<string, Array<{ value: string; label: string }>>()

    for (const program of studyPrograms) {
      if (program.active === false && program.id !== value) {
        continue
      }

      const group = program.school?.name ?? NO_SCHOOL_GROUP
      const items = groups.get(group) ?? []
      items.push({ value: program.id, label: program.name })
      groups.set(group, items)
    }

    return [...groups.entries()]
      .sort(([a], [b]) =>
        a === NO_SCHOOL_GROUP ? 1 : b === NO_SCHOOL_GROUP ? -1 : a.localeCompare(b),
      )
      .map(([group, items]) => ({
        group,
        items: items.sort((a, b) => a.label.localeCompare(b.label)),
      }))
  }, [studyPrograms, value])

  return (
    <Select
      searchable
      clearable
      nothingFoundMessage='No study program found'
      {...others}
      data={data}
      value={value ?? null}
      onChange={(next) => onChange(next)}
    />
  )
}

export default StudyProgramSelect
