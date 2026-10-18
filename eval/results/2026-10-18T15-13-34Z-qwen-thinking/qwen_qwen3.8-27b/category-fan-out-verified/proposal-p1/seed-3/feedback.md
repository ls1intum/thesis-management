# proposal-p1 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 16
- **Cost:** 18 calls, 0 tool calls, depth 3, 109028 tokens, 213s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant and interesting topic (connecting Artemis with GitLab CI) and shows a reasonable understanding of the problem, but it fails to meet several mandatory structural requirements: the Introduction, Motivation, and Objectives sections are entirely absent, the AI Transparency Statement is missing, and the figure requirements (at least two diagrams, no sequence diagrams) are not fulfilled. Additionally, the schedule uses horizontal layering rather than vertically integrated features, and the writing suffers from excessive passive voice and filler words. Substantial restructuring is needed before this proposal can be considered submittable.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section. The document jumps directly from the Abstract to Section 1 'Problem Statement'. An Introduction section is required per the guidelines and should provide context for the thesis topic before diving into the problem.

### 2. Missing Motivation section

`CRITICAL` · `structure`

The proposal lacks a standalone Motivation section. The Abstract contains a brief 'Motivation/Objective' sentence, but there is no full section that explains why the problem is scientifically important to solve, addresses what gaps exist in current practice, and focuses on the positive aspects of having the solution. Per the guidelines, a Motivation section of at most 3/4 page is required. Without it, the reader cannot understand the broader scientific significance beyond the immediate practical benefit of saving a server.

- Page 1, Abstract: “Motivation/Objective”

### 3. Missing Objectives section with subsections in action form

`CRITICAL` · `structure`

The proposal does not contain a section titled 'Objective' or 'Objectives' with subsections, as required by the guidelines. Section 5 'Expected Findings' discusses anticipated outcomes but is not structured as an Objectives section. Per the guidelines, the Objectives section must start with a short introduction and a bullet list of 3–4 high-level objectives, followed by a subsection for each objective (each at least two paragraphs long, stated in imperative action form such as 'Develop…', 'Design…', 'Implement…'). The current structure (Problem Statement, Proposed Solution, Related Work, System Requirements, Expected Findings) does not fulfill this requirement.

- Page 4, Expected Findings: “This research thesis deals with the question on how to connect Artemis with GitLab CI.”

### 4. AI Transparency Statement is entirely missing

`CRITICAL` · `completeness`

The proposal does not contain an AI transparency statement anywhere. The required section is absent from the document. The proposal must include a first-person AI transparency statement that (a) names the specific AI tools used (e.g., ChatGPT, Grammarly, GitHub Copilot), (b) specifies the purpose of their use (e.g., grammar checking, idea generation, code assistance), (c) identifies which sections of the proposal the tools were applied to, and (d) includes a confirmation sentence such as 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' This section is a mandatory component per the research group guidelines.

### 5. Figures do not meet requirements: only one figure present and it is a prohibited sequence diagram

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1 on page 4), but the guidelines require at least two diagrams/mockups. Furthermore, Figure 1 depicts numbered messages (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3) flowing between actors (Instructor, Artemis, Student) over time, which constitutes a sequence diagram—the guidelines explicitly prohibit sequence diagrams and require at least one suitable UML diagram (class diagram, activity diagram, component diagram, etc.). The student should replace or supplement Figure 1 with an appropriate UML diagram (e.g., a component diagram showing the Artemis–GitLab CI integration, a class diagram of the bridge pattern implementation, or an activity diagram of the CI pipeline) to satisfy both the quantity and type requirements.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 6. Schedule uses horizontal layering instead of vertically integrated features

`MAJOR` · `methodology`

The prototype work is split into horizontal layers across separate months: 'Connectivity to GitLab CI' (April), 'Artemis API for configuration' (May), 'Sending build results to Artemis' (June), with 'Models' as a parallel design layer. This is a horizontal split (infrastructure → API → data flow → design) rather than a series of vertically integrated, end-to-end features. A vertically integrated approach would deliver, for example, a working minimal exercise (student submits → CI runs → result displayed) in one iteration, then extend it with further features in subsequent iterations.

- Page 4, Time Schedule: “– 4th week: prototype implementation: Connectivity to GitLab CI”
- Page 4, Time Schedule: “– 3nd week: prototype implementation: Artemis API for configuration”
- Page 4, Time Schedule: “– 4th week: prototype implementation: Sending build results to Artemis”

