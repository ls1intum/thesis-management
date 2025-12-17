import {
  Card,
  Divider,
  Group,
  Paper,
  Stack,
  Title,
  Text,
  useMantineColorScheme,
  Badge,
  Button,
} from '@mantine/core'
import {
  IIntervieweeLightWithNextSlot,
  InterviewState,
} from '../../../requests/responses/interview'
import CustomAvatar from '../../../components/CustomAvatar/CustomAvatar'
import {
  createScoreLabel,
  getInterviewStateColor,
  scoreColorTranslate,
} from '../../../utils/format'
import { Link } from 'react-router'
import InterviewSlotInformation from '../../../components/InterviewSlotInformation/InterviewSlotInformation'
import { PaperPlaneTiltIcon } from '@phosphor-icons/react'
import InviteConfirmationModal from './InviteConfirmationModal'
import { useState } from 'react'

interface IIntervieweeCardProps {
  interviewee: IIntervieweeLightWithNextSlot
  navigationLink: string
  flex?: number
  disableLink?: boolean
  highlightCard?: boolean
  inviteInterviewee?: () => void
}

const IntervieweeCard = ({
  interviewee,
  navigationLink,
  flex,
  disableLink,
  inviteInterviewee,
}: IIntervieweeCardProps) => {
  const checkState = () => {
    return interviewee.score && interviewee.score >= 0
      ? InterviewState.COMPLETED
      : interviewee.nextSlot != null
        ? InterviewState.SCHEDULED
        : interviewee.lastInvited != null
          ? InterviewState.INVITED
          : InterviewState.UNCONTACTED
  }

  const state: InterviewState = checkState()

  const colorScheme = useMantineColorScheme()

  const [inviteModalOpen, setInviteModalOpen] = useState(false)

  return (
    <Paper
      withBorder
      bg={scoreColorTranslate(interviewee.score ?? -1)}
      radius='md'
      component={disableLink ? undefined : Link}
      to={disableLink ? '' : navigationLink}
      flex={flex}
    >
      <Card p={0} ml={8} radius='md'>
        <Stack p={0} gap={0}>
          <Group px={'1.5rem'} py={'0.75rem'} align='center'>
            <Group gap={'0.75rem'} wrap={'nowrap'}>
              <CustomAvatar user={interviewee.user} size={32} />
              <Title order={5} lineClamp={1} miw={250}>
                {interviewee.user.firstName} {interviewee.user.lastName}
              </Title>
            </Group>
            <Group flex={1}>
              <Divider orientation='vertical' size={'sm'} />
              <Group flex={1} justify='end' align='center'>
                <Group flex={1} justify='start' align='center'>
                  {' '}
                  {interviewee.nextSlot && <InterviewSlotInformation slot={interviewee.nextSlot} />}
                </Group>
                {interviewee.score !== null && interviewee.score >= 0 && (
                  <Badge color={scoreColorTranslate(interviewee.score)} autoContrast radius={'sm'}>
                    {createScoreLabel(interviewee.score)}
                  </Badge>
                )}
              </Group>
            </Group>
          </Group>
          <Divider />
          <Group px={'1.5rem'} py={'0.75rem'} justify='space-between' align='center'>
            <Group gap={'0.5rem'}>
              <Divider orientation='vertical' size='lg' color={getInterviewStateColor(state)} />
              <Stack gap={0}>
                <Text
                  size='sm'
                  fw={500}
                  c={colorScheme.colorScheme === 'dark' ? 'gray.3' : 'gray.7'}
                >
                  {state}
                </Text>
              </Stack>
            </Group>
            <Group>
              {(state === InterviewState.UNCONTACTED || state === InterviewState.INVITED) && (
                <Button
                  variant='outline'
                  size='xs'
                  leftSection={<PaperPlaneTiltIcon size={16} />}
                  onClick={inviteInterviewee}
                >
                  {state === InterviewState.UNCONTACTED ? 'Invite' : 'Re-invite'}
                </Button>
              )}
            </Group>
          </Group>
        </Stack>
      </Card>
      <InviteConfirmationModal
        inviteModalOpen={inviteModalOpen}
        setInviteModalOpen={setInviteModalOpen}
        interviewees={[interviewee.user]}
        sendInvite={inviteInterviewee}
      />
    </Paper>
  )
}
export default IntervieweeCard
