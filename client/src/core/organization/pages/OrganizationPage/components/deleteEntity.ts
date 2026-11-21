import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'

/** Deletes an organization entry; the server refuses (with an explaining message) if it is still in use. */
export async function deleteEntity(url: string, successMessage: string, onDeleted: () => void) {
  const response = await doRequest<undefined>(url, { method: 'DELETE', requiresAuth: true })

  if (response.ok) {
    showSimpleSuccess(successMessage)
    onDeleted()
  } else {
    showSimpleError(getApiResponseErrorMessage(response))
  }
}
