# proposal-p4 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 58
- **Findings:** 12
- **Cost:** 23 calls, 37 tool calls, depth 23, 687837 tokens, 513s
- **Coverage:** 9/9 categories

## Summary

A substantive, well-organised proposal: the problem is grounded in clearly identified actors, the motivation is visionary without repeating the problem, the bibliography contains 12 peer-reviewed works in a consistent alpha style, both UML figures are referenced in text, and the schedule consists of 2-4-week vertically integrated iterations with measurable outcomes. However, it lacks the required Abstract (critical), and it violates several explicit rules: the Problem section prescribes a solution, Objective 2 uses the 'Design and implement' double verb in both the bullet and its heading, subsection 4.4 is a single paragraph, a documentation source ([Key26]) sits in the bibliography instead of a footnote, one citation ([BBF01]) is placed after the full stop, Figure 2's caption is a generic 'System Architecture', the AI transparency statement omits the required 'I have carefully checked all texts…' confirmation, and the schedule contains a thesis-writing/presentation iteration. Fixing these 12 items (1 critical, 6 major, 5 minor) should bring the proposal into compliance.

## Findings

### 1. Required Abstract section is missing

`CRITICAL` · `COMPLETENESS`

The proposal contains no Abstract. Page 1 is the title page and page 2 begins directly with '1 Introduction'. The required sections are Abstract, Introduction, Problem, Motivation, Objective (with subsections), Schedule, Bibliography, and AI Transparency Statement. Add an Abstract of 1/3 to 1/2 page summarising the problem, approach, and objectives.

- Page 2, Abstract

### 2. Problem section presents a solution

`MAJOR` · `LOGIC`

The Problem section must describe only problems and their negative consequences, not solutions. The last sentence of the third problem paragraph prescribes a concrete solution (a self-service platform that arms the Keycloak workflow engine with an expiry date at grant time). Move this sentence out of Section 2 into Motivation or Objective, and keep the paragraph focused on the negative consequence (access accumulation, no audit trail).

- Page 3, Problem: “A self-service platform that arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Non-peer-reviewed documentation listed as a bibliography entry

`MAJOR` · `CITATION`

[Key26] is the Keycloak Server Administration Guide — a documentation/internet source, not a peer-reviewed publication. The group rules require the bibliography to contain only scientific and peer-reviewed publications; internet sources such as documentation links must be footnotes (as the author already does for the Slack/GitHub API URLs on page 2). Convert [Key26] into a footnote at its citation in Section 1.2.

- Page 8, Bibliography: “Keycloak Authors, “Keycloak Server Administration Guide. ” Accessed: Oct. 18, 2026. [Online].”

### 4. Citation placed after the full stop

`MINOR` · `CITATION`

The citation for the Bertino et al. reference is placed after the full stop: '…in their temporal model. [BBF01]'. Group rules require citations to come before the full stop, e.g. '…temporal model [BBF01].'. All other citations in the proposal follow the correct placement, so only this one needs fixing.

- Page 4, Motivation: “temporal model. [BBF01]”

### 5. Objective 2 uses a double verb

`MAJOR` · `STRUCTURE`

Objective 2 uses two verbs — 'Design and implement connectors with expiry' (bullet list, page 4) and its heading '4.2 Design and Implement Connectors with Expiry' (page 5). Group rules require each objective (including its heading) to be a single action; the guidelines explicitly name 'Design and implement' as the pattern to avoid. Pick one verb, e.g. 'Implement connectors with expiry' (page 4) and 'Implement Connectors with Expiry' (heading).

- Page 4, Objective: “Design and implement connectors with expiry”

### 6. Objective 4 subsection has only one paragraph

`MAJOR` · `STRUCTURE`

Section 4.4 'Automate the Recertification of Grants' contains a single continuous paragraph (running from page 6 onto page 7). Group rules require each objective subsection to be at least two paragraphs long. Split it, for example, into one paragraph on the campaign mechanics (interval, recertification, escalation) and one on the audit/record-keeping aspect (immutable decision records, logging, Keycloak admin event log).

- Page 6, 4.4 Automate the Recertifcation of Grants: “I will attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”

### 7. Figure 2 caption is short and generic

`MINOR` · `FIGURES`

The caption of Figure 2 is short and generic ('Figure 2: System Architecture'). Group rules require long, informative captions. Compare with the caption of Figure 1, which explains actors, roles, and why secondary systems participate. Extend the Figure 2 caption to describe what the diagram shows — e.g. the separation between the user-token path (mgmt-ui/mgmt-api), the privileged admin-API path, the outbox/reaper deprovisioning, and the read-only LDAP projection to the Linux host.

- Page 5, 4.2 Design and Implement Connectors with Expiry: “Figure 2: System Architecture”

### 8. AI transparency statement lacks the review-confirmation sentence

`MAJOR` · `COMPLETENESS`

The AI transparency statement names the tool, model/version, date, taxonomy level, purpose, and affected sections, which is specific. However, it lacks the required confirmation that the author carefully checked all AI-generated text. Add a sentence such as 'I have carefully checked all texts created with these tools to ensure that they are correct and make sense.'

- Page 10, Transparency in the Use of AI Tools: “The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel Uçan [UD26].”

### 9. Schedule includes thesis-writing and presentation tasks

`MAJOR` · `METHODOLOGY`

Iteration 7 is dedicated to writing the thesis and preparing the final presentation. Group rules exclude thesis-writing and presentation-preparation tasks from the schedule — it should cover development/research work only. Drop this iteration (or fill the 18–31 March window with development work, e.g. hardening the deployment, running the correctness scenarios, and the extensibility measurement for Objective 2).

- Page 7, Schedule: “Iteration 7 (18–31 March): Write the thesis and prepare the fnal”

### 10. Contraction "doesn't" in Problem section

`MINOR` · `WRITING`

The proposal must not use contractions. 'doesn't' here should be 'does not'. This is the only contraction found in the proposal.

- Page 3, Problem: “The current process doesn't scale and is the worst bottleneck of the chair.”

### 11. Frequent first-person "I" in the prose

`MINOR` · `WRITING`

The writing-style rules ask to avoid first-person 'I' in favour of constructions that name the actor or the platform as subject. The prose uses 'I' repeatedly (e.g. 'I will build a platform', 'I will replace the fat list', 'I will build declarative request forms', 'I will therefore split the approval', 'I measure extensibility', 'I divide it into iterations'). Rewrite most of these, e.g. 'The platform lets any member of the chair request time-limited access to any resource, a maintainer decides, and the access ends on its own.' (First person in the AI transparency statement is required and unaffected.)

- Page 4, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”

### 12. No introductory text before subsection 1.1

`MINOR` · `STRUCTURE`

Section 1 'Introduction' jumps directly into subsection 1.1 'Setting' without any introductory text. Group rules require every section to have introductory text before its first subsection. Add 2-3 sentences before 1.1 that frame what the introduction covers (the setting and the existing infrastructure) and, ideally, preview the structure of the proposal.

- Page 2, Introduction
