import { Alert, Button, Group, Stack, Text } from '@mantine/core'
import { Sparkle } from '@phosphor-icons/react'
import DocumentEditor from '@/core/components/DocumentEditor/DocumentEditor'

interface IAbstractSuggestionBannerProps {
  suggestion: string
  loading: boolean
  onUse: () => void
  onEdit: () => void
  onDismiss: () => void
}

/**
 * Shows an abstract that was automatically extracted from the uploaded PDF when we could not
 * fill it with full confidence, letting the student accept it, edit it, or dismiss it.
 */
const AbstractSuggestionBanner = (props: IAbstractSuggestionBannerProps) => {
  const { suggestion, loading, onUse, onEdit, onDismiss } = props

  return (
    <Alert
      variant='light'
      color='cyan'
      icon={<Sparkle size={16} />}
      title='Suggested abstract from your uploaded PDF'
    >
      <Stack gap='sm'>
        <Text size='sm'>
          We extracted this abstract from the document you uploaded. Review it and choose whether to
          use it for this thesis.
        </Text>
        <DocumentEditor value={suggestion} editMode={false} />
        <Group>
          <Button size='xs' loading={loading} onClick={onUse}>
            Use this
          </Button>
          <Button size='xs' variant='default' onClick={onEdit}>
            Edit
          </Button>
          <Button size='xs' variant='subtle' color='gray' loading={loading} onClick={onDismiss}>
            Dismiss
          </Button>
        </Group>
      </Stack>
    </Alert>
  )
}

export default AbstractSuggestionBanner
