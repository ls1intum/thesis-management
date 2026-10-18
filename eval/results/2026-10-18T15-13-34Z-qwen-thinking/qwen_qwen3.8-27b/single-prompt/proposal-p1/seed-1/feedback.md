# proposal-p1 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 32
- **Findings:** 17
- **Cost:** 1 calls, 0 tool calls, depth 1, 17482 tokens, 163s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a clear and well-motivated technical problem (migrating Artemis's CI from external Jenkins/Bamboo to GitLab's integrated CI) but fails to meet the required structural template: it is missing the Introduction, Motivation, and Objectives sections entirely, lacks an AI transparency statement, and has only 5 references and 1 figure (which is a forbidden sequence diagram). The schedule includes thesis-writing tasks and is not structured as 2-4 week agile iterations. While the technical content shows solid domain understanding, the proposal would need a substantial restructure to align with the group's format requirements before it could be considered ready for review.

## Findings

### 1. Missing Introduction Section

`CRITICAL` · `structure`

The proposal lacks a dedicated Introduction section. The required sections are: Abstract, Introduction, Problem, Motivation, Objective (with subsections), Schedule, Bibliography. The proposal jumps directly from Abstract to Section 1 'Problem Statement' without any introductory text or section.

- Page 2

### 2. Missing Motivation Section

`CRITICAL` · `structure`

There is no separate Motivation section in the proposal. The guidelines require a dedicated Motivation section that is visionary and outlines the scientific importance of solving the problem. The proposal has 'Proposed Solution' and 'Expected Findings' but no Motivation section.

- Page 2

### 3. Missing Objective Section with Subsections

`CRITICAL` · `structure`

The proposal lacks a dedicated Objectives section (Section 4 in the required structure) that starts with a short introduction, a bullet list of 3-4 high-level objectives, and a subsection for each objective (at least two paragraphs each). Section 4 is 'System Requirements and Top Level Design' and Section 5 is 'Expected Findings' — neither fulfills the required Objectives structure.

- Page 4

### 4. Missing AI Transparency Statement

`CRITICAL` · `completeness`

The proposal does not contain an AI transparency statement. The guidelines require a specific, first-person statement naming the AI tools used, their purpose, the sections involved, and confirming the author carefully checked all AI-generated text.

- Page 6

### 5. Bibliography Contains Only 5 Publications

`CRITICAL` · `citation`

The bibliography lists only 5 references ([Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]). The guidelines require at least 6-8 peer-reviewed publications. At least one additional peer-reviewed source must be added.

- Page 5, References: “[Booch95] Booch, G. ... [Gamma95] Gamma, E.; ... [Hilton16] Hilton, M.; ... [Krusche17] Krusche, S.; ... [Krusche18] Krusche, S.; ...”

### 6. Only One Figure in Proposal

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1 on page 4). The guidelines require at least two diagrams/mockups/figures. A second figure (e.g., a class diagram, component diagram, or architecture mockup) must be added.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Figure 1 Is a Sequence Diagram (Explicitly Forbidden)

`MAJOR` · `figures`

Figure 1 shows two lifelines (Instructor and Student) with numbered messages flowing between them (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3), which is a sequence diagram. The guidelines explicitly state: 'NO sequence diagrams — suggest activity or communication diagrams instead.' This figure must be replaced with an activity diagram, communication diagram, or component diagram.

- Page 4, System Requirements and Top Level Design: “1.1: Push code template, test cases and sample solution ... 1.2: Assign exercise ... Instructor ... 2.2: Setup student repository ... 3.2: Trigger CI ... 3.3: Send build results ... 2.1: Participate in exercise ... 3.1: Push code ... Student”

### 8. Schedule Contains Thesis Writing and Documentation Tasks

`MAJOR` · `structure`

The schedule includes multiple thesis writing and documentation tasks that violate the guideline: 'The schedule should NOT include thesis writing, documentation writing, or presentation preparation tasks.' Specifically: 'October: Writing . . .', 'November: 2nd week: Thesis ready for review', 'November: 3rd week: Review finished', 'December: 1st week: Thesis', and 'September: 2nd week: Documentation for installation and configuration of Artemis with GitLab CI'.

- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “2nd week: Thesis ready for review”
- Page 5, Time Schedule: “December – 1st week: Thesis”
- Page 5, Time Schedule: “2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 9. Schedule Not Structured as 2-4 Week Iterations

`MAJOR` · `structure`

