import { Anchor } from '@mantine/core'
import { ReactNode } from 'react'

const linkBuilders: Record<string, (value: string) => string | null> = {
  GITHUB: (value) => {
    const trimmed = value.trim().replace(/^@/, '')
    if (!trimmed) {
      return null
    }
    if (/^https?:\/\//i.test(trimmed)) {
      return trimmed
    }
    if (/^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$/.test(trimmed)) {
      return `https://github.com/${trimmed}`
    }
    return null
  },
}

export const renderCustomDataValue = (key: string, value: string): ReactNode => {
  const builder = linkBuilders[key]
  if (!builder || !value) {
    return value
  }

  const href = builder(value)
  if (!href) {
    return value
  }

  return (
    <Anchor href={href} target='_blank' rel='noreferrer'>
      {value}
    </Anchor>
  )
}
