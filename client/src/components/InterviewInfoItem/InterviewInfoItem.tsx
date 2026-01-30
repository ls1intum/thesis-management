import { Group, Text } from '@mantine/core'
import { normalizeUrl } from '../../utils/format'

interface IInterviewInfoItemProps {
  icon: React.ReactNode
  text: string
  link?: string
  truncate?: boolean
}

const InterviewInfoItem = ({ icon, text, link, truncate = false }: IInterviewInfoItemProps) => {
  return (
    <Group gap={'0.25rem'} wrap={truncate ? 'nowrap' : 'wrap'} align='center'>
      {icon}
      {link ? (
        <a
          href={normalizeUrl(link)}
          target='_blank'
          rel='noopener noreferrer'
          style={{ color: 'inherit' }}
        >
          <Text c='dimmed' size='xs' truncate={truncate}>
            {text}
          </Text>
        </a>
      ) : (
        <Text c='dimmed' size='xs' truncate={truncate}>
          {text}
        </Text>
      )}
    </Group>
  )
}

export default InterviewInfoItem
