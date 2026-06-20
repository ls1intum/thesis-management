import { ThesisState } from '@/thesis/requests/responses/thesis'
import { useState } from 'react'
import { Accordion, Button, Stack } from '@mantine/core'
import { checkMinimumThesisState, isThesisClosed } from '@/thesis/utils/thesis'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import ReplacePresentationModal from '@/presentation/components/PresentationsTable/components/ReplacePresentationModal/ReplacePresentationModal'
import PresentationCard from '@/thesis/pages/ThesisPage/components/ThesisPresentationSection/components/PresentationCard'

const ThesisPresentationSection = () => {
  const { thesis, access } = useLoadedThesisContext()

  const [createPresentationModal, setCreatePresentationModal] = useState(false)

  if (!checkMinimumThesisState(thesis, ThesisState.WRITING)) {
    return <></>
  }

  return (
    <Accordion variant='separated' defaultValue='open'>
      <Accordion.Item value='open'>
        <Accordion.Control>Presentation</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            <ReplacePresentationModal
              thesis={thesis}
              opened={createPresentationModal}
              onClose={() => setCreatePresentationModal(false)}
            />
            {access.student && !isThesisClosed(thesis) && (
              <Button ml='auto' onClick={() => setCreatePresentationModal(true)}>
                Create Presentation Draft
              </Button>
            )}

            {(thesis.presentations ?? []).map((presentation) => (
              <PresentationCard
                key={presentation.presentationId}
                presentation={presentation}
                thesis={thesis}
                thesisType={thesis.type}
                hasEditAccess={access.student || false}
                hasAcceptAccess={access.supervisor}
              />
            ))}
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}

export default ThesisPresentationSection
