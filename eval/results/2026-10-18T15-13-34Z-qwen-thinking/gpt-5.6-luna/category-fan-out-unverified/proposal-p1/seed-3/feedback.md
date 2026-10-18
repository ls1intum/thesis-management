# proposal-p1 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 32
- **Findings:** 15
- **Cost:** 10 calls, 0 tool calls, depth 2, 68231 tokens, 34s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a relevant integration problem and a plausible technical direction, but it is not ready for submission. Critical omissions include the required sections, bibliography length, figures, and AI transparency statement, while major weaknesses remain in the problem framing, objectives, writing, and schedule design.

## Findings

### 1. Required Introduction, Motivation, and Objective sections are missing

`CRITICAL` · `structure`

The proposal lacks the required dedicated Introduction, Motivation, and Objective sections. Add an Introduction before the Problem section to establish the research context and scope; add a Motivation section explaining the importance and expected benefits of integrating Artemis with GitLab CI; and add an Objective section with a short introduction, 3–4 one-line high-level objectives as bullet points, and one subsection of at least two paragraphs for each objective. Formulate objectives with clear action verbs rather than presenting them only as combined motivation/objective text in the abstract.

- Page 1, Abstract: “Motivation/Objective: Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to **reduce the efort required to maintain the servers and migrate the CI service** from external applications as Jenkins to the inbuilt CI service.”
- Page 3, System Requirements and Top Level Design: “4     System        Requirements              and     Top     Level     Design”

### 2. Too few bibliography entries

`CRITICAL` · `completeness`

The References section contains only five publications. Add at least one additional peer-reviewed publication to meet the minimum of six, preferably providing six to eight relevant sources.

- Page 5, References: “**[Booch95]** Booch, G. Object-oriented analysis and design with applications (2. ed.).”
- Page 6, References: “**[Krusche18]** Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning.”

### 3. Fewer than two required figures

`CRITICAL` · `figures`

The proposal contains only one figure, although at least two are required. Add a second meaningful figure, including at least one identifiable suitable UML diagram such as an activity, component, class, or use-case diagram; do not use a sequence diagram.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI”

### 4. Missing AI transparency statement

`CRITICAL` · `completeness`

The proposal lacks the required AI transparency statement. Add a dedicated first-person statement naming every AI tool used, explaining its purpose, identifying the sections in which it was used, and confirming that all AI-generated text was checked for correctness and coherence.

### 5. Problem definition mixes solution proposals and omits stakeholder impacts

`MAJOR` · `completeness`

The Problem section mixes the current problem with a desired solution and does not sufficiently identify affected actors or their impacts. Move proposed functionality such as changing build plans and compiler behavior to the Motivation, Requirements, or Solution section. In the Problem section, explicitly identify instructors, students, course administrators, and universities, and explain how server overhead and manual build-plan updates affect each stakeholder.

- Page 2, Problem Statement: “In exams, it would be useful to **change the student build plans after the submission date** to firstly hide the test cases during the exam and only compile the code and secondly use a debug compiler not failing due to a simple syntax error.”
- Page 2, Problem Statement: “This leads to additional server overhead, since the CI and the Version Control (VC) are running on different servers. Particularly smaller universities might then not use Artemis.”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server. Especially in large software engineering courses this is nearly impossible.”

### 6. Schedule is not organized around iterative, integrated, measurable increments

`MAJOR` · `schedule`

Reorganize the schedule into explicitly dated 2–4 week iterations, each ending with an integrated, demonstrable increment. Replace the current horizontal component sequence with vertical end-to-end capabilities, such as configuring one exercise, executing its GitLab CI build with an Artemis-provided plan, and displaying the resulting grade in Artemis. Treat requirements as a backlog selected within each iteration rather than as a separate activity, and define measurable deliverables or acceptance criteria for every iteration, including the currently vague models, requirements, and improvements tasks.

- Page 5, Schedule: “April – **2nd week: Docker GitLab CI Runner configuration**”
- Page 5, Schedule: “April – **4th week: prototype implementation: Connectivity to GitLab CI**”
- Page 5, Schedule: “May – **3nd week: prototype implementation: Artemis API for configuration**”
- Page 5, Schedule: “June – **4th week: prototype implementation: Sending build results to Artemis**”
- Page 5, Schedule: “June – 4th week: **Models (parallel to prototype implementation)**”
- Page 5, Schedule: “June – 4th week: **Requirements (parallel to prototype implementation)**”
- Page 5, Schedule: “November – 4th week: **Implement improvements**”

### 7. Schedule includes prohibited writing and documentation tasks

`MAJOR` · `completeness`

Remove thesis writing, proposal, documentation, review, and thesis-completion tasks from the development/research schedule. Use the schedule for implementation, experiments, testing, evaluation, and other research work; retain only relevant research deliverables.

- Page 4, Schedule: “Every week, Wednesday is set aside for **working on the thesis**.”
- Page 5, Schedule: “March – 5th week: **Proposal**”
- Page 5, Schedule: “September – 2nd week: **Documentation for installation and configuration of Artemis with GitLab CI**”
- Page 5, Schedule: “October: **Writing**”
- Page 5, Schedule: “November – 2nd week: **Thesis ready for review**”
- Page 5, Schedule: “December – 1st week: **Thesis**”

### 8. Figure 1 lacks a direct textual reference and informative caption

`MAJOR` · `figures`

