# proposal-p1 — agentic

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 11
- **Cost:** 9 calls, 39 tool calls, depth 9, 131688 tokens, 32s
- **Coverage:** 9/9 categories

## Summary

The proposal has a clear problem context and a plausible GitLab CI direction, but it requires substantial revision before acceptance. Critical gaps are the missing required Introduction, Motivation, Objective structure, and AI Transparency Statement, the bibliography having only five publications, and the inclusion of only one figure. The schedule is not organized as measurable 2–4-week agile iterations and includes requirements, documentation, and thesis-writing tasks. Several smaller issues concern forbidden sentence starters, caption informativeness, and terminology consistency.

## Findings

### 1. Several required proposal sections are missing

`CRITICAL` · `COMPLETENESS`

Add the required Introduction, Motivation, Objective section with 3–4 one-line action-form objectives and a two-paragraph subsection for each objective, and an AI Transparency Statement. The proposal currently has Problem Statement, Proposed Solution, Related Work, System Requirements and Top Level Design, Expected Findings, and Time Schedule, but none of these required sections.

### 2. Bibliography has fewer than six peer-reviewed publications

`CRITICAL` · `CITATION`

Expand the bibliography to at least 6–8 peer-reviewed publications. The current References section contains only five publication entries; the seven numbered URLs are footnotes rather than qualifying bibliography publications.

- Page 5, References: “[Krusche17]   Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning: Increasing student participation through shorter exercise cycles.”

### 3. Proposal contains only one figure

`CRITICAL` · `FIGURES`

Add at least one further diagram or mockup and ensure that at least one of the two figures is a suitable UML diagram. The document contains only Figure 1, so it does not meet the minimum of two figures.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 4. Sentence begins with forbidden starter “Since”

`MINOR` · `WRITING`

Rewrite sentences that begin with forbidden starters. Replace the causal opening with a direct subject-led construction, for example by making the inclusion of CI systems the subject.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 5. Section 4 is not the required objectives structure

`CRITICAL` · `STRUCTURE`

Replace the current Section 4 workflow/design discussion with the required Objective section: introduce the objectives briefly, list 3–4 high-level action-form objectives, and provide at least two paragraphs of detail for each objective. This is also necessary for the schedule to reference concrete high-level goals.

- Page 3, System Requirements and Top Level Design: “4     System        Requirements              and     Top     Level     Design”

### 6. Sentence begins with forbidden starter “Since”

`MINOR` · `WRITING`

Rewrite the abstract sentence so it does not begin with the forbidden starter “Since”; make the inclusion of CI systems or its consequence the direct subject of the sentence.

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 7. Schedule is not organized into agile 2–4-week iterations

`MAJOR` · `SCHEDULE`

Replace the month-and-week plan with explicit 2–4-week iterations, each having measurable end-to-end deliverables. Combine the isolated tasks into iterations that demonstrate working functionality and specify acceptance criteria for runner configuration, API retrieval, test execution, and result transmission.

- Page 5, Time Schedule: “2nd week: Docker GitLab CI Runner confguration”

### 8. Schedule includes a requirements task

`MAJOR` · `SCHEDULE`

Remove requirements-gathering, documentation-writing, and thesis-writing tasks from the development schedule, or replace them with research/development deliverables. The current schedule explicitly includes all three disallowed task types.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”

### 9. Schedule includes documentation and thesis-writing tasks

`MAJOR` · `SCHEDULE`

Remove documentation and generic thesis-writing tasks from the schedule, or replace them with concrete development/research deliverables. The schedule should focus on implementation and evaluation work rather than thesis production.

- Page 5, Time Schedule: “2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 10. Figure caption is not sufficiently informative

`MINOR` · `FIGURES`

Expand the caption to explain the actors, systems, and interaction flow shown, rather than merely naming the figure. The caption should be informative enough to understand the diagram without relying on the surrounding text.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 11. Version-control server terminology is inconsistent

`MINOR` · `FORMATTING`

Choose one term for the version-control server and use it consistently throughout the proposal. The prose alternates among “VC server,” “Version Control server,” and “VC Server”; standardize capitalization and wording after the initial abbreviation definition.

- Page 2, Proposed Solution: “The Version Control server GitLab     3 has an integrated CI service called GitLab CI”
