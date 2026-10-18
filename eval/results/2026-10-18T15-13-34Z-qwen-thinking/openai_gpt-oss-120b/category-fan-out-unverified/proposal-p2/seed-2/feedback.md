# proposal-p2 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 35
- **Findings:** 13
- **Cost:** 10 calls, 0 tool calls, depth 2, 90630 tokens, 59s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses an important problem and outlines clear objectives, but it suffers from major compliance failures. Critical issues include exceeding the page limit, insufficient bibliography, and prohibited schedule tasks, while numerous writing and structural shortcomings further undermine its readiness for submission.

## Findings

### 1. Proposal exceeds maximum page limit

`CRITICAL` · `structure`

The proposal text spans 12 pages, exceeding the allowed maximum of 4-6 pages for the main content.

- Page 12: “=== PAGE 12 ===”

### 2. Insufficient number of bibliography entries

`CRITICAL` · `completeness`

The bibliography contains only 5 entries, but the guidelines require at least 6‑8 peer‑reviewed publications.

- Page 11, Bibliography: “Bibliography ... [TP21] T. Tuononen ... [AAM17] P. Ashwin ... [BBE10] J. Bitchener ... [Bog+07] M. Bogo ... [NM06] D. J. Nicol ...”

### 3. Disallowed documentation task in final iteration

`CRITICAL` · `structure`

Iteration 9 includes a generic “Documentation” task, which is prohibited by the schedule guidelines (no thesis writing, documentation, or presentation preparation).

- Page 10, Schedule: “•   Documentation”

### 4. Objective subsections too short

`MAJOR` · `structure`

Each objective subsection (4.1–4.4) contains only a single paragraph; guidelines require at least two paragraphs with detailed descriptions.

- Page 5, Objective: “4.1 Generate Feedback Based on Guidelines ... The LLM may first”
- Page 6, Objective: “4.2 Highlight Changes Across Iterations ... more explanatory, more verbose, or providing concrete examples”
- Page 8, Objective: “4.3 Compare Prompting Techniques and LLM Models ... which of these options performs best in this setting.”
- Page 8, Objective: “4.4 Integrate With Thesis Management ... the feedback flow in Thesis Management only exists for the proposal”

### 5. Motivation repeats problem description

`MAJOR` · `writing`

The Motivation section restates problem details instead of focusing on the positive vision of the solution.

- Page 4, Motivation: “This Reviewer covers exactly the repetitive, non‑content‑related layer of feedback that currently **drains supervisors**, freeing them to focus on specific content and on discussions with their students.”

### 6. Missing section references in AI Transparency Statement

`MAJOR` · `completeness`

The AI Transparency Statement does not specify which sections of the proposal the AI tools were used for, as required by the guidelines.

- Page 12, Transparency in the Use of AI Tools: “**In preparing this proposal, I made use of several AI-based tools across different stages of the writing process.**”

### 7. Horizontal feature splits in schedule

`MAJOR` · `structure`

Iterations are organized around separate system layers rather than delivering end‑to‑end vertically integrated features.

- Page 9, Schedule: “•   Modularize     Thesis    Management       to  prepare    integration    of the  feedback    module (Objective    4)”
- Page 9, Schedule: “•   Implement      core  feedback    generation     pipeline   (Objective     1)”
- Page 9, Schedule: “•   Connect    feedback     tool  to Thesis    Management        (Objective    4)”
- Page 9, Schedule: “•   Allow   supervisors     to upload    custom     guidelines   (Objective     1)”
- Page 9, Schedule: “•   Build prototype implementations for agentic and prompt chaining setups (Objective    3)”
- Page 9, Schedule: “•   Build   prototype     implementations        for the   selected   LLM     models    and   providers (Objective    3)”

### 8. Passive voice constructions used

`MAJOR` · `writing`

Several sentences employ passive voice constructions that should be rewritten in active voice.

- Page 5, Objective: “Because the feedback tool **is powered** by LLMs, students can obtain instant formative feedback at any point during the writing process.”
- Page 6, Objective: “In one mode, no LLM-generated feedback **is produced** at all and all feedback comes from the supervisor.”
- Page 8, Objective: “Self-hosted models keep student drafts inside the university’s infrastructure but **are limited by** the available hardware.”

### 9. Forbidden sentence starters used

`MAJOR` · `writing`

Sentences begin with prohibited starters (As, Since, Because) contrary to writing style guidelines.

- Page 4, Motivation: “**As** Figure 1 illustrates, an LLM‑powered Reviewer works alongside the supervisor, evaluating the thesis against the research group’s writing guidelines...”
- Page 5, Objective: “**Since** guidelines may not already be in a suitable format, the LLM may first need to preprocess them.”
- Page 5, Objective: “**Because** the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 10. Use of first‑person pronoun "I"

`MAJOR` · `writing`

The proposal contains first‑person pronouns, which are forbidden by the writing style guidelines.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, **I** made use of several AI‑based tools across different stages of the writing process.”
- Page 12, Transparency in the Use of AI Tools: “For grammar and style correction, **I** used Grammarly to identify grammatical errors...”
- Page 12, Transparency in the Use of AI Tools: “**I** have carefully checked all texts created with these tools to ensure that they are correct and make sense.”

### 11. Paragraph length exceeds recommendation

`MAJOR` · `structure`

One or more paragraphs exceed the recommended 5–8 line length, making them overly long.

- Page 2, Abstract: “**Supervisors of theses spend considerable time on non-content-related feedback** such as document structure, coherence, and linguistic accuracy. Much of this feedback is repetitive across students and disciplines, draining supervisors and leaving less capacity for substantive research discussions.”
- Page 5, Objective: “**Supervisors should be able to provide their own scientific writing guidelines** as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover. Since guidelines may not already be in a suitable format, the LLM may first need to preprocess them into a structured representation it can act on.”

### 12. Objective list not formatted as bullet points

`MINOR` · `formatting`

The high‑level objectives in Section 4 are presented as a numbered list instead of a bullet list, and each objective spans multiple lines, violating formatting rules.

- Page 5, Objective: “1.  Generate    Feedback     Based    on  Guidelines
2.  Highlight    Changes     Across   Iterations
3.  Compare     Prompting      Techniques      and   LLM    Models
4.  Integrate   With    Thesis    Management”

### 13. Paragraph contains multiple unrelated ideas

`MINOR` · `writing`

A paragraph mixes ethical considerations with technical preprocessing requirements, violating the single‑idea‑per‑paragraph rule.

- Page 5, Objective: “**Ethically, it is difficult to let LLMs grade a final thesis**, as this constitutes normative feedback and requires close scrutiny. Formative feedback, on the other hand, is less critical since it serves as input for students to improve their work. The idea is that students interpret the feedback themselves... **The LLM needs to preprocess guidelines into a structured representation** it can act on.”
