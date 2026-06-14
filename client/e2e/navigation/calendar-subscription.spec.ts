import { test, expect } from '@playwright/test'

const API_BASE = process.env.SERVER_URL ?? 'http://localhost:8180'

// Seed data: DSA research group has thesis 3 (MASTER) with a scheduled PUBLIC final
// presentation. The student is "Student3 User". The thesis title is
// "Online Anomaly Detection in IoT Sensor Streams".
const RESEARCH_GROUP_ABBREVIATION = 'DSA'
const EXPECTED_THESIS_TITLE = 'Online Anomaly Detection in IoT Sensor Streams'

test.describe('Public presentation calendar subscription', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('subscription feed is unauthenticated and returns text/calendar', async ({ request }) => {
    const response = await request.get(
      `${API_BASE}/api/v2/calendar/presentations/${RESEARCH_GROUP_ABBREVIATION}`,
    )

    expect(response.status()).toBe(200)
    expect(response.headers()['content-type']).toContain('text/calendar')

    const body = await response.text()
    expect(body).toMatch(/^BEGIN:VCALENDAR/m)
    expect(body).toMatch(/END:VCALENDAR\s*$/)
    expect(body).toContain('METHOD:PUBLISH')
  })

  test('subscription feed contains no ATTENDEE lines (privacy guarantee)', async ({ request }) => {
    const response = await request.get(
      `${API_BASE}/api/v2/calendar/presentations/${RESEARCH_GROUP_ABBREVIATION}`,
    )
    expect(response.ok()).toBeTruthy()

    const body = await response.text()

    // The feed must contain at least one event from seed data so we know we're
    // actually testing a populated calendar, not an empty one.
    expect(body).toContain('BEGIN:VEVENT')
    expect(body).toContain(EXPECTED_THESIS_TITLE)

    // Privacy contract: the unauthenticated feed must not list any participant
    // emails. ical4j may fold long lines, so check the unfolded body. We
    // intentionally allow the ORGANIZER mailto (the system's own application
    // address, needed so calendar clients know where replies should go) but
    // forbid ATTENDEE lines, which is where user emails would surface.
    const unfolded = body.replace(/\r?\n[ \t]/g, '')
    expect(unfolded).not.toMatch(/^ATTENDEE[:;]/m)
    expect(unfolded).not.toMatch(/^X-ATTENDEE/im)

    // Sanity check: every mailto: occurrence must belong to an ORGANIZER line.
    const mailtoLines = unfolded.split(/\r?\n/).filter((line) => /mailto:/i.test(line))
    for (const line of mailtoLines) {
      expect(line).toMatch(/^ORGANIZER[:;]/)
    }
  })

  test('subscription feed uses the short-type + student-name title format', async ({ request }) => {
    const response = await request.get(
      `${API_BASE}/api/v2/calendar/presentations/${RESEARCH_GROUP_ABBREVIATION}`,
    )
    expect(response.ok()).toBeTruthy()

    const body = await response.text()
    const unfolded = body.replace(/\r?\n[ \t]/g, '')

    // Thesis 3 is a MASTER thesis with student "Student3 User".
    // Expected SUMMARY: "MA Presentation Student3 User: Online Anomaly Detection in IoT Sensor Streams"
    const summaryMatch = unfolded.match(
      new RegExp(`SUMMARY:.*${EXPECTED_THESIS_TITLE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`),
    )
    expect(summaryMatch, 'Expected a SUMMARY line containing the thesis title').not.toBeNull()

    const summary = summaryMatch![0]
    expect(summary).toContain('MA Presentation')
    expect(summary).toContain('Student3 User')
    expect(summary).toMatch(/MA Presentation Student3 User:/)
    // No legacy quoted-title format.
    expect(summary).not.toContain('Thesis Presentation "')
  })

  test('unknown abbreviation returns 404', async ({ request }) => {
    const response = await request.get(
      `${API_BASE}/api/v2/calendar/presentations/this-group-does-not-exist`,
    )
    expect(response.status()).toBe(404)
  })
})
