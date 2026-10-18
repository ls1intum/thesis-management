# proposal-p2 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 66
- **Findings:** 6
- **Cost:** 16 calls, 34 tool calls, depth 16, 360821 tokens, 260s
- **Coverage:** 9/9 categories

## Summary

A well-organized, complete proposal: all required sections are present (Abstract, Introduction, Problem, Motivation, Objective with four action-form subsections of at least two paragraphs each, Schedule, Bibliography, and an AI Transparency Statement), two UML figures (object model + activity diagram) are both referenced with informative captions, and citations are consistently styled and placed before the full stop. Key issues to fix: (1) CRITICAL — the bibliography has only 5 references, below the 6-8 peer-reviewed minimum; (2) the schedule's Iteration 1 (08.06.2026) begins before the stated thesis start date (26.06.2026), so the timeline is internally inconsistent; (3) several sentences open with forbidden starters (As/Since/Because) and should be rewritten; (4) Iteration 9 includes a documentation task and a vague 'check for bugs' item; (5) the AI transparency statement names tools and purposes but not which proposal sections they were used for; and (6) the Motivation is practical rather than scientifically motivated. The problem section correctly presents actors and negative consequences without solutions, and the objectives avoid double verbs.

## Findings

### 1. Sentences start with forbidden words (As / Since / Because)

`MAJOR` · `WRITING`

Several sentences begin with a forbidden starter word. Rewrite to avoid leading with 'As', 'Since', or 'Because'. Occurrences: page 4 'As Figure 1 illustrates...'; page 5 'Since guidelines may not already be in a suitable format...'; page 5 'Because the feedback tool is powered by LLMs...'.

- Page 4, Motivation: “As Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group's writing guidelines”

### 2. Bibliography below the 6-8 minimum (only 5 references)

`CRITICAL` · `CITATION`

The bibliography contains only 5 references ([TP21], [AAM17], [BBE10], [Bog+07], [NM06]). The guidelines require at least 6-8 peer-reviewed publications. Add at least 1-3 more relevant peer-reviewed sources (e.g., on LLM-based feedback, writing-guideline automation, or self-regulated learning with AI) to reach the minimum.

- Page 11, Bibliography: “T. Tuononen and A. Parpala, "The Role of Academic Competences and Learning Processes in Predicting Bachelor's and Master's Thesis Grades," Studies in Educational Evaluation, vol. 70, p. 101001, 2021”

### 3. Schedule starts before the stated thesis start date

`MAJOR` · `SCHEDULE`

The schedule states the thesis starts 26.06.2026, but Iteration 1 is dated 08.06.2026, which is before the stated start date. Align the iteration dates with the actual start date (or correct the stated start date) so the timeline is internally consistent.

- Page 9, Schedule: “The Bachelor thesis starts on 26.06.2026 and the submission is on 26.10.2026.”

### 4. Iteration 9 includes documentation and a vague deliverable

`MINOR` · `SCHEDULE`

Iteration 9 contains a bare 'Documentation' task and a vague 'Review the system and check for bugs' item. Per the guidelines, the schedule should not include documentation-writing tasks, and each item should be a measurable deliverable. Replace these with concrete development/research deliverables (or remove the documentation task).

- Page 10, Schedule: “Review the system and check for bugs”

### 5. AI transparency statement does not name the sections used

`MINOR` · `COMPLETENESS`

The AI transparency statement names the tools and their purposes well, but it groups them only by purpose ('grammar and style correction', 'content generation', etc.) and never specifies which sections of the proposal each tool was used in. The guidelines ask for the sections involved to be named. Add which parts of the proposal each tool was applied to.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across”

### 6. Motivation is practical rather than scientifically motivated

`MINOR` · `METHODOLOGY`

The Motivation section is largely practical (freeing supervisors' time, faster and more scalable feedback) and describes the envisioned LLM Reviewer and self-regulated-learning loop. It does not make the case for why this is scientifically important or novel. Add a sentence or two on the research/scientific contribution and importance of studying LLM-driven formative feedback on theses.

- Page 4, Motivation: “feedback could improve the thesis writing process.”
