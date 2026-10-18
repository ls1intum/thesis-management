# proposal-p1 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 30
- **Findings:** 16
- **Cost:** 18 calls, 0 tool calls, depth 3, 111818 tokens, 180s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a well-defined and relevant topic (integrating Artemis with GitLab CI via a bridge pattern) and demonstrates understanding of the system architecture, but it is structurally incomplete with three entire required sections missing (Introduction, Motivation, Objectives), no AI Transparency Statement, insufficient bibliography (5 of 6+), and only one of the required two figures. The schedule also contains non-development tasks, a horizontal decomposition, and vague deliverables. Substantial restructuring and content additions are needed before the proposal meets submission criteria.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain a dedicated 'Introduction' section. The required section order is Abstract, Introduction, Problem, Motivation, Objective, Schedule, Bibliography. The proposal jumps directly from the Abstract to '1 Problem Statement' with no introductory section providing background context on Artemis, the broader learning technology landscape, or the scope of the thesis before diving into the problem.

- Page 2, Problem Statement: “The interactive learning platform Artemis is an approach to use interactive learning during large computer science classes [Krusche17, Krusche18].”

### 2. Missing dedicated Motivation section

`CRITICAL` · `structure`

The proposal does not contain a standalone Motivation section. The expected structure requires a Motivation section that is visionary, outlining why it is scientifically important to solve the problem and focusing on the positive aspects of having the solution. While motivational elements are scattered across the Abstract, Problem Statement, and '5 Expected Findings' (e.g., smaller universities being deterred by setup overhead, more universities adopting Artemis), there is no dedicated section. Rules regarding visionary framing and non-repetition of the Problem cannot be satisfied without this section.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis.”
- Page 4, Expected Findings: “On long term, we expect that more universities will use Artemis, since the setup hurdle will become smaller.”

### 3. Missing Objective section with required subsections in action form

`CRITICAL` · `structure`

The proposal lacks a proper 'Objective' section. Section 4 is 'System Requirements and Top Level Design' and Section 5 is 'Expected Findings'; neither follows the required structure. Specifically: (a) there is no short introductory paragraph followed by a bullet list of 3-4 high-level objectives in action form (e.g., 'Analyze…', 'Extend…', 'Integrate…'); (b) there are no subsections dedicated to individual objectives; (c) no subsection contains at least two paragraphs elaborating on a single objective. The student should add a dedicated 'Objective' section containing individual subsections, each starting with a single action verb and containing at least two paragraphs of detailed description.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”
- Page 4, Expected Findings: “This research thesis deals with the question on how to connect Artemis with GitLab CI.”

### 4. AI Transparency Statement is entirely missing

`CRITICAL` · `completeness`

The proposal does not contain an AI Transparency Statement anywhere. The guidelines require a specific, first-person AI transparency statement that names the tools used, their purpose, the sections involved, and includes a confirmation that the author has carefully checked all AI-generated text. The statement should typically appear as its own section (e.g., 'AI Transparency Statement') near the end of the proposal, before or after the References.

- Page 6, References: “No AI Transparency Statement section is present in the document.”

### 5. Bibliography contains only 5 publications; minimum of 6 required

`CRITICAL` · `citation`

The bibliography lists only 5 entries: [Booch95], [Gamma95], [Hilton16], [Krusche17], and [Krusche18]. The guidelines require at least 6-8 peer-reviewed publications. The student must add at least one more (ideally 2-3 more) peer-reviewed publication relevant to the topic (e.g., on CI/CD, version control integration, or assessment systems) to meet the minimum threshold.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.). Benjamin/Cummings series in object-oriented software engineering. Addison-Wesley. 1995.”
- Page 5, References: “[Krusche17] Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning: Increasing student participation through shorter exercise cycles. ACE 2017.”

### 6. Only one figure present; minimum of two required

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1 on page 4). The guidelines require at least two diagrams/mockups/figures. A second figure (e.g., a class diagram showing the bridge pattern architecture, a component diagram of Artemis/GitLab CI interaction, or a deployment diagram) must be added.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Thesis writing, documentation, and review tasks included in the schedule

`CRITICAL` · `structure`

The schedule must focus exclusively on development/research work. The following entries violate this rule: 'Documentation for installation and configuration', 'October: Writing . . .', 'November: Thesis ready for review', 'November: Review finished', and 'December: Thesis'. These are thesis-writing and documentation tasks and should be removed. The schedule should end with the completion of the last development/research iteration.

- Page 5, Time Schedule: “2nd week: Documentation for installation and configuration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “2nd week: Thesis ready for review”
- Page 5, Time Schedule: “3rd week: Review finished”
- Page 5, Time Schedule: “December – 1st week: Thesis”

### 8. Excessive use of filler words

`MAJOR` · `writing`

