import { Stack } from '@mantine/core'
import { IInterviewSlot, IUpcomingInterview } from '../../requests/responses/interview'
import InterviewInfoItem from '../InterviewInfoItem/InterviewInfoItem'
import { CalendarBlankIcon, ClockIcon, MapPinIcon, WebcamIcon } from '@phosphor-icons/react'

interface IInterviewSlotInformationProps {
  slot: IInterviewSlot
}

const InterviewSlotInformation = ({ slot }: IInterviewSlotInformationProps) => {
  return (
    <Stack gap={'0.25rem'}>
      <InterviewInfoItem
        icon={<CalendarBlankIcon color='gray' />}
        text={new Date(slot.startDate).toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        })}
      />

      <InterviewInfoItem
        icon={<ClockIcon color='gray' />}
        text={`${new Date(slot.startDate).toLocaleString(undefined, {
          hour: 'numeric',
          minute: 'numeric',
        })} - ${new Date(slot.endDate).toLocaleString(undefined, {
          hour: 'numeric',
          minute: 'numeric',
        })}`}
      />

      {slot.location && (
        <InterviewInfoItem icon={<MapPinIcon color='gray' />} text={slot.location} />
      )}
      {slot.streamUrl && (
        <InterviewInfoItem
          icon={<WebcamIcon color='gray' />}
          text='Virtual'
          link={slot.streamUrl}
        />
      )}
    </Stack>
  )
}

export default InterviewSlotInformation