The schedule is organized by calendar months with sporadic week entries rather than consistent 2-4 week iterations. Gaps between work items exceed 4 weeks in several places (e.g., between May 3rd week and June 4th week is approximately 5-6 weeks; between April 4th week and May 3rd week is about 3-4 weeks but with no deliverable at the end of the iteration). The work should be restructured into consistent 2-4 week sprints with measurable deliverables at the end of each.

- Page 5, Time Schedule: “April – 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI ... May – 3nd week: prototype implementation: Artemis API for confguration ... June – 4th week: prototype implementation: Sending build results to Artemis”

### 10. Schedule Includes a Requirements Activity

`MINOR` · `structure`

The schedule lists 'Requirements (parallel to prototype implementation)' in June. While it is parallel to implementation, the guidelines state there must NOT be an iteration dedicated solely to collecting requirements. In agile practice, requirements are picked from a backlog each sprint rather than gathered in a dedicated activity. This item should be removed or reframed as part of a specific feature iteration.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”

### 11. Forbidden Sentence Starters: 'Since', 'To', 'In order to'

`MINOR` · `writing`

Multiple sentences begin with forbidden words. The guidelines prohibit sentences starting with 'As…', 'Since…', 'To…', 'In order to…', or 'Because…'. Four violations were found.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required...”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases...”
- Page 2, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations...”
- Page 3, Related Work: “**Since** the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity...”

### 12. Inconsistent Terminology: 'CI server' vs 'CI service'

`MINOR` · `formatting`

The proposal uses both 'CI server' and 'CI service' to refer to the same concept (e.g., 'migrate the CI service from external applications', 'the CI and the Version Control (VC) are running on different servers', 'CI server for executing the tests'). Consistent terminology should be used throughout.

- Page 1, Abstract: “migrate the **CI service** from external applications as Jenkins to the inbuilt **CI service**”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a **CI server** for executing the tests.”
- Page 2, Proposed Solution: “the **CI service** should be part of the VC server”

### 13. Use of First-Person Singular 'I'

`MINOR` · `writing`

The guidelines state to avoid 'I' and 'our'. The schedule section uses 'I' twice: 'I will not have time to work on my thesis' and 'I plan to work on my thesis for about 2 weeks'. These should be rephrased in a more formal register.

- Page 4, Time Schedule: “During this time, **I** will not have time to work on my thesis. Instead, **I** plan to work on my thesis for about 2 weeks during the summer holidays in August.”

### 14. Figure Not Explicitly Referenced by Number in Text

`MINOR` · `figures`

The text references the figure indirectly as 'This process is illustrated in section 4' rather than explicitly stating 'Figure 1 shows...'. Every figure must be clearly referenced in the text by its figure number.

- Page 2, Proposed Solution: “This process is illustrated in **section 4**.”

### 15. Schedule Does Not Reference High-Level Objectives

`MINOR` · `structure`

The schedule does not reference any high-level goals or objectives. The guidelines state the schedule should 'reference the high-level goals from the Objectives section.' Since no proper Objectives section exists, the schedule items should at minimum be traceable to clearly stated project goals.

- Page 5, Time Schedule: “It is planned to adhere to the following time schedule:”

### 16. Passive Voice Used Excessively

`MINOR` · `writing`

Several key technical descriptions use passive voice where active voice would be clearer and more direct. Examples include 'the build plan is copied', 'the build plan is fetched', 'the build plan is downloaded', 'the student base repository is copied'. The guidelines require active voice with identified actors/subjects.

- Page 3, System Requirements and Top Level Design: “The student base repository **is copied** [Krusche18].”
- Page 2, Proposed Solution: “the build plan **is fetched**. It could be transposed with a CI server which is dynamically updating the student build plan.”
- Page 3, System Requirements and Top Level Design: “GitLab CI fetches the build plan from the API and executes the test cases according to the plan.”

### 17. Filler Words: 'Furthermore' and 'Additionally' Used Repeatedly

`MINOR` · `writing`

The word 'Furthermore' appears in the Abstract and Section 2, and 'Additionally' appears in Section 1. While not extreme, these fillers add wordiness without adding information and should be removed or replaced with more direct phrasing.

- Page 1, Abstract: “**Furthermore**, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Proposed Solution: “**Furthermore**, the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”
- Page 2, Problem Statement: “**Additionally**, the base build plan is copied once after the student started the exercise [Krusche18].”
