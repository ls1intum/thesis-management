# proposal-p2 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 16
- **Cost:** 10 calls, 0 tool calls, depth 2, 119520 tokens, 224s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a well-motivated research topic with clear objectives and a coherent overall narrative, but it contains two critical violations (insufficient bibliography and an invalid schedule start date) alongside nine major structural and writing issues. Paragraph-length rules are frequently breached, the schedule includes prohibited tasks and non-measurable deliverables, and several explicit writing rules (forbidden sentence starters, undefined abbreviations) are not met. The core research content is sound and on track, but the document requires substantial editing and restructuring before it is submission-ready.

## Findings

### 1. Bibliography has only 5 publications; minimum of 6 required

`CRITICAL` · `citation`

The bibliography contains only 5 entries ([TP21], [AAM17], [BBE10], [Bog+07], [NM06]). The guidelines require at least 6–8 peer-reviewed publications. At least one additional peer-reviewed publication must be added to the bibliography and cited in the text.

- Page 11, Bibliography: “**[TP21]** T. Tuononen and A. Parpala … **[NM06]** D. J. Nicol and D. Macfarlane-Dick”

### 2. Iteration 1 start date precedes the official thesis start date

`CRITICAL` · `logic`

The proposal states the thesis starts on 26.06.2026, but Iteration 1 begins on 08.06.2026 — 18 days before the official start. This is a factual inconsistency that makes the schedule invalid. All iteration dates should be aligned so that the first iteration begins on or after 26.06.2026 and the final iteration ends on or before 26.10.2026.

- Page 9, Schedule: “The Bachelor thesis starts on 26.06.2026 and the submission is on 26.10.2026. We divided the time into several two-week iterations”
- Page 9, Schedule: “Iteration 1 (**08.06.2026** - 2 weeks)”

### 3. Forbidden sentence starters: 'As…', 'Because…', 'Since…'

`MAJOR` · `writing`

Three sentences begin with forbidden conjunctions. (1) Page 4: 'As Figure 1 illustrates…' — rewrite as 'Figure 1 illustrates how…'. (2) Page 4: 'Because the feedback tool is powered by LLMs…' — restructure to 'The LLM-powered feedback tool allows students to obtain instant formative feedback…'. (3) Page 5: 'Since guidelines may not already be in a suitable format…' — restructure to 'Guidelines may not already be in a suitable format, so the LLM may first need to preprocess them…'.

- Page 4, Motivation: “**As Figure 1** illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group's writing guidelines”
- Page 4, Motivation: “**Because the feedback** tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”
- Page 5, Objective: “**Since guidelines** may not already be in a suitable format, the LLM may first need to preprocess them into a structured representation it can act on.”

### 4. Paragraph too short (1–2 lines)

`MAJOR` · `structure`

The paragraph consisting solely of the sentence 'The student, by contrast, hands in the thesis itself as a single final submission.' is only 1–2 lines long, well below the 5–8 line minimum. It should be merged with the preceding paragraph or expanded with additional context.

- Page 3, Introduction: “**The student, by contrast, hands in the thesis itself as a single final submission.**”

### 5. Paragraph exceeds 10 lines (≈12 lines) in Motivation section

`MAJOR` · `structure`

The paragraph beginning with 'Better and faster feedback has significant advantages for students as well' spans pages 4–5 and is approximately 12 rendered lines, exceeding the 10-line maximum. It should be split into two paragraphs, e.g. one on the student's knowledge/beliefs and one on self-regulated learning with the Reviewer's feedback.

- Page 4, Motivation: “**Better and faster feedback has signifcant advantages for students as well. When feedback arrives sooner, students can act on it while the writing process is still fresh in their minds. The student brings both knowledge, comprising theoretical and practical domain expertise, and beliefs about how to approach the task and how to work and write scientifcally, which include the student's personal interpretation of the writing guidelines. Drawing on this knowledge and these beliefs, the student writes the thesis. Through self-regulated learning, the student engages with the Reviewer's feedback, refecting on it independently or** discussing it with the supervisor, and uses it to improve their knowledge, beliefs, and broader strategy for proceeding with the work [NM06]. Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 6. Paragraph exceeds 10 lines and conflates multiple unrelated ideas in Objective 4.1

`MAJOR` · `structure`

The first paragraph in Section 4.1 spans pages 5–6 (approximately 11 rendered lines) and conflates at least four distinct ideas: (1) supervisors providing guidelines, (2) LLM feedback restricted to guideline scope, (3) preprocessing guidelines into a structured format, and (4) the ethical distinction between formative and normative feedback. It should be split into at least two paragraphs: one on guideline-based feedback generation and preprocessing, and one on the ethical rationale for formative (rather than normative) feedback.

