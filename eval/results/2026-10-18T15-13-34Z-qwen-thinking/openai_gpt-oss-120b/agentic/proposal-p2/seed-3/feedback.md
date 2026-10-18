# proposal-p2 — agentic

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** —
- **Score:** —
- **Findings:** 10
- **Cost:** 36 calls, 35 tool calls, depth 36, 640996 tokens, 95s
- **Coverage:** 9/9 categories

## Summary

_none_

## Findings

### 1. Excessive total length

`CRITICAL` · `STRUCTURE`

Proposal exceeds the allowed total length of 4-6 pages; the document spans 12 pages including title and appendix.

- Page 12: “=== PAGE 12 ===”

### 2. Insufficient bibliography entries

`CRITICAL` · `CITATION`

Bibliography contains only 5 entries, fewer than the required minimum of 6-8 peer-reviewed publications.

- Page 11, Bibliography: “[TP21]        T. Tuononen and A. Parpala, “The Role of Academic Competences
                                   and   Learning     Processes    in  Predicting     Bachelor's    and   Master's     Thesis
                                   Grades,”     Studies   in  Educational      Evaluation,     vol.  70,  p.  101001,    2021,”

### 3. Improper inline URL

`MAJOR` · `CITATION`

A URL is included inline in the body text instead of as a footnote, violating the rule that non‑peer‑reviewed sources must be footnotes.

- Page 3: “1https://github.com/ls1intum/thesis-management”

### 4. Passive voice

`MAJOR` · `WRITING`

Passive voice used: "the feedback tool is powered by LLMs".

- Page 5: “because the feedback tool is powered by llms, students can obtain instant formative feedback at any point during the writing process.”

### 5. Filler word

`MINOR` · `WRITING`

Filler word "Additionally" used, which should be avoided.

- Page 4: “Additionally, supervisors can provide feedback faster, making the process more scalable.”

### 6. Forbidden sentence starter

`MAJOR` · `WRITING`

Sentence starts with forbidden word "As".

- Page 4: “As   Figure 1 illustrates,   an  LLM-powered        Reviewer works   alongside    the  supervisor,     evaluating the thesis”

### 7. Forbidden sentence starter

`MAJOR` · `WRITING`

Sentence starts with forbidden word "Because".

- Page 5: “because the feedback tool is powered by llms, students can obtain instant formative feedback at any point during the writing process.”

### 8. Insufficient paragraph count in objective subsection

`MAJOR` · `METHODOLOGY`

Objective subsections (e.g., 4.1) contain only one paragraph; each should have at least two paragraphs.

- Page 5, 4.1 Generate Feedback Based on Guidelines: “Supervisors should be able to provide their own scientifc writing guidelines as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover. Since guidelines may not already be in a suitable format, the LLM may frst”

### 9. Vague schedule item

`MAJOR` · `SCHEDULE`

Iteration 9 includes a vague deliverable "Documentation" without measurable specifics.

- Page 10, Iteration 9: “•   Documentation”

### 10. Short figure caption

`MINOR` · `FIGURES`

Figure 2 caption is short and generic; captions should be long and informative.

- Page 7: “Figure 2: Activity diagram of the envisioned feedback workfow.”
