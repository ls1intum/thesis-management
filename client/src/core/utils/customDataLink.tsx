import { Anchor, Text } from '@mantine/core'
import type { ReactNode } from 'react'

// GitHub username rules (from the GitHub UI):
//   - 1-39 characters
//   - alphanumeric and hyphen
//   - no leading or trailing hyphen
//   - no consecutive hyphens
const GITHUB_USERNAME_RE = /^(?=.{1,39}$)[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$/

export const buildGithubUrl = (value: string): string | null => {
  const trimmed = value.trim().replace(/^@/, '')
  if (!trimmed) {
    return null
  }
  // Only accept full URLs that point at github.com so a student can't
  // turn this field into an arbitrary external link rendered with the
  // "Github" label to other users (supervisors viewing applications).
  try {
    const url = new URL(trimmed)
    if ((url.protocol === 'https:' || url.protocol === 'http:') && url.host === 'github.com') {
      return url.toString()
    }
    return null
  } catch {
    // Not a URL — try the username rules.
  }
  if (GITHUB_USERNAME_RE.test(trimmed)) {
    return `https://github.com/${trimmed}`
  }
  return null
}

const linkBuilders: Record<string, (value: string) => string | null> = {
  GITHUB: buildGithubUrl,
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

  // Wrap in a truncating Text so long URLs/usernames don't break the
  // surrounding LabeledItem layout (which previously truncated string
  // values via the Text branch in LabeledItem).
  return (
    <Text fz='sm' truncate>
      <Anchor href={href} target='_blank' rel='noopener noreferrer' inherit>
        {value}
      </Anchor>
    </Text>
  )
}
