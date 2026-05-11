import { Anchor, Text, Title } from '@mantine/core'
import DOMPurify from 'dompurify'
import { usePageTitle } from '../../hooks/theme'
import { useEffect, useState } from 'react'
import { useAuthenticationContext } from '../../hooks/authentication'
import { Link } from 'react-router'

const PrivacyPage = () => {
  usePageTitle('Privacy')

  const [content, setContent] = useState('')
  const auth = useAuthenticationContext()

  useEffect(() => {
    const controller = new AbortController()
    fetch('/privacy.html', { signal: controller.signal })
      .then((res) => res.text())
      .then((res) => setContent(res))
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return
        console.warn('Failed to load privacy content', err)
      })
    return () => controller.abort()
  }, [])

  return (
    <div>
      <Title mb='md'>Privacy</Title>
      {/* eslint-disable-next-line @eslint-react/dom-no-dangerously-set-innerhtml -- content is sanitized via DOMPurify on the line below */}
      <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(content) }} />
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
