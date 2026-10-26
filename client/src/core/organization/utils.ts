/** Empty values are fine (the link is optional); anything else has to be a plain http(s) URL. */
export function isHttpUrl(value: string): boolean {
  if (!value.trim()) {
    return true
  }

  try {
    const url = new URL(value.trim())

    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}
