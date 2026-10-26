import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { IReviewProgressEvent } from '@/core/ws/reviewProgressSocket'

vi.mock('@/core/config/global', () => ({
  GLOBAL_CONFIG: { server_host: 'http://localhost:8180' },
}))

const stomp = vi.hoisted(() => {
  interface IFakeSubscription {
    destination: string
    callback: (message: { body: string }) => void
    unsubscribe: () => void
  }

  class FakeClient {
    connected = false
    throwOnUnsubscribe = false
    readonly subscriptions: IFakeSubscription[] = []
    readonly published: string[] = []
    unsubscribeCalls = 0
    private readonly config: { onConnect?: () => void }

    constructor(config: { onConnect?: () => void }) {
      this.config = config
      instances.push(this)
    }

    activate() {}

    subscribe(
      destination: string,
      callback: (message: { body: string }) => void,
    ): IFakeSubscription {
      const subscription: IFakeSubscription = {
        destination,
        callback,
        unsubscribe: () => {
          this.unsubscribeCalls += 1
          // Mirrors @stomp/stompjs, which publishes an UNSUBSCRIBE frame through the client and
          // throws when it is not connected.
          if (!this.connected || this.throwOnUnsubscribe) {
            throw new Error('Cannot unsubscribe: the client is not connected')
          }
        },
      }
      this.subscriptions.push(subscription)
      return subscription
    }

    publish(params: { destination: string }) {
      if (!this.connected) {
        throw new Error('Cannot publish: the client is not connected')
      }
      this.published.push(params.destination)
    }

    /** Test driver: completes the STOMP handshake, as the real client does on connect. */
    connect() {
      this.connected = true
      this.config.onConnect?.()
    }

    /** Test driver: delivers one frame to every live subscription. */
    deliver(event: unknown) {
      const message = { body: JSON.stringify(event) }
      for (const subscription of this.subscriptions) {
        subscription.callback(message)
      }
    }
  }

  const instances: FakeClient[] = []
  return { instances, FakeClient }
})

vi.mock('@stomp/stompjs', () => ({ Client: stomp.FakeClient }))

const JOB_ID = '11111111-1111-1111-1111-111111111111'

const subscribedEvent = (jobId = JOB_ID) => ({
  jobId,
  stepId: 'subscription',
  status: 'SUBSCRIBED',
  index: 0,
  total: 0,
})

const startedEvent = (jobId = JOB_ID) => ({
  jobId,
  stepId: 'structure',
  label: 'Structure & Completeness',
  status: 'STARTED',
  index: 1,
  total: 10,
})

/** Loads a pristine copy of the module, whose STOMP client and job map are module-level state. */
const loadModule = async () => {
  vi.resetModules()
  stomp.instances.length = 0
  return import('@/core/ws/reviewProgressSocket')
}

/** Lets already-resolved promise callbacks run without advancing any timer. */
const flush = () => vi.advanceTimersByTimeAsync(0)

describe('subscribeToReviewProgress', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval'] })
    vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('stays unready until the server confirms the subscription', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const { ready } = subscribeToReviewProgress(JOB_ID, () => {})
    let settled = false
    void ready.then(() => {
      settled = true
    })

    const client = stomp.instances[0]
    // Not connected yet: nothing is subscribed, so a review started now would lose every event.
    expect(client.subscriptions).toHaveLength(0)
    await flush()
    expect(settled).toBe(false)

    client.connect()
    expect(client.subscriptions[0].destination).toBe(`/user/queue/ai-review-progress/${JOB_ID}`)
    expect(client.published).toEqual([`/app/ai-review-progress/${JOB_ID}/probe`])
    await flush()
    expect(settled).toBe(false)

    client.deliver(subscribedEvent())
    await flush()
    expect(settled).toBe(true)
  })

  it('re-probes until an acknowledgement arrives, then stops', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    subscribeToReviewProgress(JOB_ID, () => {})
    const client = stomp.instances[0]
    client.connect()

    await vi.advanceTimersByTimeAsync(400)
    expect(client.published.length).toBeGreaterThan(1)

    client.deliver(subscribedEvent())
    const probesAtAck = client.published.length
    await vi.advanceTimersByTimeAsync(1000)
    expect(client.published).toHaveLength(probesAtAck)
  })

  it('does not orphan the probe interval when the socket reconnects', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const { unsubscribe } = subscribeToReviewProgress(JOB_ID, () => {})
    const client = stomp.instances[0]
    client.connect()
    // A drop and reconnect mid-review re-subscribes every live job.
    client.connect()

    client.deliver(subscribedEvent())
    unsubscribe()

    const probesAtCleanup = client.published.length
    await vi.advanceTimersByTimeAsync(2000)
    expect(client.published).toHaveLength(probesAtCleanup)
  })

  it('gives up waiting after the bounded timeout so the review still runs', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const { ready } = subscribeToReviewProgress(JOB_ID, () => {})
    let settled = false
    void ready.then(() => {
      settled = true
    })

    stomp.instances[0].connect()
    await vi.advanceTimersByTimeAsync(4000)
    expect(settled).toBe(false)

    await vi.advanceTimersByTimeAsync(2000)
    expect(settled).toBe(true)
  })

  it('subscribes immediately when the client is already connected', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const first = subscribeToReviewProgress('job-a', () => {})
    const client = stomp.instances[0]
    client.connect()
    first.unsubscribe()

    subscribeToReviewProgress(JOB_ID, () => {})
    expect(client.subscriptions.at(-1)?.destination).toBe(
      `/user/queue/ai-review-progress/${JOB_ID}`,
    )
    expect(client.published.at(-1)).toBe(`/app/ai-review-progress/${JOB_ID}/probe`)
  })

  it('forwards progress events but never the handshake acknowledgement', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const received: IReviewProgressEvent[] = []
    subscribeToReviewProgress(JOB_ID, (event) => received.push(event))
    const client = stomp.instances[0]
    client.connect()

    client.deliver(subscribedEvent())
    client.deliver(startedEvent())

    expect(received).toEqual([startedEvent()])
  })

  it('cleans up without throwing when the socket dropped mid-review', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const { ready, unsubscribe } = subscribeToReviewProgress(JOB_ID, () => {})
    let settled = false
    void ready.then(() => {
      settled = true
    })

    const client = stomp.instances[0]
    client.connect()
    client.connected = false

    expect(() => unsubscribe()).not.toThrow()
    // No UNSUBSCRIBE frame is even attempted while disconnected.
    expect(client.unsubscribeCalls).toBe(0)
    await flush()
    expect(settled).toBe(true)
  })

  it('swallows a failing unsubscribe and is idempotent', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const { unsubscribe } = subscribeToReviewProgress(JOB_ID, () => {})
    const client = stomp.instances[0]
    client.connect()
    client.throwOnUnsubscribe = true

    expect(() => unsubscribe()).not.toThrow()
    expect(client.unsubscribeCalls).toBe(1)

    expect(() => unsubscribe()).not.toThrow()
    expect(client.unsubscribeCalls).toBe(1)
  })

  it('stops probing once unsubscribed', async () => {
    const { subscribeToReviewProgress } = await loadModule()

    const { unsubscribe } = subscribeToReviewProgress(JOB_ID, () => {})
    const client = stomp.instances[0]
    client.connect()

    unsubscribe()
    const probesAtUnsubscribe = client.published.length
    await vi.advanceTimersByTimeAsync(1000)
    expect(client.published).toHaveLength(probesAtUnsubscribe)
  })
})
