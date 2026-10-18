# proposal-p1 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 35
- **Findings:** 17
- **Cost:** 10 calls, 0 tool calls, depth 2, 72578 tokens, 59s
- **Coverage:** 9/9 categories

## Summary

The proposal contains several critical structural problems, such as missing mandatory sections and an absent AI transparency statement, as well as major compliance issues in the schedule and figures. While the research motivation is clear, the document needs extensive revisions to meet formatting, writing, and completeness requirements before it can be submitted.

## Findings

### 1. Missing Introduction Section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section, which is required by the guidelines.

- Page 0

### 2. Missing Motivation Section

`CRITICAL` · `structure`

The proposal does not contain a dedicated Motivation section; only a combined “Motivation/Objective” line appears in the abstract, violating the requirement for a separate visionary Motivation section.

- Page 0

### 3. Missing Objective Section

`CRITICAL` · `structure`

There is no separate Objectives section (Section 4) listing objectives in action form, so the requirements for action‑form headings, single verbs, and detailed subsections cannot be satisfied.

- Page 0

### 4. Problem Section Too Long

`MAJOR` · `structure`

The Problem section exceeds the maximum allowed length of 3/4 page.

- Page 2, Problem: “**The interactive learning platform Artemis is an approach to use interactive learning during large computer science classes [Krusche17, Krusche18].** Main goal is to give the students immediately individual and automatic generated feedback.”

### 5. Problem section includes solution content

`CRITICAL` · `structure`

The Problem section presents solution statements such as “CI server should be migrated to the VC server” and includes a “Proposed Solution” subsection, which violates the rule that the Problem section must not contain solutions.

- Page 1, Problem: “Problem Statement: The interactive learning platform Artemis currently only supports setups with an external CI server. **The CI server should be migrated to the VC server.**”

### 6. Missing AI Transparency Statement

`CRITICAL` · `structure`

The proposal does not contain an AI transparency statement as required by the research group guidelines.

- Page 0

### 7. Bibliography contains fewer than required publications

`CRITICAL` · `completeness`

The bibliography lists only 5 peer‑reviewed publications, but the guidelines require at least 6‑8 peer‑reviewed publications.

- Page 5, References: “References ... [Booch95] ... [Gamma95] ... [Hilton16] ... [Krusche17] ... [Krusche18]”

### 8. Figure issues: insufficient number, missing UML diagram, inadequate caption, and not referenced in text

`CRITICAL` · `figures`

The proposal includes only one figure, which is not a UML diagram, has a short generic caption, and is never referenced in the surrounding text, violating multiple figure requirements.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 9. Passive voice usage

`MAJOR` · `writing`

Several sentences are written in passive voice, which reduces clarity and should be rewritten in active voice.

- Page 1, Abstract: “The CI server **should be migrated** to the VC server.”
- Page 2, Problem Statement: “The base build plan **is copied** once after the student started the exercise.”
- Page 2, Problem Statement: “Later changes to the base build plan **are not patched** automatically to all student build plans.”
- Page 2, Proposed Solution: “the build plan **is fetched** from the API on each run.”
- Page 2, Proposed Solution: “The build plan **can be made available** via the API of Artemis.”

### 10. Excessive filler words and intensifiers

`MINOR` · `writing`

The document contains filler words and intensifiers such as 'Additionally', 'Furthermore', and 'very' that should be avoided for concise academic writing.

- Page 2, Problem Statement: “**Additionally**, the base build plan is copied once after the student started the exercise.”
- Page 2, Proposed Solution: “**Furthermore**, the CI service should be part of the VC server to reduce additional effort.”
- Page 3, Discussion: “makes it **very** cumbersome to update the build plan afterwards.”

### 11. Forbidden sentence starters

`MAJOR` · `writing`

Sentences start with disallowed words such as 'Since' and 'To', which violate the writing style guidelines.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application...”
- Page 2, Problem Statement: “**To** achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server...”
- Page 3, Discussion: “**Since** the benefits of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity...”

### 12. Inconsistent terminology for CI component

`MAJOR` · `writing`

The proposal uses both “CI server” and “CI service” to refer to the same concept, which violates the consistency requirement.

- Page 2, Problem Statement: “as a CI **server**”
- Page 2, Proposed Solution: “the CI **service** should be part of the VC server”

### 13. Undefined abbreviation 'API'

`MINOR` · `writing`

The abbreviation 'API' is introduced without a prior definition, which may confuse readers unfamiliar with the term.

- Page 2, Proposed Solution: “The build plan can be made available via the **API** of Artemis.”
- Page 1, Abstract: “supports the provision of the build plan via the Artemis **API**, which makes it possible to update the build plan retrospectively”

### 14. Excessive use of bullet points in System Requirements section

`MAJOR` · `formatting`

Replace the extensive list of numbered items with prose paragraphs. Lists should be limited to 1‑2 lines each and used only for schedule or contribution objectives.

- Page 4, System Requirements and Top Level Design: “**1)  Name:      Exercise work fow**

**2)  Participating actors: Bob: Instructor, Mary: Student**

**3)  Flow of events:**

1.  Bob   prepares  the  exercise ...
2.   Artemis confgures the build plan ...
3.  Mary clicks the start exercise button ...”

### 15. Schedule violations: iteration length too short and inclusion of prohibited thesis writing tasks

`CRITICAL` · `other`

All listed iterations span only a single week, which is shorter than the required 2–4 week minimum, and the schedule contains dedicated weeks for writing, reviewing and finalizing the thesis, which are prohibited.

- Page 5: “– **2nd week**: Docker GitLab CI Runner configuration”
- Page 5: “October: **Writing . . .**”

### 16. Schedule violations: requirements‑only iteration, documentation tasks, and waterfall style organization

`MAJOR` · `other`

The schedule includes a requirements‑only sprint, documentation work, and follows a linear month‑by‑month plan, all of which violate the agile sprint guidelines.

- Page 5: “– 4th week: **Requirements** (parallel to prototype implementation)”
- Page 5: “– 2nd week: **Documentation** for installation and configuration of Artemis with GitLab CI”
- Page 4: “It is planned to adhere to the following time schedule:”

### 17. Horizontal work item (Models) not vertically integrated

`MINOR` · `other`

June lists a 'Models' work item that does not represent a vertically integrated end‑to‑end feature.

- Page 5: “– 4th week: **Models** (parallel to prototype implementation)”
