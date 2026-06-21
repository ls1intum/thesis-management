import { Button, Group, Modal, Stack, Text } from '@mantine/core'
import { Sparkle } from '@phosphor-icons/react'
import DocumentEditor from '@/core/components/DocumentEditor/DocumentEditor'

interface IAbstractSuggestionModalProps {
  opened: boolean
  currentAbstract: string
  suggestion: string
  loading: boolean
  onConfirm: () => void
  onDeny: () => void
}

// Detect visible text by parsing the HTML and reading its text content, rather than stripping
// tags with a regex (which is incomplete and flagged as unsafe sanitization). The parsed document
// is detached and inert, so no scripts run.
const hasText = (html: string) =>
  (new DOMParser().parseFromString(html, 'text/html').body.textContent ?? '').trim().length > 0

/**
 * Shown right after an upload when an abstract was extracted that would replace the current one,
 * so the student explicitly confirms or denies the change rather than overlooking an inline hint.
 * Closing the modal denies the change and keeps the current abstract.
 */
const AbstractSuggestionModal = (props: IAbstractSuggestionModalProps) => {
  const { opened, currentAbstract, suggestion, loading, onConfirm, onDeny } = props

  return (
    <Modal
      opened={opened}
      onClose={onDeny}
      size='lg'
      closeOnClickOutside={false}
      title={
        <Group gap='xs'>
          <Sparkle size={18} />
          <Text fw={600}>Use the abstract extracted from your upload?</Text>
        </Group>
      }
    >
      <Stack>
        <Text size='sm'>
          We extracted this abstract from the document you just uploaded. Replace the current
          abstract with it, or keep the one you have.
        </Text>
        <DocumentEditor label='Extracted from your upload' value={suggestion} editMode={false} />
        {hasText(currentAbstract) && (
          <DocumentEditor
            label='Current abstract (kept if you decline)'
            value={currentAbstract}
            editMode={false}
          />
        )}
        <Group justify='flex-end'>
          <Button variant='default' color='gray' loading={loading} onClick={onDeny}>
            Keep current
          </Button>
          <Button loading={loading} onClick={onConfirm}>
            Use extracted abstract
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default AbstractSuggestionModal
