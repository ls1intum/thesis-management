# proposal-p2 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 48
- **Findings:** 13
- **Cost:** 16 calls, 0 tool calls, depth 3, 134217 tokens, 272s
- **Coverage:** 9/9 categories

## Summary

The proposal demonstrates a solid understanding of the research problem and objectives, with no issues flagged in the problem/motivation/objective content, bibliography, or figures. However, it suffers from significant structural problems: the body text exceeds the page limit, the schedule follows a horizontal rather than vertical agile pattern with a non-deliverable final sprint and an impermissible documentation task, and the AI transparency statement lacks required section-level specificity. These major issues must be resolved before submission, alongside several minor writing and consistency corrections.

## Findings

### 1. Total proposal text exceeds the 4–6 page limit

`MAJOR` · `structure`

Excluding the title page, two embedded figures, and the Schedule section, the remaining body text (Abstract through end of Objective 4.4, plus Bibliography and Transparency Statement) spans approximately 7–8 pages of text. This exceeds the 4–6 page maximum specified in the guidelines. The student should trim content, particularly in the Objective subsections and/or the Motivation section, to bring the total within range.

- Page 2, Abstract: “Supervisors of theses spend considerable time on non-content-related feedback such as document structure, coherence, and linguistic accuracy.”
- Page 12, Transparency Statement: “In preparing this proposal, I made use of several AI-based tools across different stages of the writing process.”

### 2. Horizontal layering across Iterations 1–3 instead of vertically integrated features

`MAJOR` · `methodology`

Iterations 1–3 follow a horizontal split: Iteration 1 prepares the infrastructure (modularize Thesis Management), Iteration 2 builds the backend engine (core feedback generation pipeline), and Iteration 3 connects the two (integrate into Thesis Management). No single iteration in this sequence delivers an end-to-end, user-visible feature. Per the vertical integration guideline, each iteration should deliver a working (even if minimal) end-to-end slice. For example, Iteration 2 could deliver a basic end-to-end feedback generation flow (hard-coded guidelines, minimal UI in TM), and Iteration 3 could enhance it with the full TM workflow. The current split means no working product exists until the end of Iteration 3.

- Page 9, Schedule: “Iteration 1 (08.06.2026 - 2 weeks) • **Modularize Thesis Management to prepare integration of the feedback module** (Objective 4)”
- Page 9, Schedule: “Iteration 2 (22.06.2026 - 2 weeks) • **Implement core feedback generation pipeline** (Objective 1) • Define input/output contract and structured output format (Objective 1)”
- Page 9, Schedule: “Iteration 3 (06.07.2026 - 2 weeks) • **Connect feedback tool to Thesis Management** (Objective 4) • Extend feedback workflow in Thesis Management for thesis (Objective 4)”

### 3. Final iteration is a non-deliverable QA/buffer sprint (waterfall pattern)

`MAJOR` · `methodology`

Iteration 9 ('Review the system and check for bugs') does not produce a measurable, deliverable work item. It is a classic waterfall-style final testing/QA phase rather than an agile sprint delivering a vertical feature. In an agile schedule, bug-fixing and stability work should be distributed across iterations as part of each feature's completion criteria, rather than concentrated in a final 'review' sprint. The student should either distribute testing/stabilization work across earlier iterations or replace this with a concrete, measurable deliverable (e.g., a specific end-to-end scenario that must pass, or a performance benchmark to be met).

- Page 10, Schedule: “Iteration 9 (05.10.2026 - 3 weeks) • **Review the system and check for bugs**”

### 4. Documentation task included in schedule

`MAJOR` · `structure`

Iteration 9 lists 'Documentation' as a work item. Per the schedule guidelines, the schedule should focus on development/research work only and must not include documentation writing tasks. This item should be removed from the schedule.

- Page 10, Schedule: “Review the system and check for bugs • **Documentation**”

### 5. AI transparency statement does not name specific proposal sections where tools were used

`MAJOR` · `completeness`

The statement uses vague phrasing such as "across different stages of the writing process" and "throughout the proposal" but does not identify which specific sections (e.g., Abstract, Introduction, Problem, Motivation, Objective, Schedule, Bibliography) each AI tool was applied to. The guidelines require the statement to name "the sections involved." The student should revise the statement to map each tool (or at least the main ones) to the sections where it was actually used, e.g., "I used Grammarly primarily on the Introduction and Problem sections" or "I used ChatGPT to expand ideas in the Motivation and Objective sections."

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools **across different stages of the writing process**.”
- Page 12, Transparency in the Use of AI Tools: “For citation assistance, I used Zotero to manage my references and to ensure proper citation formatting **throughout the proposal**.”

### 6. Overly short paragraph (1–2 lines) in Introduction

