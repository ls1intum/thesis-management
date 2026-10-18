# proposal-p2 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 12
- **Cost:** 10 calls, 0 tool calls, depth 2, 90218 tokens, 54s
- **Coverage:** 9/9 categories

## Summary

The proposal demonstrates a clear research direction and integrates AI tools thoughtfully, but it fails to meet several mandatory requirements. Critical issues include insufficient bibliography entries and a prohibited documentation task, while major problems involve length violations, missing detail in objectives, and style infractions. Substantial revisions are needed before submission.

## Findings

### 1. Proposal exceeds maximum allowed length

`MAJOR` · `structure`

The full document spans more than 6 pages of text (excluding schedule), which violates the 4‑6 page limit.

- Page 12: “=== PAGE 12 ===”

### 2. Problem and Motivation sections exceed allowed length

`MAJOR` · `structure`

Problem and Motivation sections each occupy more than the permitted 3/4 page.

- Page 3, Problem: “**Problem** section text exceeds the permitted length.”
- Page 4, Motivation: “**Motivation** section text exceeds the permitted length.”

### 3. Objective subsection 4.1 insufficient detail and length

`MAJOR` · `completeness`

Subsection 4.1 contains fewer than two paragraphs and lacks sufficient detailed description, violating the requirement for each objective subsection.

- Page 5, Objective: “**4.1 Generate Feedback Based on Guidelines**”
- Page 5, Objective: “**Supervisors should be able to provide their own scientific writing guidelines as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover. Since guidelines may not already be in a suitable format, the LLM may first need to preprocess them into a structured representation it can act on.**”

### 4. Insufficient number of bibliography entries

`CRITICAL` · `completeness`

The bibliography contains only 5 entries, but the guidelines require at least 6‑8 peer‑reviewed publications.

- Page 11, Bibliography: “Bibliography ... [TP21] ... [AAM17] ... [BBE10] ... [Bog+07] ... **[NM06]**.”

### 5. Forbidden sentence starters (As, Since, Because)

`MAJOR` · `writing`

Sentences begin with prohibited words 'As', 'Since', and 'Because', violating the writing style rule.

- Page 4, Motivation: “**As** Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group’s writing guidelines and generating automatic formative feedback [NM06].”
- Page 5, 4.1 Generate Feedback Based on Guidelines: “**Since** guidelines may not already be in a suitable format, the LLM may first ...”
- Page 5: “**Because** the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 6. Passive voice usage

`MAJOR` · `writing`

Passive constructions are used throughout the proposal, reducing clarity and contravening the active‑voice requirement.

- Page 4, Motivation: “While the problem **is less severe** for students, they still experience its consequences.”
- Page 5: “the feedback tool **is powered** by LLMs”
- Page 6, 4.2 Highlight Changes Across Iterations: “no LLM-generated feedback **is produced** at all”
- Page 6, 4.2 Highlight Changes Across Iterations: “Formative feedback, on the other hand, **is less critical** since it serves as input for students ...”

### 7. Paragraph exceeds recommended length

`MAJOR` · `formatting`

The abstract and introductory paragraphs exceed the recommended 5‑8 line length.

- Page 2, Abstract: “**Supervisors of theses spend considerable time on non-content-related feedback such as document structure, coherence, and linguistic accuracy.**”
- Page 2, Introduction: “**Bachelor’s and Master’s theses represent important final projects that students complete before graduating [TP21].**”

### 8. Paragraph too short

`MINOR` · `formatting`

A single-line paragraph containing only a URL is shorter than the minimum of 5 lines.

- Page 3, Problem: “**1https://github.com/ls1intum/thesis-management**”

### 9. Multiple ideas in a single paragraph

`MINOR` · `writing`

A paragraph in the Motivation section combines distinct ideas, mixing discussion of LLM functionality with separate student benefits.

- Page 4, Motivation: “**Integrating large language models (LLMs) to assist supervisors with non-content-related feedback could improve the thesis writing process. As Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor...** **Better and faster feedback has signifcant advantages for students as well.**”

### 10. Missing section specifications in AI transparency statement

`MAJOR` · `other`

The AI transparency statement does not specify which sections of the proposal the AI tools were used for; it only refers vaguely to "drafts" and "throughout the proposal" instead of naming concrete sections.

- Page 12, Transparency in the Use of AI Tools: “I made use of several AI-based tools across different stages of the writing process... For citation assistance, I used Zotero to manage my references and to ensure proper citation formatting **throughout the proposal**. I have carefully checked all texts created with these tools to ensure that they are correct and make sense.”

### 11. Prohibited documentation task in schedule

`CRITICAL` · `other`

Iteration 9 includes a documentation writing task, which is not allowed in the schedule according to the group rules.

- Page 10, Schedule: “•   Documentation”

### 12. Schedule lacks vertically integrated features

`MAJOR` · `logic`

Schedule items describe horizontal component work rather than vertically integrated, end‑to‑end user‑visible features, violating the vertically integrated feature requirement.

- Page 9, Schedule: “•   Modularize Thesis Management to prepare integration of the feedback module (Objective 4)”
- Page 9, Schedule: “•   Implement core feedback generation pipeline (Objective 1)”
- Page 9, Schedule: “•   Connect feedback tool to Thesis Management (Objective 4)”
- Page 9, Schedule: “•   Allow supervisors to upload custom guidelines (Objective 1)”
