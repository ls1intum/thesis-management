# proposal-p1 — single-prompt

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 11
- **Cost:** 1 calls, 0 tool calls, depth 1, 12503 tokens, 25s
- **Coverage:** 9/9 categories

## Summary

The proposal has a concrete technical topic, a relevant problem context, and a preliminary implementation direction involving Artemis and GitLab CI. However, it is not ready for submission because several required sections are missing, the objective structure and AI transparency statement are absent, the bibliography is too short, only one figure is provided, and the schedule does not follow the required agile iteration model.

## Findings

### 1. Required Sections Are Missing

`CRITICAL` · `structure`

The proposal does not contain the required Introduction, Motivation, or Objective section. Section 4 instead presents system requirements and a workflow, while Section 5 presents expected findings. Add the required sections with the prescribed content and numbering.

### 2. Objective Structure Is Missing

`CRITICAL` · `structure`

The required Objective section is absent. Add a short introduction, a one-line bullet list containing 3–4 high-level objectives, and one subsection for each objective; each subsection must contain at least two paragraphs.

### 3. AI Transparency Statement Is Missing

`CRITICAL` · `completeness`

No AI transparency statement is included. Add a first-person statement naming the specific AI tools used, their purposes, the sections in which they were used, and confirming that all AI-generated text was carefully checked.

### 4. Minimum Figure Count Is Not Met

`CRITICAL` · `figures`

The proposal contains only one figure, but at least two figures or diagrams are required. Add a second meaningful figure or mockup, preferably an activity, component, or class diagram if Figure 1 is not intended as a valid UML diagram.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 5. Bibliography Contains Too Few Publications

`CRITICAL` · `citation`

The bibliography contains only five entries, below the required minimum of 6–8 publications. Add at least one, preferably several, additional peer-reviewed scientific publications. Rename the References section to Bibliography if that is the required section label.

- Page 5, References: “References”

### 6. Schedule Does Not Use Agile Iterations

`MAJOR` · `schedule`

The schedule is organized as isolated weekly milestones rather than 2–4-week agile iterations. Reorganize it into iterations with measurable, end-to-end deliverables tied explicitly to the high-level objectives, rather than separating work into disconnected implementation, model, and configuration tasks.

- Page 5, Time Schedule: “April – 2nd week: Docker GitLab CI Runner confguration – 4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “May – 3nd week: prototype implementation: Artemis API for confguration”
- Page 5, Time Schedule: “June – 4th week: prototype implementation: Sending build results to Artemis – 4th week: Models (parallel to prototype implementation) – 4th week: Requirements (parallel to prototype implementation)”

### 7. Schedule Contains Prohibited Tasks

`MAJOR` · `schedule`

The schedule includes prohibited requirements-gathering, documentation, thesis-writing, and review tasks. Replace these with development or research deliverables; requirements should be handled through the backlog within iterations rather than as a dedicated task, and thesis writing should not appear in the development schedule.

- Page 5, Time Schedule: “4th week: Requirements (parallel to prototype implementation)”
- Page 5, Time Schedule: “September – 2nd week: Documentation for installation and confguration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “November – 2nd week: Thesis ready for review – 3rd week: Review fnished”
- Page 6, Time Schedule: “December – 1st week: Thesis”

### 8. Figure Reference and Caption Are Insufficient

`MAJOR` · `figures`

The figure is not explicitly referenced by figure number in the surrounding prose, and its caption is not sufficiently extensive for the caption requirement. Add an explicit reference such as “Figure 1 shows…” and expand the caption to explain the actors, systems, numbered interactions, and relevance to the proposed architecture.

- Page 2, Proposed Solution: “This process is illustrated in section 4.”
- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 9. Passive Voice Obscures Actors

`MAJOR` · `writing`

The proposal uses passive or impersonal constructions extensively, which obscures the responsible actor. Rewrite sentences with explicit subjects, for example, identify who copies the repository, updates the build plans, or fetches the configuration instead of using constructions such as “is copied” and “must be updated.”

- Page 2, Problem Statement: “The student base repository **is copied** [Krusche18].”
- Page 2, Problem Statement: “the build plans **must be updated manually** in the CI Server.”
- Page 3, Related Work: “the lecture **is split up** into multiple iterations”

### 10. Forbidden Sentence Starters Occur

`MINOR` · `writing`

Several sentences begin with forbidden starters. Rewrite these sentences so that they begin with a direct subject or an independent clause.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers.”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server.”
- Page 3, Related Work: “**In order to** implement this idea, the lecture is split up into multiple iterations.”
- Page 3, Related Work: “**Since** the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university [Hilton16].”

### 11. API Is Not Defined

`MINOR` · `writing`

The abbreviation API is used without being defined at first use. Expand it as “application programming interface (API)” before using the abbreviation throughout the proposal.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis **API**.”
- Page 2, Proposed Solution: “The build plan can be made available via the **API** of Artemis.”
