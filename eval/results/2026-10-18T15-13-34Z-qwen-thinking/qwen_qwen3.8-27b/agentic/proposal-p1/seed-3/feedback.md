# proposal-p1 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 19
- **Cost:** 9 calls, 37 tool calls, depth 9, 173589 tokens, 334s
- **Coverage:** 9/9 categories

## Summary

The topic is well scoped and the problem/related-work writing is mostly sound, but the proposal deviates significantly from the required structure. Six critical gaps: no Introduction, no Motivation section, no Objective section with the required bullet list of 3-4 action-form objectives and two-paragraph subsections, no AI Transparency Statement, only 5 bibliography entries (minimum 6-8), and only one figure (minimum two). The single figure is additionally a forbidden sequence diagram. Other major issues: pervasive passive voice, four sentences starting with forbidden openers (To/Since/In order to), and a schedule built from one-off single-week calendar items rather than 2-4 week agile iterations, which also contains a requirements item and thesis-writing/documentation tasks. Minor issues include undefined "API", filler words, inconsistent "CI server"/"CI service" terminology, a list-style abstract, and several typos. The author should restructure the document around the required section list, rewrite objectives in action form, replace the sequence diagram with a UML activity/component diagram plus one additional figure, and rebuild the schedule as iterations with measurable deliverables.

## Findings

### 1. Missing required Introduction section

`CRITICAL` · `STRUCTURE`

The proposal has no Introduction section. The required structure is Abstract, Introduction, Problem, Motivation, Objective, Schedule, Bibliography. After the Abstract the document jumps straight to "1 Problem Statement". Add an Introduction section (less than one page) that orients the reader and previews the problem, solution, and objectives.

- Page 1, Introduction

### 2. Missing required Motivation section

`CRITICAL` · `STRUCTURE`

There is no dedicated Motivation section. The positive/visionary content (more universities adopting Artemis, smaller setup hurdle) is scattered across the Abstract's "(Expected) Results" and Section 5 "Expected Findings". Add a Motivation section (at most 3/4 page) that is visionary and explains why solving the problem is scientifically/practically important, without repeating the Problem section.

- Page 1, Motivation

### 3. Missing Objective section with bullet list and subsections

`CRITICAL` · `STRUCTURE`

There is no Objective section with the required structure: a short introduction, a bullet list of 3-4 high-level objectives (each fitting one line), followed by one subsection per objective of at least two paragraphs. Instead the proposal has "4 System Requirements and Top Level Design" and "5 Expected Findings", neither of which states action-form objectives. Restructure: add an Objective section, e.g. with objectives such as "Integrate GitLab CI as a new CI backend of Artemis", "Expose the build plan via the Artemis API", "Enable retrospective adjustment of student build plans", each elaborated in its own subsection.

- Page 4, Expected Findings: “This research thesis deals with the question on how to connect Artemis with GitLab CI.”

### 4. AI Transparency Statement is missing entirely

`CRITICAL` · `COMPLETENESS`

The proposal contains no AI Transparency Statement (no occurrence of "transparency" or any tool names anywhere in the document). Add a statement in first person that names the specific AI tools used, what they were used for (e.g., grammar checking, idea generation), which sections they were used in, and includes a confirmation that you have carefully checked all text created with these tools to ensure it is correct and makes sense.

- Page 6, AI Transparency Statement

### 5. Only 5 bibliography entries (minimum 6-8)

`CRITICAL` · `CITATION`

The bibliography contains only 5 entries ([Booch95], [Gamma95], [Hilton16], [Krusche17], [Krusche18]); the guidelines require at least 6-8 peer-reviewed publications. Add at least 1-3 more peer-reviewed sources, e.g., papers on continuous integration practice, on ArTEMiS evaluation, or on CI/CD in GitLab-based systems. (The web URLs are correctly kept as footnotes, and the citation style/placement is otherwise consistent.)

- Page 5, References: “[Krusche17]   Krusche,   S.;   Seitz,   A.;   Borstler,   J.;   Brugge,   B.”

### 6. Only one figure; at least two required

`CRITICAL` · `FIGURES`

The proposal contains only one figure (Figure 1 on page 4). The guidelines require at least two diagrams/mockups, with at least one a suitable UML diagram (class, activity, or component diagram; no sequence diagrams). Add at least one more figure, e.g., a UML component or activity diagram of the Artemis-GitLab CI integration.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Figure 1 is a forbidden sequence diagram

`MAJOR` · `FIGURES`

Figure 1 is drawn as a sequence/interaction diagram: numbered message exchanges (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3) between two actor lifelines (Instructor, Student). The guidelines explicitly forbid sequence diagrams. Replace it with a UML activity diagram or a communication/component diagram showing the same workflow.

- Page 4, System Requirements and Top Level Design: “2.1:  Participate  in  exercise”

### 8. Figure 1 not referenced by number in the text

`MINOR` · `FIGURES`

Figure 1 is never referenced by its number. Section 2 only says the process is "illustrated in section 4". Reference the figure explicitly in the text, e.g. "Figure 1 shows the top-level workflow of the interactions between Artemis and GitLab CI."

- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 9. Problem section does not identify actors affected by the manual update

`MINOR` · `LOGIC`

