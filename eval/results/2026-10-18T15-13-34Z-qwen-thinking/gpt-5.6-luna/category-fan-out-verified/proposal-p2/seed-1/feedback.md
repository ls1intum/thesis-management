# proposal-p2 — category-fan-out-verified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** ACCEPTABLE
- **Score:** 58
- **Findings:** 10
- **Cost:** 16 calls, 0 tool calls, depth 3, 95234 tokens, 48s
- **Coverage:** 9/9 categories

## Summary

The proposal is on the right track and presents a coherent motivation, objectives, and planned system integration. Before submission, it needs substantial revision to make the schedule measurable and vertically integrated, bring the AI transparency statement into compliance, and address several formatting and writing issues.

## Findings

### 1. AI transparency statement does not specify the sections in which tools were used

`MAJOR` · `completeness`

The AI transparency statement names tools and their purposes but does not identify which proposal sections each tool was used for. Map each tool and activity to specific sections, such as the Abstract, Introduction, Problem, Motivation, Objective, Schedule, or Bibliography; if a tool was used throughout, state that explicitly.

- Page 12, AI Transparency Statement: “In preparing this proposal, I made use of several AI-based tools **across diferent stages of the writing process**.”

### 2. Schedule deliverables lack measurable acceptance criteria

`MAJOR` · `methodology`

Several schedule activities are not measurable because they lack concrete outputs, evaluation data, or acceptance criteria. Replace vague activities such as implementing, connecting, building, and evaluating with verifiable deliverables, including an executable end-to-end prototype, a defined test set, and specified quality or performance metrics.

- Page 9, Schedule: “• Implement core feedback generation pipeline (Objective 1)”
- Page 9, Schedule: “• Connect feedback tool to Thesis Management (Objective 4)”
- Page 9, Schedule: “• Build prototype implementations for agentic and prompt chaining setups”
- Page 9, Schedule: “• Evaluate different LLM models and providers (Objective 3)”
- Page 10, Schedule: “• Review the system and check for bugs”

### 3. Early schedule iterations are organized by technical layers rather than vertical features

`MAJOR` · `structure`

The first iterations are divided into architectural and subsystem phases—modularization, a core pipeline, and later portal integration—rather than vertically integrated features. Reorganize the early work into end-to-end slices, such as generating guideline-based feedback and displaying or editing it in Thesis Management, with each iteration producing usable functionality.

- Page 9, Schedule: “• Modularize Thesis Management to prepare integration of the feedback module (Objective 4)”
- Page 9, Schedule: “• Implement core feedback generation pipeline (Objective 1)”
- Page 9, Schedule: “• Connect feedback tool to Thesis Management (Objective 4)”
- Page 9, Schedule: “• Extend feedback workflow in Thesis Management for thesis (Objective 4)”

### 4. Schedule includes a task outside the permitted scope

`MAJOR` · `completeness`

The final iteration includes documentation, which is outside the permitted schedule scope. Remove this task and use the iteration for development or research work, such as testing the integrated system against predefined cases and recording measurable evaluation results as research outputs.

- Page 10, Schedule: “• Review the system and check for bugs

• Documentation”

### 5. Objectives are not formatted as a bullet list

`MINOR` · `structure`

Section 4 presents the four objectives as a numbered list rather than the required bullet list. Replace the numbered list with four bullet points, keeping each objective to one line.

- Page 5, Objective: “**1. Generate Feedback Based on Guidelines**”

### 6. Sentences use forbidden starters

`MINOR` · `writing`

Recast the sentences so they do not begin with the forbidden starters “As” or “Because.” For example, make Figure 1 the subject of the first sentence and move the causal explanation in the second sentence to a later position or use a different sentence structure.

- Page 4, Motivation: “**As** Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor, evaluating the thesis against the research group’s writing guidelines and generating automatic formative feedback [NM06].”
- Page 5, Motivation: “**Because** the feedback tool is powered by LLMs, students can obtain instant formative feedback at any point during the writing process.”

### 7. Remove unnecessary transitional fillers

`MINOR` · `writing`

Remove transitional fillers where they do not express a necessary logical relation, or replace them with explicit statements explaining how the ideas relate.

- Page 3, Problem: “**Moreover**, the time spent on repetitive feedback leaves less time for what matters most: helping students with their research and providing feedback on substantive topics.”
- Page 4, Motivation: “**Additionally**, supervisors can provide feedback faster, making the process more scalable.”
- Page 8, Objective — Compare Prompting Techniques and LLM Models: “**In addition**, privacy regulations and cost require careful consideration of when to use locally self-hosted LLMs versus cloud-hosted ones.”

### 8. Replace unsupported intensifiers and superlatives

`MINOR` · `writing`

Replace broad intensifying wording with precise, supportable claims. Qualify or remove “wide” and “greatest” unless the comparison is defined and supported by evidence.

- Page 3, Problem: “Supervisors provide feedback on a **wide** range of topics, including the content itself, the structure of the document and its chapters, writing coherence and cohesion, and linguistic accuracy.”
- Page 8, Objective — Integrate With Thesis Management: “By building on existing processes, the integration causes the least disruption to supervisors and delivers the **greatest** simplification of their workflow.”

### 9. Several paragraphs are too long

`MINOR` · `writing`

Break up paragraphs that substantially exceed the recommended 5–8 lines. Separate distinct ideas into shorter paragraphs, such as proposal contents from thesis-lifecycle context, guideline-based feedback from formative versus normative feedback, and the AI transparency statement by tool purpose.

- Page 2, Introduction: “Before starting their research project, students typically write a research proposal. In this proposal, they define the research question and problem they want to explore, outline their methodology, and specify the scope of their work.”
- Page 3, Introduction: “At around 50 research groups at the Technical University of Munich (TUM), supervisors and students manage the thesis lifecycle through Thesis Management 1, an in-house portal developed at TUM.”
- Page 5, 4.1 Generate Feedback Based on Guidelines: “Supervisors should be able to provide their own scientific writing guidelines as well as conventions on the style of theses and proposals. Based on these guidelines, the LLM can provide formative feedback that is restricted to the aspects they cover.”
- Page 6, 4.1 Generate Feedback Based on Guidelines: “Ethically, it is difficult to let LLMs grade a final thesis, as this constitutes normative feedback and requires close scrutiny. Formative feedback, on the other hand, is less critical since it serves as input for students to improve their work.”
- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across different stages of the writing process. For grammar and style correction, I used Grammarly to identify grammatical errors, improve sentence structure, and refine the overall writing style of my drafts.”
- Page 12, Transparency in the Use of AI Tools: “For coding assistance, I used GitHub Copilot, ChatGPT, OpenAI Codex, Claude, and Claude Code to support Typst formatting, generate snippets for figures and macros, and resolve template-related issues. For citation assistance, I used Zotero to manage my references and to ensure proper citation formatting throughout the proposal.”

### 10. Use consistent hyphenation for “prompt chaining”

`MINOR` · `writing`

Use one consistent form for “prompt chaining.” The abstract hyphenates the term, while the Objective section and Schedule use the unhyphenated form; choose one form and apply it throughout the prose.

- Page 2, Abstract: “We evaluate single-prompt, **prompt-chaining**, and agentic setups”
- Page 8, Objective: “Alternatives include **prompt chaining** and agentic setups”
- Page 9, Schedule: “Build prototype implementations for agentic and **prompt chaining** setups”
