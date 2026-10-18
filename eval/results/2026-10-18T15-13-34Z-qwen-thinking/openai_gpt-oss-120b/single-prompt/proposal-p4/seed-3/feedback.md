# proposal-p4 — single-prompt

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 32
- **Findings:** 8
- **Cost:** 1 calls, 0 tool calls, depth 1, 14270 tokens, 21s
- **Coverage:** 9/9 categories

## Summary

The proposal is missing a required Abstract and contains several critical structural and compliance issues, such as a problem section that proposes a solution, non‑peer‑reviewed bibliography entries, and a schedule that includes thesis writing. Additionally, the AI transparency statement is incomplete and some objectives violate wording guidelines. These major deficiencies must be addressed before the proposal can be considered acceptable.

## Findings

### 1. Missing Abstract

`CRITICAL` · `completeness`

The proposal does not contain an Abstract section, which is required.

### 2. Problem Section Contains Solution

`CRITICAL` · `logic`

The Problem section presents a concrete solution (a self‑service platform) instead of only describing the problem and its negative consequences.

- Page 3, Problem: “A self‑service platform that arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Double Verbs in Objective

`MAJOR` · `writing`

Objective bullet 2 uses two verbs (“Design and implement”), violating the single‑verb action‑form rule.

- Page 4, Objective: “•   Design   and   implement      connectors     with   expiry”

### 4. Objective Subsections Too Short

`MAJOR` · `completeness`

Each objective subsection contains only a single paragraph; the guidelines require at least two paragraphs per subsection.

- Page 5, Structure Resources as an Inheriting Tree: “I will replace the fat list of hosts and channels with a resource tree whose inner nodes are folders and whose leaves each carry a binding to one target system.”
- Page 6, Design and Implement Connectors with Expiry: “A Slack channel, a GitHub team, and a Keycloak group share only the idea of membership. I will defne one connector contract that validates a binding, grants and revokes access, and optionally lists the current members, and I will implement the keycloak-group, slack-channel, github-team, and webhook connectors on it.”
- Page 6, Delegate Request Review Without Privilege Escalation: “A requester should not need to know which resources a project needs. I will build declarative request forms whose felds, visibility conditions, and rules turn answers into requests for a resource path, a level, and a duration.”
- Page 6, Automate the Recertifcation of Grants: “I will attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”

### 5. Non‑Peer‑Reviewed Entry in Bibliography

`CRITICAL` · `citation`

The bibliography includes a non‑peer‑reviewed source ([Key26] Keycloak Server Administration Guide), which must not appear in the bibliography.

- Page 8, Bibliography: “[Key26] Keycloak Authors, “Keycloak Server Administration Guide.””

### 6. Insufficient Figure Caption

`MINOR` · `figures`

Figure 2 caption is overly short and generic (“System Architecture”), lacking a descriptive explanation.

- Page 5, Figures: “Figure  2:  System  Architecture”

### 7. Missing AI Verification Sentence

`CRITICAL` · `other`

The AI Transparency statement does not contain the required confirmation sentence that the author has carefully checked all AI‑generated text.

- Page 10, Transparency: “I used the following generative artificial intelligence (AI) tool while preparing this proposal...”

### 8. Thesis Writing Tasks in Schedule

`CRITICAL` · `schedule`

Iteration 7 includes thesis writing and presentation preparation, which are prohibited tasks in the schedule.

- Page 7, Schedule: “Iteration 7 (18–31 March): Write the thesis and prepare the final presentation.”
