# proposal-p3 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** ACCEPTABLE
- **Score:** 68
- **Findings:** 4
- **Cost:** 10 calls, 0 tool calls, depth 2, 115223 tokens, 147s
- **Coverage:** 9/9 categories

## Summary

The proposal demonstrates a well-structured, technically concrete research plan with clear objectives, a detailed schedule, and no structural or methodological gaps. However, the pervasive use of first-person singular "I" (24+ instances) directly violates the stated guidelines and must be rewritten throughout. Additional minor issues include inconsistent terminology for the drift mechanism, a missing required clause in the AI transparency statement, and a textbook cited only via a store URL rather than as a proper bibliography entry.

## Findings

### 1. Pervasive use of first-person singular "I" violates explicit guideline

`MAJOR` · `writing`

The guidelines explicitly state to avoid "I." The proposal uses "I" as the subject 24 or more times across the Abstract, Objectives, and Schedule sections. The student should restructure sentences to use "the thesis," "this work," "the platform," or the system itself as the active subject (e.g., "This thesis develops a self-service platform…", "The platform models resources as a tree…", "Each iteration delivers…"). The AI Transparency Statement is the one context where first person is arguably unavoidable, but even there, passive or impersonal constructions are preferable.

- Page 2, Abstract: “In this thesis, **I develop** a self-service platform on which every grant carries an expiry date from the moment somebody requests it.”
- Page 2, Abstract: “**I evaluate** the platform at the chair on correctness, administrative effort, and extensibility.”
- Page 4, Objective: “In this thesis, **I develop** a self-service platform on which requesters ask for time-limited access, maintainers decide, and access ends on its own.”
- Page 4, Objective: “Following the object-oriented software engineering method of Bruegge and Dutoit, **I pursue** four objectives:”
- Page 5, 4.1 Unify Heterogeneous Resources in One Tree: “**I model** the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems.”
- Page 6, 4.1 Unify Heterogeneous Resources in One Tree: “**I implement** the keycloak-group, slack-channel, github-team, and webhook connectors, whose credentials administrators configure at runtime.”
- Page 6, 4.1 Unify Heterogeneous Resources in One Tree: “**I reach** this objective when the webhook connector, written after I freeze the framework, needs one new class and one registry entry.”
- Page 7, 4.2 Enforce an Expiry Date on Every Grant: “**I evaluate** correctness with an automated scenario that provisions SSH, Slack, and GitHub access from one approval.”
- Page 7, 4.2 Enforce an Expiry Date on Every Grant: “**I reach** this objective when each access disappears after expiry and, in a second run with the workflow engine disabled on purpose, the drift check reports the remaining SSH membership.”
- Page 8, 4.3 Delegate the Approval of Requests to Maintainers: “Keycloak gates its workflow API on a realm-wide administrative role that maintainers must not hold, so **I split** each decision into a review step, in which the maintainer records a decision without changing Keycloak, and a grant step, in which an administrator sets membership and expiry together.”
- Page 8, 4.3 Delegate the Approval of Requests to Maintainers: “**I reach** this objective when administrators perform fewer manual actions and requesters wait less than in the current process.”
- Page 8, 4.4 Automate the Recertification of Active Grants: “**I reach** this objective when the platform answers, for any grant, who requested it and why, who reviewed and granted it, when it ended, and who confirmed it since.”
- Page 9, Schedule: “**I divide** the work into six iterations of four weeks, each ending with a vertically integrated increment.”
- Page 9, Schedule: “**I reserve** the final two weeks, from 18 to 31 March 2027, as buffer for features that slip from earlier iterations.”
- Page 9, Schedule: “Iteration 1, 1–28 October (Objective 1): **I deliver** the Keycloak realm with OpenID Connect login, SSH key self-service, and the resource tree with inherited maintainers.”

### 2. Inconsistent terminology: "drift check" vs "drift report"

`MINOR` · `writing`

The same concept is referred to as a "drift check" in Section 4.2 and as a "drift report" in the Schedule (Iteration 5). In both places it is described as the thing that "lists" or "reports" memberships that outlive their grants, indicating they refer to the same deliverable/mechanism. Pick one term and use it throughout.

- Page 7, Objective – 4.2 Enforce an Expiry Date on Every Grant: “A **drift check** lists every membership that outlives its grant.”
- Page 7, Objective – 4.2 Enforce an Expiry Date on Every Grant: “the **drift check** reports the remaining SSH membership.”
- Page 9, Schedule: “recertifcation campaigns with escalation, the **drift report**, and e-mail notifca tions.”
- Page 9, Schedule: “and the **drift report** lists a membership that outlives its grant.”

### 3. AI transparency statement lacks the required 'correct and make sense' clause

`MINOR` · `completeness`

The transparency statement confirms the author checked AI-generated text but does not include the specific required phrasing about ensuring the text is correct and makes sense. The guidelines call for language similar to: 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The student's sentence omits the explicit 'to ensure that they are correct and make sense' clause. The student should add this clause to the checking sentence to fully satisfy the requirement.

- Page 12, Transparency in the Use of AI Tools: “I carefully checked all AI-generated text, revised it where necessary, verifed every citation against the original publication, and compiled and inspected every diagram.”

### 4. Textbook cited via store URL in footnote rather than as a proper bibliography entry

`SUGGESTION` · `citation`

The Bruegge and Dutoit textbook is referenced only via a commercial store URL (pearson.com) in footnote 3. Since the group guidelines explicitly allow 'scientific books' in the bibliography, this reference would be more appropriately listed as a proper bibliography entry with full bibliographic details (authors, title, edition, publisher, year). The store-page URL can remain as a supplementary footnote if desired, but the primary citation should appear in the reference list with standard book formatting.

- Page 4, Objective: “Following the object-oriented software engineering method of Bruegge and Dutoit ³, I pursue four objectives:”
- Page 4, Objective: “³https://pearson.com/store/p/object-oriented-software-engineering-using-uml-patterns-and-java/P200000003319”
