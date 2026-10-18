# proposal-p1 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 22
- **Cost:** 10 calls, 0 tool calls, depth 2, 91634 tokens, 207s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant and well-motivated research topic (integrating Artemis with GitLab CI) and contains coherent content in its Problem Statement, Related Work, and Proposed Solution sections. However, it has fundamental structural deficiencies: three required sections (Introduction, Motivation, Objective) are entirely missing, the AI Transparency Statement is absent, the sole figure is a prohibited sequence diagram, the bibliography falls short of the minimum, and the schedule violates multiple agile-structuring rules (no 2-4 week iterations, forbidden writing/documentation items, horizontal layering). Additionally, pervasive writing issues (forbidden sentence starters, passive voice, filler words, first-person 'I') require systematic revision. Substantial restructuring and rewriting are needed before this proposal can be submitted.

## Findings

### 1. Missing required section: Introduction

`CRITICAL` · `structure`

The proposal does not contain a dedicated 'Introduction' section. The document transitions directly from the Abstract to '1 Problem Statement'. A separate Introduction section (less than one page) is required by the guidelines.

- Page 1, Abstract: “The document ends with the Abstract on page 1 and proceeds directly to **1 Problem Statement** on page 2, with no Introduction section in between.”

### 2. Missing required section: Motivation

`CRITICAL` · `structure`

The proposal lacks a dedicated Motivation section, which must be at most 3/4 page and should be visionary, outlining why it is scientifically important to solve the problem. The only motivational content is a single sentence in the Abstract ('it is possible to reduce the effort required to maintain the servers...') and a brief mention in the Problem Statement. A standalone Motivation section must be added.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis. There is no separate Motivation section anywhere in the document.”
- Page 1, Abstract: “it is possible to reduce the effort required to maintain the servers”

### 3. Missing required section: Objective (with subsections in action form)

`CRITICAL` · `structure`

The proposal lacks a proper 'Objective' section as required by the guidelines. Section 5 'Expected Findings' is a single narrative block without: (a) a short introductory paragraph, (b) a bullet list of 3-4 high-level objectives in action form (e.g., 'Integrate...', 'Extend...', 'Implement...') fitting in one line each, or (c) a dedicated subsection for each objective (each at least two paragraphs long). The entire section consists of three undivided paragraphs with no hierarchical structure.

- Page 4, Expected Findings: “This research thesis deals with the question on how to connect Artemis with GitLab CI. We expect to use the bridge pattern, described by Gamma et al. in their book Design Patterns: Elements of Reusable Object-Oriented Software, for dynamically choosing the implementation with GitLab CI during startup of the system [Gamma95].”
- Page 4, Expected Findings: “On long term, we expect that more universities will use Artemis... In addition, universities will save time and money as they will need at least one less server. No subsections or bullet-point objectives are present.”

### 4. AI Transparency Statement is entirely missing

`CRITICAL` · `completeness`

The proposal does not contain an AI Transparency Statement. The guidelines require a specific, first-person statement naming the AI tools used, their purpose, the sections in which they were used, and a confirmation that the author has carefully checked all AI-generated text. None of these elements appear anywhere in the document.

- Page 1, Entire document: “No AI Transparency Statement appears in any section of the proposal.”

### 5. Only one figure present (minimum two required) and it is a prohibited sequence diagram

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1 on page 4), while the guidelines require at least two. Furthermore, Figure 1 is a sequence diagram (showing two actors with numbered message arrows 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3 between lifelines), which is explicitly prohibited. The student must replace it with an activity diagram or communication diagram and add a second figure (e.g., component diagram or class diagram). As-is, the proposal also lacks any suitable UML diagram.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 6. Bibliography contains only 5 publications (minimum 6 required)

`CRITICAL` · `completeness`

The bibliography lists only 5 references ([Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]), which is below the required minimum of 6-8 peer-reviewed publications. At least one additional peer-reviewed publication must be added.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.). Benjamin/Cummings series in object-oriented software engineering. Addison-Wesley. 1995.”
- Page 5, References: “[Gamma95] Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. Design Patterns: Elements of Reusable Object-Oriented Software. Addison-Wesley Longman Publishing Co., Inc., USA. 1995.”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning. ACM. 2018, 49th Technical Symposium on Computer Science Education, pp. 284–289.”

### 7. Schedule not structured in 2-4 week iterations and follows horizontal layering

`CRITICAL` · `structure`

The schedule is organized as a month-by-month list with sporadic single-week tasks rather than clear 2-4 week iterations. It follows a horizontal layering pattern (infrastructure → connectivity → API → results → models) rather than delivering vertically integrated, end-to-end features per iteration. It also does not reference the high-level objectives or demonstrate progressive working software at the end of each iteration. The schedule should be restructured into 2-4 week sprints, each delivering a working increment mapped to the stated objectives.

