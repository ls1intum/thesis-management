import { Anchor, Text, Title } from '@mantine/core'
import { usePageTitle } from '../../hooks/theme'
import { useEffect, useState } from 'react'
import { useAuthenticationContext } from '../../hooks/authentication'
import { Link } from 'react-router'

const PrivacyPage = () => {
  usePageTitle('Privacy')

  const [content, setContent] = useState('')
  const auth = useAuthenticationContext()

  useEffect(() => {
    fetch('/privacy.html')
      .then((res) => res.text())
      .then((res) => setContent(res))
  }, [])

  return (
    <div>
      <Title mb='md'>Privacy</Title>
      <div dangerouslySetInnerHTML={{ __html: content }} />
      {auth.isAuthenticated && (
        <div style={{ marginTop: '2rem' }}>
          <Title order={3} mb='xs'>
            Your Data
          </Title>
          <Text>
            You can request an export of all your personal data stored in the system.{' '}
            <Anchor component={Link} to='/data-export'>
              Go to Data Export
            </Anchor>
          </Text>
        </div>
      )}
    </div>
  )
}

export default PrivacyPage