The Problem section describes the consequences of the manual build-plan update but never names who must do the manual work. "the build plans must be updated manually" leaves the actor implicit (instructors, and in exams: examiners). Identify the affected actors explicitly (instructors, students, universities) and state how the problem negatively influences each of them.

- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server.”

### 10. Excessive passive voice throughout the proposal

`MAJOR` · `WRITING`

Passive voice is pervasive (e.g. "is copied", "are not patched", "must be updated manually", "is build and tested", "is usually taught", "is now often included", "gets configured", "gets triggered"). Rewrite with explicit actors in active voice, e.g. "Artemis copies the base build plan once after the student starts the exercise" and "the instructor must update the build plans manually".

- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise [Krusche18].”

### 11. Sentences start with forbidden openers (To / Since / In order to)

`MAJOR` · `WRITING`

Four sentences start with forbidden openers ("To…", "Since…", "In order to…"): (1) p.1 Abstract: "Since some Version Control (VC) systems include…"; (2) p.2 Problem: "To achieve this goal, Artemis uses…"; (3) p.2 Related Work: "In order to implement this idea, the lecture is split up…"; (4) p.3 Related Work: "Since the benefts of using CI are quite high…". Reorder each sentence so it starts with a subject or other construction.

- Page 2, Problem Statement: “To achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server”

### 12. Abbreviation "API" used without being defined

`MINOR` · `WRITING`

The abbreviation "API" is used in the Abstract ("via the Artemis API") and throughout the proposal without ever being defined (Application Programming Interface), unlike "VC" and "CI" which are defined at first use. Define it at first occurrence.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API”

### 13. Excessive filler words (Furthermore, Additionally, In addition)

`MINOR` · `WRITING`

Filler/discourse words appear excessively: "Furthermore" (Abstract and Section 2), "Additionally" (Abstract, Problem), "In addition" (twice in Section 5), plus "especially/particularly" in close proximity. Remove or consolidate them; the argument does not need this many connectives.

- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”

### 14. Schedule lacks 2-4 week iterations; single-week calendar items instead

`MAJOR` · `METHODOLOGY`

The schedule is not organized into 2-4 week iterations with measurable, vertically integrated deliverables. It is a calendar of one-off single-week tasks scattered across months (e.g. "2nd week", "4th week" items in April/May/June), with large unassigned gaps in between and no iteration boundaries. Restructure into iterations of 2-4 weeks, each with a concrete end-to-end deliverable (e.g. "Iteration 1 (wks 1-2): end-to-end connectivity prototype that fetches the build plan from the Artemis API and runs one test case on a GitLab CI Runner").

- Page 4, Time Schedule: “It is planned to adhere to the following time schedule:”

### 15. Schedule includes thesis-writing and documentation tasks

`MAJOR` · `METHODOLOGY`

The schedule contains thesis-writing and documentation tasks, which should not appear: "October: Writing . . .", "– 2nd week: Documentation for installation and confguration of Artemis with GitLab CI" (September), and "– 1st week: Thesis" (December). Also "– 4th week: Implement improvements" (November) is not a measurable deliverable. Focus the schedule on development/research deliverables.

- Page 5, Time Schedule: “–  2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 16. Dedicated Requirements item in the schedule

`MAJOR` · `METHODOLOGY`

The June plan includes a dedicated "Requirements" work item, which contradicts agile practice (requirements are picked from a backlog each iteration, not gathered in a phase). Remove the requirements item; if requirements refinement is needed, make it part of a development iteration with a concrete deliverable.

- Page 5, Time Schedule: “–  4th week: Requirements (parallel to prototype implementation)”

### 17. Typos: "research. with", "3nd week", "an newly", "is build"

`MINOR` · `WRITING`

Visible typos/capitalization errors: "The proposed solution opens up new opportunities for further research. with the envisaged implementation" (lowercase "with" after the period, Section 5); "– 3nd week: prototype implementation: Artemis API for confguration" (Schedule, should be "3rd"); also "an newly developed learning method" and "is build and tested" in Section 3.

- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. with the envisaged implementation it will be possible to tackle the problem”

### 18. Inconsistent terminology: "CI service" vs "CI server" (and "VC server/Server")

`MINOR` · `FORMATTING`

The same concept is named inconsistently: "CI service" (Abstract: "migrate the CI service from external applications…"; Section 2: "the CI service should be part of the VC server"; "an integrated CI service called GitLab CI") vs "CI server" (Section 1: "Artemis currently supports Bamboo and Jenkins as a CI server"; "the build plans must be updated manually in the CI Server"). "VC server" and "VC Server" are also mixed. Pick one term (e.g. "CI server") and use it everywhere.

- Page 1, Abstract: “it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 19. Abstract written as labeled list items instead of prose

`MINOR` · `STRUCTURE`

The Abstract is written as a list of four labeled items ("Motivation/Objective:", "Problem Statement:", "Proposed Solution:", "(Expected) Results:") instead of a single flowing paragraph of prose. Write the abstract as continuous prose covering motivation, problem, approach, and expected results in 1/3-1/2 page.

- Page 1, Abstract: “Problem Statement: The interactive learning platform Artemis currently only supports setups with an external CI server.”
