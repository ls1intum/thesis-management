import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import AuthenticatedArea from '@/app/layout/AuthenticatedArea/AuthenticatedArea'
import PageLoader from '@/core/components/PageLoader/PageLoader'
import PublicArea from '@/app/layout/PublicArea/PublicArea'
import { useAuthenticationContext } from '@/core/hooks/authentication'
import { useIsSmallerBreakpoint } from '@/core/hooks/theme'
import PasskeyRegistrationPrompt from '@/core/components/PasskeyRegistrationPrompt/PasskeyRegistrationPrompt'

const NotFoundPage = lazy(() => import('@/app/pages/NotFoundPage/NotFoundPage'))
const PrivacyPage = lazy(() => import('@/app/pages/PrivacyPage/PrivacyPage'))
const ImprintPage = lazy(() => import('@/app/pages/ImprintPage/ImprintPage'))
const AboutPage = lazy(() => import('@/app/pages/AboutPage/AboutPage'))
const ThesisOverviewPage = lazy(
  () => import('@/thesis/pages/ThesisOverviewPage/ThesisOverviewPage'),
)
const ResearchGroupAdminPage = lazy(
  () => import('@/core/group/pages/ResearchGroupAdminPage/ResearchGroupAdminPage'),
)

const PresentationOverviewPage = lazy(
  () => import('@/presentation/pages/PresentationOverviewPage/PresentationOverviewPage'),
)

const ResearchGroupSettingPage = lazy(
  () => import('@/core/group/pages/ResearchGroupSettingPage/ResearchGroupSettingPage'),
)
const EmailTemplateEditPage = lazy(
  () =>
    import('@/core/group/pages/ResearchGroupSettingPage/components/EmailTemplateSettings/EmailTemplateEditPage'),
)
const BrowseThesesPage = lazy(() => import('@/thesis/pages/BrowseThesesPage/BrowseThesesPage'))
const DashboardPage = lazy(() => import('@/core/admin/pages/DashboardPage/DashboardPage'))
const LogoutPage = lazy(() => import('@/app/pages/LogoutPage/LogoutPage'))
const SettingsPage = lazy(() => import('@/core/admin/pages/SettingsPage/SettingsPage'))
const ReplaceApplicationPage = lazy(
  () => import('@/core/application/pages/ReplaceApplicationPage/ReplaceApplicationPage'),
)
const ManageTopicsPage = lazy(() => import('@/core/topic/pages/ManageTopicsPage/ManageTopicsPage'))
const TopicPage = lazy(() => import('@/core/topic/pages/TopicPage/TopicPage'))
const PresentationPage = lazy(
  () => import('@/presentation/pages/PresentationPage/PresentationPage'),
)
const ReviewApplicationPage = lazy(
  () => import('@/core/application/pages/ReviewApplicationPage/ReviewApplicationPage'),
)
const ThesisPage = lazy(() => import('@/thesis/pages/ThesisPage/ThesisPage'))
const ThesisConfigPage = lazy(() => import('@/thesis/pages/ThesisConfigPage/ThesisConfigPage'))
const LandingPage = lazy(() => import('@/app/pages/LandingPage/LandingPage'))

const AdminPage = lazy(() => import('@/core/admin/pages/AdminPage/AdminPage'))
const DependencyOverviewPage = lazy(
  () => import('@/core/admin/pages/DependencyOverviewPage/DependencyOverviewPage'),
)

const InterviewOverviewPage = lazy(
  () => import('@/interview/pages/InterviewOverviewPage/InterviewOverviewPage'),
)
const InterviewTopicOverviewPage = lazy(
  () => import('@/interview/pages/InterviewTopicOverviewPage/InterviewTopicOverviewPage'),
)
const IntervieweeAssesmentPage = lazy(
  () => import('@/interview/pages/IntervieweeAssementPage/IntervieweeAssesmentPage'),
)
const InterviewBookingPage = lazy(
  () => import('@/interview/pages/InterviewBookingPage/InterviewBookingPage'),
)
const DataExportPage = lazy(() => import('@/core/admin/pages/DataExportPage/DataExportPage'))

const AppRoutes = () => {
  const auth = useAuthenticationContext()
  const isSmaller = useIsSmallerBreakpoint('md')

  const publicBreakpointSize = '100rem'

  return (
    <Suspense fallback={<PageLoader />}>
      <BrowserRouter>
        <PasskeyRegistrationPrompt />
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
                <PublicArea size={publicBreakpointSize} hideUnauthenticatedActions={true}>
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
            path='/theses/:thesisId/configuration'
            element={
              <AuthenticatedArea>
                <ThesisConfigPage />
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
            path='/research-groups/:researchGroupId/email-templates/:templateCase/edit'
            element={
              <AuthenticatedArea requiredGroups={['admin', 'group-admin']}>
                <EmailTemplateEditPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/admin'
            element={
              <AuthenticatedArea requiredGroups={['admin']}>
                <AdminPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/admin/dependencies'
            element={
              <AuthenticatedArea requiredGroups={['admin']}>
                <DependencyOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interviews'
            element={
              <AuthenticatedArea
                requiredGroups={['advisor', 'supervisor']}
                handleScrollInView={!isSmaller}
              >
                <InterviewOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interviews/:processId'
            element={
              <AuthenticatedArea requiredGroups={['admin', 'advisor', 'supervisor']}>
                <InterviewTopicOverviewPage />
              </AuthenticatedArea>
            }
          />
          <Route
            path='/interviews/:processId/interviewee/:intervieweeId'
            element={
              <AuthenticatedArea
                requiredGroups={['admin', 'advisor', 'supervisor']}
                handleScrollInView={!isSmaller}
              >
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
            path='/data-export'
            element={
              <AuthenticatedArea>
                <DataExportPage />
              </AuthenticatedArea>
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
            path='/supervisor/:supervisorName'
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
