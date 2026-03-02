# Data Retention Policy

This document describes the data retention periods used in the Thesis Management application and the rationale behind them. It serves as internal documentation for GDPR accountability (Art. 5(2) GDPR).

## Retention Periods

| Data Category                  | Retention Period                                        | Rationale                                                                                                         |
|--------------------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| **Thesis data (incl. accepted application)** | 5 years after end of the calendar year of final grading | Required by Bavarian examination regulations. The accepted application is retained for the same period because it documents the basis for thesis topic selection and may be needed to defend the suitability of the topic in case of disputes. |
| **Rejected application data**  | 1 year after rejection                                  | See rationale below.                                                                                              |
| **Uploaded documents**         | Same as associated thesis or application data           | Documents (CV, examination reports, degree reports, thesis files) follow the retention period of the record they belong to. |
| **Server log files**           | 90 days                                                 | Sufficient for security monitoring and incident investigation. Standard practice for web server logs.              |
| **User account data**          | Disabled after 1 year of inactivity, deleted after linked data retention periods expire | Accounts inactive for 1+ year are automatically disabled. Profile data is deleted once no linked thesis/application data requires retention. Logging in reactivates a disabled account. |

## Rationale: 1-Year Retention for Rejected Applications

Rejected applications are not examination records and therefore not subject to the 5-year retention required by examination regulations. Under GDPR's data minimization principle (Art. 5(1)(e)), they should not be kept longer than necessary.

A 1-year retention period was chosen for the following reasons:

1. **Reapplication cycles**: Students often reapply in the following semester. Retaining previous applications allows advisors to understand context and avoid redundant reviews.
2. **Inquiries and complaints**: Students may inquire about or contest a rejection. A 1-year window covers typical academic complaint timelines.
3. **Semester alignment**: One year covers at least two full semester cycles (winter and summer), which is the natural rhythm of thesis applications.
4. **Proportionality**: One year is long enough to serve legitimate operational needs while being short enough to comply with GDPR data minimization. Longer periods (e.g. 2+ years) would be difficult to justify for data where no examination relationship was established.

## Handling Deletion Requests (Art. 17 GDPR)

When a user requests deletion of their data, the response depends on the legal basis for processing:

| Data Category | Deletion on Request? | Reason |
|---|---|---|
| **Voluntarily provided profile data** (gender, nationality, interests, skills, CV, examination report) | **Yes** — delete promptly | Based on consent (Art. 6(1)(a)). User can withdraw consent at any time (Art. 7(3)). |
| **Rejected application data** | **Yes** — delete promptly | Based on legitimate interest (Art. 6(1)(f)). No overriding grounds to refuse once the user objects (Art. 17(1)(c)). The 1-year period is the maximum retention, not a mandatory minimum. |
| **Thesis data, grades, assessments, accepted applications** | **No** — retain until 5-year period expires | Based on public task (Art. 6(1)(e)) and required by examination regulations. Exempt from right to erasure under Art. 17(3)(b) (legal obligation) and Art. 17(3)(d) (archiving in public interest). Inform the user of the reason and the expected deletion date. |
| **SSO-synced data** (name, email, university ID, matriculation number) | **Not meaningful** — re-synced on every login | This data is retrieved from Keycloak on each authentication. Deleting it would have no lasting effect. The practical approach is account deactivation, which prevents further logins and data syncing. |

### Process for Handling Deletion Requests

1. Identify which data categories the user's request covers.
2. Delete all data where no legal retention obligation applies (profile data, rejected applications).
3. For thesis-related data subject to mandatory retention, inform the user:
   - Which data cannot be deleted and why (cite examination regulations).
   - When the data will be deleted (end of the 5-year retention period).
4. If the user has no active thesis and no retained examination data, offer full account deactivation.
5. Document the request and the actions taken for accountability purposes.

## Prerequisite: Automatic Application Expiration

The 1-year retention period for rejected applications (see above) requires that every application eventually receives a rejection date. Without this, unreviewed applications would remain in a "not assessed" state indefinitely, making data cleanup impossible and violating the data minimization principle.

To address this, the application includes a time-based expiration mechanism that automatically rejects applications which have not been reviewed within a configurable period (configured per research group in weeks, with a minimum of 2 weeks). When triggered, the student receives the standard rejection email notification.

**This is not automated decision-making** in the sense of GDPR Art. 22. It does not evaluate the applicant's qualifications, profile, or any personal characteristics. It is a simple timeout comparable to a deadline expiring. Its purposes are:

1. **Student transparency**: Without expiration, students whose applications are never reviewed would wait indefinitely without any response. The automatic rejection ensures they are notified and can reapply or look for alternatives.
2. **Data minimization**: The expiration assigns a rejection date, which starts the 1-year retention clock and enables eventual data cleanup.

## Account Deletion Implementation

Self-service account deletion is available via **Settings > Account** and admin deletion via the **Administration** page. The system handles two scenarios depending on whether the user has thesis data under legal retention.

### Scenario A: No Retention-Blocked Data

When the user has no completed theses (or all thesis retention periods have expired), the account is **fully deleted**:

