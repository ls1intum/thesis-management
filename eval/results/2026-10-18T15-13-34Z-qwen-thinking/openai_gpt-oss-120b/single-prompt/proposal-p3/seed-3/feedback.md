# proposal-p3 — single-prompt

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 55
- **Findings:** 3
- **Cost:** 1 calls, 0 tool calls, depth 1, 16114 tokens, 27s
- **Coverage:** 9/9 categories

## Summary

The proposal contains the required sections and most content, but the objectives lack the mandated two‑paragraph depth and their bullet items are too long. Additionally, the abstract is longer than allowed. These issues prevent the proposal from meeting the group's structural standards and need to be corrected before submission.

## Findings

### 1. Objective subsections lack required paragraph count

`MAJOR` · `structure`

Each objective subsection (4.1 – 4.4) contains only a single paragraph, but the guidelines require at least two paragraphs per subsection.

- Page 5, 4.1 Unify Heterogeneous Resources in One Tree: “**I model the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems, as the UML object diagram in Figure 2 shows.**”
- Page 6, 4.2 Enforce an Expiry Date on Every Grant: “**The platform changes Keycloak only with the token of the calling user and holds no service account, so no background process of the platform can remove a membership.**”
- Page 8, 4.3 Delegate the Approval of Requests to Maintainers: “**Requesters fill in a declarative request form instead of naming resources, and its rules turn answers into requests for a resource path, an access level, and a duration, which the platform routes to the maintainer of each resource.**”
- Page 8, 4.4 Automate the Recertifcation of Active Grants: “**Every resource carries a recertifcation interval that its descendants inherit, and each elapsed interval opens a campaign in which the maintainer keeps or revokes every grant with an immutable decision.**”

### 2. Objective bullet points exceed single‑line length

`MINOR` · `formatting`

The bullet points in the Objectives list are long and wrap onto multiple lines, violating the requirement that each objective fit on one line.

- Page 4, Objective: “**1.  Unify   heterogeneous      resources    in  one  tree.**”
- Page 4, Objective: “**2.  Enforce    an  expiry   date   on  every   grant.**”
- Page 4, Objective: “**3.  Delegate    the  approval    of  requests   to  maintainers.**”
- Page 4, Objective: “**4.  Automate      the  recertifcation     of  active   grants.**”

### 3. Abstract exceeds allowed length

`MINOR` · `completeness`

The abstract spans more than half a page, exceeding the permitted 1/3 to 1/2 page length.

- Page 2, Abstract: “**A university research chair grants access to its compute hosts, chat workspace, and code repositories by hand, while the people who need that access change every semester. Requests travel by chat message or e‑mail, nobody records an end date, and access granted for one project outlives the time a person spends at the chair. No single record states who holds which access or who approved it.**”
