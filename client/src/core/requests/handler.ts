import type { ApiResponse } from '@/core/requests/request'

export function getApiResponseErrorMessage(response: ApiResponse<unknown>) {
  if (response.status === 1005) {
    throw new Error('Should not show error')
  }

  let message

  if (response.ok) {
    message = 'Error thrown on successful response'
  } else if (response.status === 1000) {
    message = 'Failed to send request to the server'
  } else if (response.status === 400) {
    message = 'Server could not parse your request'
  } else if (response.status === 401) {
    message = 'You are not authenticated. Please try refreshing the page'
  } else if (response.status === 403) {
    message = 'You are not authorized to access this resource'
  } else if (response.status === 404) {
    message = 'Requested resource not found'
  } else if (response.status === 409) {
    message = 'Resource already exists'
  } else if (response.status === 501) {
    message = 'Endpoint not implemented yet'
  } else {
    message = `Unknown error ${response.status}`
  }

  // A message from the server is written for the end user (see ErrorDto / the request
  // exceptions) and is always more specific than the generic status text above — e.g. "AI review
  // is not set up for your research group yet…" instead of "You are not authorized to access this
  // resource". Show it on its own rather than prefixing it with the status text, which would bury
  // the actionable part and, for 403s, tell the user they lack access when they only lack setup.
  // Codes 1000/1005 are synthetic (network failure, abort); their `error` is the raw fetch error
  // ("Failed to fetch"), which only makes sense behind the generic text — so keep appending there.
  const isHttpStatus = response.status >= 400 && response.status < 600

  if (!response.ok && response.error?.message) {
    return isHttpStatus ? response.error.message : `${message}: ${response.error.message}`
  }

  return message
}

export class ApiError extends Error {
  name = 'ApiError'

  constructor(response: ApiResponse<unknown>) {
    super(getApiResponseErrorMessage(response))
  }
}
