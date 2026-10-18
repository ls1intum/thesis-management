# proposal-p4 — single-prompt

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 50
- **Findings:** 12
- **Cost:** 1 calls, 0 tool calls, depth 1, 20845 tokens, 178s
- **Coverage:** 9/9 categories

## Summary

The proposal demonstrates a well-defined problem in shared-infrastructure access management, strong motivation grounded in access-control literature, and two appropriate UML diagrams (use-case and component). However, it is missing the mandatory Abstract section, the Problem section both exceeds the length limit and leaks a solution into its final sentence, the objectives use a prohibited double verb, the bibliography includes a non-peer-reviewed documentation entry, and the schedule contains a thesis-writing iteration. The AI transparency statement also lacks the required review-confirmation sentence. Addressing these structural and compliance gaps will bring the proposal to a submittable standard.

## Findings

### 1. Missing Abstract Section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section. The document jumps directly from the title page (page 1) to '1 Introduction' (page 2). An Abstract of 1/3 to 1/2 page summarising the problem, motivation, objectives, and expected outcome is mandatory.

- Page 2, Introduction: “**1  Introduction**”

### 2. Problem Section Presents a Solution

`MAJOR` · `logic`

The Problem section must describe only problems and their negative consequences, not solutions. The final sentence of the Problem section explicitly proposes a solution: 'A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.' This should be removed from the Problem section or moved to the Motivation/Objectives.

- Page 3, Problem: “**A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.**”

### 3. Double Verb in Objective: 'Design and Implement'

`MAJOR` · `writing`

The bullet point 'Design and implement connectors with expiry' and the subsection heading '4.2 Design and Implement Connectors with Expiry' both use two verbs. The guidelines require one action verb per objective. Choose either 'Design' or 'Implement' (or another single verb such as 'Build' or 'Engineer').

- Page 4, Objective: “• **Design and implement** connectors with expiry”
- Page 5, Objective: “**4.2 Design and Implement** Connectors with Expiry”

### 4. Non-Peer-Reviewed Source in Bibliography

`MAJOR` · `citation`

Entry [Key26] ('Keycloak Server Administration Guide') is software documentation, not a peer-reviewed publication. It should be moved to a footnote where it is cited in the Introduction (section 1.2) rather than listed in the Bibliography.

- Page 8, Bibliography: “[Key26] Keycloak Authors, **"Keycloak Server Administration Guide." Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/**”

### 5. Figure 2 Caption Is Too Short and Generic

`MAJOR` · `figures`

The caption 'Figure 2: System Architecture' is a short, generic label. The guidelines require long, informative captions that describe the content in detail, similar to the caption of Figure 1. Expand the caption to explain the components, the data flow, and the roles of mgmt-ui, mgmt-api, Keycloak, the LDAP bridge, and the external connectors.

- Page 5, Objective: “Figure 2: **System Architecture**”

### 6. Citation [BBF01] Placed After the Full Stop

`MAJOR` · `citation`

The sentence 'Bertino et al. attach enabling and disabling events to roles in their temporal model. [BBF01]' places the citation after the period. The correct pattern is 'in their temporal model [BBF01].' — the citation must come before the full stop.

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model**.** [BBF01]”

### 7. Schedule Contains Thesis-Writing and Presentation Tasks

`MAJOR` · `structure`

Iteration 7 ('Write the thesis and prepare the final presentation') violates the rule that the schedule must focus on development/research work only. Thesis writing and presentation preparation should be removed from the schedule. If time is needed, it is outside the scope of the iteration plan.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation.**”

### 8. AI Transparency Statement Missing Review-Confirmation Sentence

`MAJOR` · `completeness`

The transparency statement does not include the required confirmation that the author carefully checked all AI-generated text. A sentence such as 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' must be added.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 9. Problem Section Exceeds 3/4 Page Limit

`MAJOR` · `structure`

The Problem section spans from roughly 60% down page 2 to roughly 80% down page 3, totalling approximately one full page of text. The guideline caps the Problem section at 3/4 page. Condense the description (e.g., merge the actor paragraphs or trim the literature references) to fit within the limit.

- Page 3, Problem: “**Requesters do not know who manages which resource.** … **A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.**”

### 10. No Introductory Text Before First Subsection of Section 1

`MINOR` · `structure`

Section '1 Introduction' jumps directly into subsection '1.1 Setting' without any introductory sentence or paragraph. Every section must contain at least one or two sentences of context before its first subsection heading.

- Page 2, Introduction: “**1  Introduction**

**1.1 Setting**”

### 11. Contraction 'doesn't' Used in Problem Section

`MINOR` · `writing`

The sentence 'The current process doesn't scale and is the worst bottleneck of the chair.' uses the contraction 'doesn't.' Academic writing requires the full form: 'does not.'

- Page 3, Problem: “The current process **doesn't** scale and is the worst bottleneck of the chair.”

### 12. Inconsistent Terminology: 'Maintainer' vs. 'Resource Owner'

`MINOR` · `formatting`

The Problem and Motivation sections consistently use 'maintainer' to denote the person who judges access requests. In Section 4.3, the same role is called 'the resource owner' ('the resource owner records a decision', 'The resource owner completes a request alone'). Use one term consistently throughout the proposal.

- Page 3, Problem: “**Maintainers**, the staf who look after a project, know whether a request is legitimate”
- Page 6, Objective: “in the review step, the **resource owner** records a decision without changing Keycloak”
