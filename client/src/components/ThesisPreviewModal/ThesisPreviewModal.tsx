import { Button, Center, Loader, Modal, Stack, Text } from '@mantine/core'
import { Link } from 'react-router'
import ThesisData from '../ThesisData/ThesisData'
import { useThesis } from '../../hooks/fetcher'

interface IThesisPreviewModalProps {
  thesisId: string | undefined
  opened: boolean
  onClose: () => unknown
}

const ThesisPreviewModal = (props: IThesisPreviewModalProps) => {
  const { thesisId, opened, onClose } = props

  const thesis = useThesis(opened ? thesisId : undefined)

  return (
    <Modal
      title={thesis ? thesis.title : thesis === false ? 'Failed to load thesis' : 'Loading...'}
      opened={opened}
      onClose={onClose}
      size='xl'
    >
      {thesis === undefined && (
        <Center py='xl'>
          <Loader />
        </Center>
      )}
      {thesis === false && (
        <Center py='xl'>
          <Text c='dimmed'>Could not load thesis data.</Text>
        </Center>
      )}
      {thesis && (
        <Stack gap='md'>
          <ThesisData
            thesis={thesis}
            additionalInformation={['info', 'abstract', 'state', 'keywords', 'advisor-comments']}
          />
          <Button component={Link} to={`/theses/${thesis.thesisId}`} variant='outline' fullWidth>
            View All Details
          </Button>
        </Stack>
      )}
    </Modal>
  )
}

export default ThesisPreviewModal