Reference the figure directly by number in the surrounding text and expand its caption into an informative description. Replace “This process is illustrated in section 4” with a statement such as “Figure 1 illustrates the top-level workflow between the instructor, student, Artemis, and GitLab CI.” The caption should explain the actors, systems, numbered interactions, and direction or meaning of the workflow.

- Page 2, Proposed Solution: “This process is illustrated in section 4.”
- Page 4, System Requirements and Top Level Design: “Figure 1: **Depiction of the top-level workfow of the interactions in relation to Artemis and GitLab CI**”

### 9. Frequent passive voice obscures responsible actors

`MAJOR` · `writing`

Rewrite passive constructions by naming the responsible actor as the grammatical subject, especially when Artemis, GitLab CI, instructors, or students perform the action. Revise the cited examples throughout the proposal rather than applying the change to only one sentence.

- Page 1, Abstract: “The CI server **should be migrated** to the VC server.”
- Page 1, Abstract: “the build plan **is fetched from the API** on each run.”
- Page 2, Problem Statement: “the base build plan **is copied once** after the student started the exercise”
- Page 2, Problem Statement: “the build plans **must be updated manually** in the CI Server.”
- Page 3, Related Work: “the assessment management system Artemis **was built**”
- Page 3, System Requirements and Top Level Design: “The student base repository **is copied**”
- Page 3, System Requirements and Top Level Design: “The GitLab CI build **gets triggered**”

### 10. Several sentences use forbidden sentence starters

`MAJOR` · `writing`

Rewrite sentences that begin with forbidden subordinating or infinitive constructions so that they begin with the main subject and action, or combine them with the preceding sentence. Apply this to all cited examples, including “Since,” “To achieve this goal,” “In order to,” “As a result,” and “As described in section 3.”

- Page 1, Abstract: “**Since some Version Control (VC) systems include a Continuous Integration (CI) application**, it is possible to reduce the effort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 2, Problem Statement: “**To achieve this goal**, Artemis uses a Version Control (VC) and Continuous Integration (CI) server”
- Page 2, Related Work: “**In order to implement this idea**, the lecture is split up into multiple iterations”
- Page 2, Proposed Solution: “**As a result**, instructors are able to update the build plan after the release of an exercise”
- Page 3, Related Work: “**Since the benefits of using CI are quite high** and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university”
- Page 4, Expected Findings: “**As described in section 3**, this might lead to better developers and better code”

### 11. API is undefined at first use

`MAJOR` · `writing`

Define “API” at its first use as “application programming interface (API)” before using the abbreviation elsewhere.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis **API**”

### 12. Excessive filler transitions weaken sentence precision

`MINOR` · `writing`

Remove filler transitions such as “Furthermore,” “Additionally,” “also,” and “In addition” where they do not express a precise logical relationship. Replace them with explicit causal, contrasting, or additive relationships when needed.

- Page 1, Abstract: “**Furthermore**, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “**Additionally**, the base build plan is copied once after the student started the exercise”
- Page 2, Proposed Solution: “**Furthermore**, the CI service should be part of the VC server to reduce additional effort by maintaining two servers.”
- Page 3, Related Work: “Not only projects using CI release twice as often as projects without CI, but the build is **also** less likely to fail in the master branch”
- Page 4, Expected Findings: “**In addition**, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “**In addition**, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”

### 13. Two Expected Findings paragraphs are underdeveloped

`MINOR` · `structure`

Expand or combine the two short Expected Findings paragraphs so that each develops its claim sufficiently. Explain the expected nature of the university savings and why Ansible or Kubernetes is relevant to the implementation and deployment plan.

- Page 4, Expected Findings: “**In addition, universities will save time and money as they will need at least one less server.**”
- Page 4, Expected Findings: “**In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.**”

### 14. Schedule uses first-person statements

`MINOR` · `writing`

Avoid first-person pronouns in the schedule. State the commitment impersonally or identify the thesis schedule directly.

- Page 4, Time Schedule: “During this time, **I will not have time** to work on my thesis.”
- Page 4, Time Schedule: “Instead, **I plan to work** on my thesis for about 2 weeks during the summer holidays in August.”

### 15. Inconsistent capitalization and spelling in labels and workflow terminology

`MINOR` · `formatting`

Apply consistent title case and correct spelling in schedule labels and exercise names. Use labels such as “Prototype Implementation: Connectivity to GitLab CI,” “Prototype Implementation: Sending Build Results to Artemis,” “Documentation for Installation and Configuration of Artemis with GitLab CI,” and “Revise Models with Concrete Implementation.” Rename the exercise to “Exercise Workflow” and spell “workflow” consistently throughout.

- Page 5, Time Schedule: “– 4th week: **prototype implementation**: Connectivity to GitLab CI”
- Page 5, Time Schedule: “– 4th week: **prototype implementation**: Sending **build results** to Artemis”
- Page 5, Time Schedule: “– 2nd week: Documentation for **installation and confguration** of Artemis with GitLab CI”
- Page 5, Time Schedule: “– 2nd week: **Revise models** with concrete implementation”
- Page 3, System Requirements and Top Level Design: “1) Name: **Exercise work fow**”
- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed **workfow** of an exercise.”
- Page 3, System Requirements and Top Level Design: “1) Name: Exercise **work fow**”
- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level **workfow** of the interactions in relation to Artemis and GitLab CI”
