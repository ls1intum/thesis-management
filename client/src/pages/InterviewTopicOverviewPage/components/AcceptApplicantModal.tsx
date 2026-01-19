import { Loader, Modal, Stack, Title, Text } from '@mantine/core'
import { IIntervieweeLightWithNextSlot } from '../../../requests/responses/interview'
import ApplicationReviewForm from '../../../components/ApplicationReviewForm/ApplicationReviewForm'
import { IApplication } from '../../../requests/responses/application'
import { useEffect, useState } from 'react'
import { useApplicationsContext } from '../../../providers/ApplicationsProvider/hooks'
import ApplicationRejectButton from '../../../components/ApplicationRejectButton/ApplicationRejectButton'

interface AcceptApplicantModalProps {
  modalOpen: boolean
  setModalOpen: (open: boolean) => void
  interviewee: IIntervieweeLightWithNextSlot
  type?: 'accept' | 'reject'
}

const AcceptApplicantModal = ({
  modalOpen,
  setModalOpen,
  interviewee,
  type = 'accept',
}: AcceptApplicantModalProps) => {
  const [application, setApplication] = useState<IApplication | null>(null)

  const { fetchApplication } = useApplicationsContext()

  useEffect(() => {
    if (modalOpen) {
      const fetchData = async () => {
        const app = await fetchApplication(interviewee.applicationId)
        setApplication(app)
      }
      fetchData()
    }
  }, [modalOpen])

  return (
    <Modal
      opened={modalOpen}
      onClose={() => setModalOpen(false)}
      title={<Title order={3}>Accept Applicant</Title>}
      size={'xl'}
      centered
    >
      {application ? (
        type === 'accept' ? (
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
          <Stack>
            <Text>Are you sure you want to reject this interviewee?</Text>
            <ApplicationRejectButton
              application={application}
              onUpdate={(newApplication) => {
                newApplication.state
                //TODO: SET State of interviewee
              }}
            />
          </Stack>
        )
      ) : (
        <Loader />
      )}
    </Modal>
  )
}

export default AcceptApplicantModal
