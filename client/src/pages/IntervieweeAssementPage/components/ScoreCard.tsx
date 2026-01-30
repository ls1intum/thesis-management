import { Card, SegmentedControl, Stack, Title, Text, Group } from '@mantine/core'
import { createScoreLabel, scoreColorTranslate } from '../../../utils/format'
import DeleteButton from '../../../components/DeleteButton/DeleteButton'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'

interface IScoreCardProps {
  score: number | null
  onScoreChange?: (newScore: number) => void
  disabled?: boolean
}

const ScoreCard = ({ score, onScoreChange, disabled = false }: IScoreCardProps) => {
  function createScoreData(labelScore: number): { label: React.ReactNode; value: string } {
    return {
      label: (
        <Stack>
          <Text size='sm' c={labelScore === score ? 'gray.9' : undefined}>
            {createScoreLabel(labelScore)}
          </Text>
        </Stack>
      ),
      value: labelScore.toString(),
    }
  }

  const isSmallerDisplay = useIsSmallerBreakpoint('sm')

  return (
    <Card withBorder radius='md'>
      <Stack>
        <Group>
          <Title order={4} flex={1}>
            Score
          </Title>
          {score !== null && score >= 0 && (
            <DeleteButton onClick={() => onScoreChange?.(-1)}></DeleteButton>
          )}
        </Group>
        <SegmentedControl
          value={score?.toString() || ''}
          onChange={(value) => {
            onScoreChange?.(parseInt(value, 10))
          }}
          data={[
            createScoreData(0),
            createScoreData(1),
            createScoreData(2),
            createScoreData(3),
            createScoreData(4),
            createScoreData(5),
          ]}
          color={scoreColorTranslate(score)}
          radius={'md'}
          disabled={disabled}
          orientation={isSmallerDisplay ? 'vertical' : 'horizontal'}
          w={'100%'}
        ></SegmentedControl>
      </Stack>
    </Card>
  )
}

export default ScoreCard
