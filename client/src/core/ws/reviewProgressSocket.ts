import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { GLOBAL_CONFIG } from '@/core/config/global'

export type ReviewProgressStatus = 'STARTED' | 'COMPLETED' | 'FAILED'

export interface IReviewProgressEvent {
  jobId: string
  stepId: string
  label?: string
  status: ReviewProgressStatus
  index: number
  total: number
  message?: string
}

type Listener = (event: IReviewProgressEvent) => void

// Lazily created, reused across every subscription for the lifetime of the tab — one STOMP
// session, many per-job destinations.
let client: Client | undefined
const listenersByJobId = new Map<string, Listener>()
const subscriptionsByJobId = new Map<string, StompSubscription>()

const wsBrokerUrl = () => {
  const url = new URL(GLOBAL_CONFIG.server_host)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  // The server's DispatcherServlet (and so the STOMP endpoint registered on it) sits under the
  // "/api" context path, same as every doRequest call.
  url.pathname = `${url.pathname.replace(/\/+$/, '')}/api/ws`
  return url.toString()
}

const subscribeNow = (stompClient: Client, jobId: string, listener: Listener) => {
  const subscription = stompClient.subscribe(
    `/user/queue/ai-review-progress/${jobId}`,
    (message: IMessage) => {
      listener(JSON.parse(message.body) as IReviewProgressEvent)
    },
  )
  subscriptionsByJobId.set(jobId, subscription)
}

const getClient = (): Client => {
  if (client) {
    return client
  }

  const stompClient: Client = new Client({
    brokerURL: wsBrokerUrl(),
    reconnectDelay: 5000,
    beforeConnect: async () => {
      // Mirrors the refresh-then-read pattern in core/requests/request.ts — a native WebSocket
      // handshake carries no Authorization header, so the fresh token travels as a STOMP CONNECT
      // header instead. Imported lazily so merely importing this module (e.g. transitively, from
      // a component under test) never eagerly constructs the real Keycloak client.
      const { keycloak } =
        await import('@/core/providers/AuthenticationContext/AuthenticationProvider')
      if (keycloak.isTokenExpired(5)) {
        await keycloak.updateToken(5 * 60)
      }
      stompClient.connectHeaders = { Authorization: `Bearer ${keycloak.token ?? ''}` }
    },
    onConnect: () => {
      // Re-subscribes everything still wanted after the initial connect or any reconnect.
      for (const [jobId, listener] of listenersByJobId) {
        subscribeNow(stompClient, jobId, listener)
      }
    },
  })
  client = stompClient
  stompClient.activate()

  return stompClient
}

/**
 * Subscribes to live progress events for one AI review job. Returns an unsubscribe function;
 * call it once the job is done (or the component unsubscribing is unmounted).
 */
export function subscribeToReviewProgress(jobId: string, listener: Listener): () => void {
  listenersByJobId.set(jobId, listener)

  const stompClient = getClient()
  if (stompClient.connected) {
    subscribeNow(stompClient, jobId, listener)
  }

  return () => {
    listenersByJobId.delete(jobId)
    subscriptionsByJobId.get(jobId)?.unsubscribe()
    subscriptionsByJobId.delete(jobId)
  }
}
