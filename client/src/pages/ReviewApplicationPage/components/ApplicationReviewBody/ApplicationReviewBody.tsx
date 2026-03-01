import ApplicationData from '../../../../components/ApplicationData/ApplicationData'
import ApplicationReviewForm from '../../../../components/ApplicationReviewForm/ApplicationReviewForm'
import { Divider, Group, Stack } from '@mantine/core'
import React, { useEffect } from 'react'
import { IApplication } from '../../../../requests/responses/application'
import ApplicationRejectButton from '../../../../components/ApplicationRejectButton/ApplicationRejectButton'
import ApplicationDeleteButton from '../../../../components/ApplicationDeleteButton/ApplicationDeleteButton'

interface IApplicationReviewBodyProps {
  application: IApplication
  onChange: (application: IApplication) => unknown
  onDelete: () => void
}

const ApplicationReviewBody = (props: IApplicationReviewBodyProps) => {
  const { application, onChange, onDelete } = props

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [application.applicationId])

  return (
    <Stack>
      <ApplicationData
        application={application}
        rightTitleSection={
          <Group ml='auto' gap='xs'>
            <ApplicationDeleteButton
              key={`delete-${application.applicationId}`}
              application={application}
              onDelete={onDelete}
            />
            <ApplicationRejectButton
              key={`reject-${application.applicationId}`}
              application={application}
              onUpdate={(newApplication) => {
                onChange(newApplication)
              }}
            />
          </Group>
        }
        bottomSection={
          <Stack>
            <Divider />
            <ApplicationReviewForm
              application={application}
              onUpdate={(newApplication) => {
                onChange(newApplication)
              }}
            />
          </Stack>
        }
      />
    </Stack>
  )
}

export default ApplicationReviewBody
