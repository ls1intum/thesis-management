# proposal-p2 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 60
- **Findings:** 7
- **Cost:** 12 calls, 36 tool calls, depth 12, 274514 tokens, 374s
- **Coverage:** 9/9 categories

## Summary

The proposal is well organized and reads cleanly: all required sections are present, section lengths fit the limits, the Problem stays solution-free with clear actors, the Motivation is visionary, all four objectives are in action form with single verbs and adequately developed subsections, both figures (object model and UML activity diagram) are referenced and well-captioned, and the AI transparency statement is specific and includes the required review confirmation. Two real defects keep it from being acceptable: the bibliography contains only 5 references, below the required 6-8 peer-reviewed publications (CRITICAL), and the schedule's iterations start on 08.06.2026 — before the stated start date of 26.06.2026 — so the iteration dates and cover-page dates are inconsistent (MAJOR). Remaining items are minor: three sentences open with forbidden Since/Because/As starters, repeated filler connectors (Moreover/Additionally/In addition), inconsistent naming of the tool as "Reviewer" vs "feedback tool", an Iteration 9 without measurable deliverables (including a bare "Documentation" task), and an AI statement that never names the sections where the tools were used. Fix the bibliography and schedule dates first, then the minor edits, and the proposal will be in good shape.

## Findings

### 1. Schedule iterations start before the stated thesis start date

`MAJOR` · `LOGIC`

Iteration 1 is dated 08.06.2026, which is before the stated start date of 26.06.2026, and the nine iterations span 08.06.2026–26.10.2026 (20 weeks) instead of the 18-week window from the cover page dates (26.06.2026–26.10.2026). Either the cover-page start date or all iteration dates are wrong. Fix the dates so the iterations start on or after the start date and the last iteration ends by the submission date.

- Page 9, Schedule: “Iteration 1 (08.06.2026 - 2 weeks)”

### 2. Bibliography below the required 6-8 publications

`CRITICAL` · `CITATION`

The bibliography contains only 5 references (TP21, AAM17, BBE10, Bog+07, NM06), but the guidelines require at least 6-8 peer-reviewed publications. Add at least one or two more peer-reviewed sources; work on LLM-based feedback generation or automated writing assessment, which the proposal builds on in Section 4.3, would be a natural addition.

- Page 11, Bibliography: “Studies in Higher Education, vol. 31, no. 2, pp. 199–218,”

### 3. Repeated filler connectors (Moreover / Additionally / In addition)

`MINOR` · `WRITING`

Discourse-connector filler words recur: "Moreover," (Section 2, page 3), "Additionally," (Section 3, page 4) and "In addition," (Section 4.3, page 8). Vary the structure and cut at least two of these; they add no information.

- Page 3, Problem: “Moreover, the time spent on repetitive feedback leaves less time for what matters most: helping students with their research and providing feedback on substantive topics.”

### 4. Inconsistent terminology: "Reviewer" vs "feedback tool"

`MINOR` · `FORMATTING`

The same concept is named inconsistently: Section 3 calls the tool "an LLM-powered Reviewer" (and Figure 1 labels it "an LLM Reviewer"), while Sections 4.1, 4.2 and 4.4 call it "the feedback tool". Pick one term (e.g. "the feedback tool") and use it throughout, including in the figure.

- Page 4, Motivation: “an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group’s writing guidelines and generating automatic formative feedback”

### 5. Iteration 9 lacks measurable deliverables

`MINOR` · `COMPLETENESS`

Iteration 9 contains no measurable deliverable: "Review the system and check for bugs" is vague and "Documentation" is a bare documentation-writing task, which should not appear in the schedule. Replace with concrete, verifiable outcomes (e.g. a documented bug list with resolved fixes, or a specific release criterion).

- Page 10, Schedule: “Review the system and check for bugs”

### 6. AI transparency statement does not name the sections where tools were used

`MINOR` · `OTHER`

The AI transparency statement names tools and purposes well and includes the required review-confirmation sentence, but it never states which sections of the proposal the tools were used in (it only says "across different stages of the writing process"). Add the specific sections (e.g. "in the Introduction and Motivation sections for phrasing, in Section 5 for no AI use, for the figure macros...").

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools”

### 7. Sentences start with forbidden "Since"/"Because"/"As"

`MINOR` · `WRITING`

Three sentences begin with forbidden starters: "Since guidelines may not already be in a suitable format..." (Section 4.1, page 5), "Because the feedback tool is powered by LLMs..." (page 5), and "As Figure 1 illustrates..." (Section 3, page 4). Restructure these sentences so they do not open with Since/Because/As, e.g. "Guidelines may not already be in a suitable format, so the LLM may first need to preprocess them...".

- Page 5, 4.1 Generate Feedback Based on Guidelines: “Since guidelines may not already be in a suitable format”
