# proposal-p4 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 38
- **Findings:** 12
- **Cost:** 10 calls, 0 tool calls, depth 2, 106277 tokens, 163s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a well-motivated and technically interesting problem with a clearly structured set of four objectives, a concrete architecture, and a realistic iteration-based schedule. However, three critical violations (missing Abstract, a solution embedded in the Problem section, and a schedule entry for thesis writing) combined with numerous major issues (systematic use of first-person 'I', a documentation source in the bibliography, an undersized figure caption, a single-paragraph objective, missing section introduction, and an incomplete AI transparency statement) mean the document is not yet submission-ready. The content quality is solid, but substantial structural and stylistic rework is needed before the proposal meets the stated guidelines.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `structure`

The proposal does not contain an Abstract section. The document transitions directly from the title/metadata page (page 1) to '1 Introduction' on page 2, with no Abstract in between. The guidelines require an Abstract of 1/3 to 1/2 page as the first section of the proposal body.

- Page 1, Title/Metadata: “Document transitions from page 1 directly to '1 Introduction' on page 2 with no Abstract”
- Page 2, Introduction: “1 Introduction”

### 2. Problem section presents a concrete solution

`CRITICAL` · `structure`

The Problem section must describe only problems and their negative consequences, not solutions. The final sentence of Section 2 explicitly proposes a specific solution mechanism (a self-service platform leveraging Keycloak's workflow engine with expiry dates), which belongs in the Objective or Motivation section instead.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Iteration 7 contains thesis writing and presentation preparation tasks

`CRITICAL` · `structure`

Rule 5 states that the schedule must NOT include thesis writing, documentation writing, or presentation preparation tasks. Iteration 7 (18–31 March) is dedicated entirely to 'Write the thesis and prepare the final presentation.' This is a non-development, non-research task that should be removed from the schedule. The student should either remove Iteration 7 entirely or replace its content with a development/testing/refinement iteration that delivers a measurable increment tied to one of the four objectives.

- Page 7, Schedule: “Iteration 7 (18–31 March): Write the thesis and prepare the final presentation.”

### 4. Excessive use of first-person pronoun 'I'

`MAJOR` · `writing`

The guidelines explicitly state to avoid 'I' and to use 'we' sparingly only for the thesis approach. The student uses 'I' as the subject 17 times across the Objective, Schedule, and AI Transparency sections. The text should be rewritten to use third-person constructions (e.g., 'The platform is designed to…', 'This thesis builds…') or, where the thesis approach is described, a sparing use of 'we'. The systematic reliance on 'I' gives the proposal an informal, diary-like tone rather than the formal academic register expected.

- Page 4, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 4, Objective: “I follow the object-oriented software engineering method of Bruegge and Dutoit, and Figure 1 shows the actors and use cases of the platform as a use case diagram in the Unified Modeling Language.”
- Page 5, Objective: “I will replace the flat list of hosts and channels with a resource tree whose inner nodes are folders and whose leaves each carry a binding to one target system.”
- Page 5, Objective: “I will define one connector contract that validates a binding, grants and revokes access, and optionally lists the current members, and I will implement the keycloak-group, slack-channel, github-team, and webhook connectors on it.”
- Page 6, Objective: “I measure extensibility by the size of the change that the webhook connector needs after I freeze the framework.”
- Page 6, Objective: “I will build declarative request forms whose fields, visibility conditions, and rules turn answers into requests for a resource path, a level, and a duration.”
- Page 6, Objective: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”
- Page 7, Schedule: “The thesis runs from 1 October 2026 to 31 March 2027, and I divide it into iterations of two to four weeks.”
- Page 7, Schedule: “each item names the objective it serves and the outcome that I demonstrate.”
- Page 10, AI Transparency Statement: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”

### 5. Double verb in Objective 4.2 heading and bullet

`MAJOR` · `writing`

Objective 4.2 uses two verbs ('Design and implement'), which violates the rule that each objective must use a single action verb. The guidelines explicitly cite 'Design and implement' as the example of what to avoid. Choose one verb, e.g. 'Implement Connectors with Expiry' or 'Design Connectors with Expiry'.

- Page 4, Objective: “• Design and implement connectors with expiry”
- Page 5, Objective: “Design and Implement Connectors with Expiry”

### 6. Objective 4.4 contains only one paragraph

`MAJOR` · `structure`

Each objective subsection must be at least two paragraphs long with detailed descriptions. Section 4.4 ('Automate the Recertification of Grants') consists of a single paragraph that flows from page 6 into page 7 without a paragraph break. A second paragraph is needed to add detail (e.g., describing the escalation policy in more depth, the audit-log integration, or success criteria).

- Page 6, Objective: “I will attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate and may revoke grants automat-ically, but never in a campaign that reached nobody. The platform logs every”
- Page 7, Objective: “change with the person who made it, and the admin event log of Keycloak keeps a second copy.”

### 7. Documentation source in bibliography instead of footnote

`MAJOR` · `citation`

Entry [Key26] (Keycloak Server Administration Guide) is a software documentation / internet source, not a peer-reviewed publication. Per the guidelines, the bibliography must only contain peer-reviewed publications (conference papers, journal articles, scientific books). Internet sources and documentation links should appear as footnotes, not as bibliography entries. Remove [Key26] from the bibliography and convert the reference in the text into a footnote with the URL, similar to the existing footnote 1 for the Slack/GitHub API URLs on page 2.

- Page 8, Bibliography: “[Key26] Keycloak Authors, "Keycloak Server Administration Guide." Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”
- Page 2, Introduction: “its Fine-Grained Admin Permissions restrict an administrative right to a single group [Key26].”

### 8. Figure 2 caption is too short and generic

`MAJOR` · `figures`

The caption for Figure 2 is simply 'System Architecture', which is a two-word generic label. The guidelines require captions to be long and informative — descriptive and extensive, explaining what the diagram shows and how to read it. Compare with Figure 1's caption, which is several sentences long. Figure 2's caption should describe the components shown (mgmt-ui, mgmt-api, ConnectorRegistry, PostgreSQL, Keycloak, Slack/GitHub, Linux host, ldap-bridge), the types of connections (OIDC login, admin API, LDAP, bot token calls), and the overall data/control flow the diagram illustrates.

- Page 5, Objective: “Figure 2: System Architecture”

### 9. Section 1 lacks introductory text before first subsection

`MAJOR` · `structure`

Section 1 (Introduction) jumps directly from the section heading to subsection 1.1 (Setting) without any introductory prose. Every section that contains subsections must include at least a short introductory paragraph before the first subsection heading. Add 2-4 sentences before '1.1 Setting' that orient the reader to what the Introduction covers (e.g., the context of the chair, what the proposal addresses, and a brief roadmap of the section).

- Page 2, Introduction: “1 Introduction

1.1 Setting”

### 10. Missing review confirmation sentence in AI transparency statement

`MAJOR` · `completeness`

The AI transparency statement does not include the required sentence confirming that the author has carefully checked all AI-generated text. The guidelines require language similar to: 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.' This confirmation is a mandatory component of the statement and is entirely absent from page 10.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 11. Citation placed after the full stop

`MINOR` · `citation`

On page 4, the citation [BBF01] appears after the period: 'temporal model. [BBF01]' The guidelines require citations to be placed before the full stop, i.e., 'temporal model [BBF01].' Move the citation inside the sentence, before the period.

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model. [BBF01]”

### 12. Contraction 'doesn't' used in the Problem section

`MINOR` · `writing`

The guidelines prohibit contractions. The sentence on page 3 uses "doesn't" instead of "does not". This must be corrected to 'does not'.

- Page 3, Problem: “The current process doesn't scale and is the worst bottleneck of the chair.”
