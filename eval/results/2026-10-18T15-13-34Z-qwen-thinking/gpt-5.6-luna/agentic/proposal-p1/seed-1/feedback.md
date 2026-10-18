# proposal-p1 — agentic

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 13
- **Cost:** 7 calls, 36 tool calls, depth 7, 94010 tokens, 43s
- **Coverage:** 9/9 categories

## Summary

The proposal has a promising problem context and a plausible GitLab CI direction, but it requires substantial revision before approval. Critical gaps are the missing required sections and AI transparency statement, insufficient bibliography, and only one figure without the required UML coverage. The problem section mixes in solution material, while the schedule is not organized into agile 2–4-week iterations and includes requirements, documentation, and thesis-writing tasks. Several smaller writing and figure-reference issues should also be corrected.

## Findings

### 1. Required sections are missing

`CRITICAL` · `STRUCTURE`

The required proposal structure is incomplete: there is no Introduction, Motivation, or Objective section with objective subsections, and no AI Transparency Statement. Add each required section; Section 4 is titled “System Requirements and Top Level Design” and does not satisfy the required Objective section structure.

- Page 1

### 2. Bibliography is below the minimum publication count

`CRITICAL` · `BIBLIOGRAPHY`

The bibliography contains only five publications ([Booch95], [Gamma95], [Hilton16], [Krusche17], and [Krusche18]), below the required minimum of six to eight peer-reviewed publications. Add at least one more suitable peer-reviewed publication, preferably several to reach the target range.

- Page 5, References: “References”

### 3. Required second figure and UML diagram are missing

`CRITICAL` · `FIGURES`

Only one figure is included, but the guidelines require at least two diagrams, mockups, or figures, including at least one suitable UML diagram. Add a second figure and ensure that at least one figure is a suitable UML diagram.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 4. AI transparency statement is missing

`CRITICAL` · `AI_TRANSPARENCY`

No AI transparency statement appears in the proposal. Add a first-person statement naming each AI tool used, its purpose, the sections affected, and confirmation that all AI-generated text was carefully checked.

- Page 6

### 5. Problem section proposes a solution

`MAJOR` · `METHODOLOGY`

The Problem Statement includes a proposed change rather than only describing the problem and its consequences. Move solution material to the solution/objectives section and leave the problem section focused on the negative impact and affected stakeholders.

- Page 2, Problem Statement: “In exams, it would be useful to change the student build plans after the submission date to frstly hide the test cases during the exam and only compile the code and secondly use a debug compiler not failing due to a simple syntax error.”

### 6. Stakeholder impacts are not explicitly developed

`MAJOR` · `COMPLETENESS`

The Problem Statement does not clearly identify the relevant actors and how the problem negatively affects each of them. Name the affected stakeholders (for example, instructors, students, and university administrators) in the problem section and state the concrete consequence for each.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis.”

### 7. Forbidden sentence starter in the abstract

`MINOR` · `WRITING`

This sentence starts with the forbidden starter “Since”. Rewrite it with a direct subject, for example by stating that the inclusion of CI in some VC systems enables the reduction.

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 8. API is not defined

`MINOR` · `WRITING`

The abbreviation “API” is used without being expanded on first use. Define it as “application programming interface (API)” before using the abbreviation.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”

### 9. Figure 1 is not explicitly referenced

`MINOR` · `FIGURES`

The figure is not explicitly referenced by figure number in the prose. Replace the generic reference to “section 4” with a direct reference such as “Figure 1 shows…”, so readers can locate the diagram unambiguously.

- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 10. Schedule contains prohibited writing and documentation tasks

`MAJOR` · `SCHEDULE`

The schedule includes thesis writing, documentation, review, and revision tasks, although the schedule rules require it to focus on development/research work. Remove or relocate these tasks from the proposal schedule and retain concrete research or implementation deliverables.

- Page 5, Time Schedule: “–  2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 11. Schedule includes a requirements task

`MAJOR` · `SCHEDULE`

The schedule contains a requirements-only work item, which conflicts with the agile scheduling rule. Replace it with a time-boxed, vertically integrated deliverable selected from a backlog, such as an end-to-end requirement implemented and validated through Artemis and GitLab CI.

- Page 5, Time Schedule: “–  4th week: Requirements (parallel to prototype implementation)”

### 12. Schedule lacks agile iterations and measurable deliverables

`MAJOR` · `SCHEDULE`

The plan is organized as isolated monthly or single-week activities rather than 2–4-week iterations with measurable, end-to-end deliverables. Recast the work into explicit 2–4-week iterations, each naming a demonstrable outcome and linking it to a high-level objective.

- Page 5, Time Schedule: “–  4th week: Models (parallel to prototype implementation)”

### 13. Forbidden sentence starter in related work

`MINOR` · `WRITING`

The sentence starts with the forbidden phrase “In order to”. Replace it with a direct formulation such as “The lecture is split up…” without the introductory phrase.

- Page 2, Related Work: “In order to implement this idea, the lecture is split up”
