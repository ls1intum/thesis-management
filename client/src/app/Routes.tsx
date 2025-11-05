import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import AuthenticatedArea from './layout/AuthenticatedArea/AuthenticatedArea'
import PageLoader from '../components/PageLoader/PageLoader'
import PublicArea from './layout/PublicArea/PublicArea'
import { useAuthenticationContext } from '../hooks/authentication'
import { useIsSmallerBreakpoint } from '../hooks/theme'

const NotFoundPage = lazy(() => import('../pages/NotFoundPage/NotFoundPage'))
const PrivacyPage = lazy(() => import('../pages/PrivacyPage/PrivacyPage'))
const ImprintPage = lazy(() => import('../pages/ImprintPage/ImprintPage'))
const AboutPage = lazy(() => import('../pages/AboutPage/AboutPage'))
const ThesisOverviewPage = lazy(() => import('../pages/ThesisOverviewPage/ThesisOverviewPage'))
const ResearchGroupAdminPage = lazy(
  () => import('../pages/ResearchGroupAdminPage/ResearchGroupAdminPage'),
)

const PresentationOverviewPage = lazy(
  () => import('../pages/PresentationOverviewPage/PresentationOverviewPage'),
)

const ResearchGroupSettingPage = lazy(
  () => import('../pages/ResearchGroupSettingPage/ResearchGroupSettingPage'),
)
const BrowseThesesPage = lazy(() => import('../pages/BrowseThesesPage/BrowseThesesPage'))
const DashboardPage = lazy(() => import('../pages/DashboardPage/DashboardPage'))
const LogoutPage = lazy(() => import('../pages/LogoutPage/LogoutPage'))
const SettingsPage = lazy(() => import('../pages/SettingsPage/SettingsPage'))
const ReplaceApplicationPage = lazy(
  () => import('../pages/ReplaceApplicationPage/ReplaceApplicationPage'),
)
const ManageTopicsPage = lazy(() => import('../pages/ManageTopicsPage/ManageTopicsPage'))
const TopicPage = lazy(() => import('../pages/TopicPage/TopicPage'))
const PresentationPage = lazy(() => import('../pages/PresentationPage/PresentationPage'))
const ReviewApplicationPage = lazy(
  () => import('../pages/ReviewApplicationPage/ReviewApplicationPage'),
)
const ThesisPage = lazy(() => import('../pages/ThesisPage/ThesisPage'))
const LandingPage = lazy(() => import('../pages/LandingPage/LandingPage'))

const InterviewOverviewPage = lazy(
  () => import('../pages/InterviewOverviewPage/InterviewOverviewPage'),
)
const InterviewTopicOverviewPage = lazy(
  () => import('../pages/InterviewTopicOverviewPage/InterviewTopicOverviewPage'),
)
const IntervieweeAssesmentPage = lazy(
  () => import('../pages/IntervieweeAssementPage/IntervieweeAssesmentPage'),
)
const InterviewBookingPage = lazy(
  () => import('../pages/InterviewBookingPage/InterviewBookingPage'),
)

const AppRoutes = () => {
  const auth = useAuthenticationContext()
  const isSmaller = useIsSmallerBreakpoint('md')

  const publicBreakpointSize = '100rem'

  return (
    <Suspense fallback={<PageLoader />}>
      <BrowserRouter>
        <Routes>
          <Route
            path='/management/thesis-applications/:applicationId?'
            element={<Navigate to='/applications' replace />}
          />
          <Route path='/applications/thesis' element={<Navigate to='/' replace />} />
          <Route
            path='/dashboard'
            element={
              <AuthenticatedArea>
                <DashboardPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/settings/:tab?'
            element={
              <AuthenticatedArea>
                <SettingsPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/submit-application/:topicId?'
            element={
              <AuthenticatedArea>
                <ReplaceApplicationPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/edit-application/:applicationId'
            element={
              <AuthenticatedArea>
                <ReplaceApplicationPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/presentations'
            element={
              <AuthenticatedArea handleScrollInView={!isSmaller}>
                <PresentationOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/presentations/:presentationId'
            element={
              auth.isAuthenticated ? (
                <AuthenticatedArea>
                  <PresentationPage />
                </AuthenticatedArea>
              ) : (
                <PublicArea size={publicBreakpointSize}>
                  <PresentationPage />
                </PublicArea>
              )
            }
          />
          <Route
            path='/topics'
            element={
              <AuthenticatedArea requiredGroups={['admin', 'advisor', 'supervisor']}>
                <ManageTopicsPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/topics/:topicId'
            element={
              auth.isAuthenticated ? (
                <AuthenticatedArea>
                  <TopicPage />
                </AuthenticatedArea>
              ) : (
                <PublicArea size={publicBreakpointSize}>
                  <TopicPage />
                </PublicArea>
              )
            }
          />
          <Route
            path='/applications/:applicationId?'
            element={
              <AuthenticatedArea
                collapseNavigation={true}
                requiredGroups={['admin', 'advisor', 'supervisor']}
              >
                <ReviewApplicationPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/theses'
            element={
              <AuthenticatedArea>
                <BrowseThesesPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/theses/:thesisId'
            element={
              <AuthenticatedArea>
                <ThesisPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/overview'
            element={
              <AuthenticatedArea>
                <ThesisOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/research-groups'
            element={
              <AuthenticatedArea requiredGroups={['admin']}>
                <ResearchGroupAdminPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/research-groups/:researchGroupId'
            element={
              <AuthenticatedArea requiredGroups={['admin', 'group-admin']}>
                <ResearchGroupSettingPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interviews'
            element={
              <AuthenticatedArea
                requiredGroups={['admin', 'advisor', 'supervisor']}
                handleScrollInView={!isSmaller}
              >
                <InterviewOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interviews/:topicProcessId'
            element={
              <AuthenticatedArea requiredGroups={['admin', 'advisor', 'supervisor']}>
                <InterviewTopicOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interviews/:topicProcessId/interviewee/:intervieweeId'
            element={
              <AuthenticatedArea requiredGroups={['admin', 'advisor', 'supervisor']}>
                <IntervieweeAssesmentPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interview_booking/:processId'
            element={
              <PublicArea size={publicBreakpointSize} handleScrollInView={true}>
                <InterviewBookingPage />
              </PublicArea>
            }
          />
          <Route
            path='/about'
            element={
              <PublicArea size={publicBreakpointSize}>
                <AboutPage />
              </PublicArea>
            }
          />
          <Route
            path='/imprint'
            element={
              <PublicArea size={publicBreakpointSize}>
                <ImprintPage />
              </PublicArea>
            }
          />
          <Route
            path='/privacy'
            element={
              <PublicArea size={publicBreakpointSize}>
                <PrivacyPage />
              </PublicArea>
            }
          />
          <Route path='/logout' element={<LogoutPage />} />
          <Route
            path='/'
            element={
              <PublicArea size={publicBreakpointSize}>
                <LandingPage />
              </PublicArea>
            }
          />
          <Route
            path='/:researchGroupAbbreviation'
            element={
              <PublicArea size={publicBreakpointSize}>
                <LandingPage />
              </PublicArea>
            }
          />
          <Route path='*' element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </Suspense>
  )
}

export default AppRoutes
