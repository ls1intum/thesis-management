import { useThesisCommentsContext } from '../../providers/ThesisCommentsProvider/hooks'
import { Center, Divider, Pagination, Skeleton, Stack, Text, Title } from '@mantine/core'
import { NoteIcon } from '@phosphor-icons/react/dist/ssr'
import ThesisCommentElement from './components/ThesisCommentElement'

const ThesisCommentsList = () => {
  const { thesis, comments, deleteComment, limit, page, setPage } = useThesisCommentsContext()

  return (
    <Stack pb={'0.5rem'}>
      {!comments &&
        Array.from(Array(limit).keys()).map((index) => <Skeleton key={index} height={50} />)}
      {comments && comments.content.length === 0 && (
        <Stack align='center' justify='center' py='lg' gap={'0.5rem'}>
          <NoteIcon size={48} color='gray' />
          <Stack gap={'0.25rem'} align='center'>
            <Title order={5}>No comments yet</Title>
            <Text ta='center' variant='dimmed' size='sm'>
              Be the first to add a comment to this thesis.
            </Text>
          </Stack>
        </Stack>
      )}
      <Stack gap={'1rem'}>
        {comments &&
          comments.content.map((comment, idx) => (
            <>
              <ThesisCommentElement
                key={comment.commentId}
                comment={comment}
                thesisId={thesis.thesisId}
                deleteComment={deleteComment}
              />
              {idx < comments.content.length - 1 && <Divider />}
            </>
          ))}
      </Stack>
      {comments && comments.totalPages > 1 && (
        <Center>
          <Pagination
            value={page + 1}
            onChange={(x) => setPage(x - 1)}
            total={comments.totalPages}
            siblings={2}
          />
        </Center>
      )}
    </Stack>
  )
}

export default ThesisCommentsList
