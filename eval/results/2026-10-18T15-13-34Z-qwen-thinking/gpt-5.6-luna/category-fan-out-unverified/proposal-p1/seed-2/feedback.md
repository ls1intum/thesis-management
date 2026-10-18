# proposal-p1 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 27
- **Findings:** 16
- **Cost:** 10 calls, 0 tool calls, depth 2, 72443 tokens, 71s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a relevant problem and sketches a plausible Artemis–GitLab CI integration, but it is not ready for submission. Critical structural omissions, insufficient figures and references, the missing AI transparency statement, and an underdeveloped objective section must be addressed first; the schedule, stakeholder analysis, and writing also require substantial revision.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

The required Introduction section is missing. Add it before the Problem section and briefly establish the context, scope, and purpose of the proposed work.

### 2. Missing Motivation section

`CRITICAL` · `structure`

The required standalone Motivation section is missing. Explain the scientific and educational importance of solving the problem, why the proposed approach is needed, and the positive vision of a reliable, adaptable, and lower-maintenance assessment infrastructure. The combined “Motivation/Objective” label in the abstract does not replace this section.

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 1, Abstract: “The interactive learning platform Artemis currently only supports setups with an external CI server.”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a CI server for executing the tests. This leads to additional server overhead, since the CI and the Version Control (VC) are running on diferent servers.”

### 3. Missing Objective section and required subsections

`CRITICAL` · `structure`

The required Objective section (Section 4) is missing. Replace the current “System Requirements and Top Level Design” Section 4 with an objective section containing a short introduction, a one-line bullet list of 3–4 high-level objectives, and one subsection for each objective. Each subsection must contain at least two paragraphs describing the objective’s scope, intended outcome, and evaluation. Use direct, action-oriented formulations rather than passive or compound statements.

- Page 1, Abstract: “The CI server should be migrated to the VC server. Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 3, System Requirements and Top Level Design: “4 System Requirements and Top Level Design”

### 4. Bibliography has fewer than six publications

`CRITICAL` · `completeness`

The bibliography contains only five publication entries, below the required minimum of six and the recommended range of six to eight peer-reviewed publications. Add at least one additional relevant peer-reviewed publication, preferably several to reach the recommended range.

- Page 5, References: “[Booch95] Booch, G. Object-oriented analysis and design with applications”
- Page 5, References: “[Gamma95] Gamma, E.; Helm, R.; Johnson, R.; Vlissides, J. Design Patterns: Elements of Reusable”
- Page 5, References: “[Hilton16] Hilton, M.; Tunnell, T.; Huang, K.; Marinov, D.; Dig, D. Usage, costs, and benefts of”
- Page 5, References: “[Krusche17] Krusche, S.; Seitz, A.; Borstler, J.; Brugge, B. Interactive learning: Increasing student”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for”

### 5. Required figures and UML diagram are missing

`CRITICAL` · `figures`

The proposal contains only one figure and no suitable UML diagram. Add at least one additional figure that contributes distinct information and include a non-sequence UML diagram, such as a component, activity, or class diagram, showing the relevant system structure or behavior.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”

### 6. Missing AI transparency statement

`CRITICAL` · `completeness`

The proposal does not contain an AI transparency statement. Add a dedicated first-person statement naming each AI tool used, explaining its purpose, identifying the sections in which it was used, and confirming that all AI-generated text was checked for correctness and coherence.

### 7. Problem section proposes functionality

`MAJOR` · `logic`

The Problem section includes desired functionality and concrete solution requirements rather than describing only the underlying problem and its consequences. Move the exam-related capabilities to the objectives or solution/design section, and describe the risks and difficulties caused by the current setup.

- Page 2, Problem Statement: “In exams, it would be useful to change the student build plans after the submission date to firstly hide the test cases during the exam and only compile the code and secondly use a debug compiler not failing due to a simple syntax error.”

### 8. Stakeholder impacts are insufficiently specified

`MAJOR` · `completeness`

The Problem section does not clearly identify the affected stakeholders or explain the negative impact on each. Explicitly describe how the separate-server setup and manual build-plan updates affect instructors, students, university administrators, and course operators.

- Page 2, Problem Statement: “Particularly smaller universities might then not use Artemis.”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server. Especially in large software engineering courses this is nearly impossible.”

### 9. Objectives are passive and contain multiple actions

`MAJOR` · `writing`

Rewrite the objectives in direct, active, action-oriented form, with one clear action per objective. Replace passive or impersonal constructions such as “should be migrated” and “it should be possible” with formulations such as “Integrate Artemis with GitLab CI” or “Expose build plans through the Artemis API.”

- Page 1, Abstract: “The CI server should be migrated to the VC server. Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 10. Figure caption and in-text reference are insufficient

`MAJOR` · `figures`

Expand Figure 1’s caption so it independently explains the actors, systems, and principal interactions, including the proposed GitLab CI integration. Reference the figure explicitly by number in the surrounding text and explain what the reader should observe.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”
- Page 2, Proposed Solution: “This process is illustrated in section 4.”

### 11. Schedule is not organized as objective-driven agile iterations

`MAJOR` · `methodology`

Reorganize the schedule into explicit 2–4-week iterations with coherent, measurable, end-to-end increments linked to the objectives. Replace the current horizontal sequence of runner configuration, connectivity, API configuration, and result transmission with vertically integrated capabilities, such as configuring one exercise, executing a submission with a dynamically fetched build plan, and returning and displaying the result. Include acceptance criteria and regular validation of the integrated system.

