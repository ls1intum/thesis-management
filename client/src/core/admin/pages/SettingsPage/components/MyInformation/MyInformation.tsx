import { showSimpleSuccess } from '@/core/utils/notification'
import UserInformationForm from '@/core/user/components/UserInformationForm/UserInformationForm'
import React from 'react'
import { usePageTitle } from '@/core/hooks/theme'

const MyInformation = () => {
  usePageTitle('My Information')

  return (
    <UserInformationForm
      requireCompletion={false}
      includeAvatar={true}
      onComplete={() => showSimpleSuccess('You successfully updated your profile')}
    />
  )
}

export default MyInformation
