import { Group, Box, Text } from '@mantine/core'
import { formatThesisType } from '../../../../utils/format'

interface ThesisTypeBadgeProps {
  type: string
  textColor?: string
  fontWeight?: number | string
  textSize?: 'sm' | 'xs' | 'md' | 'lg' | 'xl' | (string & {}) | undefined
}

const getTypeColor = (type: string): string => {
  switch (type.toLowerCase()) {
    case 'bachelor':
      return 'indigo.3'
    case 'master':
      return 'pink.3'
    case 'guided_research':
      return 'yellow.3'
    case 'interdisciplinary_project':
      return 'lime.3'
    default:
      return 'gray.3'
  }
}

const getIndicatorSize = (
  size: 'sm' | 'xs' | 'md' | 'lg' | 'xl' | (string & {}) | undefined,
): number => {
  switch (size) {
    case 'xs':
      return 10
    case 'sm':
      return 15
    case 'md':
      return 17
    case 'lg':
      return 20
    case 'xl':
      return 24
    default:
      return 15
  }
}

const ThesisTypeBadge = ({
  type,
  textColor,
  fontWeight,
  textSize = 'sm',
}: ThesisTypeBadgeProps) => (
  <Group key={type} gap={'0.25rem'} wrap='nowrap'>
    <Box
      w={getIndicatorSize(textSize)}
      h={getIndicatorSize(textSize)}
      style={{ borderRadius: '50%' }}
      bg={getTypeColor(type)}
    />
    <Text size={textSize} c={textColor} fw={fontWeight}>
      {type.toLowerCase() === 'interdisciplinary_project'
        ? formatThesisType(type, true)
        : formatThesisType(type)}
    </Text>
  </Group>
)

export default ThesisTypeBadge
