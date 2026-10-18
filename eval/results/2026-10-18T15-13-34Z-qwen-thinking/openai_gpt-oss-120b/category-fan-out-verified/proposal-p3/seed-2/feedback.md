# proposal-p3 — category-fan-out-verified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 45
- **Findings:** 7
- **Cost:** 14 calls, 0 tool calls, depth 3, 102790 tokens, 58s
- **Coverage:** 9/9 categories

## Summary

The proposal tackles a relevant access‑management problem and provides useful diagrams, but it exceeds the allowed page limit, contains overly long and solution‑laden problem sections, and misses required paragraph structure in the objectives. Major writing issues such as an undefined abbreviation and inconsistent spelling further detract from clarity. These significant problems must be addressed before the proposal can be submitted.

## Findings

### 1. Proposal exceeds allowed length

`MAJOR` · `completeness`

The main textual content (excluding metadata, figures, tables, and schedule) spans more than six pages, violating the 4–6 page limit for the proposal.

- Page 2, Abstract: “**Abstract**”
- Page 3, Problem: “**2  Problem**”
- Page 4, Objective: “**4  Objective**”
- Page 5, Objective: “**Figure 1:** UML use case diagram of the access management platform.”
- Page 6, Objective: “**Figure 2:** UML object diagram of a small resource tree at the chair.”
- Page 8, Objective: “**4.4 Automate the Recertifcation of Active Grants**”

### 2. Problem section too long

`MAJOR` · `completeness`

The Problem section exceeds the maximum allowed length of three‑quarters of a page.

- Page 3, Problem: “Requesters, such as thesis students and external collaborators, do not know who manages which system. **A student who needs a host, a channel, and a repository asks several people and waits for each of them, and some bridge the delay with a shared login that hides who acted.**”

### 3. Objective subsection 4.1 lacks required paragraphs

`MAJOR` · `structure`

Subsection 4.1 (Unify Heterogeneous Resources in One Tree) contains fewer than the required two paragraphs.

- Page 5, 4.1 Unify Heterogeneous Resources in One Tree: “I model the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems, as the UML object diagram in Figure 2 shows. **One approval on a folder covers every resource beneath it, including later additions.**”

### 4. Problem section includes solution description

`MAJOR` · `logic`

The Problem section presents a workaround (granting maintainers full administrative rights) which constitutes a solution, violating the rule that the Problem section must describe only problems and their negative consequences without offering solutions.

- Page 3, Problem: “The common workaround **hands maintainers full administrative rights**, which violates the principle of least privilege [SS75] and exposes every user and every host to a mistake made for a single project.”

### 5. Undefined abbreviation

`MAJOR` · `writing`

The abbreviation "UML" appears without prior definition; define it as "Unified Modeling Language (UML)" before first use.

- Page 5, Objective: “Figure 1: **UML** use case diagram of the access management platform.”

### 6. Passive voice usage

`MINOR` · `writing`

Passive construction used; rewrite in active voice.

- Page 3, Problem: “exposes every user and every host to a **mistake made** for a single project.”

### 7. Inconsistent spelling of key terms

`MINOR` · `writing`

The terms "recertification" and "workflow" are misspelled in multiple places, breaking terminology consistency.

- Page 8, 4.4 Automate the Recertifcation of Active Grants: “4.4 Automate             the     **Recertifcation**              of  Active       Grants”
- Page 8, 4.4 Automate the Recertifcation of Active Grants: “Every    resource    carries  a  **recertifcation**     interval   that   its descendants      inherit”
- Page 7, 4.2 Enforce an Expiry Date on Every Grant: “the platform therefore arms the **workfow** engine of Keycloak through its API at grant time”
- Page 8, 4.4 Automate the Recertifcation of Active Grants: “the scenario passes without the **workfow** engine”
