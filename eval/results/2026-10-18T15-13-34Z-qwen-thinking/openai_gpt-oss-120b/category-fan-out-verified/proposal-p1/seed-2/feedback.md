# proposal-p1 — category-fan-out-verified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 45
- **Findings:** 14
- **Cost:** 18 calls, 0 tool calls, depth 3, 84693 tokens, 63s
- **Coverage:** 9/9 categories

## Summary

The proposal lacks several mandatory structural sections and key compliance elements such as the AI Transparency Statement and required figures, leading to critical deficiencies. While the topic is relevant, numerous formatting, citation, and scheduling issues further reduce its readiness for submission.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal does not contain an Introduction section, which is required by the guidelines.

### 2. Missing Motivation section

`CRITICAL` · `structure`

The proposal does not contain a Motivation section, which is required by the guidelines.

### 3. Missing Objective section with subsections

`CRITICAL` · `structure`

The proposal lacks an Objective section (Section 4) with the required bullet list of objectives and subsections for each objective.

### 4. Insufficient number of figures

`CRITICAL` · `figures`

The proposal includes only one figure (Figure 1) whereas the guidelines require at least two diagrams, mockups, or figures.

- Page 4: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 5. Missing required UML diagram

`CRITICAL` · `figures`

The guidelines mandate at least one suitable UML diagram (class, activity, component, etc.). No UML diagram is present in the submission.

- Page 4: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 6. Missing AI Transparency Statement

`CRITICAL` · `other`

The proposal does not contain the required AI Transparency Statement, which must be included as a separate section detailing the AI tools used, purposes, sections affected, and a confirmation of manual review.

### 7. Undefined abbreviation "API"

`MAJOR` · `writing`

The abbreviation "API" is used without being defined on first occurrence.

- Page 2, Proposed Solution: “the build plan can be made available via the **API** of Artemis”

### 8. Figure not referenced in the text

`MAJOR` · `figures`

Figure 1 is included but there is no textual reference such as "Figure 1 shows..." in the surrounding sections.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 9. Abstract paragraphs too short

`MAJOR` · `formatting`

Paragraphs in the Abstract consist of only one or two lines, while the guidelines require each paragraph to be 5‑8 lines long.

- Page 1, Abstract: “**Motivation/Objective: Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.**”
- Page 1, Abstract: “**Problem Statement: The interactive learning platform Artemis currently only supports setups with an external CI server.**”
- Page 1, Abstract: “**Proposed Solution: GitLab CI is the CI system provided by the GitLab VC server.**”
- Page 1, Abstract: “**(Expected) Results: We expect to use the bridge pattern to dynamically bind Artemis to the concrete implementation with GitLab CI.**”

### 10. Overuse of bullet/numbered lists instead of prose

`MAJOR` · `formatting`

Section 4 contains a multi‑line enumerated list of workflow steps, which should be expressed in prose rather than a list of bullet points.

- Page 3, System Requirements and Top Level Design: “**1)  Name:      Exercise work fow**”
- Page 4, System Requirements and Top Level Design: “**1.1:  Push code  template, test  cases**”

### 11. Headings not in title case

`MAJOR` · `formatting`

Several subsection headings use sentence case instead of title case, violating formatting guidelines.

- Page 4: “1.1: **Push code template, test cases and sample solution**”
- Page 4: “1.2: **Assign exercise**”
- Page 4: “2.1: **Participate in exercise**”
- Page 4: “2.2: **Setup student repository**”
- Page 4: “3.1: **Push code**”
- Page 4: “3.3: **Send build results**”

### 12. Thesis writing tasks appear in the schedule

`MAJOR` · `other`

The schedule includes tasks such as 'Writing ...', 'Thesis ready for review', 'Review finished', and 'Thesis' in October‑December, which violate the rule that the schedule must not contain thesis writing or documentation tasks.

- Page 5, Schedule: “October: **Writing . . .**”
- Page 5, Schedule: “November – **2nd week: Thesis ready for review**”
- Page 5, Schedule: “December – **1st week: Thesis**”

### 13. Paragraph combines multiple unrelated ideas

`MINOR` · `logic`

In the Problem Statement paragraph, the text discusses both the need to migrate CI to the VC server and the requirement to adjust the build plan after the task has been assigned, mixing two separate ideas in one paragraph.

- Page 2, Problem Statement: “The interactive learning platform Artemis currently only supports setups with an external CI server. **The CI server should be migrated to the VC server.** Furthermore, **it should be possible to adjust the build plan after the task has been assigned**.”

### 14. Horizontal split of work items rather than vertically integrated features

`MINOR` · `logic`

The schedule breaks development into separate low‑level activities (e.g., Docker runner configuration, API configuration, sending build results) across different weeks instead of delivering end‑to‑end functionality in each iteration.

- Page 5, Schedule: “April – **2nd week: Docker GitLab CI Runner confguration**”
- Page 5, Schedule: “May – **3nd week: prototype implementation: Artemis API for confguration**”
- Page 5, Schedule: “June – **4th week: prototype implementation: Sending build results to Artemis**”
