# proposal-p1 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 24
- **Cost:** 10 calls, 0 tool calls, depth 2, 97202 tokens, 238s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant topic (integrating GitLab CI with Artemis for dynamic build plan management) and demonstrates domain knowledge, but it fails to meet most structural requirements: four mandatory sections (Introduction, Motivation, Objectives, AI Transparency Statement) are entirely missing. The schedule is structured as a waterfall with no iteration cadence, the bibliography falls below the minimum, and the single figure is a prohibited sequence diagram. Widespread writing issues (passive voice, forbidden sentence starters, filler words, undefined abbreviations) compound the structural deficiencies, making substantial revision necessary before submission.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section. The document proceeds directly from the Abstract to Section 1 'Problem Statement'. An Introduction section is required by the guidelines and must be less than one page in length.

### 2. Missing dedicated Motivation section

`CRITICAL` · `completeness`

The proposal contains no standalone Motivation section in the body text. The guidelines require a Motivation section (at most 3/4 page) that is visionary, explaining why it is scientifically important to solve the problem and focusing on the positive aspects of having the solution. The Abstract includes a brief 'Motivation/Objective' sentence and Section 5 'Expected Findings' touches on long-term benefits, but neither constitutes a proper Motivation section. Without this section, the visionary framing and the requirement to avoid repeating the Problem cannot be demonstrated.

### 3. Missing Objectives section with subsections in action form

`CRITICAL` · `completeness`

The proposal has no 'Objectives' section structured as required. Section 4 is 'System Requirements and Top Level Design' and Section 5 is 'Expected Findings', neither of which fulfills the requirement. The required structure is: a short introductory paragraph, a bullet list of 3-4 high-level objectives in action form (each fitting in one line, no double verbs), followed by a dedicated subsection for each objective (at least two paragraphs per subsection with detailed descriptions). Section 5 is written in expectation form ('We expect to use…') rather than action form and contains no subsections.

### 4. AI Transparency Statement is entirely missing

`CRITICAL` · `completeness`

The proposal does not contain an AI Transparency Statement section, which is listed as a required section. The statement must: (1) be written in first person, (2) name specific AI tools used and their purposes, (3) specify which sections of the proposal the tools were used for, and (4) include a confirmation sentence such as 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The student must add a dedicated 'AI Transparency Statement' section (typically after the Schedule and before/after References) addressing all of these points.

### 5. Bibliography contains fewer than the required minimum of 6 peer-reviewed publications

`CRITICAL` · `citation`

The bibliography lists only 5 entries: [Booch95], [Gamma95], [Hilton16], [Krusche17], and [Krusche18]. The guidelines require at least 6–8 peer-reviewed publications. The student must add at least one additional peer-reviewed publication to the bibliography to meet the minimum threshold.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.). Benjamin/Cummings series in object-oriented software engineering. Addison-Wesley. 1995. ISBN 978-0-8053-5340-2.”
- Page 5, References: “[Gamma95] Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. Design Patterns: Elements of Reusable Object-Oriented Software. Addison-Wesley Longman Publishing Co., Inc., USA. 1995. ISBN 0201633612.”
- Page 5, References: “[Hilton16] Hilton, M.; Tunnell, T.; Huang, K.; Marinov, D.; Dig, D. Usage, costs, and benefits of continuous integration in open-source projects. In 2016 31st IEEE/ACM International Conference on Automated Software Engineering (ASE). 2016, pp. 426–437.”
- Page 5, References: “[Krusche17] Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning: Increasing student participation through shorter exercise cycles. In Proceedings of the Nineteenth Australasian Computing Education Conference, ACE 2017, Geelong, VIC, Australia. ACM. 2017, pp. 17–26.”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning. ACM. 2018, 49th Technical Symposium on Computer Science Education, pp. 284–289.”

### 6. Only one figure present (minimum two required)

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1 on page 4). The guidelines require at least two diagrams/mockups/figures. A second figure is missing entirely. The student should add at least one additional figure, such as a class diagram showing the bridge pattern implementation, a component diagram of the Artemis/GitLab CI architecture, or an activity diagram of the exercise workflow.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Schedule lacks 2-4 week iteration structure and follows a waterfall progression

`CRITICAL` · `methodology`

The schedule does not divide the work into iterations of 2-4 weeks. Instead, it is organized by calendar months (March, April, May, etc.) with scattered single-week references, following a sequential waterfall-like progression (infrastructure → connectivity → API → results → models → documentation → writing). There is no clear iteration/sprint structure, no reference to the high-level objectives from 'Expected Findings', and no mention of incremental value delivery or agile practices (backlog refinement, sprint reviews, acceptance criteria). The schedule should be restructured into named iterations (e.g., 'Iteration 1: weeks 1-2') each spanning 2-4 weeks, with each iteration delivering a complete, testable feature toward the stated objectives.

