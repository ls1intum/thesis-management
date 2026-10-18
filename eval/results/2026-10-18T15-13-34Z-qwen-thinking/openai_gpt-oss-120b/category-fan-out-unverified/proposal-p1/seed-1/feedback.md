# proposal-p1 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 23
- **Cost:** 10 calls, 0 tool calls, depth 2, 76471 tokens, 91s
- **Coverage:** 9/9 categories

## Summary

The proposal contains several critical structural omissions and major methodological deficiencies, including missing mandatory sections, insufficient bibliography, and major figure shortcomings. While the writing displays some academic tone issues, the most urgent problems relate to compliance with required content, schedule violations, and missing AI transparency.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section, which is required by the guidelines.

### 2. Missing Motivation section

`CRITICAL` · `structure`

A dedicated Motivation section is required to present a visionary, positive outlook for solving the problem, but it is absent.

### 3. Missing Objectives section

`CRITICAL` · `structure`

The proposal lacks an Objectives section with a short introduction, a bullet list of high-level objectives, and detailed subsections for each objective, as required.

### 4. Problem section contains solution statements

`CRITICAL` · `logic`

The Problem section includes sentences that propose solutions, which should be omitted.

- Page 2, Problem: “The CI server **should be migrated** to the VC server.”
- Page 2, Problem: “It **should be possible to adjust** the build plan after the task has been assigned.”
- Page 2, Problem: “In exams, it would be **useful to change** the student build plans after the submission date...”

### 5. Problem section does not identify actors

`MAJOR` · `completeness`

The Problem section fails to explicitly name the stakeholders (e.g., instructors, students, universities) and describe how the problem negatively impacts them.

- Page 2, Problem: “The interactive learning platform Artemis is an approach to use interactive learning during large computer science classes...”

### 6. Insufficient bibliography entries

`CRITICAL` · `completeness`

The bibliography contains only 5 entries, but the guidelines require at least 6‑8 peer‑reviewed publications.

- Page 5, References: “References **[Booch95] ... [Gamma95] ... [Hilton16] ... [Krusche17] ... [Krusche18]**”

### 7. Missing AI Transparency Statement

`CRITICAL` · `completeness`

The proposal does not contain an AI transparency statement as required.

### 8. Figures deficiency

`CRITICAL` · `figures`

The proposal includes only one figure and lacks any required UML diagram, whereas the guidelines mandate at least two figures including a UML diagram.

### 9. Figure not referenced in text

`MAJOR` · `figures`

Figure 1 is never cited in the surrounding text, violating the requirement that every figure be referenced.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 10. Paragraph length insufficient in multiple sections

`MAJOR` · `structure`

Paragraphs in the Abstract and Problem Statement are too short (1‑3 lines) and do not meet the required 5‑8 line length.

- Page 1, Abstract: “Motivation/Objective: **Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.**”
- Page 1, Abstract: “Problem Statement: **The interactive learning platform Artemis currently only supports setups with an external CI server.   The CI server should be migrated to the VC server.      Furthermore, it**”
- Page 1, Abstract: “Proposed Solution: **GitLab CI is the CI system provided by the GitLab VC server. This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.**”
- Page 1, Abstract: “(Expected) Results: **We expect to use the bridge pattern to dynamically bind Artemis to the concrete implementation with GitLab CI.      The reduction in server load will mean that more universities, especially smaller ones with less capacity, will choose Artemis.**”
- Page 2, Problem Statement: “Artemis currently supports Bamboo      1 and Jenkins  2 as a CI server for executing the tests.   This leads to additional server overhead, since the CI and the Version Control (VC) are running on diferent servers.”
- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise [Krusche18].”
- Page 2, Problem Statement: “the build plans must be updated manually in the CI Server.            Especially in large software engineering courses this is nearly impossible.”
- Page 2, Problem Statement: “In exams, it would be useful to change the student build plans after the submission date to frstly hide the test cases during the exam and only compile the code and secondly use a debug compiler not failing due to a simple syntax error.”

### 11. Paragraph mixes unrelated ideas

`MAJOR` · `logic`

A single paragraph in the Related Work section discusses unrelated concepts (Booch, Fowler, Krusche et al.) together, violating the one‑idea‑per‑paragraph rule.

- Page 2, Related Work: “According to Grady Booch’s book Object-oriented analysis and design with applications integration should generally take place regularly, to avoid problems later on [Booch95].       Martin Fowler explains in his post  Continuous    Integration,  Continuous   Integration  as  a practice,  in which   each integration,  which **should take place at least once per day per developer, is build and tested with predefned test cases**.   In their paper Interactive Learning – Increasing Student Participation through Shorter Exercise Cycles, Krusche et al. have taken up this concept to improve the learning experience in large computer science courses at university [Krusche17].”

