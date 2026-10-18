# proposal-p4 — category-fan-out-unverified

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 12
- **Cost:** 10 calls, 0 tool calls, depth 2, 107436 tokens, 166s
- **Coverage:** 9/9 categories

## Summary

The proposal addresses a well-defined and technically interesting problem (automating time-limited access provisioning) with a clear four-objective structure and a reasonable schedule. However, it fails to meet several fundamental formatting and structural requirements: the mandatory Abstract is entirely missing, the Problem section violates the no-solution rule, the Introduction lacks an opening paragraph, and the schedule includes a thesis-writing iteration. Widespread use of first-person 'I' (15+ instances), a contraction, a double-verb objective, a generic figure caption, an improperly classified bibliography entry, and the missing AI attestation sentence collectively bring the document well below submission-ready quality. Addressing the critical and major items above is necessary before resubmission.

## Findings

### 1. Missing required Abstract section

`CRITICAL` · `completeness`

The proposal contains no Abstract section. The document proceeds directly from the title page (page 1) to Section 1 'Introduction' on page 2. The guidelines require an Abstract of 1/3 to 1/2 page that provides a high-level summary of the thesis problem, approach, and expected contribution. The student must add an Abstract section before the Introduction.

- Page 1, Title Page: “Document proceeds from title page directly to Section 1 Introduction on page 2 with no Abstract in between.”

### 2. Problem section presents a solution

`MAJOR` · `structure`

The final sentence of the Problem section introduces and describes a concrete solution ('a self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time'). The Problem section is restricted to describing problems and their negative consequences for identified actors; it must not propose solutions or alternatives. Remove this sentence or relocate the idea to the Motivation or Objective section.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Objective subsection 4.4 contains only one paragraph

`MAJOR` · `structure`

Subsection 4.4 ('Automate the Recertification of Grants') consists of a single continuous paragraph spanning pages 6–7. The guidelines require each objective subsection to be at least two paragraphs long with detailed descriptions. Split the content into at least two distinct paragraphs — for example, one covering the recertification interval, campaign mechanics, and escalation, and a second covering the audit/logging requirements and the immutable-record design.

- Page 6, Objective 4.4: “I will attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record.”
- Page 7, Objective 4.4: “change with the person who made it, and the admin event log of Keycloak keeps a second copy.”

### 4. Section 1 (Introduction) has no introductory text before its first subsection

`MAJOR` · `structure`

Section 1 'Introduction' jumps directly into subsection 1.1 'Setting' without any introductory prose. Every chapter/section must include at least a short introductory paragraph before its first subsection, orienting the reader on what the section covers. Add 2-3 sentences under the '1 Introduction' heading that frame the overall context of the thesis before diving into 1.1.

- Page 2, Introduction: “1 Introduction

1.1 Setting

A university research chair, or a team of similar size, operates shared infrastructure…”

### 5. Iteration 7 contains thesis writing and presentation preparation tasks

`MAJOR` · `structure`

Iteration 7 (18–31 March) is dedicated to writing the thesis and preparing the final presentation. The schedule should focus on development/research work only and must not include thesis writing, documentation writing, or presentation preparation tasks. This iteration should be removed or replaced with a development increment (e.g., final integration testing, performance validation, or additional connector work) so that every iteration delivers a measurable, usable increment of the platform.

- Page 7, Schedule: “Iteration 7 (18–31 March): Write the thesis and prepare the final presentation.”

### 6. Objective 2 uses a double verb ("Design and implement")

`MAJOR` · `writing`

Objective 2, both in the bullet list under Section 4 and in the heading of subsection 4.2, uses two verbs: 'Design and implement.' The guidelines explicitly prohibit double-verb formulations ('Design and implement' is given as the canonical bad example). Pick a single action verb (e.g., 'Implement connectors with expiry' or 'Design connectors with expiry') in both the bullet and the heading.

- Page 4, Objective: “• Design and implement connectors with expiry”
- Page 5, Objective: “4.2 Design and Implement Connectors with Expiry”

### 7. Contraction: "doesn't"

`MAJOR` · `writing`

The text must not use contractions. The sentence on page 3 uses "doesn't" instead of "does not." This is a clear, unambiguous violation of the no-contractions rule.

- Page 3, Problem: “The current process doesn't scale and is the worst bottleneck of the chair.”

### 8. Excessive use of first-person singular "I"