- Page 5, Objective / 4.1 Generate Feedback Based on Guidelines: “**Supervisors should be able to provide their own scientifc writing guidelines as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover. Since guidelines may not already be in a suitable format, the LLM may frst** need to preprocess them into a structured representation it can act on. **Ethically, it is difcult to let LLMs grade a fnal thesis, as this constitutes normative feedback and requires close scrutiny. Formative feedback, on the other hand, is less critical since it serves as input for students to improve their work. The idea is that students interpret the feedback themselves and discuss it, either independently or with their supervisor. Therefore, it is less important that every output of the LLM is perfectly correct.**”

### 7. AI Transparency Statement is a single 14-line paragraph covering multiple tools and purposes

`MAJOR` · `structure`

The entire AI Transparency Statement on page 12 is one continuous paragraph of approximately 14 rendered lines, exceeding the 10-line maximum. It also bundles five distinct topics (grammar correction, translation, content generation, coding assistance, citation management) into a single paragraph. Splitting it into 2–3 shorter paragraphs (e.g., writing/translation tools, content/coding tools, citation and verification) would improve readability and comply with the paragraph-length guideline.

- Page 12, AI Transparency Statement: “**In preparing this proposal, I made use of several AI-based tools across diferent stages of the writing process. For grammar and style correction, I used Grammarly to identify grammatical errors, improve sentence structure, and refne the overall writing style of my drafts. For translation and language enhancement, I used DeepL to translate passages between German and English and to improve the fuency of phrasings. For content generation and idea expansion, I used ChatGPT, OpenAI Codex, Claude, and Claude Code to reformulate sentences I had already drafted, expand on ideas I had outlined, and obtain suggestions for clearer phrasing. For coding assistance, I used GitHub Copilot, ChatGPT, OpenAI Codex, Claude, and Claude Code to support Typst formatting, generate snippets for fgures and macros, and resolve template-related issues. For citation assistance, I used Zotero to manage my references and to ensure proper citation formatting throughout the proposal. I have carefully checked all texts created with these tools to ensure that they are correct and make sense.**”

### 8. Abbreviation 'AI' used without prior definition

`MAJOR` · `writing`

The abbreviation 'AI' appears in the AI Transparency Statement ('AI-based tools', 'AI-generated') but is never defined as 'artificial intelligence (AI)' earlier in the document. Per the guidelines, abbreviations must be defined before first use.

- Page 12, AI Transparency Statement: “In preparing this proposal, I made use of several **AI**-based tools across different stages of the writing process.”

### 9. Documentation task included in the schedule

`MAJOR` · `methodology`

Rule 5 states that the schedule should NOT include thesis writing, documentation writing, or presentation preparation tasks. Iteration 9 lists 'Documentation' as a work item. The schedule should focus exclusively on development and research work. If documentation is needed, it is assumed to happen as a by-product of development, not as a dedicated task.

- Page 10, Schedule: “Iteration 9 (05.10.2026 - 3 weeks) • Review the system and check for bugs • **Documentation**”

### 10. Horizontal split between building the pipeline (Iteration 2) and integrating it (Iteration 3)

`MAJOR` · `methodology`

Iteration 2 builds the 'core feedback generation pipeline' in isolation, and Iteration 3 then 'connects' it to Thesis Management. This is a horizontal layering: build the backend engine first, then wire it to the UI. A vertically integrated iteration would deliver a thin end-to-end slice in a single iteration, e.g. 'Supervisor can trigger feedback generation on a submitted proposal and view the resulting feedback items within Thesis Management.' This allows the team to validate the full flow early and iterate on complexity in subsequent sprints.

- Page 9, Schedule: “Iteration 2 (22.06.2026 - 2 weeks) • **Implement core feedback generation pipeline** (Objective 1) • Define input/output contract and structured output format (Objective 1)”
- Page 9, Schedule: “Iteration 3 (06.07.2026 - 2 weeks) • **Connect feedback tool to Thesis Management** (Objective 4) • Extend feedback workflow in Thesis Management for thesis (Objective 4)”

### 11. Vague, non-measurable deliverables in Iterations 5, 6, and 9

`MAJOR` · `methodology`

Several schedule items lack measurable acceptance criteria. Iteration 5: 'Build prototype implementations' and 'Evaluate' do not specify what constitutes a working prototype or what the evaluation produces. Iteration 6: same issue for LLM model evaluation. Iteration 9: 'Review the system and check for bugs' is a catch-all cleanup task. Concrete alternatives: 'Implement agentic setup that decomposes a thesis into N sub-prompts and merges results; measure recall/precision against a labelled ground-truth set of 20 theses' or 'Ensure all feedback generation modes produce correct output on a test corpus of 10 sample theses with zero critical defects.'

