# proposal-p1 — category-fan-out-verified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 30
- **Findings:** 15
- **Cost:** 18 calls, 0 tool calls, depth 3, 86743 tokens, 67s
- **Coverage:** 9/9 categories

## Summary

The proposal lacks several mandatory structural components, an AI transparency statement, and required figures, and contains numerous methodological and writing issues. While the subject matter is promising, extensive revisions are needed before the proposal can be submitted.

## Findings

### 1. Missing Introduction Section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section, which is required by the guidelines.

### 2. Missing Motivation Section

`CRITICAL` · `structure`

The proposal does not contain a dedicated Motivation section, so the required visionary discussion is absent.

### 3. Missing Objective Section with Subsections

`CRITICAL` · `structure`

The proposal lacks an Objective section that starts with a short introduction, a bullet list of 3‑4 high‑level objectives, and separate subsections for each objective.

### 4. Problem Section Presents Solutions

`CRITICAL` · `logic`

The Problem section includes statements that suggest a solution, which is prohibited; it should only describe the problem and its negative impact.

- Page 2, Problem: “The CI server **should be migrated** to the VC server. Furthermore, it **should be possible to adjust the build plan** after the task has been assigned.”

### 5. Missing AI Transparency Statement

`CRITICAL` · `completeness`

The proposal does not contain the required AI Transparency Statement describing AI tools used, their purposes, applied sections, and author verification of AI‑generated text.

### 6. Insufficient Figures and Missing Required UML Diagram

`CRITICAL` · `figures`

The proposal includes only one figure and lacks any UML diagram, whereas the guidelines require at least two figures and at least one UML diagram.

### 7. Figure Not Referenced in Text

`MAJOR` · `figures`

Figure 1 is presented but never cited within the main body of the proposal.

- Page 4, System Requirements and Top Level Design: “**Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI**”

### 8. Writing Style Issues: Passive Voice and Undefined Abbreviation

`MAJOR` · `writing`

Multiple sentences use passive voice, reducing clarity, and the abbreviation "API" is introduced without definition.

- Page 1, Abstract: “as the build plan **is fetched** from the API on each run.”
- Page 2, Proposed Solution: “the build plan **is fetched**.”
- Page 3, Motivation: “feedback **generated** by the CI server.”
- Page 3, System Requirements and Top Level Design: “Artemis **was built**.”
- Page 1, Abstract: “Artemis **API**”

### 9. Iteration Length Too Short

`MAJOR` · `logic`

The schedule lists tasks by individual weeks, resulting in iterations of only one week, which is shorter than the required 2‑4 week iteration length.

- Page 5, Schedule: “– 5th week: **Proposal**”
- Page 5, Schedule: “– 2nd week: **Docker GitLab CI Runner confguration**”

### 10. Non‑Measurable Deliverable – Requirements Sprint

`MAJOR` · `methodology`

The entry "Requirements (parallel to prototype implementation)" does not specify a concrete, measurable deliverable and constitutes a vague requirements‑gathering activity, which is prohibited.

- Page 5, Schedule: “– 4th week: **Requirements (parallel to prototype implementation)**”

### 11. Horizontal Feature Split

`MAJOR` · `methodology`

Work items such as "Requirements" and "Models" are split horizontally across layers rather than describing end‑to‑end vertically integrated features.

- Page 5, Schedule: “– 4th week: **Models (parallel to prototype implementation)**”
- Page 5, Schedule: “– 4th week: **Requirements (parallel to prototype implementation)**”

### 12. Documentation Writing Task Included in Schedule

`MAJOR` · `writing`

The schedule lists a documentation writing activity, which is not allowed in a development‑focused schedule.

- Page 5, Schedule: “– 2nd week: **Documentation for installation and confguration of Artemis with GitLab CI**”

### 13. Formatting Issues: Short Paragraph and Heading Capitalization

`MAJOR` · `formatting`

The abstract contains a single‑line paragraph that does not meet the required length, and several headings use sentence case instead of title case.

- Page 1, Abstract: “**Motivation/Objective:** Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 1: “**with** Artemis & GitLab CI”
- Page 4: “1.1: **Push code template, test cases and sample solution**”
- Page 4: “1.2: **Assign exercise**”
- Page 4: “2.1: **Participate in exercise**”
- Page 4: “2.2: **Setup student repository**”
- Page 4: “3.1: **Push code**”
- Page 4: “3.2: **Trigger CI**”
- Page 4: “3.3: **Send build results**”

### 14. Missing Introductory Text Before Subsections

`MINOR` · `structure`

Subsections such as "1.1: Push code template, test cases and sample solution" appear without any introductory paragraph introducing their content.

- Page 4: “1.1:  Push code  template, test  cases

and  sample  solution

1.2:  Assign  exercise

Instructor”

### 15. Waterfall‑Style Schedule

`SUGGESTION` · `logic`

The schedule lists isolated weekly tasks without grouping them into 2‑4 week iterative sprints, resembling a waterfall plan rather than an agile approach.

- Page 5, Schedule: “It is planned to adhere to the following time schedule:”
