# proposal-p1 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 25
- **Findings:** 25
- **Cost:** 10 calls, 0 tool calls, depth 2, 95828 tokens, 232s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant and well-scoped problem (connecting Artemis with GitLab CI for dynamic build plans) and demonstrates domain knowledge, but it is missing multiple required sections entirely (Introduction, Motivation, Objective, AI Transparency Statement), has only one figure which is a prohibited type, falls short on bibliography entries, and presents the schedule as a waterfall timeline rather than iterative sprints. Writing issues are pervasive (passive voice, filler words, forbidden sentence starters, inconsistent terminology), and the document is below the minimum page length. Substantial restructuring and rewriting are needed before this can be considered submission-ready.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain a dedicated Introduction section. The document transitions directly from the Abstract to '1 Problem Statement'. An Introduction section is required and must be less than one page.

- Page 2, Introduction: “**1     Problem          Statement**”

### 2. Missing Motivation section

`CRITICAL` · `structure`

The proposal does not contain a standalone Motivation section as required by the guidelines. While positive aspects (e.g., more universities adopting Artemis, saving time/money, new research opportunities) are scattered in 'Expected Findings' (Section 5) and the visionary scenario in Section 4, there is no dedicated section that is visionary, explains why it is scientifically important to solve the problem, and focuses on the positive vision of having the solution. The Motivation section must be added as a separate, clearly labeled section of at most 3/4 page.

- Page 4, Expected Findings: “universities will save time and money as they will need at least one less server”
- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”

### 3. Missing Objective section with required subsections

`CRITICAL` · `structure`

The proposal lacks an 'Objective' section structured as required: a short introduction, a bullet list of 3-4 high-level objectives (each fitting in one line), and a subsection for each objective (at least two paragraphs long). The 'Expected Findings' section is framed as expectations ('We expect to use...', 'might be extended') rather than action-form objectives, and no subsections with detailed descriptions are present. All objectives must be reformulated in action form (e.g., 'Extend the Artemis API to expose build plans' rather than 'The API of Artemis might be extended').

- Page 3, Objective: “**4     System        Requirements              and     Top     Level     Design**”
- Page 4, Expected Findings: “This research thesis **deals with the question on how to connect** Artemis with GitLab CI. We **expect to use** the bridge pattern, described by Gamma et al.”
- Page 4, Expected Findings: “For making the build plan accessible, the API of Artemis **might be extended** for this service.”

### 4. AI Transparency Statement is entirely missing

`CRITICAL` · `completeness`

The proposal does not contain an AI Transparency Statement. The guidelines explicitly require this section. It must be written in the first person, name specific AI tools used, state the purpose of each tool, identify which sections they were applied to, and include a sentence confirming the author has carefully checked all AI-generated text. As the section is entirely absent, none of these requirements can be evaluated or satisfied.

- Page 5, AI Transparency Statement: “[Section entirely absent from document]”

### 5. Bibliography contains fewer than the minimum 6 required publications

`CRITICAL` · `citation`

The bibliography lists only 5 entries ([Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]), but the guidelines require at least 6–8 peer-reviewed publications. The student must add at least one (ideally 2–3) additional relevant peer-reviewed publication to meet the minimum threshold.

- Page 5, References: “**[Booch95]** Booch, G. Object-oriented analysis and design... **[Gamma95]** Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. Design Patterns... **[Hilton16]** Hilton, M.; Tunnell, T.; Huang, K.; Marinov, D.; Dig, D. Usage, costs, and benefits... **[Krusche17]** Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning... **[Krusche18]** Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System...”

### 6. Only one figure present; minimum of two required

`CRITICAL` · `completeness`

The proposal contains only a single figure (Figure 1 on page 4). The guidelines require at least two diagrams/mockups. A second figure (e.g., a component diagram, architecture diagram, or class diagram) must be added to meet the minimum requirement.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Schedule is not structured into 2-4 week iterations

`CRITICAL` · `structure`

The schedule is organized as a month-by-month linear timeline with sporadic week references (e.g., '2nd week', '4th week') rather than clearly defined 2-4 week iterations/sprints. There is no grouping of work into discrete iterative cycles. The entire schedule reads as a waterfall Gantt-chart timeline from March to December, not as a series of 2-4 week iterations with clear boundaries.

