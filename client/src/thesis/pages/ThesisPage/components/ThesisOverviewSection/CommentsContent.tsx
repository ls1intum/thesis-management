import { Stack } from '@mantine/core'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import ThesisCommentsProvider from '@/thesis/providers/ThesisCommentsProvider/ThesisCommentsProvider'
import ThesisCommentsList from '@/thesis/components/ThesisCommentsList/ThesisCommentsList'
import ThesisCommentsForm from '@/thesis/components/ThesisCommentsForm/ThesisCommentsForm'

const CommentsContent = () => {
  const { thesis } = useLoadedThesisContext()

  return (
    <Stack>
      <ThesisCommentsProvider limit={10} thesis={thesis} commentType='SUPERVISOR'>
        <ThesisCommentsList />
        <ThesisCommentsForm />
      </ThesisCommentsProvider>
    </Stack>
  )
}

export default CommentsContent