- Page 4, Time Schedule: “– 2nd week: Docker GitLab CI Runner configuration”
- Page 5, Time Schedule: “– 3nd week: prototype implementation: Artemis API for configuration”
- Page 5, Time Schedule: “– 4th week: prototype implementation: Sending build results to Artemis”
- Page 5, Time Schedule: “– 4th week: Models (parallel to prototype implementation)”
- Page 4, Time Schedule: “It is planned to adhere to the following time schedule:”

### 8. Schedule contains forbidden items: thesis writing, documentation, review, and requirements

`CRITICAL` · `structure`

Multiple schedule entries are dedicated to thesis writing, documentation, or the review process, which must not appear in the schedule. Additionally, a 'Requirements' task is included, which is prohibited since requirements are already established in the Problem and Objectives sections. Forbidden items: 'October: Writing . . .', 'November: Thesis ready for review', 'November: Review finished', 'December: 1st week: Thesis', 'September: Documentation for installation and configuration', and '4th week: Requirements (parallel to prototype implementation)'.

- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “– 2nd week: Thesis ready for review”
- Page 5, Time Schedule: “– 1st week: Thesis”
- Page 5, Time Schedule: “– 2nd week: Documentation for installation and configuration of Artemis with GitLab CI”
- Page 5, Time Schedule: “– 4th week: Requirements (parallel to prototype implementation)”

### 9. Forbidden sentence starters: 'Since', 'To', 'In order to', 'As'

`MAJOR` · `writing`

Five sentences begin with forbidden sentence starters. Each must be restructured so the subject and main verb lead the sentence: 'Since some Version Control...' (Abstract), 'To achieve this goal, Artemis uses...' (Problem Statement), 'In order to implement this idea...' (Related Work), 'Since the benefits of using CI...' (Related Work), 'As described in section 3...' (Expected Findings).

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers”
- Page 2, Problem Statement: “To achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases predefned by the instructor”
- Page 2, Related Work: “In order to implement this idea, the lecture is split up into multiple iterations”
- Page 3, Related Work: “Since the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university”
- Page 4, Expected Findings: “As described in section 3, this might lead to better developers and better code”

### 10. Use of first-person singular 'I' in the schedule section

`MAJOR` · `writing`

The guidelines prohibit the use of 'I'. The schedule section uses 'I' twice to describe personal plans. Rephrase using first-person plural 'we' or an impersonal construction.

- Page 4, Time Schedule: “During this time, I will not have time to work on my thesis. Instead, I plan to work on my thesis for about 2 weeks during the summer holidays in August.”

### 11. Excessive use of filler words and superlatives

`MAJOR` · `writing`

The document contains seven instances of filler/superlative words across six pages: 'additional' (×2), 'Furthermore' (×1), 'Additionally' (×1), 'also' (×1), 'very' (×1), and 'In addition' (×2). For a document of this length this density is excessive. Remove or replace each instance with a more precise expression.

- Page 2, Problem Statement: “This leads to additional server overhead, since the CI and the Version Control (VC) are running on diferent servers.”
- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”
- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise.”
- Page 2, Problem Statement: “The current setup and implementation makes it very cumbersome to update the build plan afterwards.”
- Page 3, Related Work: “Not only projects using CI release twice as often as projects without CI, but the build is also less likely to fail in the master branch.”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 12. Excessive passive voice constructions

`MAJOR` · `writing`

At least six sentences use passive voice, hiding the agent. The guidelines require active voice with identifiable subjects. Key instances: 'the build plans must be updated manually', 'the base build plan is copied', 'the build plan is fetched', 'the lecture is split up', 'the API of Artemis might be extended'. Rewrite so the actor performs the action.

- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server.”
- Page 2, Problem Statement: “the base build plan is copied once after the student started the exercise”
- Page 2, Proposed Solution: “each time the code in the VC Server gets updated, the build plan is fetched”
- Page 3, Related Work: “the lecture is split up into multiple iterations, each consisting of diferent phases”
- Page 4, Expected Findings: “the API of Artemis might be extended for this service”

### 13. Section 4 relies entirely on a 14-item numbered list instead of prose

`MAJOR` · `structure`

Section 4 (System Requirements and Top Level Design) consists of a 1-line introductory sentence, a 14-item numbered list (the 'Flow of events'), and a 1-line concluding sentence. The section contains no substantive prose paragraphs. The scenario steps should be recast as flowing prose describing the workflow, with perhaps a few key steps retained as a list. The introductory sentence should also be expanded to at least 5 lines describing the actors, systems, and purpose.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”
- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”

### 14. Krusche et al. paragraph in Related Work exceeds 10-line limit

