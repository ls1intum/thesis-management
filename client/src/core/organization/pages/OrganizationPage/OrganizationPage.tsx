import { Loader, Stack, Tabs, Title } from '@mantine/core'
import { useOrganization } from '@/core/organization/hooks/useOrganization'
import SchoolsTab from '@/core/organization/pages/OrganizationPage/components/SchoolsTab'
import StudyProgramsTab from '@/core/organization/pages/OrganizationPage/components/StudyProgramsTab'

const OrganizationPage = () => {
  const { schools, studyPrograms, isLoading, reload } = useOrganization()

  return (
    <Stack>
      <Title>Organization</Title>
      <Tabs defaultValue='schools' keepMounted={false}>
        <Tabs.List mb='md'>
          <Tabs.Tab value='schools'>Schools</Tabs.Tab>
          <Tabs.Tab value='study-programs'>Study Programs</Tabs.Tab>
        </Tabs.List>

        {isLoading && schools.length === 0 && studyPrograms.length === 0 ? (
          <Loader />
        ) : (
          <>
            <Tabs.Panel value='schools'>
              <SchoolsTab schools={schools} onChanged={reload} />
            </Tabs.Panel>
            <Tabs.Panel value='study-programs'>
              <StudyProgramsTab
                studyPrograms={studyPrograms}
                schools={schools}
                onChanged={reload}
              />
            </Tabs.Panel>
          </>
        )}
      </Tabs>
    </Stack>
  )
}

export default OrganizationPage
