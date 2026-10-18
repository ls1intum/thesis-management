# proposal-p2 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** ACCEPTABLE
- **Score:** 70
- **Findings:** 12
- **Cost:** 16 calls, 36 tool calls, depth 16, 434026 tokens, 460s
- **Coverage:** 9/9 categories

## Summary

Well-organized proposal that hits all structural requirements: all required sections present, section lengths within limits, objectives in action form with one-verb phrasing and two-paragraph subsections, two UML figures with informative captions that are both referenced in the text, consistent alpha citation style with citations before full stops, and a 2-3 week iteration schedule that references the objectives. Main problems: the bibliography has only 5 peer-reviewed references instead of the required 6-8 (CRITICAL - easy to fix by adding 2-3 papers on LLM feedback/evaluation and citing them, e.g. in Section 4.3); the schedule's Iteration 1 (08.06.2026) starts 18 days before the stated thesis start date (26.06.2026); and Iteration 9 contains a bare "Documentation" task, which schedules must not include. Smaller issues: three forbidden sentence starters (As/Since/Because), three over-long paragraphs (>10 lines) in Motivation and 4.1, inconsistent naming of the tool ("Reviewer" vs "feedback tool"), and the AI transparency statement does not say which sections of the proposal the tools were used in. All issues are concrete and quickly fixable.

## Findings

### 1. Schedule starts before the stated thesis start date

`MAJOR` · `LOGIC`

The schedule is internally inconsistent with the stated thesis start date. The Schedule section and the title page both give the start date as 26.06.2026, but Iteration 1 is dated 08.06.2026, which is 18 days before the thesis begins. Either the start date or the iteration dates are wrong; correct one of them so that Iteration 1 starts on or after 26.06.2026 (and re-check that the last iteration still ends at the 26.10.2026 submission date).

- Page 9, Schedule: “The Bachelor thesis starts on 26.06.2026 and the submission is on 26.10.2026.”

### 2. Bibliography has only 5 references; at least 6-8 are required

`CRITICAL` · `CITATION`

The bibliography contains only 5 entries ([TP21], [AAM17], [BBE10], [Bog+07], [NM06]), but the guidelines require at least 6-8 peer-reviewed publications. All five entries are journal articles, so the fix is to add 2-3 more peer-reviewed references relevant to the work, e.g. empirical studies of LLM-based writing feedback or LLM evaluation/benchmarking, and cite them at the appropriate points (e.g. in Section 4.3 for the prompting-technique and model comparison).

- Page 11, Bibliography: “[NM06]        D.  J.  Nicol  and   D.   Macfarlane-Dick,       “Formative      Assessment      and   Self-”

### 3. Schedule includes a documentation task, which is not allowed

`MAJOR` · `METHODOLOGY`

Iteration 9 contains a bare "Documentation" work item. Schedules must focus on development/research work and should not include documentation-writing tasks, and every item should be a measurable deliverable. Replace it with a concrete, measurable task, e.g. an end-to-end test pass of the feedback workflow in Thesis Management or a usability check with a pilot group.

- Page 10, Schedule: “Documentation”

### 4. Core pipeline scheduled before guideline input (horizontal split)

`MINOR` · `METHODOLOGY`

The schedule splits Objective 1 horizontally: the "core feedback generation pipeline" is implemented in Iteration 2, but the ability to supply custom supervisor guidelines — the primary input of objective 1 ("Generate Feedback Based on Guidelines") — only arrives in Iteration 4, two iterations later. Prefer vertically integrated iterations: in Iteration 2, have the pipeline accept guidelines in some minimal form so each iteration ends with an end-to-end usable feature, and keep Iteration 4 for the full upload/preprocessing workflow.

- Page 9, Schedule: “Implement      core feedback      generation     pipeline   (Objective    1)”

### 5. Sentence starts with forbidden “As”

`MINOR` · `WRITING`

This sentence starts with "As…", which is forbidden. Rewrite, e.g. "Figure 1 illustrates how an LLM-powered Reviewer works alongside the supervisor…"

- Page 4, Motivation: “As Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group’s writing guidelines”

### 6. Sentence starts with forbidden “Since”

`MINOR` · `WRITING`

This sentence starts with "Since…", which is forbidden. Rewrite, e.g. "Guidelines may not already be in a suitable format, so the LLM may first need to preprocess them…"

- Page 5, Objective: “Since guidelines may not already be in a suitable format, the LLM may frst”

### 7. Sentence starts with forbidden “Because”

`MINOR` · `WRITING`

This sentence starts with "Because…", which is forbidden. Rewrite, e.g. "The feedback tool is powered by LLMs, so students can obtain instant formative feedback at any point during the writing process."

- Page 5, Motivation: “Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 8. Over-long paragraph in Motivation (about 11 lines)

`MINOR` · `WRITING`

This paragraph runs to about 11 rendered lines, exceeding the 10-line maximum. It covers three separate ideas: what the LLM Reviewer does, the benefit for supervisors' time, and scalability. Split it — e.g. end the first paragraph after the [NM06] sentence and start a new paragraph with the supervisor-benefit points.

- Page 4, Motivation: “content-related feedback could improve the thesis writing process. As Figure 1”

### 9. Over-long paragraph in Motivation (about 12 lines)

`MINOR` · `WRITING`

This paragraph spans about 12 rendered lines (across pages 4-5), exceeding the 10-line maximum, and bundles several ideas: faster feedback benefiting students, the student's prior knowledge and beliefs, self-regulated learning, and the availability of instant feedback. Split it into two paragraphs — one on the student side of self-regulated learning, one on the LLM-powered availability of instant feedback.

- Page 4, Motivation: “Better and faster feedback has signifcant advantages for students as well.”

### 10. Over-long paragraph in Section 4.1 (about 11 lines)

`MINOR` · `WRITING`

This paragraph runs to about 11 rendered lines (across pages 5-6), exceeding the 10-line maximum, and combines two ideas: generating feedback restricted to the supplied guidelines (including preprocessing them) and the ethical distinction between formative and normative feedback. Split it after the preprocessing sentence.

- Page 5, Objective: “well as conventions on the style of theses and proposals. Based on these guidelines,”

### 11. Inconsistent naming: “Reviewer” vs. “feedback tool”

`MINOR` · `FORMATTING`

The central artefact of the thesis is called the "LLM-powered Reviewer" / "LLM Reviewer" in the Motivation and in the Figure 1 caption, but "the feedback tool" everywhere else (Figure 2 caption, Sections 4.1-4.4, and the Schedule). Pick one name and use it consistently throughout the proposal.

- Page 4, Motivation: “an LLM-powered Reviewer works alongside the supervisor, evaluating”

### 12. AI transparency statement does not specify which sections the tools were used in

`MINOR` · `COMPLETENESS`

The transparency statement names the tools and their purposes, but does not say which sections of the proposal the AI tools were used on. Add a sentence (or short list) mapping tools to the sections they were used in, e.g. which sections were grammar-checked with Grammarly and where ChatGPT/Claude were used for idea expansion, so the statement is specific to this proposal's sections.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across diferent”
