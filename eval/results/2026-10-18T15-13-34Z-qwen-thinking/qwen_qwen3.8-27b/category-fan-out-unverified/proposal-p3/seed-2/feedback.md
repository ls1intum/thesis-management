# proposal-p3 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** ACCEPTABLE
- **Score:** 72
- **Findings:** 4
- **Cost:** 10 calls, 0 tool calls, depth 2, 111392 tokens, 159s
- **Coverage:** 9/9 categories

## Summary

The proposal is well-structured with a clear problem statement, well-defined objectives, a concrete schedule, and appropriate figures; structure, problem/motivation, figures, and schedule sections all passed review without issues. The primary weakness is the pervasive use of first-person singular "I" (~20 instances), which directly violates the stated writing-style guideline and should be replaced with the platform or other actors as subject. Secondary issues include a terminology inconsistency ("compute hosts" vs. "Linux hosts"), a minor gap in the AI transparency statement wording, and a bibliographic formatting suggestion; all are straightforward to fix.

## Findings

### 1. Excessive use of first-person singular "I" throughout the main body

`MAJOR` · `writing`

The writing style guidelines explicitly state to avoid "I." The main body of the proposal (Abstract through Schedule, pages 2–9) uses "I" approximately 20 times (e.g., "I develop," "I evaluate," "I model," "I implement," "I reach this objective," "I divide," "I reserve," and six "I deliver" in the Schedule). The guideline advises using "we" sparingly only for the thesis approach, or restructuring sentences so the platform/actor is the subject. The student should replace most instances of "I" with the platform as subject (e.g., "The platform models resources as a tree…") or use passive-free active constructions. The AI Transparency section (page 12) is a personal disclosure where "I" is expected and can be retained.

- Page 2, Abstract: “In this thesis, **I develop** a self-service platform on which every grant carries an expiry date from the moment somebody requests it.”
- Page 2, Abstract: “**I evaluate** the platform at the chair on correctness, administrative effort, and extensibility.”
- Page 4, Objective: “Following the object-oriented software engineering method of Bruegge and Dutoit, **I pursue** four objectives:”
- Page 5, Objective: “**I model** the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems.”
- Page 6, Objective: “**I implement** the keycloak-group, slack-channel, github-team, and webhook connectors, whose credentials administrators configure at runtime.”
- Page 6, Objective: “**I reach** this objective when the webhook connector, written after **I freeze** the framework, needs one new class and one registry entry.”
- Page 7, Objective: “**I evaluate** correctness with an automated scenario that provisions SSH, Slack, and GitHub access from one approval.”
- Page 7, Objective: “**I reach** this objective when each access disappears after expiry and, in a second run with the workflow engine disabled on purpose, the drift check reports the remaining SSH membership.”
- Page 8, Objective: “Keycloak gates its workflow API on a realm-wide administrative role that maintainers must not hold, so **I split** each decision into a review step.”
- Page 8, Objective: “**I reach** this objective when administrators perform fewer manual actions and requesters wait less than in the current process.”
- Page 8, Objective: “**I reach** this objective when the platform answers, for any grant, who requested it and why, who reviewed and granted it, when it ended, and who confirmed it since.”
- Page 9, Schedule: “**I divide** the work into six iterations of four weeks, each ending with a vertically integrated increment that a user can try in the web application.”
- Page 9, Schedule: “**I reserve** the final two weeks, from 18 to 31 March 2027, as buffer for features that slip from earlier iterations.”
- Page 9, Schedule: “Iteration 1, 1–28 October (Objective 1): **I deliver** the Keycloak realm with OpenID Connect login, SSH key self-service, and the resource tree with inherited maintainers.”

### 2. Inconsistent terminology: "compute hosts" vs "Linux hosts"

`MINOR` · `writing`

The same servers are referred to as "compute hosts" in the Abstract and Problem section, but as "Linux hosts" in the Introduction and in Figure 3. Per the consistency rule, the same concept should always use the same term. Choose one term (e.g., "compute hosts" or "Linux hosts") and use it uniformly throughout the prose.

- Page 2, Abstract: “A university research chair grants access to its **compute hosts**, chat workspace, and code repositories by hand”
- Page 2, Introduction: “Linux hosts with graphics processors that its members reach over Secure Shell (SSH)”
- Page 3, Problem: “former students keep login access to the **compute hosts** long after they leave”
- Page 7, Objective: “Linux host sshd, nslcd, pam_access”

### 3. AI transparency statement omits the explicit purpose clause (correctness and coherence)

`MINOR` · `writing`

The transparency statement says 'I carefully checked all AI-generated text, revised it where necessary, verified every citation against the original publication, and compiled and inspected every diagram.' While this confirms the author checked the text, the guidelines call for language similar to 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The student's version describes the actions taken (revised, verified, compiled, inspected) but omits the explicit purpose clause — that the checking was done to ensure the text is correct and makes sense. The student should add or rephrase to include that purpose, e.g., 'I have carefully checked all AI-generated text to ensure that it is correct and makes sense, and revised it where necessary…'

- Page 12, AI Transparency Statement: “I carefully checked all AI-generated text, revised it where necessary, verified every citation against the original publication, and compiled and inspected every diagram.”

### 4. Bruegge & Dutoit book cited only as a store URL in a footnote

`SUGGESTION` · `citation`

The book by Bruegge and Dutoit ('Object-Oriented Software Engineering: Using UML, Patterns, and Java') is a scientific book, which the guidelines list as a valid bibliography entry type (conference papers, journal articles, scientific books). Currently it appears only as a Pearson store URL in footnote 3 on page 4. Consider adding it as a proper bibliography entry (authors, title, publisher, year, ISBN) and keeping the store URL (or dropping it) as a supplementary footnote. This would give the reference the formal bibliographic treatment expected for a scientific book.

- Page 4, Objective: “Following the object-oriented software engineering method of Bruegge and Dutoit **3**, I pursue four objectives:”
