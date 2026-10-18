# proposal-p4 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 45
- **Findings:** 15
- **Cost:** 10 calls, 0 tool calls, depth 2, 110736 tokens, 179s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a well-motivated research topic with clearly defined objectives, a sensible architecture, and a structured schedule, demonstrating solid technical understanding. However, it suffers from a missing required Abstract section, pervasive first-person usage (16 instances of 'I'), a contraction, inconsistent role terminology, a non-peer-reviewed bibliography entry, and several structural rule violations (solution in Problem section, thesis writing in schedule, missing section introductions). These compliance issues are numerous and must all be addressed before the proposal can be considered submission-ready.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section. The document goes directly from the title page (page 1) to Section 1 Introduction (page 2). The Abstract is a required section and must be 1/3 to 1/2 page in length. The student must add a standalone Abstract before the Introduction that summarises the problem, approach, and expected outcome.

- Page 2, Title Page / Abstract: “**1  Introduction**”

### 2. Contraction: 'doesn't'

`CRITICAL` · `writing`

The guideline states that contractions must not be used. The word 'doesn't' is a contraction of 'does not' and must be replaced with the full form.

- Page 3, Problem: “The current process **doesn't** scale and is the worst bottleneck of the chair.”

### 3. Excessive use of first-person singular 'I' (16 instances)

`MAJOR` · `writing`

The guideline explicitly states to avoid 'I' and to use 'we' sparingly, only for the thesis approach. The proposal uses 'I' 16 times across Sections 4, 5, and the AI Transparency Statement. The student should restructure these sentences to use 'we' (sparingly, for the thesis approach) or adopt an impersonal/third-person construction (e.g., 'The platform will be built…' or 'The thesis builds…').

- Page 4, Objective: “**I will** build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own. **I follow** the object-oriented software engineering method of Bruegge and Dutoit, and Figure 1 shows the actors and use cases of the platform as a use case diagram in the Unifed Modeling Language. **I pursue** four objectives:”
- Page 5, Objective: “**I will** replace the fat list of hosts and channels with a resource tree whose inner nodes are folders and whose leaves each carry a binding to one target system.”
- Page 5, Objective: “I will store the tree with a transitive-closure table of ancestor–descendant pairs and cap the fan-out, so that an approval on the root never reaches the whole infrastructure by accident.”
- Page 5, Objective: “**I will** defne one connector contract that validates a binding, grants and revokes access, and optionally lists the current members, and **I will** implement the keycloak-group, slack-channel, github-team, and webhook connectors on it.”
- Page 6, Objective: “**I will** build declarative request forms whose felds, visibility conditions, and rules turn answers into requests for a resource path, a level, and a duration.”
- Page 6, Objective: “**I will** therefore split the approval: in the review step, the resource owner records a decision without changing Keycloak, and in the grant step, the super admin sets membership and expiry atomically, in bulk for a vetted queue.”
- Page 6, Objective: “**I will** compare the manual administrative actions and the time to working access with the current process.”
- Page 6, Objective: “**I will** attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”
- Page 7, Schedule: “The thesis runs from 1 October 2026 to 31 March 2027, and **I divide** it into iterations of two to four weeks. Each iteration ends with an increment that a user can try in the web interface, and each item names the objective it serves and the outcome that **I demonstrate**.”
- Page 7, Schedule: “Iteration 6 (18 February – 17 March), Objectives 2 and 3: webhook connector and deployment at the chair. Outcome: the correctness scenarios pass, and **I measure** the efort.”
- Page 10, AI Transparency Statement: “**I used** the following generative artifcial intelligence (AI) tool while preparing this proposal.”

### 4. Problem section presents a solution

`MAJOR` · `structure`

The final sentence of the Problem section explicitly proposes a technical solution ("a self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time"). The Problem section must describe only problems and their negative consequences, not propose solutions or alternatives. This sentence should be removed from the Problem section or moved to the Motivation section as part of the positive vision.

- Page 3, Problem: “**A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.**”

### 5. Section 1 (Introduction) has no introductory text before its first subsection

`MAJOR` · `structure`

The section heading '1 Introduction' is immediately followed by subsection '1.1 Setting' with no introductory prose in between. Every chapter/section must include at least one sentence or short paragraph of introductory text before the first subsection heading appears.

- Page 2, Introduction: “**1  Introduction**  **1.1 Setting**”

### 6. Inconsistent terminology: "maintainer" vs. "resource owner"

`MAJOR` · `writing`

The document introduces the role as "maintainer" (defined on p. 3 as "the staff who look after a project") and uses it consistently in Sections 1, 2, 3, 4.1, and in Figure 1. However, in Section 4.3 the same role is suddenly called "resource owner" without any explanation or transition. Because no separate "resource owner" role is defined anywhere in the proposal, the two terms refer to the identical concept and should be unified to avoid reader confusion.

- Page 3, Problem: “**Maintainers**, the staf who look after a project, know whether a request is legitimate but lack the rights to act on it, while the super admin holds the rights but lacks the context.”
- Page 6, Objective: “in the review step, the **resource owner** records a decision without changing Keycloak, and in the grant step, the super admin sets membership and expiry atomically”
- Page 6, Objective: “The **resource owner** completes a request alone when it touches only non-privileged bindings, such as a Slack channel.”

