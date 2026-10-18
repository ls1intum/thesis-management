# proposal-p1 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 35
- **Findings:** 22
- **Cost:** 10 calls, 41 tool calls, depth 10, 223448 tokens, 428s
- **Coverage:** 9/9 categories

## Summary

The proposal describes a well-scoped, technically plausible project (migrating Artemis from external CI servers to GitLab CI with per-run build plan fetching), but it deviates fundamentally from the group's required format. Four required elements are missing entirely: an Introduction, a standalone Motivation section, an Objectives section with an action-form objective list and per-objective subsections, and an AI Transparency Statement. The bibliography has only 5 entries (6-8 required), and there is a single figure — which is a sequence diagram, an explicitly disallowed type — that is also never referenced by number in the text. The schedule is organized by single-week items rather than 2-4 week iterations, includes documentation/writing tasks, and splits the prototype into horizontal layers. Secondary issues include excessive passive voice, repeated forbidden sentence starters ("Since", "As", "To", "In order to"), filler words, the undefined abbreviation "API", inconsistent terminology ("task"/"exercise"), and minor typos. Restructuring the document into the mandated section layout and rebuilding the schedule are the top priorities before this can be re-reviewed.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `STRUCTURE`

The proposal must contain an Introduction section. The document jumps directly from the Abstract to "1 Problem Statement" and the word "Introduction" does not occur anywhere. Add a dedicated Introduction (less than one page) that presents the topic, context and scope before the Problem section.

- null

### 2. Missing Motivation section

`CRITICAL` · `STRUCTURE`

There is no dedicated Motivation section. The visionary content (e.g. "more universities will use Artemis, since the setup hurdle will become smaller") is buried in "5 Expected Findings" after the solution and design. The group format requires a standalone Motivation section (max 3/4 page) that is visionary and does not repeat the Problem; move that content there, before the solution sections.

- null

### 3. Missing Objective section with bullet list and per-objective subsections

`CRITICAL` · `STRUCTURE`

No Objectives section exists. "4 System Requirements and Top Level Design" contains a use case (visionary scenario) and a design figure, but the required structure — a short introduction plus a bullet list of 3-4 high-level objectives in action form, followed by one subsection per objective (at least two paragraphs each) — is absent. Restructure into an Objectives section with an action-form objective list (no double verbs) and one subsection per objective.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”

### 4. Missing AI Transparency Statement

`CRITICAL` · `COMPLETENESS`

The proposal contains no AI transparency statement (the terms "transparency" or "AI" do not occur). Add a first-person statement naming the specific AI tools used, their purposes (e.g. grammar checking, code assistance), the sections they were applied to, and including a confirmation sentence that the author carefully checked all AI-generated text for correctness.

- null

### 5. Bibliography has only 5 references (minimum 6-8 required)

`CRITICAL` · `CITATION`

The bibliography contains only 5 entries (Booch95, Gamma95, Hilton16, Krusche17, Krusche18); the group requires at least 6-8 peer-reviewed publications. Cite at least one or two more peer-reviewed publications (e.g. on GitLab CI / CI-in-VCS integration or software engineering education). Internet sources are already correctly kept as footnotes, so only add real bibliographic entries.

- Page 6, References: “Krusche,   S.;   Seitz, A.   ArTEMiS:     An  Automatic    Assessment    Management    System  for  Interactive  Learning.”

### 6. Only one figure included (minimum two required)

`CRITICAL` · `FIGURES`

Only one figure appears in the entire proposal (Figure 1 on page 4). The group requires at least two diagrams/mockups. Add a second figure, e.g. a component or activity diagram of the Artemis–GitLab CI integration (build plan fetch per run).

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Sole figure is a sequence diagram (not allowed)

`MAJOR` · `FIGURES`

Figure 1 is drawn as a sequence diagram (two actor lifelines, "Instructor" and "Student", with numbered message arrows 1.1–3.3 between them). Sequence diagrams are explicitly not allowed; redraw the top-level workflow as an activity diagram (or communication diagram). Note that the diagram also shows no interaction with Artemis or GitLab CI, despite the caption claiming otherwise.

- Page 4, System Requirements and Top Level Design: “2.1:  Participate  in  exercise”

### 8. Figure 1 not referenced in the text

`MINOR` · `FIGURES`

Figure 1 is never referenced by number in the body text; the closest reference is an indirect pointer to a section. Replace it with an explicit in-text reference such as "Figure 1 shows the top-level workflow ...".

- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 9. Excessive passive voice

`MINOR` · `WRITING`

Passive voice is used excessively throughout the proposal (e.g. also "the build plan is fetched", "The student base repository is copied", "The GitLab CI build gets triggered", "the lecture is split up"). Rewrite with the responsible actor as subject, e.g. "Artemis copies the base build plan once after the student starts the exercise".

- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server.”

### 10. Excessive filler words and superlatives

`MINOR` · `WRITING`

Filler words and superlatives appear excessively: "especially" (abstract, Problem Statement, Related Work), "Additionally" (Problem Statement), "Furthermore" (Abstract, Proposed Solution), "additional" ("additional server overhead", "reduce additional efort", "reduce additional work"), "also" ("the build is also less likely to fail"). Remove or rephrase these; they add no content.