### 7. Requirements-gathering task present in the schedule

`MAJOR` · `structure`

The June entry 'Requirements (parallel to prototype implementation)' constitutes a requirements-gathering activity, which is not permitted in an agile schedule. Requirements should be maintained in a product backlog and pulled into iterations as needed, not scheduled as a discrete task in the time plan.

- Page 4, Time Schedule: “– 4th week: Requirements (parallel to prototype implementation)”

### 8. Figure 1 is not explicitly referenced in the body text

`MAJOR` · `figures`

The text in Section 4 describes the visionary scenario and then places Figure 1 directly below, but never uses an explicit reference such as 'Figure 1 shows …' or 'As illustrated in Figure 1, …'. The only related sentence is 'This process is illustrated in section 4' (page 2), which references a section, not the figure. Every figure must be explicitly cited in the running text.

- Page 2, Proposed Solution: “This process is illustrated in section 4.”
- Page 4, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”

### 9. Excessive passive voice throughout the document

`MAJOR` · `writing`

The proposal relies heavily on passive constructions, obscuring the actor. At least a dozen clauses use passive voice. Rewrite to identify the agent performing the action (e.g., 'Artemis copies the base build plan' instead of 'the base build plan is copied').

- Page 1, Abstract: “The CI server should be migrated to the VC server.”
- Page 1, Abstract: “it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “the base build plan is copied once after the student started the exercise”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server.”
- Page 2, Proposed Solution: “It could be transposed with a CI server which is dynamically updating the student build plan.”
- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis”
- Page 4, Expected Findings: “the API of Artemis might be extended for this service.”
- Page 4, Expected Findings: “either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 10. Excessive use of filler words and superlatives

`MAJOR` · `writing`

Filler words ('furthermore', 'additionally', 'additional', 'in addition') and the superlative 'very' appear seven times across the document. Remove or rephrase to tighten the writing and improve readability.

- Page 1, Abstract: “Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “This leads to additional server overhead, since the CI and the Version Control (VC) are running on diferent servers.”
- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise”
- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”
- Page 2, Problem Statement: “The current setup and implementation makes it very cumbersome to update the build plan afterwards.”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 11. Two unrelated ideas combined in one paragraph (Expected Findings)

`MAJOR` · `logic`

The final paragraph of Section 5 (Expected Findings) combines two distinct ideas: (1) tackling the exam test-execution problem from Section 1, and (2) using Ansible or Kubernetes for automatic deployment of GitLab CI Runners. Each idea should be developed in its own paragraph to maintain logical coherence.

- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. with the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams described in section 1. In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 12. Standalone 1–2 line paragraph in Expected Findings

`MAJOR` · `writing`

The sentence 'In addition, universities will save time and money as they will need at least one less server.' is a standalone 1–2 line paragraph. It should be merged with the preceding or following paragraph to form a coherent 5–8 line paragraph.

- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”

### 13. Figure 1 caption is too brief and generic

`MINOR` · `figures`

The caption 'Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI' is short and does not describe what specific interactions are shown, what the figure demonstrates about the proposed solution, or how it relates to the bridge pattern / API-based build plan retrieval. A longer, informative caption should summarise the key actors, the flow, and the design insight the reader should take from the figure.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 14. Abbreviation 'API' used without prior definition

`MINOR` · `writing`

'API' appears multiple times (pages 1, 2, 4) but is never expanded on first use. Define it as 'Application Programming Interface (API)' at the first occurrence.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively”
- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis and GitLab CI is downloading the build plan for each execution of the test cases.”

### 15. Multiple 1–2 line paragraphs in System Requirements and Top Level Design

`MINOR` · `writing`

Two introductory/transitional sentences are stand-alone 1-line paragraphs: 'The visionary scenario presents the supposed workflow of an exercise.' (page 3) and 'Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.' (page 3). Both should be expanded into proper introductory paragraphs of 5–8 lines that contextualise the use case and introduce Figure 1 and the identified actors.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workflow of an exercise.”
- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”

### 16. Inconsistent capitalization: 'VC Server' vs 'VC server'

`MINOR` · `formatting`

The term appears with both a capital S ('VC Server') and a lowercase s ('VC server' / 'VC servers') in different parts of the text. A single consistent form should be used throughout the document.

- Page 2, Proposed Solution: “but each time the code in the VC Server gets updated, the build plan is fetched.”
- Page 2, Proposed Solution: “the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”
- Page 3, Related Work: “CI services are now often included with VC servers”