`MAJOR` · `structure`

The second paragraph of Section 3 (Related Work), beginning with 'In their paper Interactive Learning...', spans from page 2 into page 3 and totals approximately 12-14 rendered lines, exceeding the 10-line upper limit. It should be split into two paragraphs: one introducing the Krusche et al. contribution and the interactive learning concept, and a second describing the concrete multi-phase lecture structure.

- Page 2, Related Work: “In their paper Interactive Learning – Increasing Student Participation through Shorter Exercise Cycles, Krusche et al. have taken up this concept to improve the learning experience in large computer science courses at university [Krusche17]. They introduced an newly developed learning method which shortens the time between theory, which is usually taught in the non-interactive lecture, and practice, which typically follows about a week later, so that less knowledge is lost [Krusche17].”
- Page 3, Related Work: “into multiple iterations, each consisting of diferent phases [Krusche17]. First there is a theory phase, before the instructor shows an example to the students. After that the students can try out the concept themselves, before they get a sample solution and instant, but still individual feedback generated by the CI server.”

### 15. Schedule work items lack measurable, specific deliverables

`MAJOR` · `structure`

Multiple items are too vague to be considered measurable deliverables. 'Models (parallel to prototype implementation)' does not specify what models or what is delivered. 'Implement improvements' (November, 4th week) does not specify what improvements. 'Writing . . .' (October) is entirely undefined. Each iteration should specify concrete, verifiable deliverables (e.g., 'GitLab CI runner container deployed and executing a sample build', 'Artemis API endpoint /build-plan returns valid GitLab CI YAML for a given exercise').

- Page 5, Time Schedule: “– 4th week: Models (parallel to prototype implementation)”
- Page 5, Time Schedule: “– 4th week: Implement improvements”
- Page 5, Time Schedule: “October: Writing . . .”

### 16. Problem section does not explicitly identify the instructor/course-administrator as an affected actor

`MINOR` · `completeness`

The Problem Statement mentions universities and students as affected parties, but the instructor/course-administrator role — who is the primary actor burdened by manually updating build plans — is never explicitly named. The guidelines require the Problem section to clearly identify all actors (stakeholders) and describe how the problem negatively influences each.

- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server. Especially in large software engineering courses this is nearly impossible.”

### 17. Figure 1 is not explicitly referenced in the body text

`MINOR` · `figures`

The text in Section 4 describes the visionary scenario with numbered steps but never explicitly references 'Figure 1' (e.g., 'As shown in Figure 1, the top-level workflow...'). Section 2 mentions 'This process is illustrated in section 4' but does not name the figure. Every figure must be explicitly cited in the surrounding text.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”

### 18. Abbreviation 'API' used without prior definition

`MINOR` · `writing`

The abbreviation 'API' appears in the Abstract and throughout the document but is never defined as 'Application Programming Interface' on first use. Per the guidelines, all abbreviations must be defined before use.

- Page 1, Abstract: “which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”

### 19. Paragraph in Expected Findings is only 1-2 lines

`MINOR` · `structure`

The third paragraph in Section 5 (Expected Findings) consists of a single sentence: 'In addition, universities will save time and money as they will need at least one less server.' This falls below the minimum paragraph length of 5 lines. This point should be merged into the preceding or following paragraph to create a coherent paragraph.

- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”

### 20. Final paragraph in Expected Findings combines three unrelated ideas

`MINOR` · `logic`

The final paragraph of Section 5 combines three distinct ideas: (1) the solution opens up further research, (2) the exam test-execution problem from Section 1 can be tackled, and (3) Ansible or Kubernetes can be used for automatic deployment of GitLab CI Runners. Each should be developed in its own paragraph, or the deployment point should be removed if it is not part of the thesis scope.

- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. with the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams described in section 1. In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 21. Inconsistent capitalization of 'Server' in 'CI Server' / 'VC Server'

`MINOR` · `formatting`

The same concept is capitalized inconsistently: 'CI Server' and 'VC Server' (capital S) appear on page 2, while 'CI server' and 'VC server' (lowercase s) appear on pages 2-3. A single convention should be used throughout.

- Page 2, Problem Statement: “the build plans must be updated manually in the CI Server.”
- Page 3, Related Work: “instant, but still individual feedback generated by the CI server.”
- Page 2, Proposed Solution: “but each time the code in the VC Server gets updated, the build plan is fetched.”
- Page 2, Proposed Solution: “The Version Control server GitLab has an integrated CI service called GitLab CI.”

### 22. Figure caption could be more extensive and descriptive

`SUGGESTION` · `figures`

The caption 'Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI' is moderately informative but could be more extensive. A stronger caption would describe what specific interactions are shown (exercise assignment, repository setup, code submission, CI triggering, result display) and the purpose of the figure.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”