- Page 5, Time Schedule: “April – 2nd week: Docker GitLab CI Runner configuration – 4th week: prototype implementation: Connectivity to GitLab CI”
- Page 5, Time Schedule: “May – 3nd week: prototype implementation: Artemis API for configuration”
- Page 5, Time Schedule: “June – 4th week: prototype implementation: Sending build results to Artemis”
- Page 4, Time Schedule: “It is planned to adhere to the following time schedule:”
- Page 5, Time Schedule: “September – 2nd week: Documentation for installation and configuration of Artemis with GitLab CI”

### 12. Schedule items lack measurable deliverables

`MAJOR` · `completeness`

Several schedule items are not measurable deliverables. Replace vague entries such as “Models” and “Requirements” with concrete artifacts, implemented behavior, tests, or acceptance criteria, such as a defined data model implemented in code and validated by specified tests.

- Page 5, Time Schedule: “June – 4th week: Models (parallel to prototype implementation) – 4th week: Requirements (parallel to prototype implementation)”

### 13. Schedule mixes development with thesis and documentation tasks

`MAJOR` · `writing`

Remove proposal-writing, documentation, review, and thesis-writing entries from the development schedule or track them separately from the research iterations.

- Page 5, Time Schedule: “March – 5th week: Proposal”
- Page 5, Time Schedule: “September – 2nd week: Documentation for installation and configuration of Artemis with GitLab CI”
- Page 5, Time Schedule: “October: Writing . . .”
- Page 5, Time Schedule: “November – 2nd week: Thesis ready for review – 3rd week: Review finished”
- Page 5, Time Schedule: “December – 1st week: Thesis”

### 14. Revise passive voice, transitions, sentence openings, first-person phrasing, and abbreviation use

`MINOR` · `writing`

Revise repeated passive or impersonal constructions to identify the actor performing each action. Also recast sentences beginning with forbidden subordinators or infinitive phrases, remove filler transitions that add no meaning, replace first-person schedule statements with impersonal formulations, and expand “Application Programming Interface (API)” at first use.

- Page 1, Abstract: “The CI server should be migrated to the VC server.”
- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”
- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the efort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 1, Abstract: “Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”
- Page 2, Problem Statement: “To achieve this goal, Artemis uses a Version Control (VC) and Continuous Integration (CI) server, to execute test cases predefned by the instructor on each submission of each student, display the build results and grade the submission [Krusche18].”
- Page 2, Problem Statement: “Later changes to the base build plan are not patched automatically to all student build plans, but rather the build plans must be updated manually in the CI Server.”
- Page 2, Problem Statement: “Additionally, the base build plan is copied once after the student started the exercise [Krusche18].”
- Page 2, Proposed Solution: “Furthermore, the CI service should be part of the VC server to reduce additional efort by maintaining two servers.”
- Page 2, Proposed Solution: “As a result, instructors are able to update the build plan after the release of an exercise, GitLab CI fetches the updated build plan when needed.”
- Page 3, Related Work: “In order to implement this idea, the lecture is split up into multiple iterations, each consisting of diferent phases [Krusche17].”
- Page 3, Related Work: “Since the benefts of using CI are quite high and nearly half of the developers who do not use CI mention a lack of familiarity as the reason for not using a CI service, it is useful to teach the concepts of CI at university [Hilton16].”
- Page 3, Related Work: “Not only projects using CI release twice as often as projects without CI, but the build is also less likely to fail in the master branch [Hilton16].”
- Page 3, System Requirements and Top Level Design: “The student base repository is copied.”
- Page 3, System Requirements and Top Level Design: “GitLab CI gets confgured so that it takes the build plan from the Artemis API.”
- Page 4, Expected Findings: “As described in section 3, this might lead to better developers and better code, due to the fact, that more students will be taught the concepts of CI.”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”
- Page 4, Time Schedule: “It is planned to adhere to the following time schedule:”
- Page 4, Time Schedule: “During this time, I will not have time to work on my thesis.”
- Page 4, Time Schedule: “Instead, I plan to work on my thesis for about 2 weeks during the summer holidays in August.”
- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis API, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”

### 15. Workflow description needs prose and stronger section framing

`MINOR` · `structure`

The workflow section relies almost entirely on a 14-item numbered list. Add a substantial explanatory paragraph describing the overall workflow, roles, system boundaries, and relevance to the design before the list; retain only essential steps as a concise list. Expand the short concluding discussion so it explains how the problem and scenario lead to the actors, systems, and interactions shown in the figure.

- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.

1) Name: Exercise work fow
2) Participating actors: Bob: Instructor, Mary: Student
3) Flow of events:
1. Bob prepares the exercise via the web interface of Artemis and pushes the exercise code template, the test cases as well as the sample solution to the VC server [Krusche18].”
- Page 3, System Requirements and Top Level Design: “The visionary scenario presents the supposed workfow of an exercise.”
- Page 3, System Requirements and Top Level Design: “Using the problem statement section 1 and the visionary scenario, it is possible to determine the relevant actors and systems for solving the problem.”

### 16. Terminology for CI and assignments is inconsistent

`MINOR` · `writing`

Use consistent terminology throughout the proposal. Distinguish clearly between GitLab CI as a product or service and the server hosting it, and use one term consistently for the student assignment, preferably “exercise” rather than alternating with “task.”

- Page 1, Abstract: “Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”
- Page 2, Problem Statement: “Artemis currently supports Bamboo and Jenkins as a CI server for executing the tests.”
- Page 2, Proposed Solution: “The Version Control server GitLab has an integrated CI service called GitLab CI.”
- Page 1, Abstract: “Furthermore, it should be possible to adjust the build plan after the task has been assigned.”
- Page 2, Problem Statement: “The interactive learning platform Artemis is an approach to use interactive learning during large computer science classes.”
