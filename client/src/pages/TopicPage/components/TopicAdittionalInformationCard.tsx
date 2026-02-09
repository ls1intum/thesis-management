import { Card, Stack, Text, Divider, useMantineColorScheme } from '@mantine/core'
import { ITopic } from '../../../requests/responses/topic'
import { Buildings, Clock, GraduationCapIcon, Users } from '@phosphor-icons/react'
import TopicAdittionalInformationSection from './TopicAdditionalInformationSection'
import ThesisTypeBadge from '../../LandingPage/components/ThesisTypBadge/ThesisTypBadge'
import AvatarUserList from '../../../components/AvatarUserList/AvatarUserList'
import DateItemAdditionalInformation from './DateItemAdditonalInformation'

interface ITopicAdittionalInformationCardProps {
  topic: ITopic
}

const TopicAdittionalInformationCard = ({ topic }: ITopicAdittionalInformationCardProps) => {
  const iconSize = 20

  const { colorScheme } = useMantineColorScheme()

  return (
    <Card
      withBorder
      shadow={'xs'}
      radius='md'
      h='100%'
      w='100%'
      bg={colorScheme === 'dark' ? 'dark.6' : 'gray.1'}
    >
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
            <Stack gap={'1rem'}>
              <DateItemAdditionalInformation label='Created At' date={topic.createdAt} />
              {topic.intendedStart && (
                <DateItemAdditionalInformation label='Intended Start' date={topic.intendedStart} />
              )}
              {topic.applicationDeadline && (
                <DateItemAdditionalInformation
                  label='Application Deadline'
                  date={topic.applicationDeadline}
                />
              )}
            </Stack>
          }
        />
        <Divider />
        <TopicAdittionalInformationSection
          icon={<GraduationCapIcon size={iconSize} />}
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
          title='Examiners'
          content={<AvatarUserList users={topic.supervisors} />}
        />
        <Divider />
        <TopicAdittionalInformationSection
          icon={<Users size={iconSize} />}
          title='Supervisors'
          content={<AvatarUserList users={topic.advisors} />}
        />
      </Stack>
    </Card>
  )
}

export default TopicAdittionalInformationCard