- Page 2, Problem Statement: “Especially in large software engineering courses this is nearly impossible.”

### 11. Sentence starts with "Since" (also in Abstract)

`MINOR` · `WRITING`

A sentence starts with the forbidden word "Since". This also happens in the Abstract: "Motivation/Objective: Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort...". Restructure both sentences so they do not begin with "Since".

- Page 3, Related Work: “Since the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university [Hilton16].”

### 12. Sentence starts with "As" (also in Expected Findings)

`MINOR` · `WRITING`

A sentence starts with the forbidden word "As". This also happens in Expected Findings: "As described in section 3, this might lead to better developers and better code". Restructure both sentences so they do not begin with "As".

- Page 2, Proposed Solution: “As a result, instructors are able to update the build plan after the release of an exercise, GitLab CI fetches the updated build plan when needed.”

### 13. Sentences start with "To" / "In order to"

`MINOR` · `WRITING`

Sentences start with the forbidden words "To" and "In order to": here "To achieve this goal, Artemis uses a Version Control (VC)..." and in Related Work "In Order to implement this idea, the lecture is split up into multiple iterations, each consisting of diferent phases [Krusche17]." Rewrite both to begin with the subject of the sentence.

- Page 2, Problem Statement: “To achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases predefned by the instructor on each submission”

### 14. Use of first person "I"

`MINOR` · `WRITING`

First person singular "I" is used ("During this time, I will not have time to work on my thesis. Instead, I plan to work on my thesis for about 2 weeks..."). The group style avoids "I"; rewrite in third person or passive-neutral phrasing (e.g. "the internship leaves no time for thesis work; two weeks in August are planned instead").

- Page 4, Time Schedule: “During this time, I will not have time to work on my thesis.”

### 15. Typo "3nd week"

`MINOR` · `WRITING`

Typo in the schedule: "3nd week" should be "3rd week".

- Page 5, Time Schedule: “3nd week: prototype implementation: Artemis API for confguration”

### 16. Abbreviation "API" used without definition

`MINOR` · `WRITING`

The abbreviation "API" is used (already in the Abstract) without ever being defined as "Application Programming Interface". Define it at first use; "VC" and "CI" are defined correctly, "API" is not.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”

### 17. Inconsistent terminology: "task" vs. "exercise", "CI server" vs. "CI service"

`MINOR` · `FORMATTING`

Inconsistent terminology: the Abstract speaks of "the task has been assigned" while the rest of the document consistently uses "exercise" (e.g. "after the student started the exercise", "release of an exercise"). Also "CI server" and "CI service" are used interchangeably for the same concept (Abstract, Problem Statement, Proposed Solution). Pick one term per concept and use it throughout.

- Page 1, Abstract: “Furthermore, it should be possible to adjust the build plan after the task has been assigned.”

### 18. 1-2 line paragraphs in Expected Findings

`MINOR` · `WRITING`

Two 1-2 line paragraphs at the end of Expected Findings ("In addition, universities will save time and money as they will need at least one less server." and "In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.") are far below the 5-8 line target. Merge them with the preceding paragraph(s) so each paragraph carries one developed idea.

- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”

### 19. Schedule built on single weeks, not 2-4 week iterations

`MAJOR` · `METHODOLOGY`

The schedule is organized by single-week deliverables under month headings ("5th week: Proposal", "2nd week: Docker GitLab CI Runner confguration", "3nd week: ...") rather than 2-4 week iterations, and does not reference the high-level objectives. Restructure into 2-4 week iterations, each ending in a measurable, demoable deliverable tied to an objective.

- Page 5, Time Schedule: “–  2nd week: Docker GitLab CI Runner confguration”

### 20. Schedule includes documentation/writing tasks

`MAJOR` · `METHODOLOGY`

The schedule contains documentation and writing tasks, which must be excluded: "2nd week: Documentation for installation and confguration of Artemis with GitLab CI" (September), "October: Writing . . ." and "1st week: Thesis" (December). Keep the schedule to development/research work with deliverables only.

- Page 5, Time Schedule: “–  2nd week: Documentation for installation and confguration of Artemis with GitLab CI”

### 21. Vague, non-measurable schedule items

`MINOR` · `METHODOLOGY`

Several items are not measurable deliverables: "4th week: Requirements (parallel to prototype implementation)" (June), "4th week: Models (parallel to prototype implementation)" (June), "4th week: Implement improvements" (November) and "1st week: Thesis" (December) all name activities rather than a checkable outcome. State what will exist and be demonstrable at the end of each item.

- Page 5, Time Schedule: “–  4th week: Requirements (parallel to prototype implementation)”

### 22. Prototype split into horizontal layers instead of vertical features

`MINOR` · `METHODOLOGY`

The prototype work is split into horizontal layers across months: "Connectivity to GitLab CI" (April), then "Artemis API for confguration" (May), then "Sending build results to Artemis" (June). Each iteration should instead be a vertically integrated, end-to-end feature (e.g. "a student submission runs through GitLab CI with a build plan fetched from Artemis and results shown in Artemis"), so every iteration delivers working functionality.

- Page 5, Time Schedule: “–  4th week: prototype implementation: Connectivity to GitLab CI”
