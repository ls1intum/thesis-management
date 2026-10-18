# proposal-p3 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** ACCEPTABLE
- **Score:** 70
- **Findings:** 4
- **Cost:** 10 calls, 0 tool calls, depth 2, 115506 tokens, 141s
- **Coverage:** 9/9 categories

## Summary

The proposal is well-structured with a clear problem statement, well-defined objectives, and a realistic iteration-based schedule, and no structural, figure, or schedule issues were identified. The primary weakness is the pervasive use of first-person 'I' (approximately 24 instances), which directly violates the stated style guidelines and requires a systematic rewrite. Secondary issues include inconsistent terminology across sections, a book reference reduced to a store-page URL, and a slightly incomplete AI transparency statement—all straightforward fixes that will bring the proposal to submission-ready quality.

## Findings

### 1. Excessive use of first-person 'I' throughout the proposal

`MAJOR` · `writing`

The guidelines state: "Avoid 'one,' 'I,' and 'our.' Use 'we' sparingly, only for the thesis approach." The student uses 'I' approximately 20 times in the main body (Abstract, Objective sections, Schedule) and 4 additional times in the AI Transparency Statement. The text should be restructured to use 'we' (sparingly, for the thesis approach) or to omit the subject where the actor is clear from context. For example, 'I develop a self-service platform' becomes 'We develop a self-service platform' or 'This thesis develops a self-service platform'; 'I reach this objective when…' becomes 'This objective is reached when…' or 'We reach this objective when…'. The Schedule bullets can drop the subject entirely: 'I deliver the Keycloak realm' → 'Deliver the Keycloak realm' or 'We deliver the Keycloak realm.'

- Page 2, Abstract: “In this thesis, **I** develop a self-service platform on which every grant carries an expiry date from the moment somebody requests it.”
- Page 2, Abstract: “**I** evaluate the platform at the chair on correctness, administrative efort, and extensibility.”
- Page 4, Objective: “In this thesis, **I** develop a self-service platform on which requesters ask for time-limited access, maintainers decide, and access ends on its own”
- Page 4, Objective: “Following the object-oriented software engineering method of Bruegge and Dutoit, **I** pursue four objectives:”
- Page 5, 4.1 Unify Heterogeneous Resources in One Tree: “**I** model the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems”
- Page 6, 4.1 Unify Heterogeneous Resources in One Tree: “**I** implement the keycloak-group, slack-channel, github-team, and webhook connectors, whose credentials administrators confgure at runtime”
- Page 6, 4.1 Unify Heterogeneous Resources in One Tree: “**I** reach this objective when the webhook connector, written after **I** freeze the framework, needs one new class and one registry entry.”
- Page 7, 4.2 Enforce an Expiry Date on Every Grant: “**I** evaluate correctness with an automated scenario that provisions SSH, Slack, and GitHub access from one approval.”
- Page 7, 4.2 Enforce an Expiry Date on Every Grant: “**I** reach this objective when each access disappears after expiry and, in a second run with the workfow engine disabled on purpose, the drift check reports the remaining SSH membership.”
- Page 8, 4.3 Delegate the Approval of Requests to Maintainers: “Keycloak gates its workfow API on a realm-wide administrative role that maintainers must not hold, so **I** split each decision into a review step”
- Page 8, 4.3 Delegate the Approval of Requests to Maintainers: “**I** reach this objective when administrators perform fewer manual actions and requesters wait less than in the current process.”
- Page 8, 4.4 Automate the Recertifcation of Active Grants: “**I** reach this objective when the platform answers, for any grant, who requested it and why, who reviewed and granted it, when it ended, and who confrmed it since.”
- Page 9, Schedule: “**I** divide the work into six iterations of four weeks, each ending with a vertically integrated increment that a user can try in the web application”
- Page 9, Schedule: “**I** reserve the fnal two weeks, from 18 to 31 March 2027, as bufer for features that slip from earlier iterations.”
- Page 9, Schedule: “**I** deliver the Keycloak realm with OpenID Connect login, SSH key self-service, and the resource tree with inherited maintainers.”

### 2. Inconsistent terminology for the same concepts throughout the proposal

`MINOR` · `writing`

Three pairs of terms are used inconsistently for the same concepts. (1) 'Administrative privileges' (Abstract, Objective) vs. 'administrative rights' (Introduction, Problem) for the elevated system permissions maintainers should not hold. (2) 'Compute hosts' (Abstract, Problem) vs. 'Linux hosts' (Introduction, Figure 3 caption, Objective) for the same group of machines. (3) 'Drift check' (Section 4.2, twice) vs. 'drift report' (Schedule, Iteration 5) for the same deliverable. Pick one term per concept and use it consistently throughout. If 'drift check' and 'drift report' are genuinely different (process vs. output), make that distinction explicit.

- Page 2, Abstract: “lets maintainers decide on requests without **administrative privileges**”
- Page 2, Introduction: “its Fine-Grained Admin Permissions scope **administrative rights** to selected groups”
- Page 3, Problem: “The common workaround hands maintainers full **administrative rights**, which violates the principle of least privilege”
- Page 5, Objective: “when it touches no binding that requires **administrative privileges**”
- Page 2, Abstract: “A university research chair grants access to its **compute hosts**, chat workspace, and code repositories”
- Page 2, Introduction: “**Linux hosts** with graphics processors that its members reach over Secure Shell (SSH)”
- Page 3, Problem: “former students keep login access to the **compute hosts** long after they leave”
- Page 7, Objective: “projects users, groups, and SSH keys to the **Linux hosts** over LDAP”
- Page 7, Objective: “A **drift check** lists every membership that outlives its grant.”
- Page 7, Objective: “the **drift check** reports the remaining SSH membership”
- Page 9, Schedule: “the **drift report**, and e-mail notifications”
- Page 9, Schedule: “the **drift report** lists a membership that outlives its grant”

### 3. Scientific book cited as a store-URL footnote instead of a proper bibliography entry

`MINOR` · `citation`

The book by Bruegge and Dutoit (Object-Oriented Software Engineering Using UML, Patterns, and Java) is referenced in the body text as footnote 3, which contains only a URL to the Pearson store page. As a scientific/academic book, it is an eligible bibliography entry. A bare store-page URL does not provide standard bibliographic details (full title, publisher, edition, year, ISBN). The student should either move the reference into the Bibliography section with full publication details, or at minimum expand the footnote to include the proper bibliographic citation (authors, title, publisher, year, edition) rather than just a product-page URL.

- Page 4, Objective: “Following the object-oriented software engineering method of **Bruegge and Dutoit ³**, I pursue four objectives:”
- Page 4, Objective: “³**https://pearson.com/store/p/object-oriented-software-engineering-using-uml-patterns-and-java/P200000003319**”

### 4. AI transparency statement lacks explicit 'correct and make sense' confirmation language

`MINOR` · `completeness`

The transparency statement confirms the author checked the AI-generated text, but it does not explicitly state the purpose of that checking in the required phrasing. The guidelines expect language similar to 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The student's sentence lists specific corrective actions (revised, verified, compiled, inspected) but omits the explicit confirmation that the checking was done to ensure correctness and coherence. Adding a clause such as 'to ensure that they are correct and make sense' would fully satisfy the requirement.

- Page 12, Transparency in the Use of AI Tools: “I carefully checked all AI-generated text, revised it where necessary, verified every citation against the original publication, and compiled and inspected every diagram.”