1. All uploaded files (CV, degree report, examination report, avatar) are deleted from disk.
2. Rejected/unassessed applications and data exports are deleted.
3. Topic roles, thesis roles, and remaining applications are deleted.
4. The user record is deleted (FK cascades remove notification settings, user groups, and data exports).

### Scenario B: Thesis Data Under Retention

When the user has completed theses within the 5-year retention window, the account is **soft-deleted** (deactivated):

1. The account is disabled and marked with `deletion_requested_at` and `deletion_scheduled_for`.
2. Non-essential data is cleared: avatar, projects, interests, special skills, custom data.
3. **Profile data (name, email, university ID) and thesis-related files (CV, degree report, examination report) are preserved** so that professors can still find and reference thesis records by student name during the retention period.
4. Notification settings and user groups are deleted.
5. The authentication guard prevents the user from logging back in (SSO sync is blocked).

The **nightly job** (`DataRetentionService`) checks all soft-deleted accounts. Once all retention periods have expired for a user, it performs the full deletion (Scenario A).

### Preconditions

Deletion is blocked if the user:
- Is a **research group head** (must transfer leadership first).
- Has **active (non-terminal) theses** (must complete or drop out first).

### Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/v2/user-deletion/me/preview` | Any authenticated | Preview what would happen |
| `DELETE` | `/v2/user-deletion/me` | Any authenticated | Self-service deletion |
| `GET` | `/v2/user-deletion/{userId}/preview` | Admin | Preview for specific user |
| `DELETE` | `/v2/user-deletion/{userId}` | Admin | Admin deletes user |

### Database Changes (Migration 30)

- Added columns to `users`: `anonymized_at`, `deletion_requested_at`, `deletion_scheduled_for`.
- FK constraints changed to `ON DELETE CASCADE` for user-owned metadata (notification_settings, user_groups, data_exports).
- FK constraints changed to `ON DELETE SET NULL` for audit references on retained records (thesis_assessments.created_by, thesis_comments.created_by, thesis_feedback.requested_by, thesis_files.uploaded_by, thesis_proposals.created_by, topics.created_by, email_templates.updated_by, research_groups.created_by/updated_by, topic_roles.assigned_by, thesis_roles.assigned_by).

## Implementation TODO

Prioritized by urgency and impact on GDPR compliance.

### Priority 1 — High (address quickly)

- [x] **Automatic deletion of rejected applications after 1 year**: The privacy statement promises this retention period. Without enforcement, rejected application data accumulates indefinitely, creating a documented discrepancy between the privacy statement and actual behavior.
- [x] **Account/data deletion endpoint**: Self-service and admin account deletion (Art. 17 right to erasure). See "Account Deletion Implementation" section below for details.
- [x] **Configurable application email content**: Add a per-research-group setting to control whether application notification emails include attachments (CV, examination report) and personal details, or only contain the student name, topic, and a link to the application in the system. This addresses a user request. Responding promptly demonstrates good faith.

### Priority 2 — Medium (implement within next months)

- [x] **Data export endpoint**: Self-service GDPR data export feature (Art. 15 / Art. 20). Users can request an export from `/data-export`, which is processed overnight and generates a ZIP file containing profile data (JSON), applications, theses, assessments, and uploaded files. Users receive an email notification when ready. Downloads expire after 7 days, rate-limited to one request per 7 days.
- [x] **Automatic disabling of inactive accounts after 1 year of inactivity**: Required to fulfill the retention promise in the privacy statement. Without this, user data is retained indefinitely.
- [x] **Reactivation of disabled accounts on login**: Necessary counterpart to the above — disabled users who log in again should have their account re-enabled automatically.
- [x] **Deletion of disabled user accounts after linked data retention periods expire**: Handled by the nightly job (`DataRetentionService.processDeferredDeletions`), which checks soft-deleted accounts and performs full deletion once all retention periods have expired.

### Priority 3 — Low (implement when capacity allows)

- [ ] **Automatic deletion/archival of thesis data after 5-year retention period**: Important for long-term compliance, but the 5-year clock means this is not urgent for recently created data. Can be implemented once the higher-priority items are in place.
- [ ] **Snapshot application files at submission time**: Currently, CV (`user.cvFilename`), degree report (`user.degreeFilename`), and examination report (`user.examinationFilename`) are stored only on the User entity. If a student updates these files for a later application, the original files evaluated during an earlier thesis process are lost. Snapshot these file references onto the Application or Thesis at submission time. This would also allow immediate deletion of user-level files when a user requests account deletion during the 5-year retention period, because the snapshots on the retained thesis/application records would still be available for evaluation purposes.
- [ ] **Server-side consent tracking and privacy statement versioning**: The privacy notice consent checkbox currently stores consent only in the browser's localStorage as a UX convenience — once checked and submitted, the checkbox stays pre-ticked on future profile edits so users don't have to re-check it every time. This is not auditable and not persistent across browsers. Replace with server-side tracking: add a `privacyConsentedAt` timestamp and `privacyVersion` field to the User entity. When the user checks the box and submits, store the consent on the server. On future visits, pre-tick the checkbox based on the server record instead of localStorage. When the privacy statement is updated (new version), clear the consent and re-prompt users to agree to the new version.
- [ ] **Remove ProfilePictureMigration after successful production deployment**: One-time migration task that should be deleted once it has run successfully.