- Page 4, Time Schedule: “It is planned to adhere to the following **time schedule**:”
- Page 4, Time Schedule: “**March** – 5th week: Proposal  **April** – 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI  **May** – 3nd week: prototype implementation: Artemis API for confguration”
- Page 5, Time Schedule: “**June** – 4th week: prototype implementation: Sending build results to Artemis – 4th week: Models (parallel to prototype implementation) – 4th week: Requirements (parallel to prototype implementation)”
- Page 5, Time Schedule: “**October: Writing . . .**  **November** – 2nd week: Thesis ready for review – 3rd week: Review fnished – 4th week: Implement improvements”

### 8. Thesis writing and documentation tasks included in development schedule

`CRITICAL` · `completeness`

The schedule includes multiple thesis-writing and documentation tasks that should not appear in a development schedule. 'October: Writing…', 'November: Thesis ready for review', 'November: Review finished', 'December: 1st week: Thesis', 'September: Documentation for installation and configuration', and 'March: 5th week: Proposal' are all writing/documentation activities. The schedule should focus exclusively on development and research work items.

- Page 5, Time Schedule: “**October: Writing . . .**”
- Page 5, Time Schedule: “**November** – 2nd week: **Thesis ready for review** – 3rd week: **Review fnished**”
- Page 5, Time Schedule: “**December** – 1st week: **Thesis**”
- Page 5, Time Schedule: “**September** – 2nd week: **Documentation for installation and confguration of Artemis with GitLab CI**”
- Page 5, Time Schedule: “**March** – 5th week: **Proposal**”

### 9. Figure 1 is a prohibited sequence diagram and no suitable UML diagram is present

`MAJOR` · `figures`

Figure 1 shows numbered messages (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3) flowing between actors (Instructor, Student) and systems in a top-to-bottom chronological order, which is the characteristic structure of a UML sequence diagram. Sequence diagrams are explicitly prohibited by the guidelines. Since this is the only figure, the proposal does not contain any suitable UML diagram. The student should replace this with an activity diagram (showing the workflow as a flow of actions and decisions) or a communication diagram (showing objects linked together with numbered message identifiers on the links, without the vertical time axis), and add a second figure such as a class diagram of the bridge pattern.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 10. Forbidden sentence starters: 'Since', 'To', 'As', 'In order to'

`MAJOR` · `writing`

Multiple sentences begin with forbidden starters ('Since…', 'To…', 'As…', 'In order to…'). Each occurrence must be restructured so the sentence does not open with one of these words. The six instances below violate the explicit rule and should all be rewritten.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required to maintain the servers”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases predefined by the instructor on each submission”
- Page 2, Proposed Solution: “**As** a result, instructors are able to update the build plan after the release of an exercise, GitLab CI fetches the updated build plan when needed.”
- Page 2, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations, each consisting of different phases [Krusche17].”
- Page 3, Related Work: “**Since** the benefits of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university [Hilton16].”
- Page 4, Expected Findings: “**As** described in section 3, this might lead to better developers and better code, due to the fact, that more students will be taught the concepts of CI.”

### 11. Excessive passive voice throughout the proposal

`MAJOR` · `writing`

The text relies heavily on passive constructions, obscuring the agent. The guidelines require active voice with clearly identified actors. At least nine passive constructions were identified; each should be restructured so the performing subject appears as the grammatical subject (e.g., 'The CI server is migrated' → 'The student migrates the CI server').

- Page 1, Abstract: “The CI server **should be migrated** to the VC server.”
- Page 1, Abstract: “it should be possible to adjust the build plan after the task **has been assigned**.”
- Page 2, Problem Statement: “the base build plan **is copied** once after the student started the exercise [Krusche18].”
- Page 2, Problem Statement: “the build plans **must be updated** manually in the CI Server.”
- Page 2, Proposed Solution: “It **could be transposed** with a CI server which is dynamically updating the student build plan.”
- Page 2, Proposed Solution: “The build plan **can be made available** via the API of Artemis and GitLab CI is downloading the build plan for each execution of the test cases.”
- Page 4, Expected Findings: “the API of Artemis **might be extended** for this service.”
- Page 4, Expected Findings: “either Ansible or a Kubernetes cluster **can be used** for automatic deployment of the GitLab CI Runners.”

### 12. Excessive use of filler words ('additional', 'Additionally', 'In addition', 'Furthermore')

`MAJOR` · `writing`

The document contains repeated instances of the filler words 'additional', 'Additionally', 'In addition', and 'Furthermore' (at least seven occurrences). These add no informational content and should be removed or replaced with more precise language.

