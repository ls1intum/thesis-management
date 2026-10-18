# proposal-p2 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 16
- **Cost:** 10 calls, 0 tool calls, depth 2, 91441 tokens, 62s
- **Coverage:** 9/9 categories

## Summary

The proposal presents an innovative idea of using LLMs to automate feedback for theses, but it suffers from several critical compliance issues. Major problems include excessive length, insufficient bibliography, missing AI transparency details, and schedule violations, along with numerous structural and writing deficiencies. Substantial revisions are required before submission.

## Findings

### 1. Proposal exceeds maximum allowed length

`CRITICAL` · `structure`

The proposal spans more than the allowed 4‑6 pages of text, covering 12 pages in total, which is significantly longer than the guideline.

- Page 12: “=== PAGE 12 ===”

### 2. Objective subsections insufficient detail and paragraph count

`CRITICAL` · `structure`

Each objective subsection is written as a single short paragraph, lacking the required minimum of two paragraphs and detailed descriptions.

- Page 5, Objective 4.1: “**4.1 Generate            Feedback          Based       on    Guidelines** Supervisors     should   be  able  to provide    their  own   scientifc   writing   guidelines   as well  as conventions...”
- Page 6, Objective 4.2: “**4.2 Highlight           Changes          Across       Iterations** When     the  feedback    tool   has  access   to  previous    iterations...”
- Page 8, Objective 4.3: “**4.3 Compare             Prompting            Techniques           and     LLM       Models** Based    on  the   writing    guidelines,    the  feedback     tool  needs...”
- Page 8, Objective 4.4: “**4.4 Integrate           With       Thesis       Management** Around     50  research    groups   at  TUM     use   Thesis   Management...”

### 3. Insufficient number of bibliography entries

`CRITICAL` · `completeness`

The bibliography contains only five publications, but the guidelines require at least six to eight peer‑reviewed publications.

- Page 11, Bibliography: “**[TP21] … [NM06]**”

### 4. Schedule contains prohibited documentation task and focuses on horizontal splits rather than end‑to‑end features

`CRITICAL` · `logic`

The schedule includes a "Documentation" task, which is not allowed, and several tasks address low‑level modularization and preprocessing instead of delivering vertically integrated functionality.

- Page 10, Schedule: “•   Documentation”
- Page 9, Schedule: “•   Modularize Thesis Management to prepare integration of the feedback module (Objective 4)”
- Page 9, Schedule: “•   Preprocess guidelines into a structured representation usable by the feedback tool (Objective 1)”

### 5. Motivation repeats problem description

`MAJOR` · `logic`

The Motivation section restates the problem about repetitive, non‑content‑related feedback draining supervisors, which should be avoided according to the guidelines.

- Page 4, Motivation: “Integrating large language models (LLMs) to assist supervisors with non‑content‑related feedback could improve the thesis writing process. As Figure 1 illustrates, an LLM‑powered Reviewer works alongside the supervisor, evaluating the thesis against the research group’s writing guidelines and generating automatic formative feedback. This Reviewer covers exactly the **repetitive, non‑content‑related layer of feedback that currently drains supervisors**, freeing them to focus on specific content and on discussions with their students.”

### 6. Missing sections in AI transparency statement

`MAJOR` · `completeness`

The AI transparency statement does not specify which sections of the proposal the listed AI tools were used for, which is required by the guidelines.

- Page 12, Transparency in the Use of AI Tools: “In preparing this proposal, I made use of several AI-based tools across **different stages of the writing process**. For grammar and style correction, I used Grammarly ... I have carefully checked all texts created with these tools to ensure that they are correct and make sense.”

### 7. Non‑peer‑reviewed internet source not placed as footnote

`MAJOR` · `citation`

An internet URL is included directly in the main text instead of being formatted as a footnote, violating the rule that non‑peer‑reviewed sources must appear as footnotes.

- Page 3: “Thesis Management 1, **https://github.com/ls1intum/thesis-management**”

### 8. Passive voice usage

`MAJOR` · `writing`

The proposal contains several instances of passive voice, which reduces clarity and does not follow the active voice requirement.

- Page 2, Abstract: “feedback **is** repetitive across students and disciplines.”
- Page 3, Problem: “the thesis‑lifecycle portal **used** by around 50 research groups at the Technical University of Munich.”
- Page 6, Objective: “feedback **is** less critical to grade a final thesis.”

### 9. Forbidden sentence starters

`MAJOR` · `writing`

Sentences start with prohibited words such as "As", "Because", and "Since", which violates the writing style guidelines.

- Page 4, Motivation: “**As** Figure 1 illustrates, an LLM‑powered Reviewer works alongside the supervisor...”
- Page 8, Objective: “**Because** the feedback tool **is powered** by LLMs, students can obtain instant formative feedback...”
- Page 5, Objective: “**Since** guidelines may not already be in a suitable format, the LLM may first need to preprocess them...”

### 10. Paragraph exceeds recommended length in Abstract

`MAJOR` · `formatting`

The paragraph in the Abstract section contains more than the allowed 8 lines, making it hard to read and follow a single idea.

- Page 2, Abstract: “This thesis explores how large language models (LLMs) can generate **for mative feedback on theses and proposals based on supervisor‑provided writing guidelines. Beyond a single draft, the tool also compares iterations to highlight improvements and surface persistent issues with more explanatory feedback. We evaluate single‑prompt, prompt‑chaining, and agentic setups, and assess trade‑ofs between local and cloud hosting. The tool integrates into Thesis Management, the thesis‑lifecycle portal used by around 50 research groups at the Technical University of Munich, so supervisors can generate, revise, and publish feedback within their workfow.**”

### 11. Abstract is longer than allowed

`MINOR` · `structure`

The abstract occupies roughly a full page, exceeding the required 1/3–1/2 page length.

- Page 2, Abstract: “**Supervisors of theses  spend   considerable     time   on  non-content-related       feedback** such as document structure, coherence, and linguistic accuracy. Much of this feedback is repetitive across students and disciplines, draining supervisors and leaving less capacity for substantive research discussions.”

### 12. Problem section exceeds length limit

`MINOR` · `structure`

The Problem section is longer than the maximum of 3/4 page specified in the guidelines.

- Page 3, Problem: “**For  students’    research    proposals     and   theses,   supervisors     often  provide    feed** back that is critical to their progress. The supervisor takes the role of an experienced peer and coach...”

### 13. Motivation section exceeds length limit

`MINOR` · `structure`

The Motivation section is longer than the allowed 3/4 page.

- Page 4, Motivation: “**Integrating large language models (LLMs) to assist supervisors with non-** content-related feedback could improve the thesis writing process. As Figure 1 illustrates, an LLM-powered Reviewer works alongside the supervisor...**”

### 14. Use of filler word "also"

`MINOR` · `writing`

The word "also" is used as a filler, which should be avoided for concise academic writing.

- Page 2, Abstract: “the tool **also** compares iterations to highlight improvements...”

### 15. Paragraph too short (1 line)

`MINOR` · `formatting`

A single‑line paragraph appears, which does not satisfy the minimum length requirement of 5 lines for regular prose.

- Page 3: “**1 https://github.com/ls1intum/thesis-management**”

### 16. Incorrect Capitalization in Heading

`MINOR` · `formatting`

The heading "4.4 Integrate With Thesis Management" capitalizes the short preposition "With", which violates the title‑case rule for English headings.

- Page 8, 4.4 Integrate With Thesis Management: “4.4 Integrate **With** Thesis Management”
