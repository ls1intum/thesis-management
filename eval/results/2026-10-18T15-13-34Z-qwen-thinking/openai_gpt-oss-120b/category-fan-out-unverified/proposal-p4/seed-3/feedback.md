# proposal-p4 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 30
- **Findings:** 12
- **Cost:** 10 calls, 0 tool calls, depth 2, 90626 tokens, 62s
- **Coverage:** 9/9 categories

## Summary

The proposal suffers from several critical compliance issues, including missing mandatory sections, prohibited content, and schedule violations, which prevent it from being ready for submission. While the overall structure and length are roughly appropriate, the work needs substantial revision to meet formatting, citation, and logical presentation standards.

## Findings

### 1. Missing Abstract Section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section, which is required.

### 2. Problem section presents a solution

`CRITICAL` · `logic`

The Problem section includes a proposed self‑service platform, which is a solution rather than just describing the problem and its negative consequences.

- Page 3, Problem: “... **A self‑service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 3. Non‑peer‑reviewed source in bibliography

`CRITICAL` · `citation`

The bibliography includes an entry [Key26] which is an online documentation guide and not a peer‑reviewed scientific publication; internet sources must be placed in footnotes, not the bibliography.

- Page 8, Bibliography: “[Key26] **Keycloak Authors, “Keycloak Server Administration Guide.”** Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 4. Missing review confirmation in AI Transparency Statement

`CRITICAL` · `writing`

The AI transparency statement does not include the required sentence confirming that the author has carefully checked all AI‑generated text.

- Page 10, AI Transparency Statement: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. ... (table omitted) **[missing required review confirmation sentence]**”

### 5. Thesis writing tasks included in schedule

`CRITICAL` · `other`

The schedule contains an iteration dedicated to writing the thesis and preparing the final presentation, which is prohibited by the schedule quality rules.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation**.”

### 6. Proposal Exceeds Maximum Length

`MAJOR` · `completeness`

The proposal spans 10 pages, exceeding the allowed 4-6 pages of text.

- Page 1: “=== PAGE 1 ===”

### 7. Objectives contain double verbs

`MAJOR` · `writing`

Several objective statements use two verbs (e.g., “Design and implement”), which violates the guideline that each objective should be expressed in a single action form.

- Page 4, Objectives: “•   **Design and implement** connectors with expiry”
- Page 5, Objectives: “4.2 **Design and Implement** Connectors with Expiry”

### 8. Figure 2 caption is too short and non‑informative

`MAJOR` · `figures`

The caption for Figure 2 consists only of a brief generic label “System Architecture”, which does not provide a detailed, informative description as required.

- Page 5, Design and Implement Connectors with Expiry: “Figure  2: **System Architecture**”

### 9. Missing introductory text before first subsection in Introduction

`MAJOR` · `structure`

Section 1 (Introduction) jumps directly to subsection 1.1 without any introductory paragraph, violating the guideline that every section must include introductory text before its first subsection.

- Page 2, Introduction: “**1  Introduction**”

### 10. Citation placed after period

`MINOR` · `citation`

The citation [SMJ01] appears after the period of the author abbreviation, violating the rule that citations must be placed before the full stop of the sentence.

- Page 3, Problem: “Schaad    et  al. **[SMJ01]** and   Kern   et  al.”

### 11. Prohibited language: contractions and superlatives

`MINOR` · `writing`

Contractions and superlatives are prohibited; replace "doesn't" with "does not" and "worst" with a neutral description.

- Page 3, Problem: “The current process **doesn’t** scale and is the **worst** bottleneck of the chair.”

### 12. Inconsistent spelling of “workflow”

`MINOR` · `formatting`

The term “workflow” appears with two different spellings (“workfow” and “workflow”). Use a single consistent spelling throughout the proposal.

- Page 4: “arms the **workfow** engine of Keycloak”
- Page 6: “when the **workflow** engine fails”
