# proposal-p2 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 15
- **Cost:** 10 calls, 0 tool calls, depth 2, 84412 tokens, 36s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a relevant and potentially valuable approach to scalable formative feedback, with clear objectives and a plausible integration context. However, it requires substantial revision before submission, particularly to meet the bibliography and AI-transparency requirements, strengthen schedule methodology and acceptance criteria, and improve structure and writing consistency.

## Findings

### 1. Bibliography has fewer than the required minimum number of publications

`CRITICAL` · `completeness`

The bibliography contains only five publications, below the required minimum of six. Add at least one additional peer-reviewed publication, preferably expanding the bibliography to six to eight relevant sources.

- Page 11, Bibliography: “[NM06] D. J. Nicol and D. Macfarlane-Dick, “Formative Assessment and Self-Regulated Learning: A Model and Seven Principles of Good Feedback Practice,” Studies in Higher Education, vol. 31, no. 2, pp. 199–218, 2006, doi: 10.1080/03075070600572090.”

### 2. AI transparency statement does not identify assisted proposal sections

`MAJOR` · `completeness`

The AI transparency statement names tools and general purposes but does not identify which proposal sections each tool assisted. Specify the relevant sections, such as the Abstract, Introduction, Motivation, Objective, Schedule, or Bibliography, and indicate where each tool was used.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across different stages of the writing process.”

### 3. AI transparency statement relies on repeated first-person formulations

`MAJOR` · `completeness`

The transparency statement repeatedly uses first-person formulations while also lacking section-specific attribution. Rewrite the statement impersonally or use “the author” where necessary, and identify the proposal sections assisted by each tool.

- Page 12, Transparency in the Use of AI Tools: “I made use of several AI-based tools across different stages of the writing process.”
- Page 12, Transparency in the Use of AI Tools: “For grammar and style correction, I used Grammarly to identify grammatical errors, improve sentence structure, and refine the overall writing style of my drafts.”
- Page 12, Transparency in the Use of AI Tools: “For translation and language enhancement, I used DeepL to translate passages between German and English and to improve the fluency of phrasings.”

### 4. Schedule deliverables lack measurable acceptance criteria

`MAJOR` · `completeness`

Several schedule items describe activities without observable deliverables, test conditions, benchmarks, datasets, or quantitative success criteria. Define acceptance criteria for the feedback pipeline, Thesis Management integration, model evaluation, and bug review—for example, specify representative cases, required passing results, and evaluation metrics.

- Page 9, Schedule: “• Implement core feedback generation pipeline (Objective 1)”
- Page 9, Schedule: “• Connect feedback tool to Thesis Management (Objective 4)”
- Page 9, Schedule: “• Evaluate different LLM models and providers (Objective 3)”
- Page 10, Schedule: “• Review the system and check for bugs”

### 5. Iterations are organized as horizontal technical layers

`MAJOR` · `methodology`

The schedule is organized primarily around technical layers rather than vertically integrated end-to-end capabilities. Reorganize the iterations so that each produces a usable slice—from guideline input through feedback generation and supervisor review—then extend that slice with integration, guideline processing, and additional evaluation.

- Page 9, Schedule: “• Modularize Thesis Management to prepare integration of the feedback module (Objective 4)”
- Page 9, Schedule: “• Implement core feedback generation pipeline (Objective 1)”
- Page 9, Schedule: “• Connect feedback tool to Thesis Management (Objective 4)”
- Page 9, Schedule: “• Allow supervisors to upload custom guidelines (Objective 1)”

### 6. Schedule includes a prohibited documentation task

`MAJOR` · `structure`

The schedule includes a documentation task even though documentation is excluded by the schedule rules. Remove it from the iteration plan and manage thesis writing or documentation outside the schedule.

- Page 10, Schedule: “• Documentation”

### 7. Repeated passive-voice constructions obscure responsible actors

`MAJOR` · `writing`

Replace passive constructions with active formulations that identify the responsible actor. Revise the statements about institutional conventions, the LLM-powered tool, Figure 2, bundled criteria, and thesis submission.

- Page 4, Problem: “Having to wait for feedback on purely formal aspects of a thesis or proposal can be frustrating for students, as these issues are governed by institutional conventions rather than research substance.”
- Page 5, Motivation: “Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”
- Page 6, Objective: “A third mode combines both sources, as illustrated in Figure 2.”
- Page 8, Objective: “A single plain prompt to an LLM may not cover all of them reliably, since attention can dilute when many heterogeneous criteria are bundled into one request.”
- Page 9, Objective: “The student, by contrast, hands in the thesis itself as a single final submission.”

### 8. Motivation partially repeats the Problem

`MINOR` · `structure`

The Motivation repeats the Problem by restating the repetitive feedback burden and the time it drains from supervisors. Replace this recap with a more explicitly visionary account of the scientific and educational benefits enabled by the approach, such as scalable formative assessment, improved self-regulated learning, or new ways to study AI-supported supervision.

