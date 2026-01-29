import { Loader, Modal, Title, Group, Button } from '@mantine/core'
import { IIntervieweeLightWithNextSlot } from '../../../requests/responses/interview'
import ApplicationReviewForm from '../../../components/ApplicationReviewForm/ApplicationReviewForm'
import { IApplication } from '../../../requests/responses/application'
import { useEffect, useState } from 'react'
import { useApplicationsContext } from '../../../providers/ApplicationsProvider/hooks'
import ApplicationRejectButton from '../../../components/ApplicationRejectButton/ApplicationRejectButton'
import { CheckIcon } from '@phosphor-icons/react/dist/ssr'
import { XIcon } from '@phosphor-icons/react'

interface AcceptApplicantModalProps {
  interviewee: IIntervieweeLightWithNextSlot
}

const AcceptApplicantModal = ({ interviewee }: AcceptApplicantModalProps) => {
  const [application, setApplication] = useState<IApplication | null>(null)
  const [modalOpen, setModalOpen] = useState(false)

  const { fetchApplication } = useApplicationsContext()

  useEffect(() => {
    const fetchData = async () => {
      const app = await fetchApplication(interviewee.applicationId)
      setApplication(app)
    }
    fetchData()
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
          onUpdate={(newApplication) => {
            newApplication.state
            //TODO: SET State of interviewee
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
            onUpdate={(newApplication) => {
              newApplication.state
              //TODO: SET State of interviewee
            }}
            includeCommentSection={false}
            acceptOnly={true}
          />
        ) : (
          <Loader />
        )}
      </Modal>
    </Group>
  )
}

export default AcceptApplicantModal
