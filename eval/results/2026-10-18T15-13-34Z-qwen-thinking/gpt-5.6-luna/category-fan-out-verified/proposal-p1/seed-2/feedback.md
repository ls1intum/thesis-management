# proposal-p1 — category-fan-out-verified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 28
- **Findings:** 13
- **Cost:** 19 calls, 0 tool calls, depth 3, 80395 tokens, 30s
- **Coverage:** 9/9 categories

## Summary

The proposal has a relevant problem and a plausible proposed solution, but it currently fails several required structural, completeness, and presentation requirements. Before submission, it needs dedicated Introduction, Motivation, and Objectives sections, more scholarly sources and figures, an AI transparency statement, and revisions to the problem framing, schedule, figure integration, and prose clarity.

## Findings

### 1. Missing Introduction section

`CRITICAL` · `structure`

Add a clearly titled Introduction section, separate from the Problem Statement and Related Work, and keep it under one page.

### 2. Missing dedicated Motivation section

`CRITICAL` · `structure`

Add a dedicated Motivation section of no more than three-quarters of a page. It should explain the scientific importance of solving the problem and present a positive vision of the expected impact; the motivation currently appears only in the abstract and is combined there with objectives, the problem statement, and the proposed solution.

- Page 1, Abstract: “**Motivation/Objective:** Since some Version Control (VC) systems include a Continuous Integration (CI) application, it is possible to reduce the effort required to maintain the servers and migrate the CI service from external applications as Jenkins to the inbuilt CI service.”

### 3. Missing Objectives section and required objective subsections

`CRITICAL` · `structure`

Replace the current Section 4 with a dedicated Objectives section. Include an introductory paragraph, 3–4 one-line objective bullets formulated as actions, and a subsection of at least two paragraphs for each objective. The current section is titled System Requirements and Top Level Design and does not provide the required objective structure.

- Page 3, System Requirements and Top Level Design: “4     **System Requirements              and     Top     Level     Design**”
- Page 4, Expected Findings: “5     **Expected          Findings**”
- Page 4, System Requirements and Top Level Design: “The numbered Section 4 is instead “System Requirements and Top Level Design.””

### 4. Bibliography contains fewer than the required minimum number of publications

`CRITICAL` · `completeness`

Expand the bibliography from five publications to at least six, preferably reaching the recommended range of 6–8 peer-reviewed publications. Add relevant peer-reviewed sources rather than relying only on the current entries.

- Page 5, References: “References [Booch95] Booch, G. Object-oriented analysis and design with applications (2. ed.).”
- Page 6, References: “[Krusche18] Krusche, S.; Seitz, A. ArTEMiS: An Automatic Assessment Management System for Interactive Learning.”

### 5. Fewer than the required two figures

`CRITICAL` · `completeness`

Add at least one additional diagram, mockup, screenshot, or chart so that the proposal contains the required minimum of two figures.

- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”

### 6. Missing AI transparency statement

`CRITICAL` · `completeness`

Add a dedicated first-person AI transparency statement that names the AI tools used, explains their purposes, identifies the proposal sections in which they were used, and confirms that all AI-generated text was checked for correctness and coherence.

### 7. Problem section includes a desired solution or alternative

`MAJOR` · `logic`

Restrict the Problem Statement to the existing problem and its consequences. Move proposed changes to build plans, hidden test cases, compilation behavior, and debug compiler settings to the Motivation, Objectives, or Proposed Solution section.

- Page 2, Problem Statement: “In exams, it would be useful to **change the student build plans after the submission date to firstly hide the test cases during the exam and only compile the code and secondly use a debug compiler** not failing due to a simple syntax error.”

### 8. Thesis and documentation tasks in the schedule

`MAJOR` · `structure`

Remove thesis writing, documentation writing, review preparation, and thesis completion from the development/research schedule. Use the schedule for implementation, experimentation, evaluation, and other research work only.

- Page 5, Time Schedule: “September – 2nd week: **Documentation for installation and configuration of Artemis with GitLab CI**”
- Page 5, Time Schedule: “October: **Writing . . .**”
- Page 5, Time Schedule: “November – 2nd week: **Thesis ready for review**”
- Page 5, Time Schedule: “December – 1st week: **Thesis**”

### 9. Figures are not explicitly referenced in the text

`MAJOR` · `structure`

Reference figures explicitly by number in the surrounding prose, for example, “Figure 1 shows the top-level workflow.” Replace the indirect reference to Section 4 and similarly reference any additional figures that are added.

- Page 2, Proposed Solution: “This process is **illustrated in section 4**.”
- Page 4, System Requirements and Top Level Design: “Figure 1: Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI”

### 10. Figure caption is not sufficiently informative

`MINOR` · `writing`

Expand the Figure 1 caption to explain the actors, systems, and numbered interactions shown, so that the figure's scope and main relationships are understandable without relying on surrounding text.

- Page 4, System Requirements and Top Level Design: “Figure 1: **Depiction of the top-level workflow of the interactions in relation to Artemis and GitLab CI**”

### 11. Undefined abbreviation

`MINOR` · `writing`

Define the abbreviation at its first occurrence by writing “application programming interface (API),” then use API thereafter.

- Page 1, Abstract: “This system supports the provision of the build plan via the Artemis **API**, which makes it possible to update the build plan retrospectively, as the build plan is fetched from the API on each run.”

### 12. Inconsistent capitalization of server terms

`MINOR` · `writing`

Standardize capitalization of common server terms, preferably using “CI server” and “VC server” unless the terms begin a sentence or are part of a proper name.

- Page 2, Problem Statement: “Artemis currently supports Bamboo 1 and Jenkins 2 as a **CI server** for executing the tests.”
- Page 2, Problem Statement: “the build plans must be updated manually in the **CI Server**.”
- Page 2, Proposed Solution: “each time the code in the **VC Server** gets updated”
- Page 2, Proposed Solution: “The Version Control server GitLab 3 has an integrated CI service”

### 13. Short paragraphs and list-heavy workflow reduce readability

`MINOR` · `structure`

Merge closely related short paragraphs in Expected Findings into fuller paragraphs of approximately 5–8 lines while preserving distinct ideas as separate paragraphs. Also introduce the workflow in System Requirements and Top Level Design with explanatory prose and explain the numbered workflow in prose paragraphs, using a shorter list only for genuinely sequential steps.

- Page 4, Expected Findings: “On long term, we expect that more universities will use Artemis, since the setup hurdle will become smaller.”
- Page 4, Expected Findings: “In addition, universities will save time and money as they will need at least one less server.”
- Page 4, Expected Findings: “The proposed solution opens up new opportunities for further research. with the envisaged implementation it will be possible to tackle the problem regarding the test execution during exams described in section 1.”
- Page 4, Expected Findings: “In addition, either Ansible or a Kubernetes cluster can be used for automatic deployment of the GitLab CI Runners.”
- Page 3, System Requirements and Top Level Design: “1) Name: Exercise work fow”
- Page 3, System Requirements and Top Level Design: “3) Flow of events:”