- Page 4, Time Schedule: “It is planned to adhere to the following time schedule: March – 5th week: Proposal”
- Page 5, Time Schedule: “October: Writing . . .”

### 8. Thesis writing and documentation tasks included in schedule

`CRITICAL` · `logic`

Multiple entries in the schedule are explicitly dedicated to thesis writing or documentation: 'October: Writing . . .', 'November 2nd week: Thesis ready for review', 'November 3rd week: Review finished', 'December 1st week: Thesis', and 'September 2nd week: Documentation for installation and configuration'. The schedule should focus solely on development/research work and exclude thesis writing, documentation writing, and presentation preparation tasks.

- Page 5, Time Schedule: “October: **Writing . . .**”
- Page 5, Time Schedule: “2nd week: **Thesis ready for review**”
- Page 5, Time Schedule: “3rd week: **Review finished**”
- Page 5, Time Schedule: “1st week: **Thesis**”
- Page 5, Time Schedule: “2nd week: **Documentation for installation and configuration of Artemis with GitLab CI**”

### 9. Figure 1 is a sequence diagram, which is explicitly prohibited

`MAJOR` · `figures`

Figure 1 depicts two actors (Instructor, Student) with numbered point-to-point messages (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3) exchanged vertically, which is the structure of a UML sequence diagram. The guidelines explicitly state that sequence diagrams are not acceptable. The student should replace or supplement this with an activity diagram (showing the workflow of the exercise) or a component diagram (showing how Artemis, GitLab CI, the API, and the VC server interact). At least one figure must be a suitable UML diagram (class, activity, component, etc.).

- Page 4, System Requirements and Top Level Design: “1.1: Push code template, test cases and sample solution ... 1.2: Assign exercise ... Instructor ... 2.2: Setup student repository ... 3.2: Trigger CI ... 3.3: Send build results ... 2.1: Participate in exercise ... 3.1: Push code ... Student”

### 10. Excessive use of passive voice

`MAJOR` · `writing`

The proposal contains numerous passive-voice constructions throughout. The guidelines require active voice with clear actors/subjects. Several sentences should be restructured so the agent performs the action (e.g., 'the instructor updates the build plans' instead of 'the build plans must be updated').

- Page 1, Abstract: “the build plan **is fetched** from the API on each run”
- Page 2, Problem Statement: “the build plans **must be updated** manually in the CI Server”
- Page 2, Problem Statement: “Later changes to the base build plan **are not patched automatically** to all student build plans”
- Page 3, Related Work: “the lecture **is split up** into multiple iterations”
- Page 3, System Requirements and Top Level Design: “The student base repository **is copied**”
- Page 3, System Requirements and Top Level Design: “GitLab CI **gets configured** so that it takes the build plan from the Artemis API”
- Page 3, System Requirements and Top Level Design: “The GitLab CI build **gets triggered**”

### 11. Excessive filler words and superlatives

`MAJOR` · `writing`

The proposal uses filler words and superlatives ('furthermore', 'additional', 'also', 'very', 'in addition') excessively across multiple sections. These weaken the writing and should be removed or replaced with more direct phrasing.

- Page 1, Abstract: “**Furthermore**, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “This leads to **additional** server overhead, since the CI and the Version Control (VC) are running on different servers.”
- Page 2, Proposed Solution: “**Furthermore**, the CI service should be part of the VC server to reduce **additional** effort by maintaining two servers.”
- Page 2, Problem Statement: “The current setup and implementation makes it **very** cumbersome to update the build plan afterwards.”
- Page 3, Related Work: “but the build is **also** less likely to fail in the master branch”
- Page 4, Expected Findings: “**In addition**, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “**In addition**, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 12. Forbidden sentence starters

`MAJOR` · `writing`

Five sentences begin with forbidden words/phrases ('Since', 'To', 'In order to', 'As'). Each must be restructured so the sentence opens with the subject or another non-forbidden word.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required to maintain the servers”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases”
- Page 3, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations”
- Page 3, Related Work: “**Since** the benefits of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university”
- Page 4, Expected Findings: “**As** described in section 3, this might lead to better developers and better code, due to the fact, that more students will be taught the concepts of CI.”

### 13. Use of first-person singular 'I'

`MAJOR` · `writing`

