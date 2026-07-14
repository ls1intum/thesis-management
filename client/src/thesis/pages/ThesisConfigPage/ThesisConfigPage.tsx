import { Button, Center, Group, Stack } from '@mantine/core'
import { ArrowLeft } from '@phosphor-icons/react'
import { useEffect } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import ThesisConfigSection from '@/thesis/pages/ThesisPage/components/ThesisConfigSection/ThesisConfigSection'
import ThesisProvider from '@/thesis/providers/ThesisProvider/ThesisProvider'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import { usePageTitle } from '@/core/hooks/theme'

const ThesisConfigPageContent = () => {
  const { thesis, access } = useLoadedThesisContext()
  const navigate = useNavigate()

  usePageTitle(`Configuration – ${thesis.title}`)

  useEffect(() => {
    if (!access.supervisor) {
      void navigate(`/theses/${thesis.thesisId}`, { replace: true })
    }
  }, [access.supervisor, navigate, thesis.thesisId])

  if (!access.supervisor) {
    return <Center>Redirecting…</Center>
  }

  return (
    <Stack>
      <Group>
        <Button
          component={Link}
          to={`/theses/${thesis.thesisId}`}
          variant='subtle'
          leftSection={<ArrowLeft />}
        >
          Back to Thesis
        </Button>
      </Group>
      <ThesisConfigSection />
    </Stack>
  )
}

const ThesisConfigPage = () => {
  const { thesisId } = useParams<{ thesisId: string }>()

  usePageTitle('Configuration')

  return (
    <ThesisProvider thesisId={thesisId} requireLoadedThesis>
      <ThesisConfigPageContent />
    </ThesisProvider>
  )
}

export default ThesisConfigPage