`MAJOR` · `structure`

The sentence 'The student, by contrast, hands in the thesis itself as a single final submission.' stands alone as a 1–2 line paragraph at the end of Section 1. This is too short to constitute a proper paragraph (target is 5–8 lines). It should be merged into the preceding paragraph about the iterative proposal workflow, which already discusses the contrast between proposal and thesis submission.

- Page 3, Introduction: “The student, by contrast, hands in the thesis itself as a **single final submission**.”

### 7. Several work items lack measurable specificity

`MINOR` · `methodology`

Multiple items are too vague to constitute a clear, measurable deliverable. 'Modularize Thesis Management to prepare integration' does not specify what will be modularized or what the concrete output is. 'Implement core feedback generation pipeline' does not state what the pipeline produces or how success is measured. 'Connect feedback tool to Thesis Management' does not define what a working connection looks like. Each item should name a concrete, verifiable outcome (e.g., 'Expose /api/feedback endpoint that accepts a proposal PDF and returns structured feedback JSON' or 'Supervisor can trigger feedback generation from the TM UI and see results within the proposal view').

- Page 9, Schedule: “Iteration 1 (08.06.2026 - 2 weeks) • **Modularize Thesis Management to prepare integration of the feedback module** (Objective 4)”
- Page 9, Schedule: “Iteration 2 (22.06.2026 - 2 weeks) • **Implement core feedback generation pipeline** (Objective 1)”
- Page 9, Schedule: “Iteration 3 (06.07.2026 - 2 weeks) • **Connect feedback tool to Thesis Management** (Objective 4)”

### 8. Motivation section slightly exceeds the 3/4 page limit

`MINOR` · `structure`

The Motivation section (Section 3) spans from page 4 (after the Problem ends) through the top of page 5 (before the Objective section). The combined text is approximately 25 lines, corresponding to roughly 5/6 of a page — slightly over the 3/4 page maximum. The student should tighten the second paragraph (the student-benefits discussion) to bring the section within the limit.

- Page 4, Motivation: “Better and faster feedback has significant advantages for students as well. When feedback arrives sooner, students can act on it while the writing process is still fresh in their minds.”
- Page 5, Motivation: “discussing it with the supervisor, and uses it to improve their knowledge, beliefs, and broader strategy for proceeding with the work [NM06].”

### 9. Forbidden sentence starter: "As"

`MINOR` · `writing`

The sentence begins with "As Figure 1 illustrates," which violates the rule against starting sentences with "As…". Rephrase to begin with the subject or the figure reference, e.g., "Figure 1 illustrates how an LLM-powered Reviewer works alongside the supervisor…".

- Page 4, Motivation: “**As** Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group's writing guidelines and generating automatic formative feedback [NM06].”

### 10. Forbidden sentence starter: "Because"

`MINOR` · `writing`

The sentence begins with "Because the feedback tool is powered by LLMs," which violates the rule against starting sentences with "Because…". Rephrase to lead with the subject, e.g., "Students can obtain instant formative feedback at any point during the writing process because the feedback tool is powered by LLMs."

- Page 5, Motivation: “**Because** the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 11. Passive voice: "is produced"

`MINOR` · `writing`

The clause "no LLM-generated feedback is produced at all" uses passive voice without identifying the actor. Rephrase to active voice, e.g., "the supervisor provides all feedback and no LLM-generated feedback is part of the output" or "the system generates no LLM feedback; all feedback comes from the supervisor."

- Page 6, Objective: “In one mode, no LLM-generated feedback **is produced** at all and all feedback comes from the supervisor.”

### 12. Inconsistent terminology: "the tool" vs. "the feedback tool"

`MINOR` · `writing`

The Abstract refers to "the tool" while every occurrence in the body text uses "the feedback tool." Use the same noun phrase throughout to avoid ambiguity about which tool is meant.

- Page 2, Abstract: “Beyond a single draft, **the tool** also compares iterations to highlight improvements”
- Page 6, Objective: “When **the feedback tool** has access to previous iterations of the proposal or thesis”
- Page 8, Objective: “With a single click, supervisors invoke **the feedback tool** to write feedback directly into Thesis Management.”

### 13. Inconsistent terminology: "local" vs. "self-hosted" for the same LLM hosting option

`MINOR` · `writing`

The Abstract describes the trade-off as between "local and cloud hosting," while the body consistently uses "self-hosted" and "cloud-hosted" (e.g., "locally self-hosted LLMs versus cloud-hosted ones"). Align the Abstract with the terminology used in the main text.

- Page 2, Abstract: “assess trade-offs between **local** and **cloud** hosting”
- Page 8, Objective: “privacy regulations and cost require careful consideration of when to use **locally self-hosted** LLMs versus **cloud-hosted** ones”
