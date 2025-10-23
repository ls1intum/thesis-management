import { Card, SegmentedControl, Stack, Title, Text } from '@mantine/core'

interface IScoreCardProps {
  score: number | null
  onScoreChange?: (newScore: number) => void
}

const ScoreCard = ({ score, onScoreChange }: IScoreCardProps) => {
  //TODO: MOVE TO FORMATTER FILE
  function scoreColorTranslate(score: number | null): string {
    switch (score) {
      case 1:
        return 'red.2'
      case 2:
        return 'orange.2'
      case 3:
        return 'yellow.2'
      case 4:
        return 'lime.2'
      case 5:
        return 'green.2'
      default:
        return 'gray.3'
    }
  }

  //TODO: MOVE TO FORMATTER FILE
  function createScoreLabel(score: number): string {
    switch (score) {
      case 0:
        return 'No Show'
      case 1:
        return 'Not a Fit'
      case 2:
        return 'Some Concerns'
      case 3:
        return 'Meets expectations'
      case 4:
        return 'Great Candidate'
      case 5:
        return 'Excelent'
      default:
        return 'No Score'
    }
  }

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