The document contains nine instances of flagged filler words: 'Furthermore' (2×), 'Additionally' (1×), 'additional' (3×), 'also' (1×), and 'In addition' (2×). This is excessive for a 4–6 page proposal. Each instance should be removed or replaced with a more direct phrasing. For example, 'Furthermore, it should be possible' can become 'It should also be possible' or simply 'The build plan should be adjustable.'

- Page 1, Abstract: “Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “This leads to additional server overhead, since the CI and the Version Control (VC) are running on diferent servers.”
- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise [Krusche18].”
- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”
- Page 3, Related Work: “This is why it is now possible to migrate external CI servers as Jenkins to GitLab CI and so reduce additional work for server administration.”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 9. Schedule work items split horizontally by layer rather than describing vertically integrated features

`MAJOR` · `structure`

The iterations describe separate technical layers as independent deliverables: first infrastructure (Docker runner), then connectivity, then API, then result sending, then data models. This is a horizontal, waterfall-style decomposition. Each iteration should instead deliver an end-to-end, vertically integrated feature (e.g., 'Student pushes code → GitLab CI fetches build plan via Artemis API → executes tests → results displayed in Artemis UI'). The student should regroup the work so that each sprint produces a demonstrable, end-to-end capability.

- Page 4, Time Schedule: “2nd week: Docker GitLab CI Runner configuration”
- Page 4, Time Schedule: “4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “3nd week: prototype implementation: Artemis API for configuration”
- Page 5, Time Schedule: “4th week: prototype implementation: Sending build results to Artemis”
- Page 5, Time Schedule: “4th week: Models (parallel to prototype implementation)”

### 10. Several schedule deliverables are vague and not measurable

`MAJOR` · `completeness`

Items such as 'Models', 'Requirements', 'Writing . . .', and 'Implement improvements' do not specify what will be produced or how completion will be verified. Each iteration needs concrete, measurable deliverables (e.g., 'UML class and component diagram for the bridge-pattern integration', 'Artemis API endpoint /build-plan returns YAML configuration and is testable via Postman', 'Build result JSON payload delivered to Artemis and displayed in the student dashboard').

- Page 5, Time Schedule: “4th week: Models (parallel to prototype implementation)”
- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”
- Page 5, Time Schedule: “4th week: Implement improvements”

### 11. Requirements-gathering task appears as a standalone schedule entry

`MAJOR` · `structure`

The entry '4th week: Requirements (parallel to prototype implementation)' in June is a requirements-gathering task. In an agile process, requirements are selected from a product backlog at the start of each sprint; there is no separate 'requirements' work item. The student should remove this entry and instead specify the concrete features being pulled from the backlog in that sprint.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”

### 12. Multiple 1–2 line paragraphs in Expected Findings section

`MAJOR` · `writing`

Section 5 contains two very short paragraphs (1 and 2 lines) that do not develop a sufficient idea on their own: 'In addition, universities will save time and money as they will need at least one less server.' and 'In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.' These should be merged into the surrounding paragraph about long-term benefits and further research opportunities to form a coherent 5–8 line paragraph.

- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 13. Figure 1 is not explicitly referenced by number in the body text

`MINOR` · `figures`

The text in Section 2 says 'This process is illustrated in section 4' and Section 4 ends with 'it is possible to determine the relevant actors and systems for solving the problem,' but no sentence explicitly references the figure by number (e.g., 'as shown in Figure 1' or 'Figure 1 depicts...'). Every figure must be referenced in the surrounding text by its figure number.

- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”

### 14. Figure caption is not sufficiently descriptive

`MINOR` · `figures`

The caption 'Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI' is somewhat generic. A more informative caption should describe the specific actors, the key interaction phases (exercise preparation, execution, result retrieval), and the role of the Artemis API in providing the build plan. For example: 'Top-level interaction between the Instructor, Student, Artemis, and GitLab CI during the exercise lifecycle, showing code submission, build plan retrieval via the Artemis API, and result feedback flow.'

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 15. Abbreviation 'API' used without prior definition

`MINOR` · `writing`

The abbreviation 'API' appears multiple times (pages 2 and 4) but is never expanded to 'Application Programming Interface' on first use. The guidelines require that abbreviations be defined before use. The first occurrence on page 2 ('via the API of Artemis') should read 'via the Application Programming Interface (API) of Artemis.'

- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis and GitLab CI is downloading the build plan for each execution of the test cases.”
- Page 4, Expected Findings: “For making the build plan accessible, the API of Artemis might be extended for this service.”

### 16. One-line paragraph at end of Section 4

`MINOR` · `writing`

The sentence 'Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.' is a single line that functions as a transition to the figure. It should be expanded or merged with the preceding/following text to meet the 5-line minimum paragraph length.

- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”
