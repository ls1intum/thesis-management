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

## Implementation TODO

Prioritized by urgency and impact on GDPR compliance.

### Priority 1 — High (address quickly)

- [ ] **Automatic deletion of rejected applications after 1 year**: The privacy statement promises this retention period. Without enforcement, rejected application data accumulates indefinitely, creating a documented discrepancy between the privacy statement and actual behavior.
- [ ] **Account/data deletion endpoint**: There is currently no way for users to delete their account or data. Add a self-service account deletion feature or at minimum an admin endpoint to fully delete a user's data (Art. 17 right to erasure). Must respect the retention periods defined above (e.g. thesis data cannot be deleted before the 5-year period expires).
- [ ] **Configurable application email content**: Add a per-research-group setting to control whether application notification emails include attachments (CV, examination report) and personal details, or only contain the student name, topic, and a link to the application in the system. This addresses a user request. Responding promptly demonstrates good faith.

### Priority 2 — Medium (implement within next months)

- [ ] **Data export endpoint**: Add a GDPR data export feature (Art. 20 data portability) that lets users download all their personal data in a structured format (e.g. JSON/ZIP with profile data and uploaded files). Currently handled manually on request, but a self-service feature would reduce administrative effort and improve response time.
- [ ] **Automatic disabling of inactive accounts after 1 year of inactivity**: Required to fulfill the retention promise in the privacy statement. Without this, user data is retained indefinitely.
- [ ] **Reactivation of disabled accounts on login**: Necessary counterpart to the above — disabled users who log in again should have their account re-enabled automatically.
- [ ] **Deletion of disabled user accounts after linked data retention periods expire**: Completes the account lifecycle — once all linked thesis/application data has been cleaned up, the account itself should be removed.

### Priority 3 — Low (implement when capacity allows)

- [ ] **Automatic deletion/archival of thesis data after 5-year retention period**: Important for long-term compliance, but the 5-year clock means this is not urgent for recently created data. Can be implemented once the higher-priority items are in place.
- [ ] **Server-side consent tracking and privacy statement versioning**: The privacy notice consent checkbox currently stores consent only in the browser's localStorage as a UX convenience — once checked and submitted, the checkbox stays pre-ticked on future profile edits so users don't have to re-check it every time. This is not auditable and not persistent across browsers. Replace with server-side tracking: add a `privacyConsentedAt` timestamp and `privacyVersion` field to the User entity. When the user checks the box and submits, store the consent on the server. On future visits, pre-tick the checkbox based on the server record instead of localStorage. When the privacy statement is updated (new version), clear the consent and re-prompt users to agree to the new version.
- [ ] **Remove CalDAV push code from codebase**: CalDAV is disabled in production and has no benefit over ICS subscription feeds. Removing it simplifies the codebase.
- [ ] **Remove ProfilePictureMigration after successful production deployment**: One-time migration task that should be deleted once it has run successfully.
