# proposal-p1 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 32
- **Findings:** 16
- **Cost:** 10 calls, 0 tool calls, depth 2, 76877 tokens, 83s
- **Coverage:** 9/9 categories

## Summary

The proposal contains several critical structural and methodological deficiencies, such as missing mandatory sections, an inadequate schedule, and absent AI transparency and bibliography requirements. It also has numerous writing and formatting issues that need to be addressed before submission.

## Findings

### 1. Missing required sections (Introduction, Motivation, Objectives)

`CRITICAL` · `structure`

The proposal lacks the mandatory Introduction, Motivation, and Objectives sections required by the guidelines.

### 2. Problem section presents solutions

`CRITICAL` · `logic`

The Problem section includes statements that propose solutions, which is not allowed.

- Page 2, Problem: “The CI server **should be migrated to the VC server**.”
- Page 2, Problem: “it **should be possible to adjust the build plan after the task has been assigned**.”

### 3. Problem section does not identify actors

`MAJOR` · `completeness`

The Problem section fails to explicitly name stakeholders and describe how the problem negatively impacts them.

- Page 2, Problem: “**Artemis currently supports Bamboo and Jenkins as a CI server, leading to additional server overhead because the CI and the Version Control (VC) are running on different servers.**”

### 4. Insufficient bibliography entries

`CRITICAL` · `completeness`

The bibliography contains only 5 publications, but the guidelines require at least 6‑8 peer‑reviewed publications.

- Page 5, References: “References ... [Booch95] ... [Gamma95] ... [Hilton16] ... [Krusche17] ... **[Krusche18]**”

### 5. Figures deficiencies

`CRITICAL` · `figures`

The proposal includes only one figure, lacks any required UML diagram, and the existing figure is not referenced in the text.

- Page 4, Figures: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 6. Forbidden sentence starters

`MAJOR` · `writing`

Several sentences start with prohibited words such as 'Since', 'To', and 'As', violating the writing style guidelines.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort...”
- Page 2, Problem: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases...”
- Page 2, Proposed Solution: “**As** a result, instructors are able to update the build plan...”

### 7. Passive voice usage

`MAJOR` · `writing`

The proposal contains passive constructions; active voice is required.

- Page 1, Abstract: “as the build plan **is fetched** from the API on each run.”
- Page 2, Problem: “Later changes to the base build plan **are not patched** automatically...”
- Page 3, Problem: “CI services **are** now often **included** with VC servers 7.”

### 8. Use of filler words and superlatives

`MINOR` · `writing`

The text contains filler words (additional, also, furthermore, additionally) and superlatives (very) which should be avoided.

- Page 1, Abstract: “**Furthermore**, should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem: “This leads to **additional** server overhead, since the CI and the Version Control (VC) are running on different servers.”
- Page 2, Problem: “**Additionally**, the base build plan is copied once after the student started the exercise [Krusche18].”
- Page 2, Proposed Solution: “**Additional**ly, the CI service should be part of the VC server to reduce additional effort...”
- Page 2, Problem: “The current setup and implementation makes it **very** cumbersome to update the build plan afterwards.”
- Page 3, Problem: “but the build is **also** less likely to fail in the master branch [Hilton16].”

### 9. Undefined abbreviation 'API'

`MINOR` · `writing`

The abbreviation 'API' is used without being defined earlier in the document.

- Page 1, Abstract: “supports the provision of the build plan via the Artemis **API**, which makes it possible...”

### 10. First-person pronoun usage

`MINOR` · `writing`

The proposal uses the first‑person pronoun 'I', which should be avoided in formal thesis proposals.

- Page 5, Schedule: “During this time, **I** will not have time to work on my thesis.”

### 11. Abstract paragraphs too short

`MAJOR` · `formatting`

Paragraphs in the Abstract are shorter than the recommended 5‑8 lines, violating the paragraph length guideline.

- Page 1, Abstract: “Motivation/Objective: **Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.**”
- Page 1, Abstract: “Problem Statement: **The interactive learning platform Artemis currently only supports setups with an external CI server. The CI server should be migrated to the VC server. Furthermore, it should be possible to adjust the build plan after the task has been assigned.**”
- Page 1, Abstract: “Proposed Solution: **GitLab CI is the CI system provided by the GitLab VC server. This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.**”
- Page 1, Abstract: “(Expected) Results: **We expect to use the bridge pattern to dynamically bind Artemis to the concrete implementation with GitLab CI. The reduction in server load will mean that more universities, especially smaller ones with less capacity, will choose Artemis.**”

### 12. Excessive use of lists in System Requirements

`MAJOR` · `formatting`

The System Requirements and Top Level Design section relies heavily on numbered lists and sub‑lists instead of prose, contrary to guidelines.

- Page 3, System Requirements and Top Level Design: “**1) Name: Exercise work flow**

**2) Participating actors: Bob: Instructor, Mary: Student**

**3) Flow of events:**

  1. Bob prepares the exercise via the web interface of Artemis and pushes the exercise code template, the test cases as well as the sample solution to the VC server [Krusche18].
  2. Artemis configures the build plan accordingly to the configuration made by Bob and makes it available via the API.
  3. Mary clicks the start exercise button after the release date [Krusche18].”

### 13. Headings not in title case

`MAJOR` · `formatting`

Several headings are written in sentence case instead of the required title case.

- Page 4, System Requirements and Top Level Design: “**1.1: Push code template, test cases and sample solution**”
- Page 4, System Requirements and Top Level Design: “**1.2: Assign exercise**”
- Page 4, System Requirements and Top Level Design: “**2.1: Participate in exercise**”
- Page 4, System Requirements and Top Level Design: “**2.2: Setup student repository**”
- Page 4, System Requirements and Top Level Design: “**3.1: Push code**”
- Page 4, System Requirements and Top Level Design: “**3.3: Send build results**”

### 14. Inconsistent terminology for CI component

`MINOR` · `writing`

The proposal uses different terms (“CI application”, “CI service”, “CI server”) to refer to the same component.

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) **application**, it is possible to reduce the effort...”
- Page 1, Abstract: “the inbuilt CI **service**.”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a **CI server**.”

### 15. Schedule does not meet agile iteration guidelines and includes prohibited tasks

`CRITICAL` · `methodology`

The schedule lists weekly tasks resulting in 1‑week iterations, includes a requirements‑gathering sprint, contains thesis writing and documentation tasks, follows a waterfall‑style timeline, and separates work by horizontal layers rather than end‑to‑end features.

- Page 5, Schedule: “– **5th week**: Proposal”
- Page 5, Schedule: “– **2nd week**: Docker GitLab CI Runner confguration”
- Page 5, Schedule: “– 4th week: **Requirements** (parallel to prototype implementation)”
- Page 5, Schedule: “– 2nd week: **Documentation** for installation and confguration of Artemis with GitLab CI”
- Page 5, Schedule: “October: **Writing . . .**”
- Page 5, Schedule: “– 2nd week: **Thesis ready for review**”
- Page 5, Schedule: “– 3rd week: **Review fnished**”
- Page 5, Schedule: “– 4th week: **Implement improvements**”
- Page 5, Schedule: “– 1st week: **Thesis**”
- Page 5, Schedule: “It is planned to adhere to the following **time schedule**: …”

### 16. Missing AI Transparency Statement

`CRITICAL` · `completeness`

The proposal does not contain an AI Transparency Statement as required by the guidelines.
