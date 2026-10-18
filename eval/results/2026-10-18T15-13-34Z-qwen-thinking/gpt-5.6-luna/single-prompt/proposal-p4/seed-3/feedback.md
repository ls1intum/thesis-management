# proposal-p4 — single-prompt

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 43
- **Findings:** 10
- **Cost:** 1 calls, 0 tool calls, depth 1, 14045 tokens, 21s
- **Coverage:** 9/9 categories

## Summary

The proposal has a well-defined problem context, four substantive objectives, two appropriate UML diagrams, and a detailed bibliography with substantially more than the required number of scientific publications. However, it is not ready to submit because the Abstract is missing, the Problem section contains a proposed solution, the schedule includes a thesis-writing iteration, and the AI transparency statement lacks the required review confirmation.

## Findings

### 1. Missing Abstract

`CRITICAL` · `COMPLETENESS`

The proposal has no Abstract section. Add an Abstract before the Introduction and keep it between one-third and one-half page, summarising the problem, motivation, objectives, and planned work.

### 2. Problem Section Introduces a Solution

`MAJOR` · `LOGIC`

The Problem section proposes a concrete solution instead of describing only the problem and its consequences. Move this sentence to the Motivation or Objective section, and keep the Problem section focused on stakeholders, deficiencies, and negative effects.

- Page 3, Problem: “A **self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 3. Objective Uses Double Verbs

`MAJOR` · `WRITING`

The second high-level objective uses two coordinated action verbs, and its subsection heading repeats the same issue. Replace it with one primary action, for example “Provide Connectors with Expiry” or “Implement Expiring Connectors,” and align the bullet and subsection heading.

- Page 4, Objective: “• **Design and implement** connectors with expiry”
- Page 5, 4.2 Design and Implement Connectors with Expiry: “4.2 **Design and Implement** Connectors with Expiry”

### 4. Schedule Contains Thesis-Writing Tasks

`MAJOR` · `METHODOLOGY`

Iteration 7 is dedicated to thesis writing and presentation preparation, which is outside the required development- and research-focused schedule. Replace it with a final vertically integrated validation or evaluation increment; handle writing and presentation work outside the proposal schedule.

- Page 7, Schedule: “• Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation**.”

### 5. Non-Peer-Reviewed Documentation in Bibliography

`MAJOR` · `CITATION`

The bibliography contains a Keycloak administration guide, which is an online documentation source rather than a peer-reviewed publication. Remove it from the bibliography and retain the URL as a footnote where it is cited.

- Page 8, Bibliography: “[Key26] Keycloak Authors, **“Keycloak Server Administration Guide.”** Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/”

### 6. AI Statement Lacks Review Confirmation

`MAJOR` · `COMPLETENESS`

The AI transparency statement identifies the tool, purpose, and sections, but it does not confirm that the author carefully checked all AI-generated text. Add an explicit first-person sentence confirming that every AI-generated text and artefact was reviewed for correctness and coherence.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”

### 7. Figure 2 Caption Is Not Informative

`MINOR` · `FIGURES`

Figure 2 has a generic caption that does not explain the depicted components, interfaces, data flows, or the architectural significance. Replace it with a longer, informative caption describing how the management UI, API, connectors, Keycloak, LDAP bridge, Linux hosts, Slack, GitHub, and database interact.

- Page 5, 4.2 Design and Implement Connectors with Expiry: “Figure 2: **System Architecture**”

### 8. Citation Placed After Full Stop

`MINOR` · `CITATION`

The citation appears after the sentence-ending full stop. Move the citation before the full stop to match the citation convention used elsewhere, producing “...temporal role-based access control model [BBF01].”

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model**. [BBF01]**”

### 9. Contraction Used

`MINOR` · `WRITING`

The proposal uses a contraction. Replace “doesn’t” with “does not” to comply with the no-contractions rule.

- Page 3, Problem: “The current process **doesn’t** scale and is the worst bottleneck of the chair.”

### 10. Introduction Lacks Text Before Its First Subsection

`MINOR` · `STRUCTURE`

The Introduction jumps directly to subsection 1.1 without introductory prose. Add a short paragraph after the Introduction heading that previews the setting and existing infrastructure discussed in the subsections.

- Page 2, Introduction: “1 Introduction
**1.1 Setting**”