- Page 4, Motivation: “This Reviewer covers exactly the repetitive, non-content-related layer of feedback that currently drains supervisors, freeing them to focus on specific content and on discussions with their students.”

### 9. High-level objectives are not formatted as bullet points

`MINOR` · `structure`

The high-level objectives are formatted as a numbered list rather than the required bullet list. Replace the numbered items with bullet points while keeping each objective concise and on one line.

- Page 5, Objective: “The scope of the thesis is to fulfil the following goals:

**1.** Generate Feedback Based on Guidelines
**2.** Highlight Changes Across Iterations
**3.** Compare Prompting Techniques and LLM Models
**4.** Integrate With Thesis Management”

### 10. Motivation contains an overlong paragraph with several distinct ideas

`MINOR` · `structure`

Split this paragraph into two paragraphs because it exceeds the recommended length and combines distinct ideas: faster feedback, student knowledge and beliefs, self-regulated learning, and the role of instant LLM feedback. Separate the explanation of self-regulated learning from the benefits of instant feedback.

- Page 4, Motivation: “Better and faster feedback has signifcant advantages for students as well. When feedback arrives sooner, students can act on it while the writing process is still fresh in their minds. The student brings both knowledge, comprising theoretical and practical domain expertise, and beliefs about how to approach the task and how to work and write scientifcally, which include the student’s personal interpretation of the writing guidelines. Drawing on this knowledge and these beliefs, the student writes the thesis. Through self-regulated learning, the student engages with the Reviewer’s feedback, refecting on it independently or”
- Page 5, Motivation: “discussing it with the supervisor, and uses it to improve their knowledge, beliefs, and broader strategy for proceeding with the work [NM06]. Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 11. Guideline and ethics discussion should be divided into shorter paragraphs

`MINOR` · `structure`

Split this paragraph into shorter paragraphs. Keep the description of guideline-based feedback separate from the ethical distinction between formative feedback and grading.

- Page 5, Objective — Generate Feedback Based on Guidelines: “Supervisors should be able to provide their own scientifc writing guidelines as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover. Since guidelines may not already be in a suitable format, the LLM may frst”
- Page 6, Objective — Generate Feedback Based on Guidelines: “need to preprocess them into a structured representation it can act on. Ethically, it is difcult to let LLMs grade a fnal thesis, as this constitutes normative feedback and requires close scrutiny. Formative feedback, on the other hand, is less critical since it serves as input for students to improve their work. The idea is that students interpret the feedback themselves and discuss it, either independently or with their supervisor. Therefore, it is less important that every output of the LLM is perfectly correct.”

### 12. Repeated filler transitions weaken the prose

`MINOR` · `writing`

Remove filler transitions such as “Moreover,” “Additionally,” and “In addition” where they add no logical meaning, or replace them with transitions that explicitly state the relationship between the claims. Also revise the phrase “to also cover.”

- Page 3, Problem: “Moreover, the time spent on repetitive feedback leaves less time for what matters most: helping students with their research and providing feedback on substantive topics.”
- Page 4, Motivation: “Additionally, supervisors can provide feedback faster, making the process more scalable.”
- Page 8, Objective: “In addition, privacy regulations and cost require careful consideration of when to use locally self-hosted LLMs versus cloud-hosted ones.”
- Page 9, Objective: “We extend the existing proposal feedback flow to also cover the thesis, enabling formative feedback during the writing of the thesis itself.”

### 13. Several sentences use prohibited opening conjunctions

`MINOR` · `writing`

Avoid beginning sentences with “As,” “Because,” or “Since.” Recast each sentence so that the main clause comes first, the causal or conditional clause follows, or the figure reference appears later.

- Page 4, Motivation: “As Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group's writing guidelines and generating automatic formative feedback [NM06].”
- Page 5, Motivation: “Because the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”
- Page 5, Objective: “Since guidelines may not already be in a suitable format, the LLM may first need to preprocess them into a structured representation it can act on.”

### 14. AI is used without being defined

`MINOR` · `writing`

Define the term at its first occurrence as “artificial intelligence (AI)” before using the abbreviation in the activity-diagram caption and the transparency statement.

- Page 7, Objective: “The supervisor then revises the AI-generated draft, joins it with their own feedback, and publishes the result to the student.”
- Page 12, Transparency in the Use of AI Tools: “Transparency in the Use of AI Tools”

### 15. “Prompt chaining” is hyphenated inconsistently

`MINOR` · `formatting`

Use one spelling consistently for the technique: either “prompt-chaining” or “prompt chaining.” Apply the chosen form throughout the Abstract, Objective discussion, and Schedule.

- Page 2, Abstract: “We evaluate single-prompt, prompt-chaining, and agentic setups, and assess trade-ofs between local and cloud hosting.”
- Page 8, 4.3 Compare Prompting Techniques and LLM Models: “Alternatives include prompt chaining and agentic setups, in which the LLM itself identifes where further lookups into the proposal are required and triggers specialized feedback generations focused on a narrower set of aspects.”
- Page 9, Schedule: “• Build prototype implementations for agentic and prompt chaining setups”
