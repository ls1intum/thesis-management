# proposal-p1 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 22
- **Findings:** 21
- **Cost:** 19 calls, 0 tool calls, depth 3, 112612 tokens, 192s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant and well-scoped topic (integrating Artemis with GitLab CI), but it is severely deficient in structure and completeness: three required sections (Introduction, Motivation, Objectives) are entirely missing, the AI Transparency Statement is absent, only one figure and five references are provided, and the schedule is not organized into agile iterations and includes non-permissible thesis-writing tasks. The writing quality is passable but contains multiple short paragraphs, vague language, and minor formatting inconsistencies. Substantial rework is needed before this proposal can be considered submission-ready.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain a dedicated Introduction section. After the Abstract, the document jumps directly to '1 Problem Statement'. An Introduction section is required by the guidelines and must be less than one page.

- Page 2, Introduction: “The proposal does not contain a dedicated Introduction section. After the Abstract, the document jumps directly to '1 Problem Statement'.”

### 2. Missing Motivation section

`CRITICAL` · `structure`

The proposal lacks a dedicated Motivation section as required by the guidelines (at most 3/4 page). The closest content is a single sentence in the Abstract ('Motivation/Objective: Since some Version Control (VC) systems include a Continuous Integration (CI) application...') and parts of Section 5 'Expected Findings.' Neither constitutes a proper visionary Motivation section that explains why the problem is scientifically important to solve and focuses on the positive aspects of having the solution. A standalone Motivation section must be added.

- Page 1, Abstract: “Motivation/Objective: Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 4, Expected Findings: “On long term, we expect that more universities will use Artemis, since the setup hurdle will become smaller. As described in section 3, this might lead to better developers and better code, due to the fact, that more students will be taught the concepts of CI.”

### 3. Missing Objectives section with required subsection structure

`CRITICAL` · `structure`

The proposal has no 'Objectives' section with subsections as required by the guidelines. Section 4 is titled 'System Requirements and Top Level Design' and Section 5 is 'Expected Findings,' but neither constitutes a proper Objectives section. The guidelines require a short introduction, a bullet list of 3-4 high-level objectives (each fitting in one line, in action form), followed by a dedicated subsection for each objective of at least two paragraphs. None of this structure is present. The student must add a dedicated section titled 'Objectives' with at least two subsections, each beginning with a single action verb and containing at least two paragraphs of detailed description.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”
- Page 4, Expected Findings: “This research thesis deals with the question on how to connect Artemis with GitLab CI.”

### 4. AI Transparency Statement is entirely missing

`CRITICAL` · `completeness`

The proposal does not contain an AI Transparency Statement anywhere in the document. Per the guidelines, the proposal must include a specific, first-person AI transparency statement that (1) is written in first person, (2) names the specific AI tools used, (3) states their purpose, (4) identifies which sections they were used for, and (5) includes a confirmation sentence such as 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' A dedicated section with all of these elements must be added.

- Page 1, AI Transparency Statement: “The proposal does not contain an AI Transparency Statement anywhere in the document.”

### 5. Bibliography contains only 5 publications (minimum 6 required)

`CRITICAL` · `citation`

The bibliography lists only 5 entries: [Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]. The guidelines require at least 6–8 peer-reviewed publications. At least one additional peer-reviewed reference (e.g., a paper on GitLab CI, CI/CD in education, or the bridge design pattern applied in a software-engineering context) must be added and cited in the text.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.). Benjamin/Cummings series in object-oriented software engineering. Addison-Wesley. 1995.”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning. ACM. 2018, 49th Technical Symposium on Computer Science Education, pp. 284–289.”

### 6. Only one figure present; minimum of two required

`CRITICAL` · `figures`

The proposal contains only Figure 1 (on page 4). The guidelines require at least two diagrams/mockups/figures. A second figure (e.g., a class diagram showing the bridge pattern architecture, a component diagram of the Artemis–GitLab CI integration, or a class diagram of the Artemis API extension) must be added to meet the minimum requirement.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”