The guidelines state to avoid 'I'. The schedule section uses first-person singular twice. The student should rephrase to a neutral construction or use 'we' only if referring to the thesis approach.

- Page 4, Time Schedule: “During this time, **I** will not have time to work on my thesis. Instead, **I** plan to work on my thesis for about 2 weeks during the summer holidays in August.”

### 14. Inconsistent terminology: "CI server" vs "CI service" vs "CI system"

`MAJOR` · `writing`

The author uses three different terms — "CI server," "CI service," and "CI system" — interchangeably to refer to the same concept (the CI component of the platform). Since this is the core concept of the proposal, a single consistent term (e.g., "CI server" for standalone systems and "CI service" only when specifically referring to the integrated GitLab CI component) should be used throughout the author's own prose.

- Page 1, Abstract: “migrate the **CI service** from external applications as Jenkins to the inbuilt **CI service**”
- Page 1, Abstract: “The interactive learning platform Artemis currently only supports setups with an external **CI server**”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a **CI server** for executing the tests”
- Page 2, Proposed Solution: “a **CI system** not storing each build plan individually”
- Page 2, Proposed Solution: “the **CI service** should be part of the VC server to reduce additional efort”

### 15. Paragraph exceeds 10 lines and covers multiple unrelated ideas

`MAJOR` · `writing`

The paragraph beginning 'In their paper Interactive Learning…' spans from page 2 into page 3, making it well over 10 rendered lines. It also compresses at least three distinct ideas into a single paragraph: (a) the interactive learning concept and its purpose, (b) the specific shortening of the theory-practice gap, and (c) the lecture phase structure and its connection to Artemis. This paragraph should be split into two or three shorter paragraphs, each developing one idea.

- Page 2, Related Work: “In their paper Interactive Learning – Increasing Student Participation through Shorter Exercise Cycles, Krusche et al. have taken up this concept to improve the learning experience in large computer science courses at university [Krusche17]. They introduced an newly developed learning method which shortens the time between theory...”
- Page 3, Related Work: “**into multiple iterations, each consisting of different phases [Krusche17]. First there is a theory phase, before the instructor shows an example to the students. After that the students can try out the concept themselves, before they get a sample solution and instant, but still individual feedback generated by the CI server. Afterwards it is possible to reflect about the newly learned approach. With this fundamental idea, later the assessment management system Artemis was built [Krusche18].**”

### 16. Requirements-gathering task included in schedule

`MAJOR` · `logic`

The schedule explicitly lists 'Requirements' as a work item in June (parallel to prototype implementation). Per agile principles, there should not be a dedicated requirements-gathering phase; work items are selected from a backlog each iteration. This violates the rule against a requirements sprint.

- Page 5, Time Schedule: “4th week: **Requirements** (parallel to prototype implementation)”

### 17. Work items follow horizontal layer splits rather than vertically integrated features

`MAJOR` · `methodology`

The prototype work is split into horizontal layers: first 'Connectivity to GitLab CI' (April), then 'Artemis API for configuration' (May), then 'Sending build results to Artemis' (June), with 'Models' as a separate parallel track. This is a classic horizontal decomposition (build all networking, then all API, then all data flow) rather than vertically integrated end-to-end features. A vertically integrated approach would deliver, for example, a minimal working exercise submission cycle end-to-end in the first iteration, then extend it.

- Page 4, Time Schedule: “4th week: prototype implementation: **Connectivity to GitLab CI**”
- Page 5, Time Schedule: “3nd week: prototype implementation: **Artemis API for configuration**”
- Page 5, Time Schedule: “4th week: prototype implementation: **Sending build results to Artemis**”
- Page 5, Time Schedule: “4th week: **Models** (parallel to prototype implementation)”

### 18. Vague, non-measurable deliverables in schedule

`MAJOR` · `completeness`

Several work items lack measurable, concrete deliverables. 'Models (parallel to prototype implementation)' does not specify what models or what the deliverable is. 'Implement improvements' (November 4th week) does not state what improvements. 'Revise models with concrete implementation' (September) is unclear. Each iteration item should describe a specific, verifiable outcome.

- Page 5, Time Schedule: “4th week: **Models** (parallel to prototype implementation)”
- Page 5, Time Schedule: “2nd week: **Revise models with concrete implementation**”
- Page 5, Time Schedule: “4th week: **Implement improvements**”

