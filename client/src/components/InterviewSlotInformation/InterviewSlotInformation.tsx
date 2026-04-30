import { Stack } from '@mantine/core'
import { IInterviewSlot } from '../../requests/responses/interview'
import InterviewInfoItem from '../InterviewInfoItem/InterviewInfoItem'
import { CalendarBlankIcon, ClockIcon, MapPinIcon, WebcamIcon } from '@phosphor-icons/react'
import { formatDate, formatTime } from '../../utils/format'

interface IInterviewSlotInformationProps {
  slot: IInterviewSlot
}

const InterviewSlotInformation = ({ slot }: IInterviewSlotInformationProps) => {
  return (
    <Stack gap={'0.25rem'}>
      <InterviewInfoItem
        icon={<CalendarBlankIcon color='gray' />}
        text={formatDate(slot.startDate)}
      />

      <InterviewInfoItem
        icon={<ClockIcon color='gray' />}
        text={`${formatTime(slot.startDate)} - ${formatTime(slot.endDate)}`}
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