### 7. Schedule is not structured into 2-4 week iterations

`CRITICAL` · `methodology`

The schedule is a list of sporadic milestones organized by calendar month rather than a sequence of 2-4 week iterations. Large unaccounted gaps exist between work items (e.g., ~6 weeks between the May 3rd week item and the June 4th week item; ~7 weeks between September and November). There is no consistent iteration structure, no sprint boundaries, and no indication of what is delivered at the end of each iteration. The schedule must be restructured into clearly numbered iterations of 2-4 weeks each, with defined start/end dates and a deliverable for each.

- Page 4, Time Schedule: “April – 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “May – 3nd week: prototype implementation: Artemis API for confguration”
- Page 5, Time Schedule: “June – 4th week: prototype implementation: Sending build results to Artemis”
- Page 5, Time Schedule: “September – 2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 8. Thesis writing and documentation tasks included in schedule

`CRITICAL` · `completeness`

The schedule contains multiple entries dedicated to thesis writing and documentation: 'Documentation for installation and configuration of Artemis with GitLab CI' (September), 'Writing...' (October), 'Thesis ready for review' (November), 'Review finished' (November), and 'Thesis' (December). Per the guidelines, the schedule should focus exclusively on development and research work. Thesis writing, documentation writing, and presentation preparation must be removed.

- Page 5, Time Schedule: “2nd week: Documentation for installation and confguration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “2nd week: Thesis ready for review”
- Page 5, Time Schedule: “3rd week: Review fnished”
- Page 5, Time Schedule: “1st week: Thesis”

### 9. Problem section does not clearly identify actors and their specific negative impacts

`MAJOR` · `methodology`

The Problem section must identify actors (stakeholders) and describe how the problem negatively influences each of them. The Problem Statement (Section 1) mentions 'smaller universities' might not use Artemis, and refers to 'the instructor' and 'each student' in passing, but never explicitly states who is harmed and how. For example, it should say: 'The instructor is negatively impacted because manually updating build plans for hundreds of students in large courses is nearly impossible.' and 'Students are negatively impacted because they receive delayed or inconsistent feedback.' and 'Smaller universities cannot afford the additional server infrastructure, excluding them from using Artemis.' The current text lists consequences without tying them to clearly named actors.

- Page 2, Problem Statement: “This leads to additional server overhead, since the CI and the Version Control (VC) are running on diferent servers. Particularly smaller universities might then not use Artemis.”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server. Especially in large software engineering courses this is nearly impossible.”

### 10. Requirements-gathering task present in schedule

`MAJOR` · `methodology`

The June entry includes 'Requirements (parallel to prototype implementation).' Even though it is marked as parallel work, a dedicated requirements-gathering task is not permissible in an agile schedule. In agile, requirements are selected from a product backlog at the start of each iteration; there is no separate 'requirements' phase or task. This item should be removed or replaced with a specific, measurable development task.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”

### 11. Vague and non-measurable deliverables in schedule

`MAJOR` · `methodology`

Several schedule items are too vague to constitute measurable deliverables: 'Models (parallel to prototype implementation)' does not specify which models or what the finished state looks like; 'Revise models with concrete implementation' lacks acceptance criteria; 'Implement improvements' (November 4th week) does not state which improvements. Each iteration item should name a concrete, testable deliverable (e.g., 'GitLab CI runner executes a build plan fetched from the Artemis API and returns results to the Artemis UI').

- Page 5, Time Schedule: “4th week: Models (parallel to prototype implementation)”
- Page 5, Time Schedule: “2nd week: Revise models with concrete implementation”
- Page 5, Time Schedule: “4th week: Implement improvements”

### 12. Horizontal layering instead of vertically integrated features in schedule

`MAJOR` · `methodology`

