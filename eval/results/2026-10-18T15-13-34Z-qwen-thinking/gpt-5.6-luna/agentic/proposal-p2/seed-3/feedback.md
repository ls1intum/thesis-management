# proposal-p2 — agentic

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 68
- **Findings:** 14
- **Cost:** 5 calls, 40 tool calls, depth 5, 74082 tokens, 31s
- **Coverage:** 9/9 categories

## Summary

The proposal has the required overall sections, four clearly action-oriented objectives with detailed subsections, two referenced diagrams (including an activity diagram), and a first-person AI statement naming tools and purposes. However, it needs substantial revision: the bibliography has only five entries instead of the required 6–8 peer-reviewed publications; the Problem, Motivation, and overall substantive text exceed the prescribed length limits; the AI statement does not identify the proposal sections in which tools were used; and the schedule contains dates before the stated thesis start, an excluded documentation task, and an insufficiently measurable final iteration. Tightening the prose, correcting the schedule, expanding the scholarly basis, and adding section-level AI-use details should bring it into compliance.

## Findings

### 1. Proposal exceeds the recommended text length

`MAJOR` · `COMPLETENESS`

The main proposal text extends beyond the guideline's 4–6-page target: excluding metadata, figures, and the schedule, the substantive text runs from pages 2 through 8/9. Condense the background and objective descriptions or remove nonessential material so the proposal fits the prescribed length.

- Page 8, Objective: “By  building    on  existing   processes,    the  integration    causes    the  least  disruption    to supervisors     and  delivers   the   greatest   simplifcation     of their  workfow.”

### 2. Problem section is too long

`MAJOR` · `STRUCTURE`

The Problem section spans approximately one full page (pages 3–4), exceeding the maximum of three-quarters of a page. Consolidate the explanation of repetitive feedback and its consequences into a shorter problem statement.

- Page 3, Problem: “Giving repetitive feedback is a draining activity for supervisors [Bog+07]. This   may    lead  to  reduced    concentration,      causing    supervisors     to  overlook    actual content    or conceptual     mistakes.”

### 3. Motivation section is too long

`MAJOR` · `STRUCTURE`

The Motivation section spans approximately one full page (pages 4–5), exceeding the maximum of three-quarters of a page. Keep the positive vision, but remove repeated background and compress the learning explanation.

- Page 4, Motivation: “Better and faster feedback has signifcant advantages for students as well. When     feedback    arrives   sooner,    students    can   act  on  it  while   the  writing    process is still fresh in their minds.”

### 4. Too few bibliography entries

`CRITICAL` · `COMPLETENESS`

The bibliography contains only five publication entries, below the required minimum of six to eight peer-reviewed publications. Add at least one, preferably several, relevant peer-reviewed sources and cite them where they support the proposal.

- Page 11, Bibliography: “[NM06]        D.  J.  Nicol  and   D.   Macfarlane-Dick,       “Formative      Assessment      and   Self-”

### 5. Forbidden sentence starters

`MINOR` · `WRITING`

The proposal repeatedly starts sentences with forbidden starters, including “As”, “Since”, and “Because”. Rewrite these sentences with a direct subject-led construction; for example, combine the figure reference with the preceding sentence or write “Guidelines may not already be in a suitable format.”

- Page 4, Motivation: “As   Figure 1 illustrates,   an  LLM-powered        Reviewer     works   alongside    the  supervisor,     evaluating the  thesis  against   the  research   group’s   writing   guidelines    and  generating     automatic formative feedback [NM06].”

### 6. Use more direct active voice

`MINOR` · `WRITING`

The document uses a passive construction that obscures the responsible actor. Prefer an active formulation that names the university or its community as the subject.

- Page 4, Motivation: “Each university often defnes its own guidelines for academic writing and document structure,    and   the  academic     community       has  not   standardized      these   conventions.”

### 7. Repeated filler transitions

`MINOR` · `WRITING`

Filler and additive transitions occur repeatedly (for example, “Moreover”, “Additionally”, and “In addition”). Remove them where the logical relation is already clear, or replace them with a precise relationship between claims.

- Page 3, Problem: “Moreover,     the  time   spent   on  repetitive    feedback                                                                                   leaves  less  time   for  what   matters    most:”

### 8. Motivation paragraph is too long

`MINOR` · `STRUCTURE`

This motivation paragraph continues across pages 4–5 and is substantially longer than the recommended 5–8 lines. Split it into separate coherent paragraphs, or shorten it so each paragraph develops one idea.

- Page 4, Motivation: “Better and faster feedback has signifcant advantages for students as well. When     feedback    arrives   sooner,    students    can   act  on  it  while   the  writing    process is still fresh in their minds. The student brings both knowledge, comprising theoretical    and   practical    domain     expertise,    and   beliefs   about   how    to  approach the task and how to work and write scientifcally,”

### 9. Standardize the system terminology

`SUGGESTION` · `WRITING`

The proposal uses several near-synonyms for the same system concept—“LLM-powered Reviewer”, “feedback tool”, and “Reviewer”. Choose one primary term and define any distinct component clearly, then use the terminology consistently.

- Page 4, Motivation: “As   Figure 1 illustrates,   an  LLM-powered        Reviewer     works   alongside    the  supervisor,     evaluating    the  thesis  against   the  research   group’s   writing   guidelines    and  generating     automatic formative feedback [NM06].”

### 10. Schedule includes a documentation task

`MAJOR` · `SCHEDULE`

Iteration 9 includes “Documentation”, which the schedule rules explicitly exclude. Replace it with a development or evaluation deliverable; documentation can be handled outside the iteration plan if required.

- Page 10, Schedule: “•   Documentation”

### 11. Final iteration lacks measurable deliverables

`MINOR` · `SCHEDULE`

Iteration 9's deliverables are not measurable enough: “Review the system and check for bugs” does not specify test coverage, acceptance criteria, or a concrete output. State what will be tested and what artifact or threshold completes the iteration.

- Page 10, Schedule: “•   Review    the  system    and   check   for  bugs”

### 12. AI statement does not identify proposal sections

`MAJOR` · `COMPLETENESS`

The AI transparency statement identifies tools and purposes and confirms review, but it does not state which proposal sections used each tool. Add section-level specificity (for example, identify where translation, content generation, coding, and citation assistance were applied).

- Page 12, Transparency in the Use of AI Tools: “In  preparing    this  proposal,     I made    use  of  several   AI-based     tools  across   diferent stages  of  the  writing   process.”

### 13. Schedule dates precede the stated thesis start

`MAJOR` · `LOGIC`

The schedule dates conflict with the stated thesis start date: the thesis starts on 26.06.2026, but Iteration 1 is dated 08.06.2026 and Iteration 2 is dated 22.06.2026. Align all iteration dates with the project start and submission dates.

- Page 9, Schedule: “The   Bachelor    thesis  starts   on  26.06.2026    and   the  submission     is on  26.10.2026.”

### 14. Iteration timeline is internally inconsistent

`MAJOR` · `SCHEDULE`

The schedule's first two iterations begin before the stated start date, making the planned work sequence internally inconsistent. Correct the iteration dates or correct the metadata and explain the discrepancy.

- Page 9, Schedule: “Iteration     1  (08.06.2026       -  2 weeks)”
