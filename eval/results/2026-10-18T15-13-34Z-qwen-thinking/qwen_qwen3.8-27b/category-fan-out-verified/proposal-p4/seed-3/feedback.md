# proposal-p4 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 7
- **Cost:** 16 calls, 0 tool calls, depth 3, 117801 tokens, 135s
- **Coverage:** 9/9 categories

## Summary

The proposal demonstrates a solid understanding of the technical domain (Keycloak, workflow automation) and contains substantive content, but it fails on several structural and formatting requirements. The most serious gaps are the completely missing Abstract section and a contraction violating the no-contractions rule, while the Problem section inappropriately presents a solution. Five additional major issues (double-verb objective, generic figure caption, missing section intro text, and incomplete AI transparency statement) must also be resolved before the proposal can be considered submission-ready.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `completeness`

The proposal does not contain an Abstract section, which is a required section. The document jumps directly from the title page (page 1) to the Introduction (page 2). An Abstract of 1/3 to 1/2 page summarising the problem, approach, and expected outcome must be added before the Introduction.

- Page 1, Abstract: “No Abstract present”

### 2. Contraction "doesn't" used in Problem section

`CRITICAL` · `writing`

The text uses the contraction "doesn't", which violates the no-contractions rule. The sentence should be rewritten as "The current process does not scale."

- Page 3, Problem: “The current process **doesn't** scale and is the worst bottleneck of the chair.”

### 3. Problem section presents a solution

`MAJOR` · `logic`

The last sentence of the Problem section explicitly proposes a solution ('A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.'). The Problem section must describe only problems and their negative consequences, without presenting any solutions or alternatives. This sentence should be removed from the Problem section; the idea belongs in the Motivation or Objective section.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 4. Objective 2 uses double verbs ('Design and implement')

`MAJOR` · `writing`

The heading and bullet for Objective 2 is 'Design and implement connectors with expiry'. The guidelines explicitly prohibit double-verb formulations and cite 'Design and implement' as the canonical example to avoid. The student should choose a single action verb (e.g., 'Implement connectors with expiry' or 'Design connectors with expiry') and adjust the subsection title accordingly.

- Page 4, Objective: “•   **Design and implement** connectors with expiry”
- Page 5, Objective: “4.2 **Design and Implement** Connectors with Expiry”

### 5. Figure 2 caption is too short and generic

`MAJOR` · `figures`

Figure 2's caption is 'System Architecture' — a short, generic label that does not inform the reader about what the diagram depicts. Per the guidelines, captions must be long and descriptive. The caption should explain what components are shown (mgmt-ui, mgmt-api, PostgreSQL, ldap-bridge, Keycloak, Slack, GitHub, Linux host), how they are connected (OIDC login, admin API calls, LDAP, bot tokens), and what the diagram conveys about the platform's architecture. Compare with Figure 1's caption, which is appropriately detailed.

- Page 5, Objective: “Figure 2: **System Architecture**”

### 6. Section 1 (Introduction) lacks introductory text before first subsection

`MAJOR` · `structure`

Rule 5 requires every chapter/section to include introductory text before its first subsection. Section 1 "Introduction" jumps directly from the section heading to subsection 1.1 "Setting" with no introductory paragraph. A brief 1-3 sentence paragraph summarising what the introduction covers (the setting, the existing infrastructure, and the scope of the proposal) should be inserted between the section heading and 1.1.

- Page 2, Introduction: “**1  Introduction** 1.1 Setting”

### 7. Missing confirmation sentence in AI transparency statement

`MAJOR` · `completeness`

The AI transparency statement lacks the required confirmation sentence stating that the author has carefully checked all AI-generated text for correctness and coherence. The guidelines require language similar to: 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The statement currently ends after the table with no such attestation. The student must add this sentence to complete the statement.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artifcial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”
