# proposal-p4 — category-fan-out-verified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 48
- **Findings:** 10
- **Cost:** 17 calls, 0 tool calls, depth 3, 125312 tokens, 216s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a well-developed technical concept with detailed sub-objectives and a clear schedule, but it has a critical structural gap (missing Abstract) and numerous major issues including pervasive first-person "I" usage (~15 instances), a solution statement misplaced in the Problem section, inconsistent role terminology, a non-peer-reviewed bibliography entry, an underspecified figure caption, and a missing AI review confirmation sentence. Addressing these issues—particularly the Abstract, the first-person rewrite, and the structural/citation fixes—would bring the proposal into submittable shape.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section. The document transitions directly from the title page (Page 1) to Section 1 'Introduction' (Page 2). The guidelines require an Abstract of 1/3 to 1/2 page as the first content section after the title page.

- Page 1, Title Page: “No Abstract section present between title page and Section 1 Introduction”

### 2. Problem section contains a proposed solution

`MAJOR` · `structure`

The last sentence of the Problem section proposes a specific solution (a self-service platform arming the Keycloak workflow engine with an expiry date). The Problem section must only describe problems and their negative consequences, not present solutions or alternatives. This sentence should be removed from the Problem section (it may belong in Motivation or Objectives instead).

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Pervasive use of first-person singular "I"

`MAJOR` · `writing`

The guidelines state to avoid "I" and use "we" sparingly, only for the thesis approach. The student uses "I" in approximately 15 instances across the Objective, sub-objective, and Schedule sections. Restructure sentences to name the thesis/work as the subject (e.g., "The platform will…", "This work stores…") or use "we" sparingly for the thesis approach only. The AI Transparency statement (page 10) is a natural exception where first person is expected.

- Page 4, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 4, Objective: “I follow the object-oriented software engineering method of Bruegge and Dutoit”
- Page 5, 4.1 Structure Resources as an Inheriting Tree: “I will replace the fat list of hosts and channels with a resource tree whose inner nodes are folders”
- Page 5, 4.1 Structure Resources as an Inheriting Tree: “I will store the tree with a transitive-closure table of ancestor–descendant pairs”
- Page 5, 4.2 Design and Implement Connectors with Expiry: “I will defne one connector contract that validates a binding, grants and revokes access, and optionally lists the current members, and I will implement the keycloak-group, slack-channel, github-team, and webhook connectors on it.”
- Page 6, 4.2 Design and Implement Connectors with Expiry: “I measure extensibility by the size of the change that the webhook connector needs after I freeze the framework.”
- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “I will build declarative request forms whose felds, visibility conditions, and rules turn answers into requests”
- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “I will therefore split the approval: in the review step, the resource owner records a decision without changing Keycloak”
- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “I will compare the manual administrative actions and the time to working access with the current process.”
- Page 6, 4.4 Automate the Recertifcation of Grants: “I will attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”
- Page 7, Schedule: “The thesis runs from 1 October 2026 to 31 March 2027, and I divide it into iterations of two to four weeks.”

### 4. Objective 4.2 uses a double-verb formulation

`MAJOR` · `writing`

The objective heading and bullet point 'Design and implement connectors with expiry' combine two verbs ('Design' and 'implement'), which the guidelines explicitly prohibit. The student should choose one verb (e.g., 'Implement connectors with expiry' or 'Design connectors with expiry') to keep the objective as a single action.

- Page 4, Objective: “Design and implement connectors with expiry”
- Page 5, Objective: “4.2 Design and Implement Connectors with Expiry”

### 5. Inconsistent terminology: "maintainer" vs. "resource owner"

`MAJOR` · `writing`

The role of the person who reviews access requests and looks after a project is called "maintainer" (or "Maintainer") throughout most of the document (Problem, Motivation, Objective 4.1, Figure 1, Schedule), but in section 4.3 the same role is referred to as "resource owner" (twice). The term should be unified to "maintainer" (or, if "resource owner" is the intended term, it should be introduced explicitly in an earlier section and used consistently thereafter).

- Page 3, Problem: “Maintainers, the staf who look after a project, know whether a request is legitimate but lack the rights to act on it”
- Page 4, Motivation: “The maintainer keeps the judgement over a request without holding any privilege in the identity provider”
- Page 6, Delegate Request Review Without Privilege Escalation: “in the review step, the resource owner records a decision without changing Keycloak”
- Page 6, Delegate Request Review Without Privilege Escalation: “The resource owner completes a request alone when it touches only non-privileged bindings, such as a Slack channel.”

### 6. Section 1 (Introduction) lacks introductory text before its first subsection

`MAJOR` · `structure`

Section 1 jumps directly into subsection 1.1 (Setting) without any introductory paragraph. Per the guidelines, every chapter/section must include introductory text before its first subsection. A brief introductory sentence or two summarising the purpose of the Introduction section should be added between the '1 Introduction' heading and '1.1 Setting'.

- Page 2, Introduction: “1  Introduction

1.1 Setting”

### 7. Non-peer-reviewed source in bibliography: Keycloak documentation guide

`MAJOR` · `citation`

The entry [Key26] 'Keycloak Server Administration Guide' is an online documentation guide, not a peer-reviewed publication. Per the guidelines, internet sources and documentation links must appear as footnotes, not as bibliography entries. This entry should be removed from the Bibliography and converted to a footnote wherever it is cited in the text (Section 1.2). The remaining 12 entries are all peer-reviewed, so the 6–8 minimum is still met.

- Page 8, Bibliography: “[Key26]       Keycloak     Authors,    "Keycloak     Server    Administration      Guide. "   Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”
- Page 2, Introduction: “its Fine-Grained Admin Permissions restrict an administrative right to a single group [Key26].”

### 8. Figure 2 caption is too short and generic

`MAJOR` · `figures`

The caption for Figure 2 reads only "System Architecture" (two words). The guidelines require captions to be long and informative, describing what the figure shows, which components are involved, how they connect, and what the key relationships represent. Compare this with Figure 1's caption, which is a full paragraph. The caption for Figure 2 should at minimum name the components (mgmt-ui, mgmt-api, Keycloak, Slack, GitHub, Linux host, PostgreSQL, ldap-bridge), describe the key connections (OIDC login, admin API, bot token, LDAP), and explain the architectural pattern.

- Page 5, Objective: “Figure 2: System Architecture”

### 9. Missing review confirmation sentence in AI transparency statement

`MAJOR` · `completeness`

The AI transparency statement is missing the required review confirmation sentence. The guidelines mandate a sentence confirming that the author has carefully checked all AI-generated text, with language similar to: "I have carefully checked all texts created with these tools to ensure that they are correct and make sense." No such sentence appears in the statement, which ends after the table without any concluding confirmation of review.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 10. Contraction used: "doesn't"

`MINOR` · `writing`

The text contains the contraction "doesn't" which violates the no-contractions rule. Replace with "does not".

- Page 3, Problem: “The current process doesn't scale and is the worst bottleneck of the chair.”
