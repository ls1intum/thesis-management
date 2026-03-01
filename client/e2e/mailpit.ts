import { expect } from '@playwright/test'

const MAILPIT_API = process.env.MAILPIT_URL ?? 'http://localhost:8025/api'

interface MailpitAddress {
  Name: string
  Address: string
}

interface MailpitAttachment {
  PartID: string
  FileName: string
  ContentType: string
  Size: number
}

interface MailpitMessageSummary {
  ID: string
  MessageID: string
  From: MailpitAddress
  To: MailpitAddress[]
  Subject: string
  Created: string
  Attachments: number
  Size: number
  Read: boolean
  Snippet: string
}

export interface MailpitMessage {
  ID: string
  MessageID: string
  From: MailpitAddress
  To: MailpitAddress[]
  Subject: string
  Created: string
  HTML: string
  Text: string
  Attachments: MailpitAttachment[]
}

interface MailpitSearchResult {
  total: number
  messages_count: number
  messages: MailpitMessageSummary[]
}

/**
 * Search messages by recipient email address.
 */
async function searchByRecipient(email: string): Promise<MailpitSearchResult> {
  const res = await fetch(`${MAILPIT_API}/v1/search?query=${encodeURIComponent(`to:${email}`)}`)
  return (await res.json()) as MailpitSearchResult
}

/**
 * Fetch the full message by ID (includes decoded HTML/Text body and attachments).
 */
async function getFullMessage(id: string): Promise<MailpitMessage> {
  const res = await fetch(`${MAILPIT_API}/v1/message/${id}`)
  return (await res.json()) as MailpitMessage
}

/**
 * Fetch all messages (summaries).
 */
async function getAllMessages(): Promise<MailpitMessageSummary[]> {
  const res = await fetch(`${MAILPIT_API}/v1/messages`)
  const data = (await res.json()) as MailpitSearchResult
  return data.messages ?? []
}

/**
 * Get the subject line of a message.
 */
export function getSubject(message: MailpitMessage): string {
  return message.Subject ?? ''
}

/**
 * Get the From header of a message as a formatted string.
 */
export function getFromHeader(message: MailpitMessage): string {
  if (message.From.Name) {
    return `${message.From.Name} <${message.From.Address}>`
  }
  return message.From.Address
}

/**
 * Get the To addresses of a message as a flat list of email strings.
 */
export function getToAddresses(message: MailpitMessage): string[] {
  return message.To.map((addr) => addr.Address)
}

/**
 * Get the decoded body of a message.
 * Mailpit returns pre-decoded HTML and Text fields — no manual MIME decoding needed.
 */
export function getBody(message: MailpitMessage): string {
  return message.HTML || message.Text || ''
}

/**
 * Check whether a message has an attachment with the given filename pattern.
 */
export function hasAttachment(message: MailpitMessage, filenamePattern?: RegExp): boolean {
  if (!message.Attachments?.length) return false
  if (!filenamePattern) return true
  return message.Attachments.some((att) => filenamePattern.test(att.FileName))
}

/**
 * Find a message by subject (exact match).
 */
export function findBySubject(
  messages: MailpitMessage[],
  subject: string,
): MailpitMessage | undefined {
  return messages.find((m) => getSubject(m) === subject)
}

/**
 * Assert that a message was sent to the expected recipient.
 */
export function assertRecipient(message: MailpitMessage, expectedEmail: string): void {
  const toAddresses = getToAddresses(message)
  expect(toAddresses, `Expected email to be sent to ${expectedEmail}`).toContain(expectedEmail)
}

/**
 * Assert that the email body contains the expected text.
 */
export function assertBodyContains(message: MailpitMessage, expectedText: string): void {
  const body = getBody(message)
  expect(body, `Expected email body to contain "${expectedText}"`).toContain(expectedText)
}

/**
 * Assert that the email was sent from the application sender.
 * Verifies the From header matches the expected format: "ThesisManagement <...@...>"
 */
export function assertSentFromApp(message: MailpitMessage): void {
  const from = getFromHeader(message)
  expect(from, 'Expected email to be sent from ThesisManagement').toMatch(
    /ThesisManagement\s*<.+@.+>/,
  )
}

// ---------------------------------------------------------------------------
// Snapshot-based helpers: safe for parallel test execution.
//
// Instead of clearing the mailbox (which would destroy emails from concurrent
// tests), we snapshot message IDs before an action and then wait for new
// messages that were not present in the snapshot.
// ---------------------------------------------------------------------------

/**
 * Snapshot the current set of message IDs for a specific recipient.
 * Call this BEFORE the action that should trigger emails.
 */
export async function snapshotMailbox(recipient: string): Promise<Set<string>> {
  const result = await searchByRecipient(recipient)
  return new Set(result.messages.map((m) => m.ID))
}

/**
 * Snapshot all current message IDs (any recipient).
 * Call this BEFORE the action that should trigger emails.
 */
export async function snapshotAllMessages(): Promise<Set<string>> {
  const messages = await getAllMessages()
  return new Set(messages.map((m) => m.ID))
}

/**
 * Wait for new messages to a specific recipient that were not in the snapshot.
 * Returns the full messages (with decoded body and attachments).
 */
export async function waitForNewMessages(
  recipient: string,
  beforeIds: Set<string>,
  expectedCount: number = 1,
  timeoutMs: number = 15_000,
): Promise<MailpitMessage[]> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const result = await searchByRecipient(recipient)
    const newSummaries = result.messages.filter((m) => !beforeIds.has(m.ID))
    if (newSummaries.length >= expectedCount) {
      return Promise.all(newSummaries.map((m) => getFullMessage(m.ID)))
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000))
  }
  // Final check with assertion error
  const result = await searchByRecipient(recipient)
  const newSummaries = result.messages.filter((m) => !beforeIds.has(m.ID))
  expect(
    newSummaries.length,
    `Expected at least ${expectedCount} new email(s) to ${recipient}, but found ${newSummaries.length}`,
  ).toBeGreaterThanOrEqual(expectedCount)
  return Promise.all(newSummaries.map((m) => getFullMessage(m.ID)))
}

/**
 * Wait for new messages (any recipient) that were not in the snapshot.
 * Returns the full messages (with decoded body and attachments).
 */
export async function waitForNewTotalMessages(
  beforeIds: Set<string>,
  expectedCount: number = 1,
  timeoutMs: number = 15_000,
): Promise<MailpitMessage[]> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const messages = await getAllMessages()
    const newSummaries = messages.filter((m) => !beforeIds.has(m.ID))
    if (newSummaries.length >= expectedCount) {
      return Promise.all(newSummaries.map((m) => getFullMessage(m.ID)))
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000))
  }
  const messages = await getAllMessages()
  const newSummaries = messages.filter((m) => !beforeIds.has(m.ID))
  expect(
    newSummaries.length,
    `Expected at least ${expectedCount} new email(s), but found ${newSummaries.length}`,
  ).toBeGreaterThanOrEqual(expectedCount)
  return Promise.all(newSummaries.map((m) => getFullMessage(m.ID)))
}
