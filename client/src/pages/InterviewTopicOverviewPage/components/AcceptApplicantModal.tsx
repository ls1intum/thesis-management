import { Loader, Modal, Title, Group, Button } from '@mantine/core'
import type { IIntervieweeLightWithNextSlot } from '../../../requests/responses/interview'
import ApplicationReviewForm from '../../../components/ApplicationReviewForm/ApplicationReviewForm'
import type { IApplication } from '../../../requests/responses/application'
import { useEffect, useState } from 'react'
import { useApplicationsContext } from '../../../providers/ApplicationsProvider/hooks'
import ApplicationRejectButton from '../../../components/ApplicationRejectButton/ApplicationRejectButton'
import { CheckIcon } from '@phosphor-icons/react/dist/ssr'
import { XIcon } from '@phosphor-icons/react'

interface AcceptApplicantModalProps {
  interviewee: IIntervieweeLightWithNextSlot
  onAcceptSuccessfull?: () => void
}

const AcceptApplicantModal = ({ interviewee, onAcceptSuccessfull }: AcceptApplicantModalProps) => {
  const [application, setApplication] = useState<IApplication | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  const { fetchApplication } = useApplicationsContext()

  useEffect(() => {
    const fetchData = async () => {
      const app = await fetchApplication(interviewee.applicationId)
      setApplication(app)
    }
    void fetchData()
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- fetchApplication is recreated each render by the provider; effect should only re-run when applicationId changes
  }, [interviewee.applicationId])

  return (
    <Group>
      <Button
        variant='outline'
        size='xs'
        leftSection={<CheckIcon size={16} />}
        onClick={() => {
          setModalOpen(true)
        }}
        color={'green'}
      >
        Accept Applicant
      </Button>

      {application && (
        <ApplicationRejectButton
          application={application}
          onUpdate={() => {
            //TODO: SET State of interviewee
            onAcceptSuccessfull?.()
          }}
          size='xs'
          leftSection={<XIcon size={16} />}
        >
          Reject Applicant
        </ApplicationRejectButton>
      )}

      <Modal
        opened={modalOpen}
        onClose={() => setModalOpen(false)}
        title={<Title order={3}>Accept Applicant</Title>}
        size={'xl'}
        centered
      >
        {application ? (
          <ApplicationReviewForm
            application={application}
            onUpdate={() => {
              //TODO: SET State of interviewee
            }}
            includeCommentSection={false}
            acceptOnly={true}
            onAcceptSuccessfull={() => {
              setModalOpen(false)
              onAcceptSuccessfull?.()
            }}
          />
        ) : (
          <Loader />
        )}
      </Modal>
    </Group>
  )
}

export default AcceptApplicantModal
