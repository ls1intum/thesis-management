# proposal-p1 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 22
- **Findings:** 15
- **Cost:** 1 calls, 0 tool calls, depth 1, 18951 tokens, 165s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant and well-scoped problem (migrating CI from Jenkins/Bamboo to GitLab CI for the Artemis learning platform) with a clear related-work discussion, but it has fundamental structural deficiencies: four required sections (Introduction, Motivation, Objective with subsections, AI Transparency Statement) are entirely missing, the bibliography has only 5 entries (minimum 6), there is only one figure and it is a forbidden sequence diagram, and the schedule is organized by months with thesis-writing tasks rather than agile 2-4 week iterations. These critical gaps must be resolved before the proposal is viable.

## Findings

### 1. Missing Introduction Section

`CRITICAL` · `structure`

The proposal lacks a dedicated 'Introduction' section. The document jumps directly from the Abstract to '1 Problem Statement'. An Introduction section (less than one page) is required to provide context, background, and an overview of the thesis topic before diving into the problem.

### 2. Missing Motivation Section

`CRITICAL` · `structure`

The proposal has no dedicated 'Motivation' section. While the Abstract contains a 'Motivation/Objective' label and 'Expected Findings' touches on benefits, there is no standalone Motivation section that is visionary, focuses on the positive aspects of solving the problem, and is at most 3/4 page. This is a required section.

### 3. Missing Objective Section with Subsections

`CRITICAL` · `structure`

The proposal lacks a dedicated 'Objective' section. The required structure is: a short introduction, a bullet list of 3-4 high-level objectives (each fitting one line), followed by a subsection per objective (each at least two paragraphs). The existing '4 System Requirements and Top Level Design' and '5 Expected Findings' do not fulfil this structure. Objectives must be in action form with single verbs.

### 4. Missing AI Transparency Statement

`CRITICAL` · `completeness`

The proposal contains no AI transparency statement. A first-person statement is required that names specific AI tools used, their purpose, the sections they were applied to, and includes a confirmation sentence that the author has carefully checked all AI-generated text.

### 5. Bibliography Contains Only 5 Publications

`CRITICAL` · `citation`

The bibliography lists only 5 entries ([Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]). The minimum requirement is 6-8 peer-reviewed publications. At least one additional peer-reviewed source must be added.

- Page 5, References: “[Booch95] Booch, G. ... [Gamma95] Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. ... [Hilton16] Hilton, M.; ... [Krusche17] Krusche, S.; ... [Krusche18] Krusche, S.; Seitz, A.”

### 6. Only One Figure in the Proposal

`CRITICAL` · `figures`

The proposal contains only Figure 1 (a sequence diagram on page 4). The minimum requirement is at least 2 diagrams/mockups/figures. A second figure (e.g., a component diagram, class diagram, or architecture mockup) must be added.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Figure 1 is a Sequence Diagram (Forbidden)

`MAJOR` · `figures`

Figure 1 shows numbered messages (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3) between actors (Instructor, Student) in a classic UML sequence diagram format. The guidelines explicitly prohibit sequence diagrams. Replace with an activity diagram or a communication diagram to show the same interactions.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 8. Schedule Organized by Months Instead of 2-4 Week Iterations

`MAJOR` · `structure`

The schedule is structured by calendar months (March, April, May, June, September, October, November, December) with scattered week references. It does not divide work into consistent 2-4 week iterations/sprints. Restructure into numbered iterations (e.g., 'Iteration 1: Weeks 1-3', 'Iteration 2: Weeks 4-6') with clear start/end and measurable deliverables per iteration.

- Page 4, Time Schedule: “It is planned to adhere to the following time schedule:”
- Page 5, Time Schedule: “– 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI”

### 9. Schedule Includes Thesis Writing and Documentation Tasks

`MAJOR` · `structure`

The schedule contains multiple thesis-writing and documentation tasks that should be removed: 'October: Writing', 'November: Thesis ready for review', 'November: Review finished', 'December: 1st week: Thesis', and 'September: Documentation for installation and configuration of Artemis with GitLab CI'. The schedule should focus exclusively on development/research work.

- Page 5, Time Schedule: “– 2nd week: Documentation for installation and confguration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “– 2nd week: Thesis ready for review – 3rd week: Review fnished – 4th week: Implement improvements”
- Page 5, Time Schedule: “– 1st week: Thesis”

### 10. Inconsistent Terminology for CI Component

`MAJOR` · `formatting`

The same concept is referred to inconsistently throughout: 'CI server' (Section 1), 'CI service' (Abstract, Section 2), 'CI system' (Section 2, Section 5), and 'CI application' (Abstract). Pick one term (e.g., 'CI server') and use it consistently throughout the proposal.

- Page 1, Abstract: “migrate the CI service from external applications as Jenkins to the inbuilt CI service”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a CI server for executing the tests”
- Page 2, Proposed Solution: “The solution for the problem described in section 1 is a CI system not storing each build plan individually”
- Page 2, Proposed Solution: “the CI service should be part of the VC server to reduce additional efort”

### 11. Excessive Use of Passive Voice

`MAJOR` · `writing`

Passive voice is used throughout the proposal, obscuring the actors performing actions. Examples include: 'the base build plan is copied', 'changes are not patched automatically', 'the build plans must be updated manually', 'GitLab CI gets configured', 'the student base repository is copied', 'the build plan is fetched', 'the lecture is split up'. Rewrite in active voice identifying the subject (e.g., 'Artemis copies the base build plan', 'GitLab CI fetches the build plan').

- Page 2, Problem Statement: “the base build plan is copied once after the student started the exercise”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server”
- Page 2, Proposed Solution: “GitLab CI is downloading the build plan for each execution of the test cases”
- Page 3, System Requirements and Top Level Design: “GitLab CI gets confgured so that it takes the build plan from the Artemis API”
- Page 3, Related Work: “the lecture is split up into multiple iterations”

### 12. Forbidden Sentence Starters

`MINOR` · `writing`

Several sentences begin with forbidden words: 'Since' (Abstract, Section 3), 'To' (Section 1), and 'In order to' (Section 3). Rewrite these sentences to avoid starting with these words. For example, 'Since some VC systems include a CI application' could become 'Some VC systems include a CI application, which allows...'.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server”
- Page 3, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations”
- Page 3, Related Work: “**Since** the benefts of using CI are quite high and nearly half of the developers”

### 13. Figure Caption is Too Short and Generic

`MINOR` · `figures`

The caption 'Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI' is relatively brief. It should be more descriptive and informative, explaining what the diagram illustrates, which actors are involved, and the key process flow it depicts. Aim for a 2-3 sentence caption.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 14. Schedule Contains a Requirements Task

`MINOR` · `structure`

The June entry 'Requirements (parallel to prototype implementation)' is a requirements-gathering task. In agile, requirements are pulled from a backlog in each sprint rather than having a dedicated requirements phase. Remove this item or fold requirements into specific sprint deliverables.

- Page 5, Time Schedule: “– 4th week: Requirements (parallel to prototype implementation)”

### 15. Problem Section Does Not Explicitly Identify Actors

`MINOR` · `completeness`

While the Problem section mentions students and smaller universities, it does not explicitly identify the affected actors/stakeholders and systematically describe how the problem negatively impacts each one. Add a clear identification of actors (e.g., instructors, students, university IT administrators, smaller universities) and describe the specific negative consequence for each.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis”
