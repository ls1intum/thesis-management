# proposal-p4 — single-prompt

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 10
- **Cost:** 1 calls, 0 tool calls, depth 1, 15409 tokens, 28s
- **Coverage:** 9/9 categories

## Summary

The proposal is missing the required Abstract section, exceeds the allowed length, and contains several critical content issues such as presenting a solution within the Problem section. Major problems include a double‑verb objective description, missing AI‑transparency verification, and a schedule that includes thesis writing tasks. Minor issues affect figure captions, citation formatting, and spelling consistency.

## Findings

### 1. Missing Abstract Section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section, which is required by the guidelines.

### 2. Proposal Exceeds Allowed Length

`CRITICAL` · `completeness`

The document spans more than the permitted 4–6 pages of text, making it too long for the proposal format.

### 3. Problem Section Contains Solution Description

`CRITICAL` · `logic`

The Problem section should only describe the problem and its negative consequences, but it proposes a self‑service platform as a solution.

- Page 2, Problem: “A self‑service platform that arms the workflow engine of Keycloak with an expiry date at grant time would **end this accumulation**.”

### 4. Problem Section Too Long

`MAJOR` · `completeness`

The Problem section exceeds the allowed maximum of three‑quarters of a page.

- Page 2, Problem: “The current process doesn’t scale and is the **worst bottleneck** of the chair.”

### 5. Objective Bullet Uses Double Verb

`MAJOR` · `writing`

Objective 2 is phrased as "Design and implement connectors with expiry", which contains two verbs and violates the single‑verb rule.

- Page 4, Objective: “•   **Design and implement** connectors with expiry”

### 6. AI Transparency Statement Missing Confirmation

`MAJOR` · `other`

The AI transparency statement does not include a sentence confirming that the author has carefully checked all AI‑generated text.

- Page 10, Transparency: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”

### 7. Schedule Includes Thesis Writing Task

`MAJOR` · `other`

Iteration 7 lists "Write the thesis and prepare the final presentation", which is prohibited in the schedule according to the guidelines.

- Page 7, Schedule: “•   Iteration 7 (18–31 March): **Write the thesis** and prepare the final presentation.”

### 8. Figure 2 Caption Too Short

`MINOR` · `figures`

The caption for Figure 2 is only "System Architecture", which is insufficiently descriptive.

- Page 5, Figures: “Figure 2: **System Architecture**”

### 9. Inline URLs Not Presented as Footnotes

`MINOR` · `citation`

Non‑peer‑reviewed URLs are embedded directly in the text instead of being formatted as footnotes.

- Page 2, Problem: “**https://api.slack.com/** and **https://docs.github.com/en/rest**”

### 10. Spelling Inconsistency of "recertifcation"

`MINOR` · `writing`

The word "recertifcation" is misspelled; the correct spelling is "recertification", leading to inconsistency.

- Page 5, Objective: “Automate the **recertifcation** of grants”
- Page 6, Objective: “The platform logs every change … **recertifcation** interval …”
