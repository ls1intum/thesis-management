import { Card, Stack, Text, Divider, Title, Group } from '@mantine/core'
import { ITopic } from '../../../requests/responses/topic'
import { Buildings, CalendarBlank, Clock, GraduationCap, Users } from 'phosphor-react'
import TopicAdittionalInformationSection from './TopicAdditionalInformationSection'
import ThesisTypeBadge from '../../LandingPage/components/ThesisTypBadge/ThesisTypBadge'
import AvatarUserList from '../../../components/AvatarUserList/AvatarUserList'
import { formatDate } from '../../../utils/format'

interface ITopicAdittionalInformationCardProps {
  topic: ITopic
}

const TopicAdittionalInformationCard = ({ topic }: ITopicAdittionalInformationCardProps) => {
  const iconSize = 20

  return (
    <Card withBorder shadow={'xs'} radius='md' h='100%' w='100%' bg={'gray.0'}>
      <Stack gap={'1rem'}>
        <TopicAdittionalInformationSection
          icon={<Buildings size={iconSize} />}
          title='Research Group'
          content={<Text>{topic.researchGroup.name}</Text>}
        />
        <Divider />
        <TopicAdittionalInformationSection
          icon={<Clock size={iconSize} />}
          title='Dates'
          content={
            <Stack gap={'0.5rem'}>
              <Title order={6} c={'gray.7'}>
                Created At
              </Title>
              <Group gap={'0.5rem'}>
                <CalendarBlank size={16} />
                <Text>{formatDate(topic.createdAt, { withTime: false })}</Text>
              </Group>
            </Stack>
          }
        />
        <Divider />
        <TopicAdittionalInformationSection
          icon={<GraduationCap size={iconSize} />}
          title='Thesis Types'
          content={
            <Stack gap={'0.5rem'}>
              {(topic.thesisTypes ?? []).length > 0 ? (
                (topic.thesisTypes ?? []).map((type) => <ThesisTypeBadge key={type} type={type} />)
              ) : (
                <ThesisTypeBadge type='Any' key={'any'} />
              )}
            </Stack>
          }
        />
        <Divider />
        <TopicAdittionalInformationSection
          icon={<Users size={iconSize} />}
          title='Supervisors'
          content={<AvatarUserList users={topic.supervisors} />}
        />
        <Divider />
        <TopicAdittionalInformationSection
          icon={<Users size={iconSize} />}
          title='Advisors'
          content={<AvatarUserList users={topic.advisors} />}
        />
      </Stack>
    </Card>
  )
}

export default TopicAdittionalInformationCard
