# proposal-p1 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 16
- **Cost:** 1 calls, 0 tool calls, depth 1, 19087 tokens, 174s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a well-motivated and practically relevant problem (integrating Artemis with GitLab CI) and demonstrates solid domain knowledge. However, it fundamentally fails to follow the required proposal structure: three mandatory sections (Introduction, Motivation, Objective with subsections) and the AI Transparency Statement are entirely absent. Additionally, the figures and bibliography fall below minimum requirements (1 figure instead of 2; 5 references instead of 6–8), and the single figure is a prohibited sequence diagram. The schedule includes thesis-writing tasks and lacks agile iteration structure. Significant restructuring is required before this proposal can be submitted.

## Findings

### 1. Missing Introduction Section

`CRITICAL` · `structure`

The proposal does not contain a dedicated Introduction section, which is a required section. The document jumps directly from the Abstract to Section 1 (Problem Statement) without an introductory section that provides context, background, and an overview of the proposal's structure.

### 2. Missing Motivation Section

`CRITICAL` · `structure`

The proposal does not contain a dedicated Motivation section. The required Motivation section should be visionary, explaining why the problem is scientifically important to solve and focusing on the positive aspects of having the solution. Instead, the proposal includes a 'Related Work' section and 'Expected Findings' which do not fulfil this role.

### 3. Missing Objective Section with Subsections

`CRITICAL` · `structure`

The proposal lacks a proper Objectives section. The required structure is: a short introduction, a bullet list of 3–4 high-level objectives (each fitting in one line), followed by a dedicated subsection for each objective (at least two paragraphs each). The current 'System Requirements and Top Level Design' and 'Expected Findings' sections do not meet this requirement.

### 4. Missing AI Transparency Statement

`CRITICAL` · `completeness`

The proposal does not contain an AI transparency statement. This is a required section that must be written in first person, naming specific AI tools used, their purposes, the sections they were applied to, and must include a confirmation that the author carefully checked all AI-generated text.

### 5. Only One Figure Included (Minimum Two Required)

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1 on page 4). The guidelines require at least two diagrams, mockups, or figures. A second figure (e.g., a class diagram showing the bridge pattern, a component diagram of the Artemis–GitLab CI architecture, or a deployment diagram) must be added.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 6. Bibliography Contains Only 5 References (Minimum 6–8 Required)

`CRITICAL` · `citation`

The References section lists only 5 publications ([Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]). The guidelines require at least 6–8 peer-reviewed publications. At least 1–3 additional relevant peer-reviewed sources should be added (e.g., on the bridge pattern in CI/CD integration, GitLab CI architecture, or assessment management systems).

- Page 5, References: “[Booch95] Booch, G. … [Gamma95] Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. … [Hilton16] Hilton, M.; … [Krusche17] Krusche, S.; … [Krusche18] Krusche, S.; Seitz, A.”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning. ACM. 2018”

### 7. Figure 1 Is a Sequence Diagram; No Suitable UML Diagram Present

`MAJOR` · `figures`

Figure 1 depicts lifelines (Instructor, Student) with numbered messages (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3), making it a sequence diagram. The guidelines explicitly prohibit sequence diagrams and require at least one suitable UML diagram (class diagram, activity diagram, component diagram, etc.). Replace or supplement with an activity diagram of the exercise workflow or a component/class diagram showing the bridge pattern binding Artemis to GitLab CI.

- Page 4, System Requirements and Top Level Design: “1.1: Push code template, test cases and sample solution … 1.2: Assign exercise … 2.2: Setup student repository … 3.2: Trigger CI … 3.3: Send build results … 2.1: Participate in exercise … 3.1: Push code”

### 8. Schedule Contains Thesis-Writing and Documentation Tasks

`MAJOR` · `structure`

The schedule includes items such as 'October: Writing . . .', 'November: Thesis ready for review', 'Review finished', 'December: Thesis', and 'September: Documentation for installation and configuration'. Per the guidelines, the schedule should focus exclusively on development/research work and must not include thesis writing, documentation writing, or presentation preparation tasks.

- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “2nd week: Thesis ready for review – 3rd week: Review fnished – 4th week: Implement improvements”
- Page 5, Time Schedule: “1st week: Thesis”
- Page 5, Time Schedule: “2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 9. Schedule Not Organized in 2–4 Week Iterations

`MAJOR` · `structure`

The schedule is organized by calendar months and individual weeks rather than in agile iterations of 2–4 weeks each. It does not reference the high-level objectives and does not follow agile sprint structure. The work items should be reorganized into named iterations (e.g., Sprint 1: weeks 1–2, Sprint 2: weeks 3–4, etc.) each with measurable, vertically integrated deliverables.

- Page 4, Time Schedule: “March – 5th week: Proposal – April – 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI – May – 3nd week: prototype implementation: Artemis API for confguration”

### 10. Excessive Use of Passive Voice

`MAJOR` · `writing`

The proposal relies heavily on passive voice and impersonal constructions throughout. Examples include: 'the build plans must be updated manually', 'the base build plan is copied', 'changes are not patched automatically', 'the build plan is fetched', 'the build plan is made available', 'the code gets updated', 'it is possible to migrate', 'it is useful to teach', 'it would be useful to change', 'it is possible to determine', 'it will be possible to tackle'. Rewrite in active voice with clear actors (e.g., 'the instructor updates the build plan', 'GitLab CI fetches the build plan').

- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server.”
- Page 2, Proposed Solution: “the build plan is fetched. It could be transposed with a CI server which is dynamically updating the student build plan.”
- Page 3, Related Work: “Since the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university”
- Page 4, Expected Findings: “with the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams”

### 11. Inconsistent Terminology: 'CI Server' vs. 'CI Service'

`MINOR` · `formatting`

The proposal alternates between 'CI server' and 'CI service' to refer to the same concept (e.g., Abstract: 'CI service', Section 1: 'CI server', Section 2: 'CI service'). Pick one term and use it consistently throughout the document.

- Page 1, Abstract: “migrate the CI service from external applications as Jenkins to the inbuilt CI service”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a CI server for executing the tests.”
- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort”

### 12. Forbidden Sentence Starters ('Since', 'As')

`MINOR` · `writing`

Several sentences begin with forbidden words: 'Since' (Abstract, Related Work) and 'As' (Proposed Solution). Rewrite these sentences to avoid starting with the forbidden word.

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort”
- Page 2, Proposed Solution: “As a result, instructors are able to update the build plan after the release of an exercise”
- Page 3, Related Work: “Since the benefts of using CI are quite high and nearly half of the developers who do not use CI”

### 13. Abbreviation 'API' Used Without Prior Definition

`MINOR` · `writing`

The abbreviation 'API' is used multiple times (e.g., 'via the API of Artemis', 'the Artemis API for configuration') but is never defined as 'Application Programming Interface (API)' on first use. Define it before first occurrence.

- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis and GitLab CI is downloading the build plan for each execution of the test cases.”

### 14. Repetitive Filler Words ('Additionally', 'Furthermore', 'In addition')

`MINOR` · `writing`

The words 'Additionally' (×2), 'Furthermore' (×1), and 'In addition' (×2) appear across the document as sentence-initial fillers. Reduce their frequency and restructure sentences to flow more naturally without these connective fillers.

- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise”
- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment”

### 15. Figure 1 Not Explicitly Referenced by Number in Text

`MINOR` · `figures`

The text says 'This process is illustrated in section 4' (Section 2) but never explicitly references 'Figure 1' by its number. Each figure must be referenced in the body text with its figure number (e.g., 'Figure 1 illustrates the top-level workflow…').

- Page 2, Proposed Solution: “GitLab CI fetches the updated build plan when needed. This process is illustrated in section 4.”

### 16. Schedule Includes a 'Requirements' Item

`MINOR` · `structure`

The June schedule lists 'Requirements (parallel to prototype implementation)' as a work item. Per agile principles and the guidelines, there should not be a dedicated requirements-gathering phase. Requirements should be discovered and addressed continuously within iterations.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”
