# proposal-p1 — single-prompt

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 10
- **Cost:** 1 calls, 0 tool calls, depth 1, 12949 tokens, 25s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a relevant problem, a plausible GitLab CI direction, and a useful initial workflow. However, it needs substantial revision before submission because several required sections and the AI transparency statement are missing, the bibliography and figure count are insufficient, and the schedule does not follow the required agile iteration model.

## Findings

### 1. Required Sections and Objective Structure Are Missing

`CRITICAL` · `completeness`

The proposal does not contain the required Introduction, Motivation, or Objective section. Section 4 is titled "System Requirements and Top Level Design" and Section 5 is titled "Expected Findings", but neither provides the required Objectives structure: a short introduction, 3–4 one-line high-level objectives, and one subsection of at least two paragraphs for each objective. Add and structure these sections explicitly.

### 2. AI Transparency Statement Missing

`CRITICAL` · `completeness`

No AI transparency statement is included. Add a first-person statement naming the specific AI tools used, their purposes, the proposal sections in which they were used, and confirmation that all AI-generated text was carefully checked.

### 3. Fewer Than Two Required Figures

`CRITICAL` · `figures`

The proposal contains only one figure, although at least two diagrams, mockups, or figures are required. Add a second meaningful figure, such as an activity, component, or deployment diagram, and ensure that at least one figure is a suitable UML diagram.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 4. Bibliography Has Too Few Publications

`CRITICAL` · `citation`

The bibliography contains only five entries: Booch, Gamma, Hilton et al., Krusche et al. (2017), and Krusche and Seitz (2018). Add at least one more scientific, peer-reviewed publication so that the bibliography contains the required minimum of six publications; preferably include six to eight relevant sources.

- Page 5, References: “[Krusche17] Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning: Increasing student participation through shorter exercise cycles.”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning.”

### 5. Problem Section Does Not Clearly Cover Stakeholders

`MAJOR` · `completeness`

The Problem section identifies the platform and smaller universities, but it does not clearly identify all relevant human stakeholders or explain the negative consequences for each. State explicitly how the current architecture affects instructors, students, and university administrators, including the operational and educational consequences.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis.”
- Page 2, Problem Statement: “Especially in large software engineering courses this is nearly impossible.”

### 6. Schedule Is Not Iterative or Vertically Integrated

`MAJOR` · `methodology`

The schedule is not divided into 2–4-week iterations and uses isolated component tasks rather than measurable, vertically integrated increments. Replace the month/week milestones with iterations that each deliver an end-to-end capability, such as configuring one complete exercise workflow, updating a build plan through the API, and returning results to Artemis, with explicit acceptance criteria.

- Page 5, Time Schedule: “April – 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “May – 3nd week: prototype implementation: Artemis API for confguration”
- Page 5, Time Schedule: “June – 4th week: prototype implementation: Sending build results to Artemis”

### 7. Schedule Contains Prohibited and Vague Tasks

`MAJOR` · `methodology`

The schedule includes work that should not appear in the development/research schedule, including documentation and thesis writing, and it contains vague items without measurable deliverables. Remove thesis-writing and documentation tasks from this schedule and replace vague entries such as "Models" and "Writing" with concrete research or implementation deliverables.

- Page 5, Time Schedule: “4th week: Models (parallel to prototype implementation)”
- Page 5, Time Schedule: “2nd week: Documentation for installation and confguration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “1st week: Thesis”

### 8. Forbidden Sentence Starters Occur Repeatedly

`MINOR` · `writing`

Several sentences begin with forbidden subordinating or infinitive starters. Rewrite these sentences with explicit subjects and active constructions, for example by replacing "Since" or "In order to" with a direct statement of the cause or purpose.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers.”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server.”
- Page 2, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations.”
- Page 3, Related Work: “**Since** the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university.”
- Page 4, Expected Findings: “**As** described in section 3, this might lead to better developers and better code.”

### 9. Passive Voice Obscures Actors

`MINOR` · `writing`

The proposal uses passive constructions frequently, which obscures the responsible actor. Rewrite them in active voice by naming the component or stakeholder performing the action, for example, "Artemis copies the student repository" and "GitLab CI fetches the build plan from the Artemis API."

- Page 3, System Requirements and Top Level Design: “The student base repository **is copied**.”
- Page 3, System Requirements and Top Level Design: “GitLab CI **gets confgured** so that it takes the build plan from the Artemis API.”
- Page 1, Abstract: “the build plan **is fetched** from the API on each run.”

### 10. Figure Is Not Directly Referenced in the Text

`MINOR` · `figures`

The only figure is not explicitly referenced by figure number in the prose. Replace the vague reference to "section 4" with a direct reference such as "Figure 1 shows the top-level interactions". Add an equivalent direct reference for the second figure.

- Page 2, Proposed Solution: “This process is illustrated in **section 4**.”
- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”
