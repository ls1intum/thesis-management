import { useMemo, useState } from 'react'
import { Badge, Button, Group, Stack, Table, Text, TextInput } from '@mantine/core'
import {
  GraduationCapIcon,
  MagnifyingGlassIcon,
  PencilSimpleIcon,
  PlusIcon,
} from '@phosphor-icons/react'
import ConfirmationButton from '@/core/components/ConfirmationButton/ConfirmationButton'
import NoContentFoundCard from '@/core/components/NoContentFoundCard/NoContentFoundCard'
import type { ISchool, IStudyProgram } from '@/core/organization/requests/responses/organization'
import StudyProgramFormModal from '@/core/organization/pages/OrganizationPage/components/StudyProgramFormModal'
import { deleteEntity } from '@/core/organization/pages/OrganizationPage/components/deleteEntity'

interface IStudyProgramsTabProps {
  studyPrograms: IStudyProgram[]
  schools: ISchool[]
  onChanged: () => void
}

const StudyProgramsTab = ({ studyPrograms, schools, onChanged }: IStudyProgramsTabProps) => {
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState<{ studyProgram?: IStudyProgram } | undefined>()

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase()

    return studyPrograms.filter(
      (program) =>
        !query ||
        program.name.toLowerCase().includes(query) ||
        program.key.toLowerCase().includes(query) ||
        (program.school?.name.toLowerCase().includes(query) ?? false) ||
        (program.school?.abbreviation.toLowerCase().includes(query) ?? false),
    )
  }, [studyPrograms, search])

  return (
    <Stack gap='md'>
      <Group justify='space-between' wrap='nowrap'>
        <TextInput
          placeholder='Search study programs...'
          leftSection={<MagnifyingGlassIcon size={16} />}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ flex: 1 }}
        />
        <Button leftSection={<PlusIcon />} onClick={() => setModal({})}>
          Add Study Program
        </Button>
      </Group>

      {filtered.length === 0 ? (
        <NoContentFoundCard
          icon={<GraduationCapIcon size={32} />}
          title='No Study Programs Found'
          subtle={search ? 'Try changing the search term.' : 'Create the first study program.'}
        />
      ) : (
        <Table.ScrollContainer minWidth={600}>
          <Table highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Name</Table.Th>
                <Table.Th>Key</Table.Th>
                <Table.Th>School</Table.Th>
                <Table.Th>Status</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {filtered.map((program) => (
                <Table.Tr key={program.id}>
                  <Table.Td>{program.name}</Table.Td>
                  <Table.Td>
                    <Text size='xs' c='dimmed' ff='monospace'>
                      {program.key}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    {program.school ? (
                      <Badge variant='light'>{program.school.abbreviation}</Badge>
                    ) : (
                      <Text size='sm' c='dimmed'>
                        -
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td>
                    {program.active === false ? (
                      <Badge color='gray' variant='light'>
                        Inactive
                      </Badge>
                    ) : (
                      <Badge color='green' variant='light'>
                        Active
                      </Badge>
                    )}
                  </Table.Td>
                  <Table.Td>
                    <Group gap={4} justify='flex-end' wrap='nowrap'>
                      <Button
                        variant='subtle'
                        size='compact-xs'
                        leftSection={<PencilSimpleIcon size={14} />}
                        aria-label={`Edit study program ${program.name}`}
                        onClick={() => setModal({ studyProgram: program })}
                      >
                        Edit
                      </Button>
                      <ConfirmationButton
                        variant='subtle'
                        color='red'
                        size='compact-xs'
                        confirmationTitle='Delete Study Program'
                        confirmationText={`Delete ${program.name}? This only works if no student or thesis uses it. Deactivate it instead to keep the history.`}
                        onClick={() =>
                          void deleteEntity(
                            `/v2/study-programs/${program.id}`,
                            'Study program deleted',
                            onChanged,
                          )
                        }
                      >
                        Delete
                      </ConfirmationButton>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      <StudyProgramFormModal
        opened={modal !== undefined}
        studyProgram={modal?.studyProgram}
        schools={schools}
        onClose={() => setModal(undefined)}
        onSaved={onChanged}
      />
    </Stack>
  )
}

export default StudyProgramsTab
