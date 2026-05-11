import { Title } from '@mantine/core'
import DOMPurify from 'dompurify'
import { usePageTitle } from '../../hooks/theme'
import { useEffect, useState } from 'react'

const ImprintPage = () => {
  usePageTitle('Imprint')

  const [content, setContent] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    fetch('/imprint.html', { signal: controller.signal })
      .then((res) => res.text())
      .then((res) => setContent(res))
      .catch(() => {
        /* aborted or failed; leave content empty */
      })
    return () => controller.abort()
  }, [])

  return (
    <div>
      <Title mb='md'>Imprint</Title>
      {/* eslint-disable-next-line @eslint-react/dom-no-dangerously-set-innerhtml -- content is sanitized via DOMPurify on the line below */}
      <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(content) }} />
    </div>
  )
}

export default ImprintPage
