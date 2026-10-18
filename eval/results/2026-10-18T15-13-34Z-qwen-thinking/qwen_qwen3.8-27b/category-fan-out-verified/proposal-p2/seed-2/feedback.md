# proposal-p2 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** ACCEPTABLE
- **Score:** 64
- **Findings:** 9
- **Cost:** 15 calls, 0 tool calls, depth 3, 135825 tokens, 240s
- **Coverage:** 9/9 categories

## Summary

The proposal has a solid foundation: structure, problem statement, objectives, bibliography, and figures all pass review. However, there are six major issues that need addressing before submission—primarily mechanical writing violations (forbidden sentence starters, overlong paragraphs mixing distinct ideas) in the Motivation and Objective sections, plus two schedule problems (a documentation item and a waterfall-style final iteration with an unmeasurable deliverable). These are all fixable with targeted edits, but the accumulation of paragraph-splitting and sentence-restructuring work in the core narrative sections means the proposal is not yet ready to submit as-is.

## Findings

### 1. Forbidden sentence starter: "As…"

`MAJOR` · `writing`

The sentence begins with 'As Figure 1 illustrates,' which violates the rule against starting sentences with 'As…'. Restructure the sentence so the subject or a different word opens it (e.g., 'Figure 1 illustrates how an LLM-powered Reviewer works alongside the supervisor').

- Page 4, Motivation: “As Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group's writing guidelines and generating automatic formative feedback [NM06].”

### 2. Forbidden sentence starter: "Because…"

`MAJOR` · `writing`

The sentence begins with 'Because the feedback tool is powered by LLMs,' which violates the rule against starting sentences with 'Because…'. Restructure so the main clause leads (e.g., 'Students can obtain instant formative feedback at any point during the writing process, since the feedback tool is powered by LLMs.'—or rephrase entirely).

- Page 5, Motivation: “Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 3. Motivation second paragraph exceeds 10 lines and mixes distinct ideas

`MAJOR` · `writing`

The second paragraph of the Motivation section spans approximately 12 rendered lines (continuing from page 4 to page 5). It combines three distinct ideas: (1) students benefit from faster feedback, (2) a detailed explanation of self-regulated learning theory (knowledge, beliefs, interpretation of guidelines), and (3) the practical advantage of instant LLM-powered feedback. Split the paragraph after the first sentence or after the self-regulated-learning explanation to keep each paragraph focused on one idea and within the 5-8 line range.

- Page 4, Motivation: “Better and faster feedback has signifcant advantages for students as well. When feedback arrives sooner, students can act on it while the writing process is still fresh in their minds. The student brings both knowledge, comprising theoretical and practical domain expertise, and beliefs about how to approach the task and how to work and write scientifcally, which include the student's personal interpretation of the writing guidelines. Drawing on this knowledge and these beliefs, the student writes the thesis. Through self-regulated learning, the student engages with the Reviewer's feedback, refecting on it independently or”
- Page 5, Motivation: “discussing it with the supervisor, and uses it to improve their knowledge, beliefs, and broader strategy for proceeding with the work [NM06]. Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 4. Section 4.1 first paragraph exceeds 10 lines and shifts topic

`MAJOR` · `writing`

The first paragraph under 4.1 spans approximately 11 rendered lines (pages 5-6) and shifts from describing the technical mechanism (supervisors provide guidelines → LLM preprocesses and generates feedback) to an ethical discussion of normative vs. formative feedback. These are two distinct ideas: the system design and the ethical justification for using formative rather than normative feedback. Split after the sentence about preprocessing guidelines, starting a new paragraph with 'Ethically, it is difcult…'.

- Page 5, Objective – Generate Feedback Based on Guidelines: “Supervisors should be able to provide their own scientifc writing guidelines as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover. Since guidelines may not already be in a suitable format, the LLM may frst”
- Page 6, Objective – Generate Feedback Based on Guidelines: “need to preprocess them into a structured representation it can act on. Ethically, it is difcult to let LLMs grade a fnal thesis, as this constitutes normative feedback and requires close scrutiny. Formative feedback, on the other hand, is less critical since it serves as input for students to improve their work. The idea is that students interpret the feedback themselves and discuss it, either independently or with their supervisor. Therefore, it is less important that every output of the LLM is perfectly correct.”

### 5. Documentation task included in the schedule

`MAJOR` · `structure`

Iteration 9 lists 'Documentation' as a work item. Per the schedule guidelines, the schedule should focus on development/research work only and must not include documentation writing tasks. This item should be removed from the schedule; documentation is an implicit byproduct of each iteration, not a separate scheduled task.

- Page 10, Schedule: “• Review the system and check for bugs
• Documentation”

### 6. Final iteration follows a waterfall pattern with a vague, non-measurable deliverable

`MAJOR` · `methodology`

Iteration 9 is dedicated entirely to 'Review the system and check for bugs,' which is (a) a waterfall-style pattern (build → test → fix) rather than agile practice where each iteration produces working, tested software, and (b) not a measurable, deliverable work item—it does not specify what will be reviewed, what the acceptance criteria are, or what the concrete output is. The student should restructure so that each iteration includes testing of its own deliverables, and the final iteration contains a specific, measurable development or research task (e.g., 'Run end-to-end test suite covering all four objectives; fix all P1/P2 bugs').

- Page 10, Schedule: “Iteration 9 (05.10.2026 - 3 weeks)
• Review the system and check for bugs
• Documentation”

### 7. Inconsistent terminology: 'local' vs. 'self-hosted' for on-premises LLMs

`MINOR` · `writing`

The Abstract refers to 'local and cloud hosting', while the body of section 4.3 uses 'locally self-hosted LLMs' and 'Self-hosted models'. The same concept (LLMs deployed on university infrastructure) should be referred to with a consistent term throughout the author's prose.

- Page 2, Abstract: “assess trade-offs between local and cloud hosting”
- Page 8, Objective: “privacy regulations and cost require careful consideration of when to use locally self-hosted LLMs versus cloud-hosted ones”
- Page 8, Objective: “Self-hosted models keep student drafts inside the university's infrastructure”

### 8. AI Transparency Statement does not specify which sections the tools were used for

`MINOR` · `completeness`

The statement uses general phrasing such as 'across different stages of the writing process' and 'throughout the proposal' but does not name specific sections (e.g., Abstract, Introduction, Problem, Motivation, Objective, Schedule, Bibliography) where each tool was applied. The guidelines require specifying which sections of the proposal the AI tools were used for. For example, stating that Grammarly was primarily used on the Introduction and Problem sections, or that ChatGPT was used to expand ideas in the Motivation and Objective sections, would satisfy this requirement.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across diferent stages of the writing process.”
- Page 12, Transparency in the Use of AI Tools: “I used Zotero to manage my references and to ensure proper citation formatting throughout the proposal.”

### 9. Iteration 1 lacks a concrete, user-facing deliverable

`MINOR` · `methodology`

'Modularize Thesis Management to prepare integration of the feedback module' describes internal refactoring without a measurable outcome. What does 'modularize' mean concretely? The student should specify a verifiable deliverable, e.g., 'Extract feedback-related logic into a separate module with a defined API; existing TM functionality passes regression tests.' As written, there is no way to measure whether the iteration is complete.

- Page 9, Schedule: “• Modularize Thesis Management to prepare integration of the feedback module (Objective 4)”
