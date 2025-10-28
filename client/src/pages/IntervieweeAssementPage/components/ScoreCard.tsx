import { Card, SegmentedControl, Stack, Title, Text } from '@mantine/core'
import { createScoreLabel, scoreColorTranslate } from '../../../utils/format'

interface IScoreCardProps {
  score: number | null
  onScoreChange?: (newScore: number) => void
}

const ScoreCard = ({ score, onScoreChange }: IScoreCardProps) => {
  //TODO: Think about better design for each label
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

  return (
    <Card withBorder radius='md'>
      <Stack>
        <Title order={4}>Score</Title>
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
        ></SegmentedControl>
      </Stack>
    </Card>
  )
}

export default ScoreCard
