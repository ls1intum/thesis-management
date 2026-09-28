import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { GLOBAL_CONFIG } from '@/core/config/global'

export type ReviewProgressStatus = 'SUBSCRIBED' | 'STARTED' | 'COMPLETED' | 'FAILED'

export interface IReviewProgressEvent {
  jobId: string
  stepId: string
  label?: string
  status: ReviewProgressStatus
  index: number
  total: number
  message?: string
}

export interface IReviewProgressSubscription {
  /**
   * Resolves once the server has confirmed the subscription is live — or once the bounded wait
   * below elapses, so a broken socket degrades to "no progress bar" instead of blocking the
   * review. Await this before starting the request whose progress is being tracked.
   */
  ready: Promise<void>
  /** Idempotent and never throws, so it is safe to call from a `finally` block. */
  unsubscribe: () => void
}

type Listener = (event: IReviewProgressEvent) => void

interface IJob {
  listener: Listener
  subscription?: StompSubscription
  markReady: () => void
  probeTimer?: ReturnType<typeof setInterval>
  readyTimer?: ReturnType<typeof setTimeout>
}

// How often to re-ask the server whether the subscription has landed. The SUBSCRIBE and the probe
// travel the same socket but are dispatched on the server's inbound executor independently, so the
// first probe can overtake its own subscription; retrying closes that window.
const PROBE_INTERVAL_MS = 150
// Upper bound on the whole wait (connect, authenticate, subscribe, confirm). Past it the review
// starts anyway, without live progress, rather than leaving the user staring at a dead button.
const READY_TIMEOUT_MS = 5000

// Lazily created, reused across every subscription for the lifetime of the tab — one STOMP
// session, many per-job destinations.
let client: Client | undefined
const jobs = new Map<string, IJob>()

const wsBrokerUrl = () => {
  const url = new URL(GLOBAL_CONFIG.server_host)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  // The server's DispatcherServlet (and so the STOMP endpoint registered on it) sits under the
  // "/api" context path, same as every doRequest call.
  url.pathname = `${url.pathname.replace(/\/+$/, '')}/api/ws`
  return url.toString()
}

const clearProbeTimer = (job: IJob) => {
  if (job.probeTimer !== undefined) {
    clearInterval(job.probeTimer)
    job.probeTimer = undefined
  }
}

const clearTimers = (job: IJob) => {
  clearProbeTimer(job)
  if (job.readyTimer !== undefined) {
    clearTimeout(job.readyTimer)
    job.readyTimer = undefined
  }
}

/**
 * Asks the server to echo a SUBSCRIBED event back down this job's own destination, repeatedly
 * until one arrives (which resolves readiness and stops the timer) or the bounded wait elapses.
 * The echo travels the same broker path as real progress events, so receiving it proves events
 * sent from now on will reach this client.
 */
const probeUntilReady = (stompClient: Client, jobId: string, job: IJob) => {
  const probe = () => {
    if (!stompClient.connected) {
      return
    }
    try {
      stompClient.publish({ destination: `/app/ai-review-progress/${jobId}/probe` })
    } catch (error) {
      // The socket dropped between the check and the publish; the next probe or the bounded
      // fallback covers it.
      console.warn('Failed to probe AI review progress subscription', error)
    }
  }

  // A reconnect re-subscribes every live job, so drop any interval from the previous session
  // first — overwriting the handle would orphan it, leaving it probing for the life of the tab
  // with nothing able to clear it.
  clearProbeTimer(job)
  probe()
  job.probeTimer = setInterval(probe, PROBE_INTERVAL_MS)
}

const subscribeNow = (stompClient: Client, jobId: string, job: IJob) => {
  job.subscription = stompClient.subscribe(
    `/user/queue/ai-review-progress/${jobId}`,
    (message: IMessage) => {
      const event = JSON.parse(message.body) as IReviewProgressEvent
      if (event.status === 'SUBSCRIBED') {
        job.markReady()
        return
      }
      job.listener(event)
    },
  )
  probeUntilReady(stompClient, jobId, job)
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
      for (const [jobId, job] of jobs) {
        subscribeNow(stompClient, jobId, job)
      }
    },
  })
  client = stompClient
  stompClient.activate()

  return stompClient
}

/**
 * Subscribes to live progress events for one AI review job. Await the returned `ready` promise
 * before starting the review itself — events are not replayed, so anything the server emits before
 * the subscription lands is lost — and call `unsubscribe` once the job is done (or the component
 * subscribing is unmounted).
 */
export function subscribeToReviewProgress(
  jobId: string,
  listener: Listener,
): IReviewProgressSubscription {
  const job: IJob = { listener, markReady: () => {} }

  const ready = new Promise<void>((resolve) => {
    const settle = () => {
      clearTimers(job)
      resolve()
    }
    job.markReady = settle
    job.readyTimer = setTimeout(settle, READY_TIMEOUT_MS)
  })

  jobs.set(jobId, job)

  const stompClient = getClient()
  if (stompClient.connected) {
    subscribeNow(stompClient, jobId, job)
  }

  const unsubscribe = () => {
    if (!jobs.delete(jobId)) {
      return
    }

    try {
      // Only send an UNSUBSCRIBE frame while the socket is up: StompSubscription.unsubscribe()
      // publishes through the client and throws when disconnected. Callers run this from a
      // `finally` around the review request, where a throw would mask the request's own result.
      if (client?.connected) {
        job.subscription?.unsubscribe()
      }
    } catch (error) {
      // Best effort — the server drops the subscription along with the session anyway.
      console.warn('Failed to unsubscribe from AI review progress', error)
    } finally {
      job.subscription = undefined
      // Nothing will confirm this subscription any more; never leave an awaited promise pending.
      job.markReady()
    }
  }

  return { ready, unsubscribe }
}
