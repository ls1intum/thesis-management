import type { IThesis } from '@/thesis/requests/responses/thesis'
import { ThesisState } from '@/thesis/requests/responses/thesis'
import { Accordion, Center, Group, Stack, Text } from '@mantine/core'
import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import ConfirmationButton from '@/core/components/ConfirmationButton/ConfirmationButton'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import { ApiError, getApiResponseErrorMessage } from '@/core/requests/handler'
import { formatThesisFilename } from '@/core/utils/format'
import ThesisFeedbackRequestButton from '@/thesis/pages/ThesisPage/components/ThesisFeedbackRequestButton/ThesisFeedbackRequestButton'
import ThesisFeedbackOverview from '@/thesis/pages/ThesisPage/components/ThesisFeedbackOverview/ThesisFeedbackOverview'
import ThesisAIFeedbackButton from '@/thesis/pages/ThesisPage/components/ThesisAIFeedbackButton/ThesisAIFeedbackButton'
import { AuthenticatedFilePreview } from '@/core/components/AuthenticatedFilePreview/AuthenticatedFilePreview'
import { UploadFileButton } from '@/core/components/UploadFileButton/UploadFileButton'
import FileHistoryTable from '@/thesis/pages/ThesisPage/components/FileHistoryTable/FileHistoryTable'
import { checkMinimumThesisState, isThesisClosed } from '@/thesis/utils/thesis'

const ThesisProposalSection = () => {
  const { thesis, access, updateThesis } = useLoadedThesisContext()

  const [accepting, onAccept] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/proposal/accept`, {
      method: 'PUT',
      requiresAuth: true,
    })

    if (response.ok) {
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Proposal accepted successfully')

  const onUpload = async (file: File) => {
    const formData = new FormData()

    formData.append('proposal', file)

    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/proposal`, {
      method: 'POST',
      requiresAuth: true,
      formData: formData,
    })

    if (response.ok) {
      showSimpleSuccess('Proposal uploaded successfully')

      updateThesis(response.data)
    } else {
      showSimpleError(getApiResponseErrorMessage(response))
    }
  }

  const proposals = thesis.proposals ?? []
  const proposal = proposals[0]

  if (!checkMinimumThesisState(thesis, ThesisState.PROPOSAL)) {
    return <></>
  }

  // Hide the section entirely when the thesis has moved past the proposal phase
  // but no proposal was ever uploaded (i.e. the thesis skipped this step).
  if (thesis.state !== ThesisState.PROPOSAL && proposals.length === 0) {
    return <></>
  }

  return (
    <Accordion
      variant='separated'
      defaultValue={thesis.state === ThesisState.PROPOSAL ? 'open' : ''}
    >
      <Accordion.Item value='open'>
        <Accordion.Control>Proposal</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            {proposal ? (
              <AuthenticatedFilePreview
                url={`/v2/theses/${thesis.thesisId}/proposal/${proposal.proposalId}`}
                filename={formatThesisFilename(
                  thesis,
                  'Proposal',
                  proposal.filename,
                  proposals.length,
                )}
                type='pdf'
                aspectRatio={16 / 6}
                actionButton={
                  ((access.student && thesis.state === ThesisState.PROPOSAL) ||
                    access.supervisor) &&
                  !isThesisClosed(thesis) ? (
                    <UploadFileButton
                      onUpload={onUpload}
                      maxSize={25 * 1024 * 1024}
                      accept='pdf'
                      ml='auto'
                    >
                      Upload Proposal
                    </UploadFileButton>
                  ) : undefined
                }
                key={proposal.proposalId}
              />
            ) : (
              <Stack>
                <Text ta='center'>No proposal uploaded yet</Text>
                <Center>
                  <UploadFileButton onUpload={onUpload} maxSize={25 * 1024 * 1024} accept='pdf'>
                    Upload Proposal
                  </UploadFileButton>
                </Center>
              </Stack>
            )}
            <ThesisFeedbackOverview
              type='PROPOSAL'
              allowEdit={thesis.state === ThesisState.PROPOSAL}
            />
            {access.student && (
              <FileHistoryTable
                data={proposals.map((row, index) => ({
                  filename: formatThesisFilename(
                    thesis,
                    'Proposal',
                    row.filename,
                    proposals.length - index,
                  ),
                  url: `/v2/theses/${thesis.thesisId}/proposal/${row.proposalId}`,
                  type: 'pdf',
                  uploadedBy: row.createdBy,
                  uploadedAt: row.createdAt,
                  name: `Proposal v${proposals.length - index}`,
                  onDelete:
                    access.supervisor && !isThesisClosed(thesis)
                      ? async () => {
                          const response = await doRequest<IThesis>(
                            `/v2/theses/${thesis.thesisId}/proposal/${row.proposalId}`,
                            {
                              method: 'DELETE',
                              requiresAuth: true,
                            },
                          )

                          if (response.ok) {
                            updateThesis(response.data)
                          } else {
                            showSimpleError(getApiResponseErrorMessage(response))
                          }
                        }
                      : undefined,
                }))}
              />
            )}
            <Group ml='auto'>
              {proposal && access.student && thesis.state === ThesisState.PROPOSAL && (
                <ThesisAIFeedbackButton type='PROPOSAL' />
              )}
              {proposal && access.supervisor && thesis.state === ThesisState.PROPOSAL && (
                <ThesisFeedbackRequestButton type='PROPOSAL' />
              )}
              {access.supervisor && thesis.state === ThesisState.PROPOSAL && (
                <ConfirmationButton
                  confirmationTitle='Accept Proposal'
                  confirmationText='Are you sure you want to accept the proposal?'
                  variant='outline'
                  color='green'
                  loading={accepting}
                  disabled={!proposal}
                  onClick={onAccept}
                >
                  Accept Proposal
                </ConfirmationButton>
              )}
            </Group>
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}

export default ThesisProposalSection
