# proposal-p1 — agentic

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 15
- **Cost:** 6 calls, 39 tool calls, depth 6, 77702 tokens, 24s
- **Coverage:** 9/9 categories

## Summary

The proposal has a clear technical direction and a relevant workflow, but it requires substantial revision before it meets the group's guidelines. Critical gaps are the missing Introduction, Motivation, properly structured Objective section, AI Transparency Statement, insufficient peer-reviewed bibliography, and second/UML figure. The problem statement also contains a solution prescription, while the schedule is largely a waterfall-style set of horizontal tasks with vague deliverables and includes documentation/thesis-writing work. Reorganize the proposal around the required sections, add measurable end-to-end iterations tied to explicit objectives, and revise the flagged wording and figure coverage.

## Findings

### 1. Missing Introduction Section

`CRITICAL` · `STRUCTURE`

Add the required Introduction section before the problem discussion; it should orient the reader and lead into the proposal's problem, motivation, and objectives.

- Page 1, Introduction

### 2. Missing Motivation Section

`CRITICAL` · `STRUCTURE`

Add a distinct Motivation section that explains the scientific and practical value of solving the problem, rather than leaving motivation only as a label in the abstract.

- Page 1, Motivation: “**Motivation/Objective:** Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 3. Required Objective Structure Is Absent

`CRITICAL` · `STRUCTURE`

Replace Section 4 with an Objective section: begin with a short introduction and a one-line bullet list of 3–4 high-level objectives, then provide one subsection per objective with at least two paragraphs each. The current section is a requirements/design scenario and contains no objective list or objective subsections.

- Page 3, System Requirements and Top Level Design: “4     **System        Requirements              and     Top     Level     Design**”

### 4. Missing AI Transparency Statement

`CRITICAL` · `STRUCTURE`

Add an AI Transparency Statement as a dedicated required section, in addition to correcting the other required-section omissions.

- Page 6, AI Transparency Statement

### 5. Too Few Bibliography Publications

`CRITICAL` · `BIBLIOGRAPHY`

Expand the bibliography to at least 6–8 peer-reviewed publications. It currently contains five entries; the five visible entries are Booch95, Gamma95, Hilton16, Krusche17, and Krusche18.

- Page 5, References: “**References**”

### 6. Fewer Than Two Required Figures

`CRITICAL` · `FIGURES`

Add at least one more diagram or mockup and ensure that at least one of the figures is a suitable UML diagram. The proposal currently contains only Figure 1.

- Page 4, Figures: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Solution Prescription Appears in Problem Statement

`MAJOR` · `PROBLEM`

Keep the Problem section strictly diagnostic: move solution prescriptions such as migrating the CI server out of the problem statement and into the Proposed Solution or objectives. State the affected actors and consequences there instead.

- Page 1, Abstract: “**Problem Statement:** The interactive learning platform Artemis currently only supports setups with an external CI server.   **The CI server should be migrated to the VC server.**”

### 8. Forbidden Sentence Starter

`MINOR` · `WRITING`

Rewrite the sentence without a forbidden “Since” opening, for example by making the causal relationship a main-clause statement. Apply the same correction to other sentences beginning with forbidden starters.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 9. Repeated Filler Transitions

`MINOR` · `WRITING`

Reduce repeated additive/filler transitions such as “Furthermore,” and “Additionally,” by combining related sentences or stating the contribution directly; these occur repeatedly across the proposal.

- Page 2, Problem Statement: “**Additionally,** the base build plan is copied once after the student started the exercise [Krusche18].”

### 10. Passive or Awkward Agent Construction

`MINOR` · `WRITING`

Replace the passive construction with an active subject, such as “GitLab CI fetches the build plan...” This makes the actor and operation explicit.

- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis and **GitLab CI is downloading the build plan** for each execution of the test cases.”

### 11. Workflow Relies Heavily on Enumeration

`MINOR` · `WRITING_STRUCTURE`

Convert the 14-step workflow into concise prose around the diagram, or retain only a short, necessary list and explain the key end-to-end behavior in paragraphs. The current section relies heavily on a long enumerated list outside the schedule/objective-list exceptions.

- Page 3, System Requirements and Top Level Design: “**3)  Flow of events:**”

### 12. Schedule Includes Documentation and Thesis Writing

`MAJOR` · `SCHEDULE`

Remove thesis-writing and documentation tasks from the development schedule, or separate them from the agile research iterations if a separate administrative plan is allowed. The listed tasks include documentation and writing rather than research/development deliverables.

- Page 5, Time Schedule: “**2nd week: Documentation for installation and confguration of Artemis with GitLab CI**”

### 13. Vague and Requirements-Focused Schedule Items

`MAJOR` · `SCHEDULE`

Replace vague or non-measurable entries with verifiable deliverables and define 2–4-week iterations. “Models” and “Requirements” do not state what artifact or tested capability will be completed.

- Page 5, Time Schedule: “**4th week: Models (parallel to prototype implementation)**

                          –  4th week: Requirements (parallel to prototype implementation)”

### 14. Schedule Lacks Objective-Linked Acceptance Criteria

`MAJOR` · `SCHEDULE`

Tie each iteration to explicit high-level objectives and acceptance criteria. The schedule lists activities but never identifies which objective each increment satisfies or how completion will be measured.

- Page 4, Time Schedule: “It is planned to adhere to the following time schedule:”

### 15. Awkward Passive and Progressive Construction

`MINOR` · `WRITING`

Use an active construction with GitLab CI as the subject, for example “GitLab CI fetches the build plan from the Artemis API for each test execution.”

- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis and **GitLab CI is downloading the build plan** for each execution of the test cases.”
