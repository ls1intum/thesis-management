# proposal-p2 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** ACCEPTABLE
- **Score:** 63
- **Findings:** 6
- **Cost:** 1 calls, 0 tool calls, depth 1, 20949 tokens, 162s
- **Coverage:** 9/9 categories

## Summary

The proposal is well-structured with clear objectives, two informative UML figures, and a coherent narrative from problem through motivation to a concrete integration plan. The main weaknesses are a critically short bibliography (5 of the required 6–8 entries), an over-length Problem section, a documentation task that should not appear in the schedule, and a few style issues (forbidden sentence starters, inconsistent system naming). Addressing the bibliography gap and trimming the Problem section will bring the proposal to a submittable state.

## Findings

### 1. Bibliography contains only 5 publications (minimum 6–8 required)

`CRITICAL` · `citation`

The bibliography lists only 5 entries ([TP21], [AAM17], [BBE10], [Bog+07], [NM06]). The guidelines require at least 6–8 peer-reviewed publications. Add at least 1–3 additional relevant peer-reviewed sources (e.g., on LLM-based educational feedback, prompt engineering, or self-regulated learning in writing).

- Page 11, Bibliography: “**[TP21] T. Tuononen and A. Parpala, … [AAM17] P. Ashwin, … [BBE10] J. Bitchener, … [Bog+07] M. Bogo, … [NM06] D. J. Nicol and D. Macfarlane-Dick**”

### 2. Problem section exceeds the 3/4-page length limit

`MAJOR` · `structure`

The Problem section (Section 2) spans approximately one full page of text (from the '2 Problem' heading on page 3 to the '3 Motivation' heading on page 4). The guidelines cap this section at 3/4 page. Condense the description of actors and consequences, merging the supervisor and student impact paragraphs or trimming redundant phrasing.

- Page 3, Problem: “**For students' research proposals and theses, supervisors often provide feedback that is critical to their progress.**”
- Page 4, Problem: “**While the problem is less severe for students, they still experience its consequences.**”

### 3. Schedule includes a documentation/thesis-writing task

`MAJOR` · `structure`

Iteration 9 lists 'Documentation' as a work item. The guidelines explicitly state that thesis writing, documentation writing, and presentation preparation tasks must not appear in the schedule; the schedule should focus on development/research work only. Remove this item or replace it with a concrete engineering task (e.g., 'Perform regression testing and fix identified bugs in the feedback pipeline').

- Page 10, Schedule: “Iteration 9 (05.10.2026 - 3 weeks) • Review the system and check for bugs • **Documentation**”

### 4. Three sentences begin with forbidden starters (As, Since, Because)

`MINOR` · `writing`

The guidelines prohibit sentences starting with 'As…', 'Since…', or 'Because…'. Three occurrences were found: 'As Figure 1 illustrates…' (p. 4), 'Since guidelines may not already be in a suitable format…' (p. 5), and 'Because the feedback tool is powered by LLMs…' (p. 5). Rewrite each sentence to start with the subject or a different structure.

- Page 4, Motivation: “**As** Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor”
- Page 5, Objective: “**Since** guidelines may not already be in a suitable format, the LLM may first need to preprocess them”
- Page 5, Motivation: “**Because** the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point”

### 5. Inconsistent terminology for the LLM-based system

`MINOR` · `formatting`

The system under development is referred to as 'the tool' (Abstract), 'an LLM-powered Reviewer' / 'an LLM Reviewer' (Motivation, Figure 1), 'the LLM' (Objective 4.1), 'the feedback tool' (Objectives 4.2–4.4, Figure 2), and 'the feedback tool' (Schedule). Choose one primary term (e.g., 'the feedback tool') and use it consistently throughout the prose, reserving 'Reviewer' only for the Figure 1 entity label if desired.

- Page 2, Abstract: “Beyond a single draft, **the tool** also compares iterations to highlight improvements”
- Page 4, Motivation: “an LLM-powered **Reviewer** works alongside the supervisor”
- Page 6, Objective: “When **the feedback tool** has access to previous iterations of the proposal or thesis”
- Page 5, Objective: “Based on these guidelines, **the LLM** can provide formative feedback”

### 6. AI Transparency Statement does not specify which sections the tools were applied to

`MINOR` · `completeness`

The statement lists tools and general purposes (grammar, translation, content generation, coding, citations) but states only 'across different stages of the writing process' without naming specific proposal sections. The guidelines require the statement to specify which sections of the proposal the AI tools were used for (e.g., 'I used Grammarly primarily in the Introduction and Problem sections; I used ChatGPT to expand ideas in the Motivation and Objective sections').

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools **across different stages of the writing process**.”
