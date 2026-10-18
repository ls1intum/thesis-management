# proposal-p4 — single-prompt

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 11
- **Cost:** 1 calls, 0 tool calls, depth 1, 13811 tokens, 18s
- **Coverage:** 9/9 categories

## Summary

The proposal has a coherent technical direction, identifies relevant stakeholders, provides two UML-style diagrams, and includes a substantial peer-reviewed bibliography. However, it is not ready for submission because the required Abstract is missing, the Problem and Motivation sections are too long, the Problem section includes a solution, the schedule contains prohibited thesis-writing work, and the AI transparency statement lacks the required review confirmation.

## Findings

### 1. Missing Abstract

`CRITICAL` · `completeness`

Add a dedicated Abstract section. The proposal currently starts with the Introduction and therefore omits a required section; the abstract should occupy approximately one-third to one-half of a page.

### 2. Problem and Motivation Sections Exceed Their Length Limits

`MAJOR` · `structure`

Shorten the Problem section to no more than three-quarters of a page and shorten the Motivation section to no more than three-quarters of a page. Both sections occupy approximately a full page in the rendered proposal.

- Page 2, Problem: “2 Problem”
- Page 3, Motivation: “3 Motivation”

### 3. Problem Section Presents a Solution

`MAJOR` · `logic`

Remove the proposed solution from the Problem section. Move this sentence to the Motivation or Objective section, and leave the Problem section focused only on the existing problems and their consequences.

- Page 3, Problem: “A **self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 4. Schedule Includes Prohibited Thesis Tasks

`MAJOR` · `schedule`

Replace Iteration 7 with development or research work. The schedule must not contain thesis writing or presentation preparation tasks, and all iterations should produce project-related deliverables.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation**.”

### 5. Non-Peer-Reviewed Documentation in Bibliography

`MAJOR` · `citation`

Remove the Keycloak administration guide from the bibliography and place it in a footnote where it is used. The bibliography should contain only scientific, peer-reviewed publications; documentation and URLs belong in footnotes.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “**Keycloak Server Administration Guide**.” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 6. AI Statement Lacks Review Confirmation

`MAJOR` · `completeness`

Complete the AI transparency statement with an explicit first-person confirmation that you carefully checked all AI-generated text for correctness and coherence.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”

### 7. Contraction Used in Formal Academic Prose

`MINOR` · `writing`

Avoid contractions throughout the proposal. Replace “doesn’t” with “does not” and check the full document for any other contractions.

- Page 3, Problem: “The current process **doesn’t** scale and is the worst bottleneck of the chair.”

### 8. Overuse of First-Person Formulations

`MINOR` · `writing`

Reduce repeated first-person formulations such as “I will” and express the planned work through direct active formulations or clearly stated research objectives. The current wording uses first person extensively rather than sparingly.

- Page 4, Objective: “**I will build** a platform on which any member of the chair requests time-limited access to any resource.”
- Page 5, Structure Resources as an Inheriting Tree: “**I will replace** the fat list of hosts and channels with a resource tree.”
- Page 6, Delegate Request Review Without Privilege Escalation: “**I will therefore split** the approval: in the review step, the resource owner records a decision without changing Keycloak.”

### 9. Citation Appears After the Full Stop

`MINOR` · `writing`

Place the citation before the full stop. Change “model. [BBF01]” to “model [BBF01].” and check the document for the same punctuation pattern elsewhere.

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model**. [BBF01]**”

### 10. Objective Heading Uses Two Verbs

`MINOR` · `writing`

Avoid combining two independent action verbs in one objective heading. Split this into one objective or reformulate it around a single main action, for example “Define Connectors with Expiry” or “Implement Expiring Connectors.”

- Page 5, Design and Implement Connectors with Expiry: “4.2 **Design and Implement** Connectors with Expiry”

### 11. Figure 2 Caption Is Not Informative Enough

`MINOR` · `figures`

Replace the generic Figure 2 caption with a longer, informative caption that explains the principal components, trust boundaries, data flows, and the roles of Keycloak, LDAP, Linux hosts, Slack, and GitHub in the architecture.

- Page 5, Design and Implement Connectors with Expiry: “Figure 2: **System Architecture**”
