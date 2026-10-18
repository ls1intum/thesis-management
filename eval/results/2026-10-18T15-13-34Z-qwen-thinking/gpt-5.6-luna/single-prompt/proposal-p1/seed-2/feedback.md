# proposal-p1 — single-prompt

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 25
- **Findings:** 7
- **Cost:** 1 calls, 0 tool calls, depth 1, 12024 tokens, 23s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a relevant problem, a plausible GitLab CI direction, and a small set of appropriate technical references. However, it is not ready for submission because the required Introduction, Motivation, Objectives, and AI transparency statement are absent, the bibliography is below the minimum, and the proposal contains only one figure. The schedule also needs substantial restructuring into measurable agile iterations without writing, documentation, or requirements-gathering tasks.

## Findings

### 1. Required Sections Are Missing

`CRITICAL` · `structure`

The proposal is missing several required sections: Introduction, Motivation, and Section 4 Objectives. Add an Introduction, a distinct visionary Motivation section, and an Objectives section containing a short introduction, 3–4 one-line high-level objectives, and one subsection per objective with at least two paragraphs each.

### 2. AI Transparency Statement Missing

`CRITICAL` · `completeness`

No AI transparency statement is included. Add a first-person statement naming the specific AI tools used, their purposes, the proposal sections in which they were used, and confirming that all AI-generated text was carefully checked for correctness and sense.

### 3. Bibliography Has Too Few Publications

`CRITICAL` · `citation`

The bibliography contains only five publication entries: Booch95, Gamma95, Hilton16, Krusche17, and Krusche18. The URL-based footnotes do not count as bibliography publications. Add at least one to three additional peer-reviewed scientific publications to reach the required minimum of six to eight.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.).”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning.”

### 4. Required Figures and Explicit Figure References Are Incomplete

`CRITICAL` · `figures`

Only one figure is included, which does not satisfy the requirement for at least two figures or diagrams. Add a second meaningful diagram, mockup, or chart, ensure that at least one figure is a suitable UML diagram, reference each figure explicitly by number in the prose, and retain informative captions.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”
- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 5. Problem Impacts Are Not Fully Mapped to Actors

`MAJOR` · `methodology`

The Problem section does not clearly identify all affected stakeholders and connect the problem to their negative consequences. Explicitly state how the separate CI and version-control servers affect instructors, students, university administrators, and smaller universities, and distinguish these consequences from the proposed solution.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis.”
- Page 2, Problem Statement: “Especially in large software engineering courses this is nearly impossible.”

### 6. Schedule Does Not Follow the Required Agile Iteration Structure

`MAJOR` · `methodology`

The schedule is not organized into 2–4 week iterations with measurable, vertically integrated deliverables. Replace isolated tasks such as runner configuration, API work, and build-result handling with end-to-end increments that implement and validate complete objectives. Remove the requirements task and all thesis-writing, documentation, and review tasks from the development schedule.

- Page 5, Time Schedule: “April – 2nd week: Docker GitLab CI Runner confguration”
- Page 5, Time Schedule: “June – 4th week: Requirements (parallel to prototype implementation)”
- Page 5, Time Schedule: “September – 2nd week: Documentation for installation and confguration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”

### 7. Forbidden Sentence Starters Occur Repeatedly

`MINOR` · `writing`

Several sentences begin with forbidden starters. Rewrite these sentences so that they begin with an explicit subject or main clause rather than 'Since', 'To', 'In order to', or 'As'.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers.”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server.”
- Page 2, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations.”
- Page 3, Related Work: “**Since** the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university.”
- Page 4, Expected Findings: “**As described** in section 3, this might lead to better developers and better code.”
