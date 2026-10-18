# proposal-p4 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 22
- **Findings:** 14
- **Cost:** 10 calls, 0 tool calls, depth 2, 94203 tokens, 80s
- **Coverage:** 9/9 categories

## Summary

The proposal contains several critical structural and compliance issues, including a missing abstract, excessive length, and violations in AI transparency and scheduling. Additionally, numerous major writing and citation problems need correction before the document can be considered ready for submission.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `structure`

The proposal does not contain the required Abstract section.

### 2. Proposal length exceeds limit

`CRITICAL` · `structure`

The total length of the proposal (excluding metadata, figures, tables, and schedule) is longer than the allowed maximum of 6 pages.

- Page 1: “=== PAGE 1 ===”

### 3. Problem section presents a proposed solution

`CRITICAL` · `logic`

The Problem section should only describe the problem and its negative consequences, but it mentions a self-service platform that would resolve the issue, which is a solution.

- Page 3, Problem: “A **self-service platform that arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 4. Non‑peer‑reviewed entry in bibliography

`CRITICAL` · `citation`

The bibliography includes an entry for a documentation guide, which is not a peer‑reviewed scientific publication. Internet sources should be placed in footnotes, not the bibliography.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “**Keycloak Server Administration Guide**.” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 5. Missing AI review confirmation sentence

`CRITICAL` · `writing`

The AI transparency statement does not include a sentence confirming that the author has carefully checked all AI‑generated text, as required by the guidelines.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. ... (no sentence confirming careful checking of AI‑generated text is present)”

### 6. Thesis writing tasks included in schedule

`CRITICAL` · `other`

The schedule contains an iteration dedicated to writing the thesis and preparing the final presentation, which violates the rule that thesis writing tasks must not be part of the development schedule.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis** and prepare the final presentation.”

### 7. Problem section too long

`MAJOR` · `structure`

The Problem section exceeds the maximum allowed length of three quarters of a page.

- Page 2, Problem: “2  Problem       Requesters do not know who manages which resource...”

### 8. Objective uses double verbs

`MAJOR` · `writing`

Objective bullet combines two verbs "Design and implement", violating the rule that each objective should contain only a single action verb.

- Page 4, Objective: “**Design and implement** connectors with expiry”

### 9. Citation placed after full stop

`MAJOR` · `citation`

Citation [BBF01] appears after a period instead of before it, violating the required citation placement rule.

- Page 4, Objective: “temporal model. **[BBF01]** Joshi et al. generalise this idea to role assignments with a limited duration [Jos+05].”

### 10. Passive voice usage

`MAJOR` · `writing`

The sentence uses passive voice; rewrite in active voice to clarify the actor.

- Page 4, Objective: “The objective **is met** when a grant on a folder with bindable descendants yields exactly provisioning commands and a resource added later inherits the grant without a new request.”

### 11. Contraction present

`MAJOR` · `writing`

Contractions are not allowed; replace with the full form.

- Page 3, Problem: “The current process **doesn’t** scale and is the worst bottleneck of the chair.”

### 12. Paragraphs exceed recommended length

`MAJOR` · `writing`

Several paragraphs in Problem, Motivation, and subsections 4.1 and 4.3 exceed the recommended 5‑8 line length.

- Page 2, Problem: “**Requesters       do  not  know    who   manages     which    resource.   A  student    who   needs** a host, a channel, and a repository asks several people and meanwhile borrows the session of a colleague or works under a shared account. Bauer et al. observed this workaround in far more mature organisations [Bau+09], and Beautement et al.”
- Page 3, Motivation: “**Access    that  ends   on   its own    changes    the question     an  organisation     answers:** it no longer asks who still needs a grant, but only who wants more time. The requester describes a need once, in terms of a project, and receives working access within minutes.”
- Page 5, 4.1 Structure Resources as an Inheriting Tree: “**I will  replace   the   fat  list of  hosts   and   channels    with   a  resource    tree  whose** inner nodes are folders and whose leaves each carry a binding to one target system.”
- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “**A  requester    should    not  need   to  know    which   resources    a  project   needs.** I will build declarative request forms whose fields, visibility conditions, and rules turn answers into requests for a resource path, a level, and a duration.”

### 13. Caption too generic

`MINOR` · `figures`

Figure 2 caption is overly short and not informative; it should provide a detailed description of the architecture components shown in the diagram.

- Page 5, 4.2 Design and Implement Connectors with Expiry: “Figure  2: **System Architecture**”

### 14. Undefined abbreviation

`MINOR` · `writing`

The abbreviation "AI" is used without being defined on first occurrence.

- Page 10, Transparency: “I used the following **AI** tool while preparing this proposal.”
