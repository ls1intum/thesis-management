# proposal-p4 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 15
- **Cost:** 10 calls, 0 tool calls, depth 2, 80433 tokens, 22s
- **Coverage:** 9/9 categories

## Summary

The proposal has a coherent access-management topic and several concrete objectives, but it is not ready for submission because it lacks the required Abstract and has multiple structural, completeness, and organization problems. Addressing the missing sections, objective and schedule requirements, role terminology, bibliography issue, and AI transparency statement should take priority; the remaining writing and figure-caption issues are lower-impact refinements.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `completeness`

Add the required Abstract section, approximately one-third to one-half page, summarizing the problem, motivation, proposed objectives, and intended outcome.

### 2. Missing introductory text before the first subsection

`MAJOR` · `structure`

Add a brief introductory paragraph after the “1 Introduction” heading to explain the purpose and scope of the chapter before subsection 1.1.

- Page 2, Introduction: “1 Introduction

1.1 Setting”

### 3. Problem section presents a solution

`MAJOR` · `structure`

The Problem section should describe only the problem and its negative consequences. Remove the proposed self-service platform from this section or move it to the Motivation/Objectives section.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 4. Objective 4 lacks the required second paragraph

`MAJOR` · `structure`

Objective 4 must contain at least two substantive paragraphs. Split the recertification design from its evaluation and audit behavior, and add detail about the workflow, escalation conditions, and evaluation criteria.

- Page 6, 4.4 Automate the Recertification of Grants: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate and may revoke grants automatically, but never in a campaign that reached nobody. The platform logs every”
- Page 7, 4.4 Automate the Recertification of Grants: “change with the person who made it, and the admin event log of Keycloak keeps a second copy.”

### 5. Objective heading uses double verbs

`MAJOR` · `writing`

Reformulate the second objective heading around one primary action rather than two coordinated verbs. For example, use “Implement expiring connectors” or “Specify connectors with expiry,” and describe the other activity in the subsection body.

- Page 4, Objective: “Design and implement connectors with expiry”

### 6. Inconsistent term for the request-review role

`MAJOR` · `writing`

Define whether “maintainer” and “resource owner” are the same request-review role. Choose one term and use it consistently, or explicitly distinguish the responsibilities of the two roles.

- Page 4, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 6, Delegate Request Review Without Privilege Escalation: “I will therefore split the approval: in the review step, the resource owner records a decision without changing Keycloak, and in the grant step, the super admin sets membership and expiry atomically.”

### 7. Missing confirmation that AI-generated text was carefully checked

`MAJOR` · `completeness`

Add a first-person sentence explicitly confirming that all AI-generated text was carefully checked for correctness and coherence.

- Page 10, AI Transparency Statement: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”
- Page 10, AI Transparency Statement: “Anthropic Claude Code ... Purpose: Drafting text, authoring the Typst sources and bibliography entries, researching references ... Scope: Sections 1-5, Figures 1-2, and bibliography”

### 8. Schedule includes thesis writing and presentation preparation

`MAJOR` · `structure`

Remove Iteration 7 from the development schedule because thesis writing and final-presentation preparation are excluded. Replace it with a development or research increment aligned with the objectives, or end the development schedule after the final development iteration and track writing separately.

- Page 7, Schedule: “Iteration 7 (18–31 March): Write the thesis and prepare the fnal presentation.”

### 9. Non-peer-reviewed documentation included in bibliography

`MAJOR` · `citation`

Remove the product documentation page from the bibliography and cite it only as a footnote where needed, since it is not a peer-reviewed source.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “Keycloak Server Administration Guide.” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 10. Citation placed after full stop

`MINOR` · `citation`

Move citation [BBF01] before the preceding sentence's full stop, for example: “...in their temporal model [BBF01].”

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model. [BBF01] Joshi et al. generalise this idea to role assignments with a limited duration [Jos+05].”

### 11. Figure 2 caption is not sufficiently informative

`MINOR` · `figures`

Replace the generic Figure 2 caption with an informative caption describing the principal platform components, their responsibilities, database and connector interactions, and integrations with Keycloak, Slack, GitHub, and Linux hosts.

- Page 5, Objective: “Figure 2: System Architecture”

### 12. Repeated first-person constructions

`MINOR` · `writing`

Replace repeated first-person constructions with explicit subject-based formulations such as “The platform will…” or “The evaluation will measure…”.

- Page 4, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 5, Structure Resources as an Inheriting Tree: “I will store the tree with a transitive-closure table of ancestor–descendant pairs and cap the fan-out...”
- Page 6, Delegate Request Review Without Privilege Escalation: “I will therefore split the approval: in the review step, the resource owner records a decision without changing Keycloak...”
- Page 7, Schedule: “The thesis runs from 1 October 2026 to 31 March 2027, and I divide it into iterations of two to four weeks.”

### 13. Passive-voice constructions

`MINOR` · `writing`

Rewrite passive-voice constructions by naming the actor responsible for each action, such as the system, evaluator, or another responsible party.

- Page 2, Setting: “A university research chair, or a team of similar size, operates shared infrastructure: Linux hosts with graphics processors reached over Secure Shell (SSH)...”
- Page 5, Structure Resources as an Inheriting Tree: “The objective is met when a grant on a folder with bindable descendants yields exactly provisioning commands...”

### 14. Figure 1 caption is not in title case

`MINOR` · `formatting`

Use title case for the Figure 1 caption title, for example “Use Case Diagram of the Access Platform.”

- Page 4, Objective: “Figure 1: Use case diagram of the access platform. Requesters fll in forms and request access, maintainers review, grant, and recertify, and the super admin maintains the resource tree and completes every grant that needs privileged rights.”

### 15. Contraction used

`MINOR` · `writing`

Avoid contractions in the proposal. Replace “doesn’t” with “does not.”

- Page 3, Problem: “The current process doesn’t scale and is the worst bottleneck of the chair.”
