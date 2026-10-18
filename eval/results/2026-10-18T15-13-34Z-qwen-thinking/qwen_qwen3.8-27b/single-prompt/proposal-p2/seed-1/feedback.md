# proposal-p2 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** ACCEPTABLE
- **Score:** 60
- **Findings:** 6
- **Cost:** 1 calls, 0 tool calls, depth 1, 22253 tokens, 198s
- **Coverage:** 9/9 categories

## Summary

The proposal is well-structured with all required sections, a clear and well-motivated problem, properly formulated objectives with detailed subsections, and two well-captioned UML diagrams. The main weaknesses are a short bibliography (5 references instead of the required 6–8), a schedule date inconsistency, and a forbidden documentation task in the last iteration. Once these are corrected along with minor writing fixes, the proposal will be submission-ready.

## Findings

### 1. Bibliography contains only 5 publications (minimum 6 required)

`CRITICAL` · `citation`

The bibliography lists only 5 references ([TP21], [AAM17], [BBE10], [Bog+07], [NM06]), but the guidelines require at least 6–8 peer-reviewed publications. At least one additional relevant, peer-reviewed source must be added (e.g., a paper on LLM-based feedback generation, automated essay scoring, or NLP for academic writing assessment).

- Page 11, Bibliography: “[TP21] T. Tuononen and A. Parpala, … [AAM17] P. Ashwin, … [BBE10] J. Bitchener, … [Bog+07] M. Bogo, … [NM06] D. J. Nicol and D. Macfarlane-Dick, …”

### 2. Schedule date inconsistency: first iteration starts before thesis start date

`MAJOR` · `logic`

The proposal states the thesis starts on 26.06.2026, but Iteration 1 is dated 08.06.2026, which is 18 days earlier. This is a clear inconsistency. The iteration dates should be aligned with the stated start date (e.g., Iteration 1 should begin on or after 26.06.2026).

- Page 9, Schedule: “The Bachelor thesis starts on **26.06.2026** and the submission is on 26.10.2026.”
- Page 9, Schedule: “Iteration 1 (**08.06.2026** - 2 weeks)”

### 3. Schedule includes a documentation-writing task

`MAJOR` · `structure`

Iteration 9 lists 'Documentation' as a work item. The guidelines explicitly state that thesis writing, documentation writing, and presentation preparation tasks must not appear in the schedule. Remove this item or replace it with a concrete development/testing deliverable.

- Page 10, Schedule: “Iteration 9 (05.10.2026 - 3 weeks) • Review the system and check for bugs • **Documentation**”

### 4. Three sentences begin with forbidden sentence starters

`MINOR` · `writing`

The guidelines prohibit sentences starting with 'As…', 'Since…', or 'Because…'. Three instances were found: (1) 'As Figure 1 illustrates…' (page 4), (2) 'Since guidelines may not already be in a suitable format…' (page 5), (3) 'Because the feedback tool is powered by LLMs…' (page 5). Reword each to start with the subject or another permissible word.

- Page 4, Motivation: “**As Figure 1 illustrates**, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group's writing guidelines”
- Page 5, Objective: “**Since guidelines may not already be in a suitable format**, the LLM may first need to preprocess them into a structured representation it can act on.”
- Page 5, Motivation: “**Because the feedback tool is powered by LLMs**, students can obtain instant formative feedback at any point during the writing process.”

### 5. Inconsistent terminology: 'Reviewer' vs. 'feedback tool'

`MINOR` · `formatting`

The Motivation section introduces the component as 'an LLM-powered Reviewer' / 'the Reviewer' (capitalised, proper name), while the Objectives section consistently calls it 'the feedback tool' (lowercase, generic). Pick one term and use it throughout to avoid confusing the reader about whether two different things are being described.

- Page 4, Motivation: “an LLM-powered **Reviewer** works alongside the supervisor”
- Page 6, Objective: “the **feedback tool** should adapt its style”

### 6. AI Transparency Statement lacks section-specific references

`MINOR` · `completeness`

The statement describes tools and purposes well but does not specify which sections of the proposal each tool was applied to (e.g., 'I used Grammarly on the Problem and Motivation sections' or 'I used ChatGPT to expand the Motivation section'). The guidelines require the statement to name the sections involved.

- Page 12, Transparency in the Use of AI Tools: “For grammar and style correction, I used Grammarly to identify grammatical errors, improve sentence structure, and refine the overall writing style of **my drafts**.”
