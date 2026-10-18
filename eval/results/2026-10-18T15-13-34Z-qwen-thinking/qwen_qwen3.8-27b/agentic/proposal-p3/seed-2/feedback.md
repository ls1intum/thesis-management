# proposal-p3 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** GOOD
- **Score:** 90
- **Findings:** 3
- **Cost:** 9 calls, 20 tool calls, depth 9, 214346 tokens, 368s
- **Coverage:** 9/9 categories

## Summary

A strong, complete proposal. All required sections are present; the Problem is actor-focused and solution-free; the Motivation is visionary and grounded in the literature; the four objectives are in clean action form (no double verbs) with two-paragraph subsections; the schedule is six 4-week, vertically integrated, measurable iterations that reference the objectives with no requirements or writing sprints; the three figures are all suitable UML diagrams (use-case, object, component — no sequence diagrams), each referenced in text with detailed captions; the bibliography has 12 peer-reviewed entries in a consistent alpha style placed before the full stop; and the AI transparency statement is specific, first-person, and confirms the author checked all AI-generated text. No critical or major issues. Three minor polish items: (1) the Bruegge & Dutoit textbook is cited only via a bookstore-URL footnote instead of a proper bibliography entry; (2) the body uses first-person "I" throughout where the group rule prefers "we" for the thesis approach; (3) the same servers are called "compute hosts" in the Abstract/Problem but "Linux hosts" in the Introduction.

## Findings

### 1. Bruegge & Dutoit book cited only via a bookstore-URL footnote, not a bibliography entry

`MINOR` · `CITATION`

The Bruegge & Dutoit textbook is cited in the Objective section but is supported only by a bookstore-URL footnote (footnote 3 = a pearson.com/store product link), not by a bibliography entry. A scientific book belongs in the Bibliography with full details (authors, title, edition, publisher, year) and should be cited with an alpha-style key like the other references, not left as a documentation/URL footnote. Add a proper bibliography entry and cite it as [BD..].

- Page 4, 4 Objective: “Following the object-oriented software engineering method of **Bruegge and Dutoit** 3, I pursue four objectives:”

### 2. First-person "I" used throughout the body where the rule prefers "we"

`MINOR` · `WRITING`

The group's writing-style rule says to avoid "one", "I" and "our", and to use "we" (sparingly) only for the thesis approach. The proposal instead uses first-person "I" pervasively across the body (Abstract, Section 4, and each 4.x subsection via "I reach this objective when..."). Switch the thesis-approach statements to "we" (or an impersonal active voice naming the platform as actor) to comply with the rule. The AI transparency statement may keep first person, as required there.

- Page 4, 4 Objective: “In this thesis, **I develop** a self-service platform on which requesters ask for time-limited access, maintainers decide, and access ends on its own”

### 3. Inconsistent term for the same servers: "compute hosts" vs "Linux hosts"

`MINOR` · `FORMATTING`

The same shared servers are named "compute hosts" in the Abstract and Problem section but "Linux hosts" in the Introduction ("Linux hosts with graphics processors ..."). Pick a single term for this concept and use it consistently throughout the proposal.

- Page 2, Abstract: “A university research chair grants access to its **compute hosts**, chat workspace, and code repositories by hand, while the people who need that access change every semester.”