- Page 9, Schedule: “Iteration 5 (03.08.2026 - 2 weeks) • **Build prototype implementations** for agentic and prompt chaining setups (Objective 3) • **Evaluate** agentic and prompt chaining setups (Objective 3)”
- Page 9, Schedule: “Iteration 6 (17.08.2026 - 2 weeks) • **Build prototype implementations** for the selected LLM models and providers (Objective 3) • **Evaluate** different LLM models and providers (Objective 3)”
- Page 10, Schedule: “**Review the system and check for bugs**”

### 12. Multiple filler words used across the proposal

`MINOR` · `writing`

The proposal contains several filler/discourse words: 'Moreover' (p. 3), 'Additionally' (p. 4), 'In addition' (p. 8), and 'also' (p. 2). Individually these are minor, but together they give the text a padded feel. Consider removing or replacing them with more direct transitions.

- Page 2, Abstract: “Beyond a single draft, the tool **also** compares iterations to highlight improvements and surface persistent issues”
- Page 3, Problem: “**Moreover,** the time spent on repetitive feedback leaves less time for what matters most”
- Page 4, Motivation: “**Additionally,** supervisors can provide feedback faster, making the process more scalable.”
- Page 8, Objective: “**In addition,** privacy regulations and cost require careful consideration of when to use locally self-hosted LLMs versus cloud-hosted ones.”

### 13. Use of first-person 'I' in AI Transparency Statement

`MINOR` · `writing`

The guidelines state to avoid 'I'. While the AI Transparency Statement is inherently personal, the consistent use of 'I' (7 instances) can be reduced. Consider restructuring with passive or nominal constructions where natural, e.g., 'Grammarly was used for grammar correction' or 'The author used …' to align with the stated voice guidelines.

- Page 12, AI Transparency Statement: “In preparing this proposal, **I** made use of several AI-based tools across different stages of the writing process.”
- Page 12, AI Transparency Statement: “For grammar and style correction, **I** used Grammarly to identify grammatical errors”
- Page 12, AI Transparency Statement: “For translation and language enhancement, **I** used DeepL to translate passages”
- Page 12, AI Transparency Statement: “For content generation and idea expansion, **I** used ChatGPT, OpenAI Codex, Claude, and Claude Code”

### 14. Inconsistent naming of the LLM-based feedback system

`MINOR` · `writing`

The same system is referred to as 'Reviewer' / 'LLM-powered Reviewer' / 'LLM Reviewer' in the Motivation section and Figure 1 caption, but as 'the feedback tool' in the Objective sections, and simply 'the tool' in the Abstract. A single consistent term should be used throughout the author's prose, or the document should explicitly link the two terms at first use.

- Page 2, Abstract: “Beyond a single draft, **the tool** also compares iterations to highlight improvements”
- Page 4, Motivation: “an **LLM-powered Reviewer** works alongside the supervisor, evaluating the thesis against the research group's writing guidelines”
- Page 5, Motivation: “Figure 1: Analysis object model of the envisioned feedback process: an **LLM Reviewer** works alongside the supervisor”
- Page 6, Objective: “When **the feedback tool** has access to previous iterations of the proposal or thesis”
- Page 8, Objective: “supervisors invoke **the feedback tool** to write feedback directly into Thesis Management”

### 15. Inconsistent phrasing: 'non-content-related' vs. 'non-content-specific'

`MINOR` · `writing`

The document uses 'non-content-related feedback' (pages 2, 4) and 'non-content-specific categories' (page 3) to describe the same class of feedback (structural, linguistic, coherence issues as opposed to content issues). These should be unified to a single phrase throughout the author's prose to avoid implying a subtle distinction that is not intended.

- Page 2, Abstract: “Supervisors of theses spend considerable time on **non-content-related** feedback”
- Page 3, Problem: “especially for **non-content-specifc** categories, highly repetitive”
- Page 4, Motivation: “This Reviewer covers exactly the repetitive, **non-content-related** layer of feedback”

### 16. AI transparency statement does not specify which proposal sections the tools were used for

`MINOR` · `completeness`

Rule 5 requires the statement to specify which sections of the proposal the AI tools were used for. The statement describes purposes and tools but never names specific proposal sections (e.g., Abstract, Introduction, Problem, Motivation, Objective, Schedule). The phrase 'across different stages of the writing process' is too general. The student should add at least a brief mapping of which tools were applied to which sections, or state explicitly that the tools were used across all sections.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across **diferent stages of the writing process**.”
