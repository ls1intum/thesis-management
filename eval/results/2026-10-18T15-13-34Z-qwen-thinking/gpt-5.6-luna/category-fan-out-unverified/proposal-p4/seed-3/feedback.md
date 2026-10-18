# proposal-p4 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 17
- **Cost:** 10 calls, 0 tool calls, depth 2, 82380 tokens, 28s
- **Coverage:** 9/9 categories

## Summary

The proposal has a promising and concrete platform concept, with defined objectives, architecture, and workflow details. However, it needs substantial revision before submission because it lacks the required abstract, has structural and methodological problems, includes non-peer-reviewed bibliography material, and requires improvements to AI transparency, schedule compliance, and writing consistency.

## Findings

### 1. Missing Abstract section

`CRITICAL` · `completeness`

Add the required Abstract section, approximately one-third to one-half page long, summarizing the problem, motivation, objectives, and proposed approach.

### 2. Missing confirmation of review of AI-generated text

`MAJOR` · `completeness`

Add an explicit confirmation that all AI-generated text was carefully checked for correctness and coherence, such as: “I have carefully checked all text created with these tools to ensure that it is correct and makes sense.”

- Page 10, AI Transparency Statement: “Tool Model / Date(s) Level Purpose Scope ... Drafting text, authoring the Typst sources and bibliography entries, researching references”

### 3. Objective 4 lacks two substantive paragraphs

`MAJOR` · `structure`

Add a second substantive paragraph to Objective 4.4, separating the recertification workflow from its logging, escalation, and evaluation details. The text currently continues across a page break but remains one paragraph.

- Page 6, 4.4 Automate the Recertification of Grants: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate and may revoke grants automatically, but never in a campaign that reached nobody. The platform logs every change with the person who made it, and the admin event log of Keycloak keeps”
- Page 7, 4.4 Automate the Recertification of Grants: “change with the person who made it, and the admin event log of Keycloak keeps a second copy.”
- Page 6, Objective 4.4: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate and may revoke grants automatically, but never in a campaign that reached nobody. The platform logs every”
- Page 7, Objective 4.4: “change with the person who made it, and the admin event log of Keycloak keeps a second copy.”

### 4. Problem section proposes a solution

`MAJOR` · `methodology`

The Problem section should describe the problem and its consequences rather than propose a solution. Move the proposed self-service platform to the Objectives or Motivation section.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 5. Objective heading uses double verbs

`MAJOR` · `writing`

Replace the heading containing two coordinated action verbs with one action verb, or split it into two objectives. For example, use “Provide connectors with expiry” if that accurately captures the intended objective.

- Page 5, Objective 2: “Design and implement connectors with expiry”

### 6. Documentation source listed in the bibliography

`MAJOR` · `citation`

Move the Keycloak Administration Guide to a footnote at its point of use and remove it from the Bibliography, which should contain only scientific, peer-reviewed publications.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “Keycloak Server Administration Guide.” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”

### 7. Repeated first-person narration

`MAJOR` · `writing`

Replace repeated first-person narration with explicit subject-based formulations such as “The platform will...” or “The implementation will...” while retaining active voice.

- Page 4, Objective: “I will build a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 5, Structure Resources as an Inheriting Tree: “I will replace the fat list of hosts and channels with a resource tree whose inner nodes are folders and whose leaves each carry a binding to one target system.”
- Page 6, Design and Implement Connectors with Expiry: “I will define one connector contract that validates a binding, grants and revokes access, and optionally lists the current members.”
- Page 7, Schedule: “The thesis runs from 1 October 2026 to 31 March 2027, and I divide it into iterations of two to four weeks.”

### 8. Introduction lacks text before its first subsection

`MAJOR` · `structure`

Add a short introductory paragraph between the Section 1 heading and subsection 1.1 to explain the purpose or scope of the Introduction chapter.

- Page 2, Introduction: “1 Introduction
1.1 Setting”

### 9. Schedule includes thesis writing and presentation preparation

`MAJOR` · `methodology`

Remove thesis writing and presentation preparation from the iteration plan because iterations must focus on development or research work. Use Iteration 7 for a research or development increment and schedule final writing and presentation preparation separately if required.

- Page 7, Schedule: “• Iteration 7 (18–31 March): Write the thesis and prepare the final presentation.”

### 10. Main objective heading is not in action form

`MINOR` · `writing`

Replace the noun-based main heading with an action-oriented heading that states what the proposal will do, such as “Build a Platform for Time-Limited Access,” if this accurately reflects the intended objective.

- Page 4, Objective: “4 Objective”

### 11. Citation placed after the full stop

`MINOR` · `citation`

Place the citation before the full stop so that the sentence ends with the citation, for example: “Bertino et al. attach enabling and disabling events to roles in their temporal model [BBF01].”

- Page 4, Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model. [BBF01] Joshi et al. generalise this idea to role assignments with a limited duration [Jos+05].”

### 12. Figure 2 has an insufficiently informative caption

`MINOR` · `figures`

Replace the generic caption with an informative caption explaining the major components, integrations, data and control flows, and how the architecture supports time-limited granting and revocation.

- Page 5, 4.2 Design and Implement Connectors with Expiry: “Figure 2: System Architecture”

### 13. Unspecified actor in passive construction

`MINOR` · `writing`

Name the actor responsible for reaching the hosts and rewrite the reduced passive construction in active voice, for example: “Users reach Linux hosts with graphics processors over Secure Shell (SSH).”

- Page 2, Setting: “A university research chair, or a team of similar size, operates shared infrastructure: Linux hosts with graphics processors reached over Secure Shell (SSH), a Slack workspace, a GitHub organisation, and a handful of internal dashboards and databases.”

### 14. Passive voice obscures responsibility

`MINOR` · `writing`

Replace the passive construction with an active subject. State which condition or evaluation determines that the objective is met and identify who adds the resource later.

- Page 5, Structure Resources as an Inheriting Tree: “The objective is met when a grant on a folder with bindable descendants yields exactly provisioning commands and a resource added later inherits the grant without a new request.”

### 15. Contraction used

`MINOR` · `writing`

Expand the contraction “doesn’t” to “does not.”

- Page 3, Problem: “The current process doesn’t scale and is the worst bottleneck of the chair.”

### 16. Figure 1 caption is not in title case

`MINOR` · `formatting`

Change the Figure 1 caption title to title case for consistency: “Use Case Diagram of the Access Platform.”

- Page 4, Objective: “Figure 1: Use case diagram of the access platform. Requesters fll in forms and request access, maintainers review, grant, and recertify, and the super admin maintains the resource tree and completes every grant that needs privileged rights.”

### 17. Inconsistent term for the request-reviewing role

`MINOR` · `writing`

Use one term consistently for the request-reviewing role, or explicitly distinguish “maintainer” from “resource owner” if they are intended to be different roles.

- Page 3, Problem: “Maintainers, the staf who look after a project, know whether a request is legitimate but lack the rights to act on it, while the super admin holds the rights but lacks the context.”
- Page 6, Delegate Request Review Without Privilege Escalation: “Keycloak requires a realm-wide administrative role for its workfow API, and no resource owner may hold that role.”
- Page 6, Delegate Request Review Without Privilege Escalation: “The resource owner completes a request alone when it touches only non-privileged bindings, such as a Slack channel.”
