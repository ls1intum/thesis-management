import AuthenticatedArea from '../../app/layout/AuthenticatedArea/AuthenticatedArea'
import ContentContainer from '../../app/layout/ContentContainer/ContentContainer'
import React from 'react'
import { usePageTitle } from '../../hooks/theme'

const MyInformationPage = () => {
  // TODO: implement component
  usePageTitle('My Information')

  return (
    <AuthenticatedArea>
      <ContentContainer>
        <h1>My Information</h1>
      </ContentContainer>
    </AuthenticatedArea>
  )
}

export default MyInformationPage