### 7. Objective 2 uses two verbs (double verb violation)

`MAJOR` · `writing`

The objective heading "Design and Implement Connectors with Expiry" and the corresponding bullet "Design and implement connectors with expiry" both use two verbs. The guidelines require one verb per objective. The student should pick a single action verb (e.g., "Implement connectors with expiry" or "Design connectors with expiry") and adjust the subsection heading accordingly.

- Page 4, Objective: “• **Design and implement** connectors with expiry”
- Page 5, Objective: “4.2 **Design and Implement** Connectors with Expiry”

### 8. Non-peer-reviewed documentation source in bibliography

`MAJOR` · `citation`

The entry [Key26] 'Keycloak Server Administration Guide' is an online product documentation page, not a peer-reviewed publication. According to the guidelines, the bibliography must only contain scientific and peer-reviewed publications. Internet sources and documentation links should appear as footnotes, not as bibliography entries. This entry should be removed from the bibliography and, if the citation is still needed in the text, converted to a footnote (similar to how the Slack/GitHub API URLs are already handled as footnote 1 on page 2).

- Page 8, Bibliography: “[Key26]       Keycloak     Authors,    "Keycloak     Server    Administration      Guide. "   Accessed:                                   Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/                                                                                                            latest/server_admin/”
- Page 2, Introduction: “its Fine-Grained       Admin     Permissions     restrict   an  administrative       right  to  a  single                                                                                   group   [Key26].”

### 9. Figure 2 caption is too short and generic

`MAJOR` · `figures`

The caption for Figure 2 is extremely short and generic — just two words: "System Architecture". The guidelines require long, informative captions that describe what the diagram shows, similar to the detailed caption of Figure 1. The caption should describe the components shown (mgmt-ui, mgmt-api, PostgreSQL, ldap-bridge, Keycloak, Slack, GitHub, Linux host), the nature of the connections (OIDC login, API calls, LDAP, bot token), and the architectural pattern (component interactions with the connector framework).

- Page 5, Objective: “Figure 2: **System Architecture**”

### 10. Iteration 7 includes thesis writing and presentation preparation

`MAJOR` · `structure`

Rule 5 explicitly states that the schedule should NOT include thesis writing, documentation writing, or presentation preparation tasks; it should focus on development/research work only. Iteration 7 is dedicated entirely to writing the thesis and preparing the final presentation. This iteration should be removed from the schedule.

- Page 7, Schedule: “Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation.**”

### 11. Missing review confirmation sentence in AI Transparency Statement

`MAJOR` · `completeness`

The AI transparency statement does not include the required confirmation that the author has carefully reviewed all AI-generated text. The guidelines require a sentence similar to: 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' The statement currently ends after the table without this declaration.

- Page 10, AI Transparency Statement: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 12. Problem section exceeds 3/4 page limit

`MINOR` · `structure`

The Problem section (Section 2) spans from the lower portion of page 2 through roughly the first half of page 3, comprising three full paragraphs. This totals approximately 20–22 lines of body text, which is at or slightly above the 3/4-page maximum. The student should trim the Problem section to fit within 3/4 page, e.g. by condensing one of the three paragraphs or moving supporting citations to the Motivation section.

- Page 2, Problem: “**Requesters do not know who manages which resource.** A student who needs a host, a channel, and a repository asks several people and meanwhile borrows the session of a colleague or works under a shared account.”
- Page 3, Problem: “**Access outlives its purpose and leaves no trace.** No grant carries an expiry date, so access piles up and former students keep SSH access months after they leave, the same decay that Schaad et al. [SMJ01] and Kern et al. [Ker+02] report from industry.”

### 13. Section 4.4 second paragraph lacks detail

`MINOR` · `completeness`

The second paragraph of subsection 4.4 consists of a single short sentence ("The platform logs every change with the person who made it, and the admin event log of Keycloak keeps a second copy."). While the subsection technically has two paragraphs, the second one does not provide a detailed description comparable to the other objective subsections. The student should expand this paragraph to describe the recertification workflow in more depth (e.g., what the campaign UI looks like, how escalation thresholds work, what the immutable record contains).

- Page 7, Objective: “**The platform logs every change with the person who made it, and the admin event log of Keycloak keeps a second copy.**”

### 14. Citation placed after the full stop

`MINOR` · `citation`

On page 4, the citation [BBF01] is placed after the period: 'temporal model. [BBF01]'. The guideline requires citations to be placed before the full stop, i.e., 'temporal model [BBF01].'. This is the only instance of this pattern in the document; all other citations are correctly placed before the period.

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their                                                                                                             temporal    model.    [BBF01]”

### 15. Passive voice: 'is met'

`MINOR` · `writing`

The sentence uses passive voice without identifying the actor. Rewrite to use an active construction that names who meets the objective (e.g., restructure to make the platform the agent: 'A grant on a folder with bindable descendants yields exactly provisioning commands and a resource added later inherits the grant without a new request, fulfilling this objective.').

- Page 5, Objective: “The objective **is met** when a grant on a folder with bindable descendants yields exactly provisioning commands and a resource added later inherits the grant without a new request.”
