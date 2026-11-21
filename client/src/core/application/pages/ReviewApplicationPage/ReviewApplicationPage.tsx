import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import type { IApplication } from '@/core/application/requests/responses/application'
import { ApplicationState } from '@/core/application/requests/responses/application'
import { doRequest } from '@/core/requests/request'
import ApplicationsProvider from '@/core/application/providers/ApplicationsProvider/ApplicationsProvider'
import { Grid, Text, Center } from '@mantine/core'
import ApplicationsSidebar from '@/core/application/pages/ReviewApplicationPage/components/ApplicationsSidebar/ApplicationsSidebar'
import ApplicationReviewBody from '@/core/application/pages/ReviewApplicationPage/components/ApplicationReviewBody/ApplicationReviewBody'
import { useIsSmallerBreakpoint, usePageTitle } from '@/core/hooks/theme'
import ApplicationModal from '@/core/application/components/ApplicationModal/ApplicationModal'

const ReviewApplicationPage = () => {
  const navigate = useNavigate()
  const { applicationId } = useParams<{ applicationId: string }>()

  usePageTitle('Review Applications')

  const isSmallScreen = useIsSmallerBreakpoint('md')

  const [application, setApplication] = useState<IApplication>()

  useEffect(() => {
    if (applicationId) {
      return doRequest<IApplication>(
        `/v2/applications/${applicationId}`,
        {
          method: 'GET',
          requiresAuth: true,
        },
        (res) => {
          if (res.ok) {
            setApplication(res.data)
          }
        },
      )
    }
  }, [applicationId])

  return (
    <ApplicationsProvider
      fetchAll={true}
      limit={10}
      defaultStates={[ApplicationState.NOT_ASSESSED]}
      showOnlyAssignedTopics={true}
    >
      {isSmallScreen && (
        <ApplicationModal
          application={application}
          onClose={() => {
            void navigate('/applications', { replace: true })

            setApplication(undefined)
          }}
          allowReviews={true}
          onUpdate={(newApplication) => {
            setApplication(newApplication)
          }}
        />
      )}
      <Grid>
        <Grid.Col span={{ md: 3 }}>
          <ApplicationsSidebar
            selected={application}
            isSmallScreen={isSmallScreen}
            onSelect={(newApplication) => {
              void navigate(`/applications/${newApplication.applicationId}`, {
                replace: true,
              })

              setApplication(newApplication)
            }}
          />
        </Grid.Col>
        {!isSmallScreen && (
          <Grid.Col span={{ md: 9 }}>
            {application ? (
              <ApplicationReviewBody
                application={application}
                onChange={setApplication}
                onDelete={() => {
                  setApplication(undefined)
                  void navigate('/applications', { replace: true })
                }}
              />
            ) : (
              <Center h='80vh'>
                <Text ta='center' fw='bold'>
                  No Application selected
                </Text>
              </Center>
            )}
          </Grid.Col>
        )}
      </Grid>
    </ApplicationsProvider>
  )
}

export default ReviewApplicationPage
