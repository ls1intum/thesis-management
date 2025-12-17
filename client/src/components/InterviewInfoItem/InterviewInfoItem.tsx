import { Group, Text } from '@mantine/core'

interface IInterviewInfoItemProps {
  icon: React.ReactNode
  text: string
  link?: string
}

const InterviewInfoItem = ({ icon, text, link }: IInterviewInfoItemProps) => {
  return (
    <Group gap={'0.25rem'}>
      {icon}
      {link ? (
        <a href={link} target='_blank' rel='noopener noreferrer' style={{ color: 'inherit' }}>
          <Text c='dimmed' size='xs'>
            {text}
          </Text>
        </a>
      ) : (
        <Text c='dimmed' size='xs'>
          {text}
        </Text>
      )}
    </Group>
  )
}

export default InterviewInfoItem
