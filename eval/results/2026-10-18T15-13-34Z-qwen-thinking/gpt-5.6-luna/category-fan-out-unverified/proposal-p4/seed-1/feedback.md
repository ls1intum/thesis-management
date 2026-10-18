# proposal-p4 — category-fan-out-unverified

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 16
- **Cost:** 10 calls, 0 tool calls, depth 2, 81499 tokens, 29s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a relevant platform concept with clearly defined infrastructure and objectives, but it is not ready for submission. A missing abstract, incomplete objective structure, several major structural and methodological issues, citation changes, and the prohibited schedule tasks require substantial revision before submission.

## Findings

### 1. Missing Abstract

`CRITICAL` · `completeness`

The proposal has no Abstract section. Add an Abstract of approximately one-third to one-half page summarizing the context, problem, proposed platform, objectives, and intended evaluation.

### 2. Objective 4 lacks the required two substantive paragraphs

`MAJOR` · `completeness`

Objective 4 is presented as a single continuous paragraph, although each objective subsection must contain at least two substantive paragraphs. Split the content into separate paragraphs covering recertification behaviour and the related evaluation, audit, escalation, and revocation criteria.

- Page 6, 4.4 Automate the Recertification of Grants: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate and may revoke grants automatically, but never in a campaign that reached nobody. The platform logs every”
- Page 7, 4.4 Automate the Recertification of Grants: “change with the person who made it, and the admin event log of Keycloak keeps a second copy.”
- Page 6, Objective 4: “I will attach a recertification interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate and may revoke grants automatically, but never in a campaign that reached nobody. The platform logs every change with the person who made it, and the admin event log of Keycloak keeps a second copy.”

### 3. Problem section presents a solution

`MAJOR` · `methodology`

The Problem section proposes the platform instead of focusing only on the problem and its consequences. Remove the solution claim from this section and reserve the platform proposal for the objectives or solution-design sections.

- Page 3, Problem: “A **self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation**.”

### 4. Objective heading uses double verbs

`MAJOR` · `structure`

Reformulate the second objective heading around one principal action verb. For example, use “Implement connectors with expiry” or “Specify connectors with expiry,” depending on whether the objective is implementation or specification.

- Page 5, Objective 2: “**Design and implement** connectors with expiry”

### 5. Online documentation listed in the bibliography

`MAJOR` · `citation`

The bibliography contains a non-peer-reviewed online documentation source. Move the Keycloak Administration Guide out of the bibliography and cite it as a footnote instead, updating the corresponding in-text citation.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “Keycloak Server Administration Guide.” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/latest/server_admin/”
- Page 2, 1.2 Existing Infrastructure: “Its Fine-Grained Admin Permissions restrict an administrative right to a single group **[Key26]**.”

### 6. Section 1 lacks introductory text

`MAJOR` · `structure`

Add a short introduction after the “1 Introduction” heading that previews the section’s purpose and subsections before introducing the setting.

### 7. Inconsistent term for the resource-managing role

`MAJOR` · `writing`

Use one term consistently for the resource-managing role. If “maintainer” and “resource owner” represent distinct roles, define the distinction explicitly.

- Page 3, Problem: “Maintainers, the staff who look after a project, know whether a request is legitimate but lack the rights to act on it”
- Page 6, Delegate Request Review Without Privilege Escalation: “The resource owner completes a request alone when it touches only non-privileged bindings, such as a Slack channel.”
- Page 6, Automate the Recertifcation of Grants: “The **maintainer** decides to keep or revoke each grant”

### 8. Missing confirmation of review of AI-generated text

`MAJOR` · `completeness`

Add a first-person sentence confirming that all AI-generated text was carefully checked for correctness and coherence, such as: “I have carefully checked all texts created with these tools to ensure that they are correct and make sense.”

### 9. Schedule includes prohibited thesis-writing and presentation tasks

`MAJOR` · `methodology`

Remove thesis-writing and presentation-preparation tasks from the schedule. Omit Iteration 7 from the development schedule or replace it with development or research work such as final end-to-end validation, evaluation, or analysis tied to the stated objectives.

- Page 7, Schedule: “• Iteration 7 (18–31 March): **Write the thesis and prepare the final presentation.**”

### 10. Citation placed after the full stop

`MINOR` · `citation`

Place the citation before the full stop so the sentence ends with the citation.

- Page 4, 3 Motivation: “Bertino et al. attach enabling and disabling events to roles in their temporal model. **[BBF01]** Joshi et al. generalise this idea to role assignments with a limited duration [Jos+05].”

### 11. Figure 2 has an insufficiently informative caption

`MINOR` · `figures`

Replace the generic Figure 2 caption with an informative caption describing the principal platform components, their connections to Keycloak, PostgreSQL, Slack/GitHub, and the Linux host, and the role of the connector and deprovisioning flows.

- Page 5, 4.2 Design and Implement Connectors with Expiry: “Figure 2: **System Architecture**”

### 12. Passive voice obscures responsibility

`MINOR` · `writing`

Name the responsible actor or component and use active voice where possible, particularly when describing which component reaches the hosts and which condition satisfies the objective.

- Page 2, Setting: “A university research chair, or a team of similar size, operates shared infrastructure: Linux hosts with graphics processors **reached over Secure Shell (SSH)**, a Slack workspace, a GitHub organisation, and a handful of internal dashboards and databases.”
- Page 5, Structure Resources as an Inheriting Tree: “**The objective is met** when a grant on a folder with bindable descendants yields exactly provisioning commands and **a resource added later** inherits the grant without a new request.”

### 13. Repeated first-person phrasing

`MINOR` · `writing`

Reduce repeated first-person phrasing such as “I will” and “I measure” by making the thesis, platform, or evaluation the grammatical subject, except where first-person wording is necessary to describe the approach.

- Page 4, Objective: “**I will build** a platform on which any member of the chair requests time-limited access to any resource, a maintainer decides, and the access ends on its own.”
- Page 5, Design and Implement Connectors with Expiry: “**I will define** one connector contract that validates a binding, grants and revokes access, and optionally lists the current members, and **I will implement** the keycloak-group, slack-channel, github-team, and webhook connectors on it.”
- Page 6, Delegate Request Review Without Privilege Escalation: “**I will therefore split** the approval: in the review step, the resource owner records a decision without changing Keycloak, and in the grant step, the super admin sets membership and expiry atomically.”

### 14. Contraction used

`MINOR` · `writing`

Replace the contraction “doesn’t” with “does not” to comply with the no-contractions rule.

- Page 3, Problem: “The current process **doesn’t** scale and is the worst bottleneck of the chair.”

### 15. Undefined technical abbreviations

`MINOR` · `writing`

Expand or briefly identify “nslcd” and “sshd” at first use so readers unfamiliar with the implementation terminology can follow the description.

- Page 2, Existing Infrastructure: “The Linux hosts read this directory over the Lightweight Directory Access Protocol (LDAP) through **nslcd, sshd**, pam_access, and sudoers, and an Ansible role configures each host.”

### 16. Figure 1 caption uses sentence case

`MINOR` · `formatting`

Capitalize the Figure 1 caption title as “Use Case Diagram” to maintain title case for titles.

- Page 4, Objective: “Figure 1: **Use case diagram** of the access platform.”
