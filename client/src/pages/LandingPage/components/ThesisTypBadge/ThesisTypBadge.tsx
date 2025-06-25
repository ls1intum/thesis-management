import { Group, Box, Text } from '@mantine/core'
import { formatThesisType } from '../../../../utils/format'

interface ThesisTypeBadgeProps {
  type: string
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

const ThesisTypeBadge = ({ type }: ThesisTypeBadgeProps) => (
  <Group key={type} gap={3} wrap='nowrap'>
    <Box w={15} h={15} style={{ borderRadius: '50%' }} bg={getTypeColor(type)} />
    <Text size='sm'>
      {type.toLowerCase() === 'interdisciplinary_project'
        ? formatThesisType(type, true)
        : formatThesisType(type)}
    </Text>
  </Group>
)

export default ThesisTypeBadge
