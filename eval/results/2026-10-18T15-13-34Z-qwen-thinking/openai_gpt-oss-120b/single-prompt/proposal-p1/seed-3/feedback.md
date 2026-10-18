# proposal-p1 — single-prompt

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 32
- **Findings:** 15
- **Cost:** 1 calls, 0 tool calls, depth 1, 13395 tokens, 28s
- **Coverage:** 9/9 categories

## Summary

The proposal is missing several mandatory sections, has an insufficient bibliography and figures, and contains major methodological and writing issues. Important corrections are required before it can be accepted.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section required by the guidelines.

### 2. Missing Motivation section

`CRITICAL` · `structure`

The proposal does not contain a Motivation section required by the guidelines.

### 3. Missing Objective section with required subsections

`CRITICAL` · `structure`

There is no Objective section that starts with a short introduction, a bullet list of 3‑4 high‑level objectives, and detailed subsections for each objective.

### 4. Missing AI Transparency statement

`CRITICAL` · `structure`

The proposal does not include an AI transparency statement, which is a mandatory component.

### 5. Insufficient bibliography entries

`CRITICAL` · `citation`

Only five bibliography entries are provided; the guidelines require at least six to eight peer‑reviewed publications.

- Page 5, References: “[Booch95]     Booch,     G.       Object-oriented    analysis    and   design    with   applications   (2.   ed.).”

### 6. Insufficient number of figures

`CRITICAL` · `figures`

The proposal contains only one figure (Figure 1), whereas at least two diagrams or figures are required.

- Page 4, System Requirements: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. No UML diagram present

`CRITICAL` · `figures`

The required UML diagram (class, activity, component, etc.) is missing from the proposal.

### 8. Figure 1 is not referenced in the text

`MAJOR` · `figures`

The only figure included (Figure 1) is never cited or discussed within the body of the proposal.

- Page 4, System Requirements: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 9. Problem section includes solution description

`MAJOR` · `logic`

The Problem section contains statements that describe a solution (e.g., migrating the CI server), which violates the rule that the problem description must not present solutions.

- Page 2, Problem Statement: “The CI server should be migrated to the VC server.”

### 10. Problem section does not identify actors

`MAJOR` · `logic`

The Problem section fails to name the stakeholders (e.g., instructors, students) and describe how the problem affects them.

- Page 2, Problem Statement: “The interactive learning platform Artemis is an approach to use interactive learning during large computer science classes ...”

### 11. Schedule includes thesis‑writing tasks

`MAJOR` · `methodology`

The schedule lists activities such as "Writing...", "Thesis ready for review", and "Review finished", which are prohibited; the schedule must contain only research/development work.

- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “November – 2nd week: Thesis ready for review”

### 12. Schedule includes separate requirements tasks

`MAJOR` · `methodology`

The schedule contains a "Requirements (parallel to prototype implementation)" item, which violates the rule that no dedicated requirements‑gathering sprint is allowed.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”

### 13. Schedule iterations not defined as 2‑4 week sprints

`MAJOR` · `methodology`

The presented timeline groups work by months and weeks but does not clearly define iterative sprints of 2‑4 weeks each.

- Page 5, Time Schedule: “5th week: Proposal”

### 14. Forbidden sentence starters used

`MAJOR` · `writing`

Multiple sentences begin with prohibited starters such as "Since" and "Because", which is not allowed.

- Page 3, System Requirements: “Since the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity ...”
- Page 3, System Requirements: “Because the build plan is fetched from the API on each run ...”

### 15. Use of filler words

`MINOR` · `writing`

The text contains filler and redundant words such as "Additionally", "Furthermore", and "In order to", which should be avoided.

- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise ...”
- Page 2, Problem Statement: “Furthermore, the CI service should be part of the VC server ...”