`MAJOR` · `writing`

The guidelines state: Avoid 'one,' 'I,' and 'our.' The student uses "I" as the subject in 15+ sentences throughout the Objective, Schedule, and AI Transparency sections. The text should restructure sentences to use the platform, the thesis, or the system as the subject (e.g., "The proposed platform builds…", "The thesis follows…", "The objective section pursues four objectives"). Only a single transitional "we" is permitted for describing the thesis approach, and even that should be sparing. The current density of "I" makes the proposal read as a personal narrative rather than a structured research document.

- Page 5, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 5, Objective: “I follow the object-oriented software engineering method of Bruegge and Dutoit, and Figure 1 shows the actors and use cases of the platform as a use case diagram in the Unified Modeling Language. I pursue four objectives:”
- Page 6, Objective 4.1: “I will replace the flat list of hosts and channels with a resource tree whose inner nodes are folders and whose leaves each carry a binding to one target system.”
- Page 6, Objective 4.1: “I will store the tree with a transitive-closure table of ancestor–descendant pairs and cap the fan-out, so that an approval on the root never reaches the whole infrastructure by accident.”
- Page 6, Objective 4.2: “I will define one connector contract that validates a binding, grants and revokes access, and optionally lists the current members, and I will implement the keycloak-group, slack-channel, github-team, and webhook connectors on it.”
- Page 7, Objective 4.3: “I will build declarative request forms whose fields, visibility conditions, and rules turn answers into requests for a resource path, a level, and a duration.”
- Page 7, Objective 4.3: “I will therefore split the approval: in the review step, the resource owner records a decision without changing Keycloak, and in the grant step, the super admin sets membership and expiry atomically, in bulk for a vetted queue.”
- Page 7, Objective 4.3: “I will compare the manual administrative actions and the time to working access with the current process.”
- Page 7, Objective 4.4: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”
- Page 7, Schedule: “The thesis runs from 1 October 2026 to 31 March 2027, and I divide it into iterations of two to four weeks.”
- Page 7, Schedule: “Outcome: the correctness scenarios pass, and I measure the effort.”
- Page 10, AI Transparency Statement: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”

### 9. Non-peer-reviewed documentation source in bibliography

`MAJOR` · `citation`

The entry [Key26] ('Keycloak Server Administration Guide') is an online documentation guide, not a peer-reviewed publication. Per the guidelines, internet sources and documentation links must appear as footnotes, not as bibliography entries. This entry should be removed from the bibliography and the corresponding citation in the text should be converted to a footnote (similar to how the Slack/GitHub API URLs are already handled on page 2). The remaining 12 entries are all peer-reviewed, so the minimum count is still met.

- Page 8, Bibliography: “[Key26] Keycloak Authors, "Keycloak Server Administration Guide." Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”
- Page 2, Introduction: “its Fine-Grained Admin Permissions restrict an administrative right to a single group [Key26].”

### 10. Figure 2 caption is too short and generic

`MAJOR` · `figures`

The caption for Figure 2 is simply "System Architecture", which is a two-word generic label. The guidelines require every figure to have a long, informative caption that is descriptive and extensive. Compare with Figure 1's caption, which spans multiple sentences explaining the actors, their roles, and the rationale for secondary actors. Figure 2's caption should describe what the diagram shows (the component decomposition, the connector architecture, the data flow through the outbox, the separation of user-token vs. admin-API calls, the read-only LDAP projection, etc.) and why the architecture is structured this way.

- Page 5, Objective 4.2: “Figure 2: System Architecture”

### 11. Missing review confirmation sentence in AI Transparency Statement

`MAJOR` · `completeness`

The AI Transparency Statement (Page 10) does not include the required confirmation that the author has carefully checked all AI-generated text. The guidelines require a sentence such as: "I have carefully checked all texts created with these tools to ensure that they are correct and make sense." The statement currently ends after the tool table without any such attestation. The student must add this explicit confirmation sentence.

- Page 10, AI Transparency Statement: “I used the following generative artificial intelligence (AI) tool while preparing this proposal. The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 12. Citation placed after the full stop

`MINOR` · `citation`

The citation [BBF01] is placed after the period at the end of a sentence: 'temporal model. [BBF01]'. Per the guidelines, citations must be placed before the full stop, e.g., 'temporal model [BBF01].'. The period should move after the citation.

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model. [BBF01]”
