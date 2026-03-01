> [!NOTE]
> Please contact us if you want to get onboarded: **[thesis-management-support.aet@xcit.tum.de](thesis-management-support.aet@xcit.tum.de)**.

# Thesis Management

Thesis Management is a web-based thesis management system designed to streamline the thesis process in academic institutions by integrating essential stages into a single platform.
Developed to address challenges in managing large volumes of theses, it facilitates seamless interactions between students, advisors, and supervisors.
Key features include a centralized application process, guided workflows for thesis writing, automated notifications, and a comprehensive Gantt chart for tracking progress.
By consolidating communication, feedback, and file management, ThesisManagement enhances transparency, reduces administrative burdens, and fosters efficient thesis supervision and assessment.

Thesis Management was developed as part of multiple bachelor's and master's thesis, e.g. [Development of a Thesis Management System](docs/files/ba-thesis-fabian-emilius.pdf).

## User Documentation

A short description and a demo video of the most important functionality of the platform.
The videos are grouped by the roles student, supervisor, examiner, and research group admin.

#### Student

- [Submit Thesis Application](https://live.rbg.tum.de/w/artemisintro/53606)  
  Allows students to apply for available thesis topics. Students can choose a topic, provide relevant personal details, and submit a motivation letter. This structured process helps reduce application stress by offering clear guidance on required steps.

- [Edit Thesis Application](https://live.rbg.tum.de/w/artemisintro/53607)  
  Enables students to modify their submitted application details before it is reviewed by a supervisor. This feature allows for adjustments to personal information and application motivation to ensure accuracy.

- [Upload Proposal](https://live.rbg.tum.de/w/artemisintro/53608)  
  Facilitates the initial submission of the thesis proposal document. Students can submit proposals for review, and supervisors can provide feedback directly through the platform, helping improve proposal quality.

- [Upload Thesis Files](https://live.rbg.tum.de/w/artemisintro/53609)  
  Allows students to upload their completed thesis documents and presentations. This section supports version history and locks file uploads after final submission, ensuring that no changes are made post-submission.

- [Create Presentation Draft](https://live.rbg.tum.de/w/artemisintro/53604)  
  Provides students with a section to draft their presentation, which can be reviewed and adjusted by supervisors. Finalizing this draft ensures that the presentation aligns with thesis requirements and is accessible to invited attendees.

- [Manage User Settings](https://live.rbg.tum.de/w/artemisintro/53605)  
  Enables students to configure their account settings, including personal information such as study program and contact details, ensuring all details are up-to-date.

- [Book Interview Slot](https://live.rbg.tum.de/w/artemisintro/70067)
  Allows students to view available interview slots and book a preferred timeslot.

- **Request Data Export**
  Allows students to request an export of all their personal data (profile, applications, theses, uploaded files) as a ZIP file. Accessible from the Privacy page or directly at `/data-export`.

- **Import Profile Picture**
  Allows students to import their profile picture from Gravatar via the profile settings page. The lookup is performed server-side to protect the user's IP address.

#### Supervisor

- [Create Thesis Topic](https://live.rbg.tum.de/w/artemisintro/53599)  
  Allows supervisors to create new thesis topics with relevant details, enabling students to browse and apply for them. This feature helps streamline the matching of students to research-aligned topics.

- [Review Applications](https://live.rbg.tum.de/w/artemisintro/53601)  
  Provides supervisors with tools to review student applications. Supervisors can assess motivation letters, academic backgrounds, and make an informed decision on each applicant.

- [Review Proposal](https://live.rbg.tum.de/w/artemisintro/53602)  
  Enables supervisors to review submitted proposals, provide structured feedback, and help students refine their project objectives and approach before starting full thesis work.

- [Add Comments](https://live.rbg.tum.de/w/artemisintro/53600)  
  Lets supervisors post comments and attach relevant files as milestones or feedback for students. This ensures key information is documented and easily referenced during the thesis process.

- [Schedule Presentation](https://live.rbg.tum.de/w/artemisintro/53603)  
  Provides a scheduling feature for thesis presentations, allowing supervisors to set dates and invite relevant attendees, ensuring that students have a formal opportunity to present their work.

- [Submit Thesis Assessment](https://live.rbg.tum.de/w/artemisintro/53598)  
  Enables supervisors to submit an evaluation of the thesis, including a recommended grade. This assessment informs the final grading and captures key feedback for student growth.

- [Create Interview Process (Interview Page)](https://live.rbg.tum.de/w/artemisintro/70059)  
  Shows how supervisors can create an interview process directly from the interview page.

- [Create Interview Process (Application Page)](https://live.rbg.tum.de/w/artemisintro/70060)  
  Shows how supervisors can create an interview process while reviewing applications.

- [Add Interviewees to Process](https://live.rbg.tum.de/w/artemisintro/70061)  
  Demonstrates how supervisors can add selected applicants to an interview process.

- [Create Interview Slots](https://live.rbg.tum.de/w/artemisintro/70062)  
  Demonstrates how supervisors can define available interview timeslots.

- [Invite Interviewees (Bulk)](https://live.rbg.tum.de/w/artemisintro/70063)  
  Shows how supervisors can send invitations to multiple interviewees at once.

- [Invite Interviewees (Individual)](https://live.rbg.tum.de/w/artemisintro/70064)  
  Shows how supervisors can send invitations to a single interviewee.

- [Assign Interviewee](https://live.rbg.tum.de/w/artemisintro/70065)  
  Demonstrates assigning an interviewee to a specific slot or interviewer.

- [Assess Interviewees](https://live.rbg.tum.de/w/artemisintro/70066)  
  Shows how supervisors can evaluate interviewees during or after interviews.

#### Examiner

- [Add Final Grade + Complete Thesis](https://live.rbg.tum.de/w/artemisintro/53610)  
  Allows examiners to add the final grade and officially mark the thesis as complete. This feature consolidates all feedback and grading, ensuring the thesis lifecycle is fully documented.

#### Research Group Admin

- [Update Research Group Information](https://live.rbg.tum.de/w/artemisintro/70058)  
  Shows how research group admins can update general research group details.

- [Change Research Group Settings](https://live.rbg.tum.de/w/artemisintro/70057)  
  Demonstrates how research group admins can configure research group settings.

- [Add Members to Research Group](https://live.rbg.tum.de/w/artemisintro/70056)  
  Shows how research group admins can add members to the research group.

- [Make a Member Research Group Admin](https://live.rbg.tum.de/w/artemisintro/70055)
  Demonstrates how research group admins can grant admin permissions to a member.

- **Configure Scientific Writing Guide**
  Allows research group admins to set a custom link to scientific writing guidelines in the research group settings. This link is shown to students during the thesis writing phase.

#### Admin

- **Data Retention Management**
  Admins can view data retention status and manually trigger the cleanup process from the Data Retention admin page. The nightly cleanup automatically deletes rejected applications older than 1 year and expired data export files.

- **Delete Rejected Applications**
  Admins can permanently delete rejected applications from the application detail page.

#### Thesis Page Permissions

Admins can view and edit all theses on the platform.
For the other roles, please view this access table.
Examiner, Supervisor and Student means that the user is directly assigned to the thesis with that role.

| Permission                     | Examiner | Supervisor | Student | Viewer |
| ------------------------------ | -------- | ---------- | ------- | ------ |
| Configure Thesis               | ✅       | ✅         | ❌      | ❌     |
| View Student Information       | ✅       | ✅         | ❌      | ❌     |
| Update Thesis Credits          | ✅       | ✅         | ❌      | ❌     |
| View Thesis Info               | ✅       | ✅         | ✅      | ✅     |
| Add Abstract / Links           | ✅       | ✅         | ✅      | ❌     |
| Edit Thesis Titles             | ✅       | ✅         | ✅      | ❌     |
| Upload Proposal                | ✅       | ✅         | ✅      | ❌     |
| View Proposal                  | ✅       | ✅         | ✅      | ✅     |
| View Proposal History          | ✅       | ✅         | ✅      | ❌     |
| Delete Proposal                | ✅       | ✅         | ❌      | ❌     |
| Request Proposal Changes       | ✅       | ✅         | ❌      | ❌     |
| Accept Proposal                | ✅       | ✅         | ❌      | ❌     |
| Upload Thesis Files            | ✅       | ✅         | ✅      | ❌     |
| View Thesis Files              | ✅       | ✅         | ✅      | ✅     |
| View Thesis File History       | ✅       | ✅         | ✅      | ❌     |
| Delete Thesis File             | ✅       | ✅         | ❌      | ❌     |
| Create Presentation Draft      | ✅       | ✅         | ✅      | ❌     |
| Schedule Presentation          | ✅       | ✅         | ❌      | ❌     |
| Post / View Student Comment    | ✅       | ✅         | ✅      | ❌     |
| Post / View Supervisor Comment | ✅       | ✅         | ❌      | ❌     |
| Submit Thesis                  | ✅       | ✅         | ✅      | ❌     |
| Add Assessment                 | ✅       | ✅         | ❌      | ❌     |
| View Assessment                | ✅       | ✅         | ❌      | ❌     |
| Add Final Grade                | ✅       | ❌         | ❌      | ❌     |
| View Final Grade               | ✅       | ✅         | ✅      | ❌     |
| Complete Thesis                | ✅       | ❌         | ❌      | ❌     |
| Create Interview Process       | ✅       | ✅         | ❌      | ❌     |
| Add Interviewee                | ✅       | ✅         | ❌      | ❌     |
| Create Interview Slots         | ✅       | ✅         | ❌      | ❌     |
| Send Invitation                | ✅       | ✅         | ❌      | ❌     |
| Schedule Interview             | ✅       | ✅         | ✅      | ❌     |
| Cancel Interview               | ✅       | ✅         | ✅      | ❌     |
| Assess Interviewee             | ✅       | ✅         | ❌      | ❌     |

#### Research Group Permissions

Supervisors and Examiners, who are not Group Admins, don't have any of the permissions below.
Group heads have the Group Admin role for their group by default (this cannot be changed).

| Permission                        | Admins | Group Admins |
| --------------------------------- | ------ | ------------ |
| Create Research Group             | ✅     | ❌           |
| Browse Research Groups            | ✅     | ❌           |
| Update Research Group Details     | ✅     | ✅           |
| Update Research Group Settings    | ✅     | ✅           |
| Add Member to Research Group      | ✅     | ✅           |
| Remove Member from Research Group | ✅     | ✅           |
| Assign Group Admin role to Member | ✅     | ✅           |

## Developer Documentation

1. [Production Setup](docs/PRODUCTION.md)
2. [Configuration](docs/CONFIGURATION.md)
3. [Customizing E-Mails](docs/MAILS.md)
4. [Development Setup](docs/DEVELOPMENT.md) (includes [E2E Tests](docs/DEVELOPMENT.md#e2e-tests-playwright))
5. [Database Changes](docs/DATABASE.md)
6. [Data Retention Policy](docs/DATA_RETENTION.md)

## Features

The following flowchart diagrams provide a visual overview of the thesis processes implemented in ThesisManagement.
These diagrams illustrate the step-by-step workflows involved, from thesis topic selection and application submission to the final grading and completion stages.
They highlight key actions, decision points, and interactions between students, supervisors, and examiners, clarifying how tasks are sequenced and managed within the system.
These flowcharts offer a quick reference for understanding how each role engages in the thesis process, ensuring transparency and consistency in task progression and responsibilities across different stages.

#### Thesis Application Flowchart

![Thesis Application Flowchart](docs/files/thesis-application-flowchart.svg)

#### Automatic Application Expiration

Applications that have not been reviewed within a configurable period are automatically rejected. Research group admins can configure the expiration delay in weeks (minimum 2 weeks) in the research group settings. When an application expires, the student receives the standard rejection email notification, so they can reapply or pursue other options.

This mechanism ensures that students are not left waiting indefinitely for a response and enables the system to clean up application data after the retention period.

#### Thesis Writing Flowchart

![Thesis Writing Flowchart](docs/files/thesis-writing-flowchart.svg)

#### Privacy and Data Protection

The platform includes GDPR-compliant privacy and data protection features:

- **Privacy Statement**: A comprehensive privacy page accessible to all users (authenticated and unauthenticated) that documents all data processing activities, legal bases, retention periods, and data subject rights.
- **Data Export (Art. 15 / Art. 20)**: Authenticated users can request an export of all their personal data from the Data Export page (also linked from the Privacy page). Exports are generated as ZIP files containing structured JSON data (profile, applications, theses, assessments) and uploaded documents (CV, degree report, examination report). Exports are processed overnight and the user receives an email notification with a link to download. Downloads are available for 7 days and users can request a new export every 7 days. See the [Data Retention Policy](docs/DATA_RETENTION.md) for details.
- **Data Retention**: Automated cleanup of expired data runs nightly. Rejected applications are deleted after 1 year. Data export files are deleted after 7 days. Admins can trigger the cleanup manually from the Data Retention admin page. See the [Data Retention Policy](docs/DATA_RETENTION.md) for the full retention schedule and rationale.
- **Application Deletion**: Admins can permanently delete rejected applications from the application detail page.
- **Profile Picture Import**: Users can import their profile picture from Gravatar via their profile settings. The lookup is performed server-side to avoid exposing the user's IP address to external services.

#### Research Group Settings

Research group admins can configure per-group settings:

- **Scientific Writing Guide**: A customizable link to scientific writing guidelines shown to students during the thesis writing phase. Each research group can configure its own link in the research group settings page.

> [!NOTE]
> **Couldn't find what you were looking for?**
> If you need any further help or want to be onboarded to the system, reach out to us at **[thesis-management-support.aet@xcit.tum.de](thesis-management-support.aet@xcit.tum.de)**.
