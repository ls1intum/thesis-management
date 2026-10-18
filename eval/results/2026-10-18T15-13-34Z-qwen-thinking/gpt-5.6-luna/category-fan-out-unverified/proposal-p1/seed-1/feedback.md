# proposal-p1 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 35
- **Findings:** 18
- **Cost:** 10 calls, 0 tool calls, depth 2, 68007 tokens, 31s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant problem and outlines a plausible technical direction, but it is not ready for submission. Major required sections, bibliography and figure requirements, AI transparency, schedule structure, measurable deliverables, and several clarity and organization issues must be addressed first.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The proposal lacks a distinct Introduction section. Add an Introduction before the Problem Statement to establish the context, scope, and purpose of the proposal.

### 2. Missing Motivation section

`CRITICAL` · `structure`

The proposal lacks a standalone Motivation section. Add a section explaining the scientific and practical importance of solving the problem, why the proposed work is needed, and what positive outcome it should enable without merely repeating the Problem Statement.

### 3. Missing Objective section and required subsections

`CRITICAL` · `structure`

The required Objective section and objective subsections are missing. Replace or reorganize Section 4 so that it includes a short introduction, 3–4 one-line high-level objectives, and one action-form subsection per objective; each subsection should contain at least two paragraphs.

### 4. Bibliography has fewer than the required minimum number of publications

`CRITICAL` · `completeness`

The bibliography contains only five publications, below the required minimum of 6–8. Add at least one additional relevant peer-reviewed publication.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.).”
- Page 5, References: “[Gamma95] Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. Design Patterns: Elements of Reusable Object-Oriented Software.”
- Page 5, References: “[Hilton16] Hilton, M.; Tunnell, T.; Huang, K.; Marinov, D.; Dig, D. Usage, costs, and benefits of continuous integration in open-source projects.”
- Page 5, References: “[Krusche17] Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning: Increasing student participation through shorter exercise cycles.”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning.”

### 5. Fewer than two figures

`CRITICAL` · `completeness`

The proposal contains only one figure, but at least two substantive diagrams, mockups, or figures are required. Add a second figure, such as an architecture, activity, or deployment diagram, that contributes information beyond the prose.

### 6. AI transparency statement is missing

`CRITICAL` · `completeness`

The proposal lacks the required AI transparency statement. Add a dedicated first-person statement naming each AI tool used, explaining its purpose, identifying the sections in which it was used, and confirming that all AI-generated text was checked for correctness and coherence.

### 7. Problem Statement mixes current impacts with solution prescriptions and does not identify affected actors clearly

`MAJOR` · `logic`

The Problem Statement prescribes desired functionality and implementation behavior instead of describing only the current problem and its consequences. Rephrase the exam example as a limitation affecting exam administration, and explicitly identify the affected actors—including instructors and system administrators—and the concrete impact on each.

- Page 2, Problem Statement: “In exams, it would be useful to change the student build plans after the submission date to firstly hide the test cases during the exam and only compile the code and secondly use a debug compiler not failing due to a simple syntax error.”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server. Especially in large software engineering courses this is nearly impossible.”
- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis.”

### 8. Schedule is not organized into integrated 2–4 week iterations

`MAJOR` · `methodology`

Reorganize the schedule into explicit 2–4 week iterations with review points and vertically integrated, end-to-end increments. Each iteration should maintain requirements in its backlog while delivering and validating a complete user-facing capability, such as configuring one exercise, executing a student submission, and displaying its result in Artemis, rather than progressing only through separate components.

- Page 4, Time Schedule: “Every week, Wednesday is set aside for working on the thesis.”
- Page 5, Time Schedule: “March – 5th week: Proposal”
- Page 5, Time Schedule: “April – 2nd week: Docker GitLab CI Runner configuration”
- Page 5, Time Schedule: “April – 4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “May – 3nd week: prototype implementation: Artemis API for configuration”
- Page 5, Time Schedule: “June – 4th week: prototype implementation: Sending build results to Artemis”
- Page 5, Time Schedule: “June – 4th week: Requirements (parallel to prototype implementation)”

### 9. Schedule work items lack measurable deliverables

`MAJOR` · `completeness`

Several schedule entries lack measurable deliverables and acceptance criteria. Replace vague activities with concrete outputs such as a tested model change, a passing integration test, a benchmark, or a completed end-to-end scenario with defined success criteria.

- Page 5, Time Schedule: “June – 4th week: Models (parallel to prototype implementation)”
- Page 5, Time Schedule: “June – 4th week: Requirements (parallel to prototype implementation)”
- Page 5, Time Schedule: “September – 2nd week: Revise models with concrete implementation”
- Page 5, Time Schedule: “November – 4th week: Implement improvements”

### 10. Schedule contains non-development activities

`MAJOR` · `completeness`

The development schedule includes thesis writing, documentation, and review or presentation activities. Remove these entries from the development iterations or track them separately so the schedule clearly represents research, implementation, and validation work.

