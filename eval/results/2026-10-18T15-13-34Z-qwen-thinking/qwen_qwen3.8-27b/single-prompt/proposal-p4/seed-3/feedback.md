# proposal-p4 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 47
- **Findings:** 11
- **Cost:** 1 calls, 0 tool calls, depth 1, 20679 tokens, 160s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a well-structured and technically coherent research topic on time-limited access management, with clear objectives, two appropriate UML diagrams, and a solid bibliography of 12 peer-reviewed sources. However, it is missing the mandatory Abstract section (critical), the Problem section inappropriately proposes a solution, and several explicit rule violations exist: a double-verb objective, vendor documentation in the bibliography, a non-compliant AI transparency statement (missing review confirmation), excessive first-person 'I' usage, and a thesis-writing task in the schedule. These issues are all fixable with targeted edits.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section. The guidelines require an Abstract of 1/3 to 1/2 page as a mandatory section. The document jumps from the title page directly into Section 1 (Introduction). Add an Abstract summarising the problem, approach, and expected contribution.

- Page 1

### 2. Problem section presents a solution

`MAJOR` · `logic`

The Problem section must describe only problems and their negative consequences, not propose solutions. The sentence 'A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation' introduces the proposed solution. Remove this sentence from the Problem section; the solution belongs in the Motivation or Objectives.

- Page 3, Problem: “A self-service platform that **arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 3. Double verb in objective formulation

`MAJOR` · `logic`

The objective 'Design and implement connectors with expiry' uses two verbs. The guidelines require one verb per objective. Choose a single action verb (e.g., 'Implement connectors with expiry' or 'Build connectors with expiry') and update both the bullet list and the subsection heading 4.2 accordingly.

- Page 4, Objective: “**Design and implement** connectors with expiry”
- Page 5, Objective: “4.2 **Design and Implement** Connectors with Expiry”

### 4. Non-peer-reviewed documentation in bibliography

`MAJOR` · `citation`

Entry [Key26] ('Keycloak Server Administration Guide') is vendor documentation, not a peer-reviewed publication. The bibliography must contain only scientific publications. Move this reference to a footnote in the text where it is cited (Section 1.2) and remove it from the Bibliography section.

- Page 8, Bibliography: “[Key26] Keycloak Authors, "Keycloak Server Administration Guide." Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 5. AI Transparency Statement missing review confirmation

`MAJOR` · `completeness`

The transparency statement must include a sentence confirming the author has carefully checked all AI-generated text (e.g., 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.'). Add this sentence after the table.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 6. Excessive use of first-person 'I'

`MAJOR` · `writing`

The guidelines state to avoid 'I'. The student uses 'I' approximately 14 times across Sections 4 and 5 (e.g., 'I will build', 'I will replace', 'I will store', 'I divide it into iterations'). Replace with 'we' (sparingly, for the thesis approach) or restructure sentences to use the platform/system as the subject (e.g., 'The platform will be built on…', 'The resource tree will replace…').

- Page 4, Objective: “**I will build** a platform on which any member of the chair requests time-limited access to any resource.”
- Page 5, Objective: “**I will replace** the flat list of hosts and channels with a resource tree.”
- Page 7, Schedule: “and **I divide** it into iterations of two to four weeks.”

### 7. Schedule includes thesis-writing task

`MAJOR` · `methodology`

Iteration 7 ('Write the thesis and prepare the final presentation') must be removed. The schedule should focus exclusively on development/research work. Thesis writing and presentation preparation are not part of the iterative development plan.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation.**”

### 8. Citation placed after the full stop

`MINOR` · `citation`

In the Motivation section, the citation [BBF01] appears after the period: 'their temporal model. [BBF01]'. Citations must be placed before the full stop: 'their temporal model [BBF01].'

- Page 3, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model**.** **[BBF01]**”

### 9. Figure 2 caption is too short and generic

`MINOR` · `figures`

The caption 'Figure 2: System Architecture' is a single phrase and does not describe the diagram's content. Expand it to an informative sentence describing the components shown (mgmt-ui, mgmt-api, Keycloak, ldap-bridge, Slack, GitHub, Linux host) and how they interact, similar to the detailed caption of Figure 1.

- Page 5, Objective: “Figure 2: **System Architecture**”

### 10. Contraction used in Problem section

`MINOR` · `writing`

The text uses the contraction 'doesn't' which is not allowed in academic writing. Replace with 'does not'.

- Page 3, Problem: “The current process **doesn't** scale and is the worst bottleneck of the chair.”

### 11. Section 1 lacks introductory text before first subsection

`MINOR` · `structure`

Section 1 (Introduction) jumps directly into subsection 1.1 (Setting) without any introductory paragraph. Every section must include at least one or two sentences of introductory text before its first subsection. Add a brief introductory sentence or two before '1.1 Setting'.

- Page 2, Introduction: “1 Introduction

1.1 Setting”
