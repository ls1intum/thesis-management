import { useState } from 'react'
import {
  Accordion,
  ActionIcon,
  Anchor,
  Badge,
  Button,
  Group,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core'
import { BuildingsIcon, PencilSimpleIcon, PlusIcon } from '@phosphor-icons/react'
import ConfirmationButton from '@/core/components/ConfirmationButton/ConfirmationButton'
import NoContentFoundCard from '@/core/components/NoContentFoundCard/NoContentFoundCard'
import type { IDepartment, ISchool } from '@/core/organization/requests/responses/organization'
import SchoolFormModal from '@/core/organization/pages/OrganizationPage/components/SchoolFormModal'
import DepartmentFormModal from '@/core/organization/pages/OrganizationPage/components/DepartmentFormModal'
import { deleteEntity } from '@/core/organization/pages/OrganizationPage/components/deleteEntity'

interface ISchoolsTabProps {
  schools: ISchool[]
  onChanged: () => void
}

const SchoolsTab = ({ schools, onChanged }: ISchoolsTabProps) => {
  const [schoolModal, setSchoolModal] = useState<{ school?: ISchool } | undefined>()
  const [departmentModal, setDepartmentModal] = useState<
    { school: ISchool; department?: IDepartment } | undefined
  >()

  return (
    <Stack gap='md'>
      <Group justify='space-between'>
        <Text c='dimmed' size='sm'>
          Schools (faculties) group departments and study programs. Research groups belong to a
          school and a department of it.
        </Text>
        <Button leftSection={<PlusIcon />} onClick={() => setSchoolModal({})}>
          Add School
        </Button>
      </Group>

      {schools.length === 0 ? (
        <NoContentFoundCard
          icon={<BuildingsIcon size={32} />}
          title='No Schools'
          subtle='Create the first school to structure research groups and study programs.'
        />
      ) : (
        <Accordion variant='separated' multiple>
          {schools.map((school) => (
            <Accordion.Item
              key={school.id}
              value={school.id}
              data-testid={`school-${school.abbreviation}`}
            >
              <Accordion.Control>
                <Group gap='sm'>
                  <Text fw={500}>{school.name}</Text>
                  <Badge variant='light'>{school.abbreviation}</Badge>
                  {school.active === false && (
                    <Badge color='gray' variant='light'>
                      Inactive
                    </Badge>
                  )}
                </Group>
              </Accordion.Control>
              <Accordion.Panel>
                <Stack gap='md'>
                  <Group gap='xl' align='flex-start'>
                    <Stack gap={2}>
                      <Text size='xs' c='dimmed'>
                        Website
                      </Text>
                      {school.websiteUrl ? (
                        <Anchor
                          href={school.websiteUrl}
                          target='_blank'
                          rel='noopener noreferrer'
                          size='sm'
                        >
                          {school.websiteUrl}
                        </Anchor>
                      ) : (
                        <Text size='sm'>-</Text>
                      )}
                    </Stack>
                    <Stack gap={2}>
                      <Text size='xs' c='dimmed'>
                        Thesis portal
                      </Text>
                      {school.thesisPortalUrl ? (
                        <Anchor
                          href={school.thesisPortalUrl}
                          target='_blank'
                          rel='noopener noreferrer'
                          size='sm'
                        >
                          {school.thesisPortalUrl}
                        </Anchor>
                      ) : (
                        <Text size='sm'>Default portal</Text>
                      )}
                    </Stack>
                  </Group>

                  <Group gap='xs'>
                    <Button
                      variant='light'
                      size='xs'
                      leftSection={<PencilSimpleIcon />}
                      onClick={() => setSchoolModal({ school })}
                    >
                      Edit School
                    </Button>
                    <ConfirmationButton
                      variant='light'
                      color='red'
                      size='xs'
                      confirmationTitle='Delete School'
                      confirmationText={
                        `Delete ${school.name}? This only works if it has no departments, ` +
                        'study programs or research groups. Deactivate it instead to keep the history.'
                      }
                      onClick={() =>
                        void deleteEntity(`/v2/schools/${school.id}`, 'School deleted', onChanged)
                      }
                    >
                      Delete School
                    </ConfirmationButton>
                  </Group>

                  <Stack gap='xs'>
                    <Group justify='space-between'>
                      <Text fw={500} size='sm'>
                        Departments
                      </Text>
                      <Button
                        variant='subtle'
                        size='xs'
                        leftSection={<PlusIcon />}
                        onClick={() => setDepartmentModal({ school })}
                      >
                        Add Department
                      </Button>
                    </Group>
                    {(school.departments ?? []).length === 0 && (
                      <Text size='sm' c='dimmed'>
                        No departments yet.
                      </Text>
                    )}
                    {(school.departments ?? []).map((department) => (
                      <Group key={department.id} justify='space-between' wrap='nowrap'>
                        <Group gap='xs'>
                          <Text size='sm'>{department.name}</Text>
                          {department.abbreviation && (
                            <Badge variant='outline' size='sm'>
                              {department.abbreviation}
                            </Badge>
                          )}
                          {department.active === false && (
                            <Badge color='gray' variant='light' size='sm'>
                              Inactive
                            </Badge>
                          )}
                        </Group>
                        <Group gap={4} wrap='nowrap'>
                          <Tooltip label='Edit department'>
                            <ActionIcon
                              variant='subtle'
                              aria-label={`Edit department ${department.name}`}
                              onClick={() => setDepartmentModal({ school, department })}
                            >
                              <PencilSimpleIcon size={16} />
                            </ActionIcon>
                          </Tooltip>
                          <ConfirmationButton
                            variant='subtle'
                            color='red'
                            size='compact-xs'
                            aria-label={`Delete department ${department.name}`}
                            confirmationTitle='Delete Department'
                            confirmationText={`Delete ${department.name}? This only works if no research group belongs to it.`}
                            onClick={() =>
                              void deleteEntity(
                                `/v2/departments/${department.id}`,
                                'Department deleted',
                                onChanged,
                              )
                            }
                          >
                            Delete
                          </ConfirmationButton>
                        </Group>
                      </Group>
                    ))}
                  </Stack>
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
          ))}
        </Accordion>
      )}

      <SchoolFormModal
        opened={schoolModal !== undefined}
        school={schoolModal?.school}
        onClose={() => setSchoolModal(undefined)}
        onSaved={onChanged}
      />
      <DepartmentFormModal
        opened={departmentModal !== undefined}
        school={departmentModal?.school}
        department={departmentModal?.department}
        onClose={() => setDepartmentModal(undefined)}
        onSaved={onChanged}
      />
    </Stack>
  )
}

export default SchoolsTab
