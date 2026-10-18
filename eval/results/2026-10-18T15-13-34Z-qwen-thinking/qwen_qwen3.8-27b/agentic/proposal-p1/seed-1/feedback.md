# proposal-p1 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 22
- **Cost:** 12 calls, 41 tool calls, depth 12, 246413 tokens, 335s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a coherent, well-motivated engineering problem (migrating Artemis's CI from Jenkins/Bamboo to GitLab CI) with a plausible solution and a concrete scenario, but it does not follow the required proposal structure. Six critical gaps: no Introduction, no Motivation section, no Objectives section in the required bullet-plus-subsections form (Section 4 is requirements/design instead), no AI Transparency Statement, only 5 references (minimum 6-8), and only one figure, which is a disallowed sequence diagram with no suitable UML diagram. Major schedule issues include thesis-writing/documentation tasks, a requirements task, and a ~12-week gap between milestones. The text also has several fixable style and grammar problems (forbidden sentence starters, passive voice, duplicated word 'take', 'is build', '3nd week', inconsistent CI terminology, undefined 'API', and a figure never referenced by number). Restructuring the sections per the guidelines and fixing the figures and schedule would bring the proposal close to acceptable.

## Findings

### 1. Required Introduction section is missing

`CRITICAL` · `STRUCTURE`

The proposal has no Introduction section. It jumps from the Abstract directly into '1 Problem Statement'. Add an Introduction section (less than one page) that motivates the context of Artemis/CI before the problem statement.

- Page 1, Introduction

### 2. Required Motivation section is missing

`CRITICAL` · `STRUCTURE`

There is no standalone Motivation section. The visionary/positive-aspect content is only scattered in the Abstract ('(Expected) Results') and '5 Expected Findings'. Add a dedicated Motivation section (at most 3/4 page) that is visionary and does not repeat the Problem.

- Page 1, Motivation

### 3. No Objectives section with action-form objectives and subsections

`CRITICAL` · `STRUCTURE`

Section 4 is 'System Requirements and Top Level Design', not an Objectives section. It lacks the required structure: a short introduction, a bullet list of 3-4 high-level objectives, and one subsection per objective (each at least two paragraphs, objectives in action form). Restructure Section 4 accordingly (e.g., objectives such as 'Implement the GitLab CI backend for Artemis', 'Extend the Artemis API to serve build plans').

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”

### 4. AI Transparency Statement is missing

`CRITICAL` · `COMPLETENESS`

The proposal contains no AI Transparency Statement at all (the term does not appear anywhere in the document). Add a first-person statement that names the specific AI tools used, their purposes, and the sections they were used for, and include a sentence confirming that you have carefully checked all texts created with these tools to ensure that they are correct and make sense.

- Page 1, AI Transparency Statement

### 5. Only 5 publications in the bibliography (minimum 6-8 required)

`CRITICAL` · `CITATION`

The bibliography contains only 5 publications (Booch95, Gamma95, Hilton16, Krusche17, Krusche18), which is below the required minimum of 6-8 peer-reviewed publications. Add at least 1-3 more peer-reviewed sources (e.g., papers on CI/CD, GitLab CI, or automated assessment systems).

- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning. ACM. 2018, 49th Technical Symposium on Computer Science Education, pp. 284–289.”

### 6. Only one figure (at least two required)

`CRITICAL` · `FIGURES`

The proposal contains only one figure (Figure 1 on page 4). The guidelines require at least two diagrams/mockups. Add a second figure, e.g., a component or deployment diagram showing the Artemis server, Artemis API, GitLab VC/CI, and CI runners.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 7. Only figure is a sequence diagram; no suitable UML diagram

`MAJOR` · `FIGURES`

The sole figure is a sequence diagram (Instructor/Student lifelines with numbered messages 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3). Sequence diagrams are explicitly disallowed, and at least one suitable UML diagram (e.g., activity, class, or component diagram) is required. Replace the sequence diagram with a communication diagram or activity diagram, and ensure at least one suitable UML diagram is present.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 8. Figure 1 is not referenced in the text by figure number

`MINOR` · `FIGURES`

Figure 1 is never referenced in the running text by its figure number; the text only points to a section. Reference it explicitly in the text, e.g., 'Figure 1 shows the top-level workflow between the instructor, the student, Artemis, and GitLab CI.'

- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 9. Sentences start with forbidden words (Since/To/As/In order to)

`MINOR` · `WRITING`

Several sentences begin with forbidden words ('Since', 'To', 'As', 'In order to'). Occurrences: 'Since some Version Control (VC) systems...' (Abstract, p.1); 'To achieve this goal, Artemis uses...' and 'In order to implement this idea, the lecture is split up...' (p.2); 'As a result, instructors are able to update the build plan...' (p.2); 'Since the benefts of using CI are quite high...' (p.3); 'As described in section 3, this might lead to better developers...' (p.4). Rephrase each so the sentence starts with a subject.

- Page 1, Abstract: “**Since** some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 10. Excessive filler words (Additionally / Furthermore / In addition)

`MINOR` · `WRITING`

Filler words are used excessively: 'Additionally' appears 3 times (p.2, p.4 twice as 'In addition'), 'Furthermore' once (p.2), 'also' (p.3). Vary sentence structure or remove the connectives.

- Page 2, Problem Statement: “**Additionally**, the base build plan is copied once after the student started the exercise [Krusche18].”

### 11. Excessive passive voice

`MINOR` · `WRITING`

Passive voice is used heavily where an actor exists, e.g., 'the base build plan is copied', 'the build plans must be updated manually', 'the build plan is fetched from the API on each run', 'can be made available via the API'. Rewrite with active subjects (the instructor updates the base build plan, GitLab CI fetches the build plan from the Artemis API on each run).

- Page 2, Problem Statement: “Later changes to the base build plan are **not patched automatically** to all student build plans, but rather the build plans **must be updated manually** in the CI Server.”

### 12. First-person 'I' used in the schedule assumptions

`MINOR` · `WRITING`

First-person 'I' is used in the Time Schedule assumptions. The guidelines ask to avoid 'I'; rephrase in a neutral voice, e.g., 'During this time, the thesis work is paused; instead, about 2 weeks of thesis work are planned during the summer holidays in August.'

- Page 4, Time Schedule: “During this time, **I will not have time** to work on my thesis. Instead, **I plan** to work on my thesis for about 2 weeks during the summer holidays in August.”

### 13. Full stop followed by lowercase continuation ('research. with')

`MINOR` · `WRITING`

Typo: a sentence ends with a full stop but the continuation starts with a lowercase letter ('further research. with'). Merge into one sentence or capitalize: '...further research: with the envisaged implementation...'.

- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. **with** the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams described in section 1.”

### 14. Grammatical error: 'is build and tested'

`MINOR` · `WRITING`

Grammatical error: 'is build and tested' should be 'is built and tested'.

- Page 2, Related Work: “Martin Fowler explains in his post Continuous Integration, Continuous Integration as a practice, in which each integration, which should take place at least once per day per developer, **is build** and tested with predefned test cases 6.”

### 15. Duplicated word: 'should take generally take place'

`MINOR` · `WRITING`

Duplicated word: 'should take generally take place regularly' contains 'take' twice. Should be 'integration should generally take place regularly'.

- Page 2, Related Work: “According to Grady Booch’s book Object-oriented analysis and design with applications integration **should take generally take place** regularly, to avoid problems later on [Booch95].”

### 16. Typo: '3nd week' should be '3rd week'

`MINOR` · `WRITING`

Typo in the schedule: '3nd week' should be '3rd week' (May).

- Page 5, Time Schedule: “– **3nd** week: prototype implementation: Artemis API for confguration”

### 17. Abbreviation 'API' never defined on first use

`MINOR` · `WRITING`

The abbreviation 'API' is used throughout but never defined on first use (unlike VC and CI, which are defined in the Abstract). Define it at first occurrence, e.g., 'application programming interface (API)'.

- Page 2, Proposed Solution: “The build plan can be made available via the **API** of Artemis and GitLab CI is downloading the build plan for each execution of the test cases.”

### 18. Schedule contains thesis-writing and documentation tasks

`MAJOR` · `SCHEDULE`

The schedule includes thesis-writing and documentation tasks, which should not be part of the schedule: 'October: Writing . . .', 'September – 2nd week: Documentation for installation and confguration of Artemis with GitLab CI', 'November – 2nd week: Thesis ready for review', 'December – 1st week: Thesis'. Replace with concrete development/research milestones.

- Page 5, Time Schedule: “**October: Writing . . .**”

### 19. Dedicated requirements task in the schedule

`MAJOR` · `SCHEDULE`

The schedule contains a dedicated requirements task ('June – 4th week: Requirements (parallel to prototype implementation)'). In agile planning, requirements are pulled from the backlog within feature iterations rather than collected in their own slot; fold requirement refinement into the vertical feature iterations.

- Page 5, Time Schedule: “– 4th week: **Requirements (parallel to prototype implementation)**”

### 20. Irregular iteration length: ~12-week gap between June and September milestones

`MAJOR` · `SCHEDULE`

Iteration length is irregular: the last milestone is 'June – 4th week' and the next is 'September – 2nd week', leaving about 12 weeks with no deliverable (the assumptions mention ~2 weeks of thesis work in August, but the schedule line says only 'August: Summer holidays'). Restructure into consistent 2-4 week iterations with a deliverable in each, and make the schedule items explicitly reference the high-level objectives.

- Page 4, Time Schedule: “**During July there will be an internship** as part of the TUMKolleg programm.”

### 21. Inconsistent terminology: CI server / CI service / CI application / CI system

`MINOR` · `FORMATTING`

Inconsistent terminology for the same concept: the document uses 'CI server' (Abstract, p.1-2), 'CI application' and 'CI service' (Abstract, p.1), 'CI system' ('GitLab CI is the CI system provided...', p.1; 'a CI system not storing each build plan individually', p.2) and 'CI service' (p.2, p.3) interchangeably. Pick one term (e.g., 'CI server') and use it consistently throughout.

- Page 1, Abstract: “The interactive learning platform Artemis currently only supports setups with an external **CI server**. The **CI server** should be migrated to the VC server.”

### 22. Too-short one-line paragraph before the scenario list

`MINOR` · `WRITING`

This one-line paragraph before the scenario list is too short (target is 5-8 lines). Expand it to briefly introduce what the visionary scenario covers (the actors, where it starts and ends) before the enumerated flow of events.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”
