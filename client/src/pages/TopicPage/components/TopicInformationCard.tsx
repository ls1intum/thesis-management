import { Card, Stack, Title, Text, TypographyStylesProvider, Divider } from '@mantine/core'
import DocumentEditor from '../../../components/DocumentEditor/DocumentEditor'

interface ITopicInformationCardProps {
  title: string
  content: string
}

const TopicInformationCard = ({ title, content }: ITopicInformationCardProps) => {
  return (
    <Card withBorder shadow={'xs'} radius='md' h='100%' w='100%' px={0} pt={'1rem'} pb={0}>
      <Stack gap={0} p={0}>
        <Title order={3} px={'1rem'}>
          {title}
        </Title>
        <DocumentEditor value={content} noBorder />
      </Stack>
    </Card>
  )
}

export default TopicInformationCard