### 12. Enumerated list used instead of prose in System Requirements

`MAJOR` · `formatting`

The System Requirements and Top Level Design section presents the workflow as a numbered list, which is not allowed outside of schedule or objective sections.

- Page 3, System Requirements and Top Level Design: “**1)  Name:      Exercise work fow**

**2)  Participating actors: Bob: Instructor, Mary: Student**

**3)  Flow of events:**
   1.  Bob prepares the exercise via the web interface of Artemis and pushes the exercise code template, the test cases as well as the sample solution to the VC server [Krusche18].
   2.  Artemis configures the build plan accordingly to the configuration made by Bob and makes it available via the API.
   3.  Mary clicks the start exercise button after the release date [Krusche18].”

### 13. Vague or non‑measurable deliverables in schedule

`MAJOR` · `methodology`

Several schedule entries describe vague activities without concrete, measurable outcomes, such as "Requirements" and "Writing…".

- Page 5, Schedule: “— **4th week**: Requirements (parallel to prototype implementation)”
- Page 5, Schedule: “October: **Writing . . .**”

### 14. Horizontal feature splits instead of vertical integration

`MAJOR` · `logic`

Work items are split by technical layer (Docker configuration, API implementation, build‑result handling) rather than delivering end‑to‑end vertically integrated features.

- Page 5, Schedule: “— **2nd week**: Docker GitLab CI Runner configuration
— **4th week**: prototype implementation: Connectivity to GitLab CI
— **4th week**: prototype implementation: Sending build results to Artemis”

### 15. Iteration length too short

`CRITICAL` · `other`

The schedule defines iterations of only one week, which is shorter than the required 2‑4 week minimum.

- Page 5, Schedule: “— **5th week**: Proposal”

### 16. Dedicated requirements sprint present

`CRITICAL` · `other`

The schedule includes a standalone "Requirements" task, which is prohibited by the guidelines.

- Page 5, Schedule: “— **4th week**: Requirements (parallel to prototype implementation)”

### 17. Thesis writing tasks embedded in schedule

`CRITICAL` · `other`

The schedule contains multiple thesis‑writing related activities, which must not be part of the development schedule.

- Page 5, Schedule: “October: **Writing . . .**”
- Page 5, Schedule: “— **2nd week**: Thesis ready for review”
- Page 5, Schedule: “— **3rd week**: Review fnished”
- Page 5, Schedule: “— **1st week**: Thesis”

### 18. Passive voice usage

`MINOR` · `writing`

Passive constructions are used; revise to active voice.

- Page 2, Problem Statement: “The **CI server should be migrated** to the VC server.”
- Page 2, Problem Statement: “the base build plan **is copied** once after the student started the exercise.”
- Page 2, Problem Statement: “Later changes to the base build plan **are not patched** automatically to all student build plans, but rather the build plans **must be updated** manually in the CI Server.”

### 19. Excessive filler words and superlatives

`MINOR` · `writing`

Filler words and superlatives such as 'Furthermore', 'Additional', 'Very' dilute academic tone; remove or replace.

- Page 1, Abstract: “**Furthermore**, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “This leads to **additional** server overhead, since the CI and the Version Control (VC) are running on different servers.”
- Page 2, Problem Statement: “**Additionally**, the base build plan is copied once after the student started the exercise.”
- Page 2, Proposed Solution: “**Furthermore**, the CI service should be part of the VC server to reduce **additional** efort by maintaining two servers.”
- Page 2, Proposed Solution: “The current setup and implementation makes it **very** cumbersome to update the build plan afterwards.”

### 20. Forbidden sentence starters

`MINOR` · `writing`

Sentences start with prohibited words (Since, To, As, etc.); rewrite to avoid.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required...”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server...”
- Page 3, Motivation: “**Since** the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university.”
- Page 2, Proposed Solution: “**As** a result, instructors are able to update the build plan after the release of an exercise, GitLab CI fetches the updated build plan when needed.”
- Page 4, Expected Findings: “**As** described in section 3, this might lead to better developers and better code...”

### 21. Undefined abbreviation

`MINOR` · `writing`

Abbreviation 'API' is used without prior definition.

- Page 2, Proposed Solution: “the Artemis **API**...”

### 22. Inconsistent terminology for CI component

`MINOR` · `writing`

The proposal uses both “CI server” and “CI service” to refer to the same Continuous Integration component, leading to inconsistency.

- Page 1, Abstract: “It is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt **CI service**.”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a **CI server**.”

### 23. Waterfall‑style schedule, not agile

`MINOR` · `logic`

The overall timeline is a linear, date‑driven plan rather than an agile, sprint‑based approach, conflicting with the agile principles required for the schedule.

- Page 5, Schedule: “It is planned to adhere to the following time schedule: … March … April … May … June … July … August … September … October … November … December”