- Page 4, Time Schedule: “Every week, Wednesday is set aside for working on the thesis.”
- Page 5, Time Schedule: “September – 2nd week: Documentation for installation and configuration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing”
- Page 5, Time Schedule: “November – 2nd week: Thesis ready for review”
- Page 5, Time Schedule: “November – 3rd week: Review finished”
- Page 5, Time Schedule: “December – 1st week: Thesis”

### 11. Repeated passive voice and prohibited sentence starters weaken clarity

`MAJOR` · `writing`

Rewrite repeated passive constructions and forbidden sentence starters so that sentences begin with explicit subjects and active verbs. Identify who performs actions such as migrating services, copying repositories, updating build plans, fetching configurations, and triggering builds; recast sentences beginning with “Since,” “To achieve,” “In order to implement,” “As a result,” and “As described.”

- Page 1, Abstract: “The CI server should be migrated to the VC server.”
- Page 1, Abstract: “the build plan is fetched from the API on each run”
- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 2, Problem Statement: “the build plans must be updated manually in the CI Server.”
- Page 2, Problem Statement: “To achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases predefined by the instructor on each submission of each student, display the build results and grade the submission [Krusche18].”
- Page 2, Related Work: “each integration, which should take place at least once per day per developer, is build and tested with predefined test cases”
- Page 2, Related Work: “In order to implement this idea, the lecture is split up into multiple iterations, each consisting of different phases [Krusche17].”
- Page 2, Proposed Solution: “As a result, instructors are able to update the build plan after the release of an exercise, GitLab CI fetches the updated build plan when needed.”
- Page 3, Related Work: “With this fundamental idea, later the assessment management system Artemis was built [Krusche18].”
- Page 3, Related Work: “Since CI services are now often included with VC servers, it is now possible to migrate external CI servers as Jenkins to GitLab CI and so reduce additional work for server administration.”
- Page 3, Related Work: “Since the benefits of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university [Hilton16].”
- Page 3, System Requirements and Top Level Design: “The student base repository is copied [Krusche18].”
- Page 3, System Requirements and Top Level Design: “The GitLab CI build gets triggered [Krusche18].”
- Page 3, System Requirements and Top Level Design: “GitLab CI fetches the build plan from the API and executes the test cases according to the plan.”
- Page 4, Expected Findings: “As described in section 3, this might lead to better developers and better code, due to the fact, that more students will be taught the concepts of CI.”

### 12. Workflow section relies heavily on a numbered list

`MAJOR` · `structure`

The System Requirements and Top Level Design section presents the complete workflow as a long numbered list. Introduce the stages and their relationships in explanatory prose, retaining only a concise enumerated list for the individual steps.

- Page 3, System Requirements and Top Level Design: “Flow of events: 1. Bob prepares the exercise via the web interface of Artemis and pushes the exercise code template, the test cases as well as the sample solution to the VC server [Krusche18]. 2. Artemis configures the build plan accordingly to the configuration made by Bob and makes it available via the API.”

### 13. Figure 1 lacks a direct reference and sufficiently informative caption

`MAJOR` · `figures`

Reference the figure directly by number in the surrounding prose and explain what it shows and how it supports the proposed design. Expand the caption to identify the actors, systems, and interaction flow, and format the caption consistently in title case.

- Page 2, Proposed Solution: “This process is illustrated in section 4.”
- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”

### 14. Repeated filler and additive wording

`MINOR` · `writing`

Remove repeated filler and additive expressions such as “furthermore,” “additionally,” “also,” and “in addition” where they do not express a necessary logical relationship. Replace them with direct statements of the contribution or result.

- Page 1, Abstract: “Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “This leads to additional server overhead, since the CI and the Version Control (VC) are running on different servers.”
- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise [Krusche18].”
- Page 3, Related Work: “Not only projects using CI release twice as often as projects without CI, but the build is also less likely to fail in the master branch [Hilton16].”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 15. Several paragraphs are too short

`MINOR` · `structure`

Several paragraphs in Expected Findings are only one or two rendered lines long. Merge closely related statements or expand them with explanation and evidence so that the discussion is developed rather than presented as isolated fragments.

- Page 4, Expected Findings: “On long term, we expect that more universities will use Artemis, since the setup hurdle will become smaller.”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 16. Undefined abbreviation

`MINOR` · `completeness`

Define “API” at its first occurrence by spelling out the term and giving the abbreviation in parentheses, then use “API” consistently.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API”
- Page 2, Proposed Solution: “The build plan can be made available via the API of Artemis”

### 17. Use of first-person singular

`MINOR` · `writing`

Avoid first-person singular in the schedule. Replace the personal formulation with an impersonal or active formulation whose subject is the schedule or thesis plan.

- Page 4, Time Schedule: “During this time, I will not have time to work on my thesis. Instead, I plan to work on my thesis for about 2 weeks during the summer holidays in August.”

### 18. Scenario labels are not consistently formatted

`MINOR` · `formatting`

Format the scenario labels consistently as title-case headings and correct the apparent typo in “work fow.” Use labels such as “Exercise Workflow,” “Participating Actors,” and “Flow of Events.”

- Page 3, System Requirements and Top Level Design: “1) Name: Exercise work fow

2) Participating actors: Bob: Instructor, Mary: Student

3) Flow of events:”