### 19. Schedule follows waterfall progression without referencing high-level objectives

`MAJOR` · `logic`

The overall schedule follows a linear waterfall progression: setup → prototype phases → documentation → thesis writing. It does not reference the high-level goals stated in the Expected Findings/Objectives section (bridge pattern, dynamic build plan, reduced server overhead). An agile schedule should iterate toward the stated objectives, with each iteration delivering incremental value tied to the project goals.

- Page 4, Time Schedule: “It is planned to adhere to the following time schedule: March – 5th week: Proposal”
- Page 5, Time Schedule: “October: Writing . . .”

### 20. Total text length is below the 4-page minimum

`MINOR` · `structure`

Excluding metadata, figures, and the schedule, the proposal text spans approximately 3.5 pages (Abstract ~1/3, Problem ~1/2, Proposed Solution ~1/4, Related Work ~1, System Requirements ~3/4, Expected Findings ~1/2). This falls just below the 4-6 page target range. Adding the missing Introduction and Motivation sections would help bring the document into range.

- Page 1, Document: “[Document spans approximately 3.5 pages of body text]”

### 21. Figure 1 caption is generic and figure is not referenced by number in text

`MINOR` · `figures`

The caption 'Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI' is too generic. A more informative caption should describe the specific participants, the nature of the interactions (build plan retrieval via API, test execution, result reporting), and the context. Additionally, Section 2 states 'This process is illustrated in section 4,' referencing the section but not the figure by number. The guidelines require every figure to be explicitly referenced in the text (e.g., 'Figure 1 shows...').

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”
- Page 2, Proposed Solution: “This process is **illustrated in section 4**.”

### 22. Abbreviation 'API' used without prior definition

`MINOR` · `writing`

The abbreviation 'API' (Application Programming Interface) is used multiple times starting from the Abstract without ever being spelled out on first use. All other abbreviations (VC, CI) are properly defined, but API is not.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis **API**, which makes it possible to update the build plan retrospectively”
- Page 2, Proposed Solution: “The build plan can be made available via the **API** of Artemis”
- Page 3, System Requirements and Top Level Design: “GitLab CI fetches the build plan from the **API** and executes the test cases according to the plan.”

### 23. Multiple paragraphs below minimum length across sections

`MINOR` · `writing`

Several paragraphs fall below the minimum length requirement (5 lines): (1) 'In addition, universities will save time and money as they will need at least one less server.' (1 line, Expected Findings); (2) 'In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.' (1 line, Expected Findings); (3) 'The visionary scenario presents the supposed workflow of an exercise.' (1 line, System Requirements); (4) 'Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.' (2 lines, System Requirements). Each should be expanded or merged with adjacent paragraphs.

- Page 4, Expected Findings: “In addition, universities will **save time and money** as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either **Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners**.”
- Page 3, System Requirements and Top Level Design: “The visionary scenario **presents the supposed workfow of an exercise**.”
- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the **relevant actors and systems for solving the problem**.”

### 24. Inconsistent capitalization of compound nouns "CI server" and "VC server"

`MINOR` · `formatting`

The compound nouns "CI server" and "VC server" are capitalized inconsistently. In the Problem Statement, the author writes "the CI Server" (capital S) in one sentence and "a CI server" (lowercase s) in another. In the Proposed Solution, "the VC Server" (capital S) appears alongside "the VC server" (lowercase s). The capitalization should be uniform throughout (recommend lowercase as a common noun).

- Page 2, Problem Statement: “the build plans must be updated manually in the **CI Server**”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a **CI server** for executing the tests”
- Page 2, Proposed Solution: “each time the code in the **VC Server** gets updated”
- Page 2, Proposed Solution: “the CI service should be part of the **VC server**”

### 25. Inconsistent terminology: "task" vs "exercise"

`MINOR` · `writing`

The Abstract uses "task" ("after the task has been assigned") while the rest of the document consistently uses "exercise" to denote the same concept (e.g., "after the student started the exercise," "release of an exercise," "start exercise button"). Since "exercise" is the established term in the Artemis context and used throughout the body, the Abstract should also use "exercise" for consistency.

- Page 1, Abstract: “it should be possible to adjust the build plan after the **task** has been assigned”
- Page 2, Problem Statement: “the base build plan is copied once after the student started the **exercise**”