- Page 1, Abstract: “**Furthermore**, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “This leads to **additional** server overhead, since the CI and the Version Control (VC) are running on different servers.”
- Page 2, Problem Statement: “**Additionally**, the base build plan is copied once after the student started the exercise [Krusche18].”
- Page 2, Proposed Solution: “**Furthermore**, the CI service should be part of the VC server to reduce **additional** effort by maintaining two servers.”
- Page 3, Related Work: “This is why it is now possible to migrate external CI servers as Jenkins to GitLab CI and so reduce **additional** work for server administration.”
- Page 4, Expected Findings: “**In addition**, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “**In addition**, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 13. Abbreviation 'API' used without prior definition

`MAJOR` · `writing`

The abbreviation 'API' is used multiple times (pages 1, 2, 4) but is never expanded to 'Application Programming Interface' on first use. The guideline requires that abbreviations be defined before use so that readers unfamiliar with the term can follow the text.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis **API**, which makes it possible to update the build plan retrospectively”
- Page 2, Proposed Solution: “The build plan can be made available via the **API** of Artemis and GitLab CI is downloading the build plan for each execution of the test cases.”

### 14. Paragraph in Related Work exceeds 10 lines and contains multiple unrelated ideas

`MAJOR` · `writing`

The paragraph describing Krusche et al.'s interactive learning concept spans approximately 14-16 rendered lines, well exceeding the 10-line maximum. It also covers multiple distinct ideas: (1) adopting CI for learning, (2) shortening the theory-practice gap, (3) transfer of skills to use cases, (4) splitting the lecture into iterations, (5) describing the phases in detail, and (6) the subsequent building of Artemis. This paragraph should be split into at least 2-3 paragraphs of 5-8 lines each, with one coherent idea per paragraph.

- Page 2, Related Work: “In their paper Interactive Learning – Increasing Student Participation through Shorter Exercise Cycles, Krusche et al. have taken up this concept to improve the learning experience in large computer science courses at university [Krusche17]. They introduced an newly developed learning method which shortens the time between theory…With this fundamental idea, later the assessment management system Artemis was built [Krusche18].”

### 15. Schedule builds horizontal layers instead of vertically integrated features

`MAJOR` · `methodology`

The schedule builds the system in horizontal layers: first infrastructure (Docker GitLab CI Runner), then connectivity (Connectivity to GitLab CI), then API (Artemis API for configuration), then result reporting (Sending build results to Artemis), then data models. Each item addresses a single technical component rather than an end-to-end feature. A vertically integrated schedule would describe complete user stories, e.g., 'Iteration: Student submits code, GitLab CI executes build, and result is displayed in Artemis (end-to-end).' The current split means no iteration delivers a complete, testable feature.

- Page 5, Time Schedule: “April – 2nd week: **Docker GitLab CI Runner confguration** – 4th week: prototype implementation: **Connectivity to GitLab CI**”
- Page 5, Time Schedule: “May – 3nd week: prototype implementation: **Artemis API for confguration**”
- Page 5, Time Schedule: “June – 4th week: prototype implementation: **Sending build results to Artemis** – 4th week: **Models** (parallel to prototype implementation)”

### 16. Vague and non-measurable work items in schedule

`MAJOR` · `methodology`

Several items lack specificity and measurable acceptance criteria: 'Models (parallel to prototype implementation)' does not specify what models or what the deliverable is; 'Implement improvements' (November) does not state what improvements or how success is measured; 'Revise models with concrete implementation' is unclear in scope. Each iteration item should state a concrete, verifiable deliverable (e.g., 'Artemis API endpoint /build-plan returns valid YAML for a configured exercise, verified by integration test').

- Page 5, Time Schedule: “4th week: **Models (parallel to prototype implementation)**”
- Page 5, Time Schedule: “4th week: **Implement improvements**”
- Page 5, Time Schedule: “2nd week: **Revise models with concrete implementation**”

### 17. Requirements-gathering task present in development schedule

`MAJOR` · `methodology`

The June entry 'Requirements (parallel to prototype implementation)' is a requirements-gathering task. In agile practice, requirements are not collected in a dedicated task; instead, items are pulled from a backlog each iteration. This item should be removed and replaced with concrete development work.

- Page 5, Time Schedule: “4th week: **Requirements (parallel to prototype implementation)**”

### 18. Problem section does not explicitly identify all affected actors

`MINOR` · `completeness`

The Problem section mentions 'smaller universities' and 'students' but never explicitly names instructors or teaching staff, who are the primary actors affected by the inability to update build plans manually. The sentence 'the build plans must be updated manually in the CI Server' implies an actor (the instructor) without naming them. To satisfy the rule that the Problem section must identify actors and describe how the problem negatively influences each one, the section should explicitly name stakeholders (e.g., instructors, students, university administrators) and map each negative consequence to a specific actor.

- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans **must be updated manually in the CI Server**. Especially in large software engineering courses this is nearly impossible.”
- Page 2, Problem Statement: “The current setup and implementation makes it **very cumbersome** to update the build plan afterwards.”

### 19. Figure 1 not explicitly referenced in body text and caption lacks specificity

`MINOR` · `figures`

The figure is only indirectly referenced via the phrase 'This process is illustrated in section 4' (page 2). There is no explicit reference to 'Figure 1' anywhere in the body text. Additionally, the caption 'Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI' does not identify the specific actors involved (Instructor, Student), the concrete process being illustrated (exercise submission, CI build triggering, result delivery), or the interaction pattern depicted. Every figure must be explicitly referenced by its number, and the caption should specify actors, systems, and workflow steps shown.

- Page 2, Proposed Solution: “This process is illustrated in **section 4**.”
- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the **top-level workfow of the interactions in relation to Artemis and GitLab CI**”

### 20. Use of first-person singular 'I' in the schedule section

`MINOR` · `writing`

The guidelines instruct authors to avoid 'I'. Two instances appear in the time-schedule section. Replace with passive or third-person phrasing (e.g., 'The internship period is excluded from the thesis work schedule' or 'The author will not have time…').

- Page 4, Time Schedule: “During this time, **I** will not have time to work on my thesis.”
- Page 4, Time Schedule: “Instead, **I** plan to work on my thesis for about 2 weeks during the summer holidays in August.”

### 21. Paragraph structure issues in Section 5 (Expected Findings)

`MINOR` · `writing`

Two issues exist in Section 5: (1) A standalone paragraph 'In addition, universities will save time and money as they will need at least one less server' is only about 2 rendered lines, below the minimum of 5 lines, and should be merged with an adjacent paragraph. (2) The final paragraph combines two distinct ideas: tackling the exam test-execution problem and automatic deployment of GitLab CI Runners using Ansible/Kubernetes. The phrase 'In addition, either Ansible or a Kubernetes cluster…' introduces an unrelated topic. Split into two separate paragraphs or merge the Ansible/Kubernetes point with a more appropriate paragraph.

- Page 4, Expected Findings: “In addition, universities will save time and money as they will need **at least one less server**.”
- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. with the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams described in section 1. **In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.**”

### 22. Section 4 has only a single-line introduction before a long structured list

`MINOR` · `structure`

Section 4 ('System Requirements and Top Level Design') provides only one sentence of introductory text before the 14-step use case list. While a use-case specification is a standard structured format, the section would benefit from a short 3-5 line paragraph that frames the section's content (what the use case illustrates, what the figure shows, what the reader should take away) before and after the list, to bring the section closer to a prose-driven structure.

- Page 3, System Requirements and Top Level Design: “**The visionary scenario presents the supposed workfow of an exercise.**”

### 23. Inconsistent capitalization of 'Server' in compound terms

`MINOR` · `formatting`

On page 2, the author uses 'VC Server' and 'CI Server' with a capital S in two places, but consistently uses lowercase 'VC server' and 'CI server' in all other instances throughout the document (pages 1, 2, and 3). The capitalization should be uniform.

- Page 2, Problem Statement: “the build plans must be updated manually in the **CI Server**”
- Page 2, Proposed Solution: “each time the code in the **VC Server** gets updated, the build plan is fetched”
- Page 2, Proposed Solution: “the CI service should be part of the VC **server** to reduce additional efort by maintaining two servers”
- Page 3, Related Work: “CI services are now often included with **VC servers**”

### 24. Inconsistent terminology for the CI component

`MINOR` · `writing`

The same concept (the CI component, e.g., Jenkins or GitLab CI) is referred to using four different terms throughout the proposal: 'CI application' (page 1), 'CI service' (pages 1-2), 'CI system' (pages 1-2), and 'CI server' (pages 2-3). The author should pick one term and use it consistently for this concept.

- Page 1, Abstract: “Since some Version Control (VC) systems include a **CI application**, it is possible to reduce the efort required to maintain the servers and migrate the **CI service** from external applications as Jenkins to the inbuilt CI service.”
- Page 1, Abstract: “**GitLab CI is the CI system** provided by the GitLab VC server.”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a **CI server** for executing the tests.”
- Page 2, Proposed Solution: “The solution for the problem described in section 1 is a **CI system** not storing each build plan individually”
- Page 2, Proposed Solution: “Furthermore, the **CI service** should be part of the VC server to reduce additional efort”
- Page 3, Related Work: “it is now possible to migrate external **CI servers** as Jenkins to GitLab CI”
