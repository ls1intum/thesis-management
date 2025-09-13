import { Title } from '@mantine/core'
import { usePageTitle } from '../../hooks/theme'
import { useEffect, useState } from 'react'

const PrivacyPage = () => {
  usePageTitle('Privacy')

  const [content, setContent] = useState('')

  useEffect(() => {
    fetch('/privacy.html')
      .then((res) => res.text())
      .then((res) => setContent(res))
  }, [])

  return (
    <div>
      <Title mb='md'>Privacy</Title>
      <div dangerouslySetInnerHTML={{ __html: content }} />
    </div>
  )
}

export default PrivacyPage
