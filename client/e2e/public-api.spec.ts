import { test, expect } from '@playwright/test'
import fs from 'fs'
import path from 'path'

const API_BASE = process.env.SERVER_URL ?? 'http://localhost:8180'

// Fixed UUID from seed data for the avatar_test_supervisor user.
// This user has a SUPERVISOR role on a non-public thesis (Thesis 3: ASSESSED/INTERNAL)
// but no open published topics, no finished PUBLIC theses, and is not a research group head.
const AVATAR_TEST_USER_ID = '00000000-0000-4000-aaaa-000000000001'
const AVATAR_TEST_FILENAME = 'avatar_test_supervisor.png'

test.describe('Public API - Avatar access control', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('unauthenticated request for avatar of user with public thesis is not blocked by auth', async ({
    request,
  }) => {
    // First, verify the published-theses endpoint returns structured data
    const thesesResponse = await request.get(`${API_BASE}/api/v2/published-theses`)
    expect(thesesResponse.ok()).toBeTruthy()
    const theses = await thesesResponse.json()
    expect(theses.content).toBeDefined()
    expect(Array.isArray(theses.content)).toBeTruthy()
    expect(theses.content.length).toBeGreaterThan(0)

    // Find the known public thesis from seed data
    const publicThesis = theses.content.find(
      (t: { title: string }) => t.title === 'Systematic Monolith to Microservices Migration',
    )
    expect(publicThesis, 'Public thesis should exist in seed data').toBeDefined()

    // Verify thesis has expected structure with students
    expect(publicThesis.students).toBeDefined()
    expect(Array.isArray(publicThesis.students)).toBeTruthy()
    expect(publicThesis.students.length).toBeGreaterThan(0)
    expect(publicThesis.students[0].firstName).toBeDefined()
    expect(publicThesis.students[0].lastName).toBeDefined()

    const studentUserId = publicThesis.students[0].userId
    expect(studentUserId, 'Public thesis student should have a userId').toBeDefined()
    expect(studentUserId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)

    // Avatar request for a publicly visible user should pass the visibility check.
    // Returns 200 if avatar exists, or 404 if no avatar file is set.
    // Must NOT be 401 or 403 (blocked by authentication/authorization).
    const avatarResponse = await request.get(`${API_BASE}/api/v2/avatars/${studentUserId}`)
    expect(avatarResponse.status()).not.toBe(401)
    expect(avatarResponse.status()).not.toBe(403)
    expect([200, 404]).toContain(avatarResponse.status())
  })

  test('unauthenticated request for avatar of supervisor on non-public thesis returns 200', async ({
    request,
  }) => {
    // Ensure the avatar file exists on disk so the endpoint can serve it.
    // The server runs from the server/ directory, so uploads are at ../server/uploads/.
    const uploadsDir = path.resolve(__dirname, '..', '..', 'server', 'uploads')
    const avatarPath = path.join(uploadsDir, AVATAR_TEST_FILENAME)
    if (!fs.existsSync(avatarPath)) {
      fs.mkdirSync(uploadsDir, { recursive: true })
      // Minimal valid 1x1 PNG
      const png = Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
        'base64',
      )
      fs.writeFileSync(avatarPath, png)
    }

    // This user (avatar_test_supervisor) is a SUPERVISOR on Thesis 3 (ASSESSED/INTERNAL).
    // They have NO open published topics, NO finished PUBLIC theses, and are NOT a research group head.
    // With the old visibility check (only public theses/open topics/group heads), this would return 404.
    // With the new check (any non-STUDENT thesis role), this should return 200.
    const avatarResponse = await request.get(
      `${API_BASE}/api/v2/avatars/${AVATAR_TEST_USER_ID}`,
    )
    expect(avatarResponse.status()).toBe(200)
    expect(avatarResponse.headers()['content-type']).toContain('image/png')
  })

  test('unauthenticated request for avatar of non-existent user returns 404', async ({
    request,
  }) => {
    const avatarResponse = await request.get(
      `${API_BASE}/api/v2/avatars/00000000-0000-0000-0000-000000000000`,
    )
    expect(avatarResponse.status()).toBe(404)
  })

  test('published-theses endpoint returns correct data structure', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v2/published-theses`)
    expect(response.ok()).toBeTruthy()
    const data = await response.json()

    // Verify pagination structure
    expect(data.content).toBeDefined()
    expect(data.totalElements).toBeDefined()
    expect(data.totalElements).toBeGreaterThan(0)
    expect(data.pageNumber).toBeDefined()
    expect(data.pageSize).toBeDefined()

    // Verify thesis data structure
    const thesis = data.content[0]
    expect(thesis.thesisId).toBeDefined()
    expect(thesis.title).toBeDefined()
    expect(typeof thesis.title).toBe('string')
    expect(thesis.type).toBeDefined()
    expect(['BACHELOR', 'MASTER']).toContain(thesis.type)
    expect(thesis.state).toBe('FINISHED')

    // Verify thesis includes role holders with userId
    expect(thesis.students).toBeDefined()
    expect(thesis.supervisors).toBeDefined()
    expect(thesis.examiners).toBeDefined()

    // Verify research group is included as minimal DTO
    expect(thesis.researchGroup).toBeDefined()
    expect(thesis.researchGroup.name).toBeDefined()

    // All published theses should be FINISHED (visibility is enforced server-side)
    for (const t of data.content) {
      expect(t.state).toBe('FINISHED')
      expect(t.thesisId).toBeDefined()
      expect(t.title).toBeDefined()
    }
  })

  test('published-theses contains known seed data theses', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v2/published-theses`)
    expect(response.ok()).toBeTruthy()
    const data = await response.json()

    const titles = data.content.map((t: { title: string }) => t.title)

    // Thesis 4 (FINISHED, PUBLIC) should be in published theses
    expect(titles).toContain('Systematic Monolith to Microservices Migration')

    // Thesis 1 (WRITING, PRIVATE) should NOT be in published theses
    expect(titles).not.toContain('Automated Code Review Using Large Language Models')

    // Thesis 3 (ASSESSED, INTERNAL) should NOT be in published theses
    expect(titles).not.toContain('Online Anomaly Detection in IoT Sensor Streams')
  })

  test('published-theses supports page parameter', async ({ request }) => {
    const response = await request.get(`${API_BASE}/api/v2/published-theses?page=0`)
    expect(response.ok()).toBeTruthy()
    const data = await response.json()

    // Verify pagination metadata
    expect(data.content).toBeDefined()
    expect(data.pageNumber).toBe(0)
    expect(data.pageSize).toBeGreaterThan(0)
    expect(data.totalElements).toBeGreaterThan(0)
    expect(data.totalPages).toBeGreaterThan(0)

    // Request a page beyond available data — content is omitted when empty (@JsonInclude NON_EMPTY)
    const response2 = await request.get(`${API_BASE}/api/v2/published-theses?page=999`)
    expect(response2.ok()).toBeTruthy()
    const data2 = await response2.json()
    expect(data2.content ?? []).toHaveLength(0)
    expect(data2.pageNumber).toBe(999)
  })
})

test.describe('Public API - Consent validation', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('unauthenticated application creation is rejected with 401', async ({ request }) => {
    const response = await request.post(`${API_BASE}/api/v2/applications`, {
      data: {
        thesisTitle: 'Test',
        thesisType: 'MASTER',
        desiredStartDate: new Date().toISOString(),
        motivation: 'Test',
        consentToPrivacyPolicy: true,
      },
    })
    // Unauthenticated requests are rejected before consent check
    expect(response.status()).toBe(401)
  })
})
