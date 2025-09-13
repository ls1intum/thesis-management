import { Title } from '@mantine/core'
import { usePageTitle } from '../../hooks/theme'
import { useEffect, useState } from 'react'

const ImprintPage = () => {
  usePageTitle('Imprint')

  const [content, setContent] = useState('')

  useEffect(() => {
    fetch('/imprint.html')
      .then((res) => res.text())
      .then((res) => setContent(res))
  }, [])

  return (
    <div>
      <Title mb='md'>Imprint</Title>
      <div dangerouslySetInnerHTML={{ __html: content }} />
    </div>
  )
}

export default ImprintPage