The prototype work is split into sequential horizontal layers: (1) 'Connectivity to GitLab CI', (2) 'Artemis API for configuration', (3) 'Sending build results to Artemis', and a parallel 'Models' task. This builds the system layer by layer rather than delivering end-to-end functionality in each iteration. A vertically integrated approach would deliver, for example, 'Student pushes code, CI fetches build plan from Artemis API, executes tests, and displays results in Artemis UI' as a single feature within one iteration, then extend it in the next.

- Page 5, Time Schedule: “4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “3nd week: prototype implementation: Artemis API for confguration”
- Page 5, Time Schedule: “4th week: prototype implementation: Sending build results to Artemis”

### 13. Section 4 opening paragraph is only 1 line

`MAJOR` · `structure`

The introductory text for Section 4 (System Requirements and Top Level Design) consists of a single sentence: 'The visionary scenario presents the supposed workfow of an exercise.' This is too short to adequately introduce the section's content. It should be expanded to 5-8 lines, providing context about the system requirements and design approach before presenting the use case workflow.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”

### 14. Section 4 closing paragraph is only 2 lines

`MAJOR` · `structure`

The paragraph following the use case workflow in Section 4 is only 2 lines long: 'Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.' It should be expanded to 5-8 lines to properly transition from the use case description to the next content and to elaborate on how the problem statement and scenario determine the actors and systems.

- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”

### 15. Section 5 contains a 1-line paragraph

`MAJOR` · `structure`

The paragraph 'In addition, universities will save time and money as they will need at least one less server.' renders as a single line. This is too short and should be merged with an adjacent paragraph or expanded with additional supporting detail to reach the 5-8 line guideline.

- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”

### 16. Figure 1 is not explicitly referenced by name in the body text

`MINOR` · `figures`

No sentence in the proposal text contains an explicit reference such as 'Figure 1 shows…' or 'as illustrated in Figure 1.' The only cross-reference is on page 2: 'This process is illustrated in section 4,' which points to the section rather than the figure itself. Every figure must be referenced by name in the surrounding text so the reader knows where to look.

- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 17. Figure caption is too brief and generic

`MINOR` · `figures`

The caption 'Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI' is only one sentence and does not describe the specific actors involved, the key steps depicted, or what design decisions the figure communicates. A longer, more informative caption (naming the actors, summarising the workflow steps, and noting what the figure communicates) would meet the 'long and informative' requirement.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”

### 18. Use of superlative 'very'

`MINOR` · `writing`

The word 'very' is a flagged filler/superlative. The phrase 'makes it very cumbersome to update the build plan afterwards' should be reworded to a more precise expression, e.g., 'makes updating the build plan impractical' or 'makes updating the build plan excessively laborious.'

- Page 2, Problem Statement: “The current setup and implementation makes it very cumbersome to update the build plan afterwards.”

### 19. Abbreviation 'API' used without prior definition

`MINOR` · `writing`

The abbreviation 'API' appears in the Abstract (page 1) without first being expanded to 'Application Programming Interface.' While 'VC' and 'CI' are defined on the same page, 'API' is not. It should be introduced as 'Application Programming Interface (API)' on first use.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively”

### 20. Section 5 final paragraph mixes multiple unrelated ideas

`MINOR` · `writing`

The last paragraph of Section 5 combines three distinct ideas: (1) new opportunities for further research, (2) tackling test execution during exams, and (3) using Ansible or Kubernetes for automatic deployment of GitLab CI Runners. Each is a separate point and should be developed in its own paragraph, or the least relevant one (Ansible/Kubernetes) should be removed to maintain one coherent idea per paragraph.

- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. with the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams described in section 1. In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 21. Inconsistent capitalization: 'CI Server' vs 'CI server'

`MINOR` · `formatting`

On page 2, the term appears as 'CI Server' (capital S) in 'the build plans must be updated manually in the CI Server,' while in all other occurrences it is written as 'CI server' (lowercase s). The capitalization should be consistent throughout the document.

- Page 2, Problem Statement: “the build plans must be updated manually in the CI Server.”
