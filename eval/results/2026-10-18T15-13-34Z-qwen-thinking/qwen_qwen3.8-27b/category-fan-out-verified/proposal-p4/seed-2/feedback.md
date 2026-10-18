# proposal-p4 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 8
- **Cost:** 16 calls, 0 tool calls, depth 3, 117820 tokens, 166s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a well-motivated and technically detailed project on automated resource access management, with clear objectives and a reasonable architecture. However, it has a critical structural gap (the mandatory Abstract is entirely missing) and seven major issues spanning logic (problem section leaking a solution), structural length violations, inconsistent terminology, a prohibited double-verb formulation, an under-described figure caption, a contraction, and an incomplete AI transparency statement. Addressing these issues—particularly adding the Abstract, trimming and refocusing the Problem section, and unifying terminology—will bring the proposal up to submission-ready quality.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `completeness`

The proposal does not contain an Abstract section. The document jumps directly from the title page to Section 1 (Introduction). The guidelines require an Abstract of 1/3 to 1/2 page as a mandatory section. The student must add an Abstract that concisely summarises the problem, motivation, objectives, and expected outcome.

- Page 1, Abstract: “The document jumps directly from the title page to Section 1 (Introduction).”

### 2. Problem section exceeds 3/4 page limit

`MAJOR` · `structure`

The Problem section (Section 2) spans from the lower portion of page 2 through approximately three-quarters of page 3, totalling roughly 1 to 1.25 pages of text. The guidelines stipulate that the Problem section must not exceed 3/4 of a page. The student should condense the problem description, e.g., by merging the three paragraphs (requester confusion, maintainer/admin privilege mismatch, access decay) into a tighter two-paragraph treatment.

- Page 2, Problem: “Requesters do not know who manages which resource. A student who needs a host, a channel, and a repository asks several people and meanwhile borrows the session of a colleague or works under a shared account.”
- Page 3, Problem: “Access outlives its purpose and leaves no trace. No grant carries an expiry date, so access piles up and former students keep SSH access months after they leave, the same decay that Schaad et al. [SMJ01] and Kern et al. [Ker+02] report from industry.”

### 3. Problem section presents a solution

`MAJOR` · `logic`

The final sentence of the Problem section describes a concrete technical solution (a self-service platform that arms the Keycloak workflow engine with an expiry date). The Problem section must only describe problems and their negative consequences, not propose or hint at solutions. Remove this sentence from the Problem section; the solution belongs in the Objectives.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 4. Objective 2 uses a double verb ('Design and implement')

`MAJOR` · `writing`

Objective 2, both in the bullet list and in the subsection heading, uses two verbs joined by 'and': 'Design and implement connectors with expiry'. The guidelines explicitly prohibit double-verb formulations. Choose a single action verb, e.g. 'Implement connectors with expiry' or 'Build connectors with expiry', and apply the same fix to the heading of subsection 4.2.

- Page 4, Objective: “Design and implement connectors with expiry”
- Page 5, Objective: “4.2 Design and Implement Connectors with Expiry”

### 5. Figure 2 caption is too short and generic

`MAJOR` · `figures`

The caption for Figure 2 is only two words: "System Architecture". Per the guidelines, every figure must have a long, informative caption that is descriptive and extensive. The caption should describe what the diagram shows (the components, their relationships, the data flow, the role of each subsystem), similar in depth to the well-written caption of Figure 1. For example, it should mention the mgmt-ui SPA, the mgmt-api with its GrantPlanner/ConnectorRegistry/DeprovisionReaper components, the PostgreSQL database, the Keycloak identity provider, the LDAP bridge to Linux hosts, and the Slack/GitHub managed workspaces, as well as the nature of the connections (OIDC login, user-token API calls, admin API, bot-token API calls, LDAP).

- Page 5, Objective 4.2: “Figure 2: System Architecture”

### 6. Contraction "doesn't" used in Problem section

`MAJOR` · `writing`

The text must not use contractions. The sentence on page 3 uses "doesn't" instead of the full form "does not". This is a direct violation of the no-contractions rule.

- Page 3, Problem: “The current process doesn't scale and is the worst bottleneck of the chair.”

### 7. Inconsistent terminology: "maintainer" vs. "resource owner"

`MAJOR` · `writing`

The same role (the person who looks after a resource/project and reviews requests) is called "maintainer" throughout most of the document but switches to "resource owner" in section 4.3. The use case diagram (Figure 1) labels the actor "Maintainer" with the use case "Review a request," and section 4.4 refers back to "The maintainer decides to keep or revoke each grant" for the recertification step. However, section 4.3 describes the same review step using "the resource owner records a decision" and "The resource owner completes a request alone." Readers may wonder whether these are two distinct roles. Pick one term ("maintainer" is used in 8+ places vs. 2 for "resource owner") and apply it consistently.

- Page 3, Problem: “Maintainers, the staf who look after a project, know whether a request is legitimate but lack the rights to act on it”
- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “in the review step, the resource owner records a decision without changing Keycloak”
- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “The resource owner completes a request alone when it touches only non-privileged bindings”
- Page 6, 4.4 Automate the Recertifcation of Grants: “The maintainer decides to keep or revoke each grant”

### 8. Missing review confirmation sentence in AI transparency statement

`MAJOR` · `completeness`

The AI transparency statement does not include a sentence confirming that the author has carefully checked all AI-generated text. The rules require language similar to: 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The statement ends after the table with no such confirmation. Please add a closing sentence to the transparency statement explicitly confirming that you have reviewed all AI-generated content for correctness and coherence.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”
