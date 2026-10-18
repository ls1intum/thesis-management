# proposal-p4 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 48
- **Findings:** 12
- **Cost:** 1 calls, 0 tool calls, depth 1, 21281 tokens, 166s
- **Coverage:** 9/9 categories

## Summary

The proposal demonstrates a well-researched, technically coherent project with strong citations (12 peer-reviewed sources), appropriate UML diagrams, and a well-structured Objectives section with measurable acceptance criteria. However, the missing Abstract is a critical structural gap, the Problem section leaks a solution, the bibliography includes a non-peer-reviewed documentation entry, the AI transparency statement lacks the required review-confirmation sentence, and the schedule includes a thesis-writing iteration. Several minor issues (contraction, first-person 'I' overuse, inconsistent actor terminology, generic figure caption) round out a proposal that is on the right track but needs focused revision before submission.

## Findings

### 1. Missing Abstract Section

`CRITICAL` · `structure`

The proposal lacks an Abstract section entirely. The document transitions directly from the title page (page 1) to '1 Introduction' (page 2). The required Abstract (1/3 to 1/2 page) is absent and must be added before the Introduction.

- Page 1: “Submission Date: 31.03.2027”

### 2. Problem Section Presents a Solution

`MAJOR` · `logic`

The Problem section must only describe problems and their negative consequences, not propose solutions. The sentence 'A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation' introduces a specific architectural solution (Keycloak workflow engine with expiry dates) that belongs in the Motivation or Objectives, not in the Problem description.

- Page 3, Problem: “A **self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 3. Double Verb in Objective Formulation

`MAJOR` · `writing`

The second objective uses two verbs: 'Design and implement connectors with expiry.' The guidelines explicitly prohibit double-verb formulations (e.g., 'Design and implement' is given as the canonical bad example). Choose one action verb. The same issue appears in the subsection heading '4.2 Design and Implement Connectors with Expiry.'

- Page 4, Objective: “• **Design and implement** connectors with expiry”
- Page 5, Objective: “4.2 **Design and Implement** Connectors with Expiry”

### 4. Non-Peer-Reviewed Documentation in Bibliography

`MAJOR` · `citation`

The entry [Key26] 'Keycloak Server Administration Guide' is product documentation, not a peer-reviewed scientific publication. It should be moved to a footnote in the text (similar to how the Slack and GitHub API URLs are already footnoted on page 2) and removed from the bibliography.

- Page 8, Bibliography: “[Key26] Keycloak Authors, "Keycloak Server Administration Guide." Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 5. Citation Placed After the Full Stop

`MAJOR` · `citation`

The citation [BBF01] appears after the period: 'temporal model. [BBF01]'. The guideline requires citations to be placed before the full stop, e.g., 'temporal model [BBF01].' Fix the punctuation order.

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their **temporal model. [BBF01]**”

### 6. AI Transparency Statement Lacks Review Confirmation

`MAJOR` · `completeness`

The transparency statement on page 10 names the tool, purpose, and scope, but omits the required confirmation sentence. The statement must include language equivalent to: 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' Add this sentence to the statement.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 7. Schedule Includes Thesis Writing Task

`MAJOR` · `structure`

Iteration 7 (18–31 March) is dedicated to 'Write the thesis and prepare the final presentation.' The schedule must focus on development/research work only and must not include thesis writing, documentation writing, or presentation preparation tasks. Remove this iteration and redistribute the remaining time or end the schedule at Iteration 6.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation.**”

### 8. Excessive First-Person Singular 'I' in Objectives

`MINOR` · `writing`

The guidelines state to avoid 'I' and use 'we' sparingly only for the thesis approach. The Objectives section (4.1–4.4) uses 'I will' or 'I' in nearly every sentence (e.g., 'I will replace', 'I will store', 'I will define', 'I will implement', 'I will build', 'I will attach', 'I will compare'). Replace with 'we' where referring to the thesis approach, or restructure sentences to use the system/platform as subject.

- Page 4, Objective: “**I** will build a platform on which any member of the chair requests time-limited access”
- Page 5, Objective: “**I** will replace the flat list of hosts and channels with a resource tree”
- Page 5, Objective: “**I** will define one connector contract that validates a binding, grants and revokes access”
- Page 6, Objective: “**I** will therefore split the approval: in the review step, the resource owner records a decision”
- Page 6, Objective: “**I** will attach a recertification interval to each resource”

### 9. Contraction in Problem Section

`MINOR` · `writing`

The Problem section uses the contraction 'doesn't' in 'The current process doesn't scale and is the worst bottleneck of the chair.' Academic writing must not use contractions. Replace with 'does not.'

- Page 3, Problem: “The current process **doesn't** scale and is the worst bottleneck of the chair.”

### 10. Figure 2 Caption Is Too Short and Generic

`MINOR` · `figures`

The caption for Figure 2 reads only 'System Architecture,' which is a short, generic label. Captions must be long and informative, describing what the figure shows and how the components interact. Expand it to describe the roles of mgmt-ui, mgmt-api, ConnectorRegistry, Keycloak, the LDAP bridge, and the external systems, similar to the detailed caption of Figure 1.

- Page 5, Objective: “Figure 2: **System Architecture**”

### 11. No Introductory Text Before First Subsection of Section 1

`MINOR` · `structure`

Section '1 Introduction' jumps directly into subsection '1.1 Setting' without any introductory paragraph. Every section that contains subsections must include a short introductory text (1–3 sentences) before the first subsection heading, summarising what the section covers.

- Page 2, Introduction: “1 Introduction

1.1 Setting”

### 12. Inconsistent Terminology: 'Maintainer' vs 'Resource Owner'

`MINOR` · `formatting`

The use case diagram (Figure 1), the Problem section, and most of the proposal use 'maintainer' for the person who reviews access requests. However, Section 4.3 switches to 'resource owner' ('the resource owner records a decision', 'The resource owner completes a request alone'). Pick one term and use it consistently throughout the proposal.

- Page 6, Objective: “in the review step, the **resource owner** records a decision without changing Keycloak”
- Page 6, Objective: “The **resource owner** completes a request alone when it touches only non-privileged bindings”
- Page 3, Problem: “**Maintainers**, the staff who look after a project, know whether a request is legitimate but lack the rights to act on it”
