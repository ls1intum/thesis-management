# proposal-p4 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 35
- **Findings:** 15
- **Cost:** 10 calls, 0 tool calls, depth 2, 92552 tokens, 69s
- **Coverage:** 9/9 categories

## Summary

The proposal contains several critical shortcomings, including a missing abstract, improper content in the Problem section, and violations of citation and AI‑transparency requirements. Additionally, there are numerous major issues related to length limits, writing style, figure captions, and scheduling that must be resolved before submission.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `structure`

The proposal does not contain the required Abstract section.

### 2. Document exceeds allowed length limits

`MAJOR` · `completeness`

The total text length exceeds the 4–6 page limit and the Problem section exceeds its 3/4‑page maximum.

- Page 2, Overall: “Overall document length exceeds the permitted 6 pages.”
- Page 2, Problem: “**Requesters do not know who manages which resource. A student who needs a host, a channel, and a repository asks several people and meanwhile borrows the session of a colleague or works under a shared account.**”

### 3. Problem section contains solution description

`CRITICAL` · `writing`

The Problem section presents a proposed solution, violating the rule that it must only describe the problem and its negative consequences.

- Page 3, Problem: “A self-service **platform that arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 4. Objectives contain double verbs

`MAJOR` · `writing`

One of the listed objectives uses two verbs ('Design and implement'), which is prohibited; each objective should be expressed with a single verb.

- Page 4, Objectives: “•   **Design and implement** connectors with expiry”
- Page 4, Objectives: “•   **Design and Implement** Connectors with Expiry”

### 5. Non-peer-reviewed source in bibliography

`CRITICAL` · `citation`

The bibliography includes an entry that is not a peer‑reviewed publication, which violates the requirement that only scientific, peer‑reviewed sources be listed.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “**Keycloak Server Administration Guide.**” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 6. Internet URLs not presented as footnotes

`MAJOR` · `citation`

Plain URLs appear in the main text instead of being placed in footnotes, which contradicts the guideline that internet sources must be footnoted.

- Page 2, Problem: “1**https://api.slack.com/** and **https://docs.github.com/en/rest**”

### 7. Figure captions insufficiently detailed

`MAJOR` · `figures`

Captions are too brief or consist of single‑line paragraphs, lacking the required detailed description of the figure content.

- Page 5, Design and Implement Connectors with Expiry: “Figure  2: **System Architecture**”
- Page 4, Objective: “**Access platform**”
- Page 4, Objective: “**Fill in a request form**”
- Page 4, Objective: “**«include»**”

### 8. Prohibited language usage (superlatives, contractions)

`MAJOR` · `writing`

The document uses prohibited language such as superlative terms ('worst') and contractions ('doesn’t'), which violate the writing style guidelines.

- Page 3, Problem: “The current process **doesn’t** scale and is the **worst** bottleneck of the chair.”

### 9. Passive voice usage

`MAJOR` · `writing`

The text contains passive voice constructions; the guidelines require active voice throughout.

- Page 2, 1.1 Setting: “Linux hosts with graphics processors **reached** over Secure Shell (SSH), a Slack workspace, a GitHub organisation, and a handful of internal dashboards and databases.”

### 10. Paragraph structure issues (excessive length and mixed ideas)

`MAJOR` · `writing`

A paragraph exceeds the recommended length and mixes multiple unrelated ideas, reducing readability.

- Page 3, Problem: “**explain it as a limited compliance budget [BSW08]. The current process doesn’t scale and is the worst bottleneck of the chair. Maintainers, the staf who look after a project, know whether a request is legitimate but lack the rights to act on it, while the super admin holds the rights but lacks the context. Access outlives its purpose and leaves no trace. No grant carries an expiry date, so access piles up and former students keep SSH access months after they leave.**”
- Page 3, Problem: “**Maintainers, the staf who look after a project, know whether a request is legitimate but lack the rights to act on it, while the super admin holds the rights but lacks the context. The usual workaround hands the maintainer full administrative rights over every user and every host. It contradicts the protection principles of Saltzer and Schroeder [SS75] and leaves open the delegation problem of administrative role models [BS00, SBM99].**”

### 11. Missing introductory text before first subsection

`MAJOR` · `structure`

Section 1 Introduction jumps directly to subsection 1.1 without any introductory paragraph.

- Page 2, Introduction: “1 Introduction

**1.1 Setting**”

### 12. Paragraph too short (single line)

`MINOR` · `formatting`

Paragraph consists of only one line, which is below the minimum recommended length.

- Page 2, Problem: “**2**”

### 13. Heading not in Title Case

`MINOR` · `formatting`

The heading capitalizes the preposition "Without", which should be lowercased in English title case.

- Page 6, Objective: “4.3 Delegate Request Review **Without** Privilege Escalation”

### 14. Missing review confirmation in AI transparency statement

`CRITICAL` · `completeness`

The AI transparency statement does not contain the required sentence confirming that the author has carefully checked all AI‑generated text for correctness and sense.

- Page 10, AI Transparency Statement: “**[Missing confirmation sentence: I have carefully checked all texts created with these tools to ensure that they are correct and make sense.]**”

### 15. Thesis writing tasks included in schedule

`CRITICAL` · `methodology`

Iteration 7 contains tasks for writing the thesis and preparing the final presentation, which are prohibited in the schedule according to the guidelines.

- Page 7, Schedule: “•   Iteration 7  (18–31      March):      **Write the thesis** and   prepare    the  fnal   presen­ tation.”
