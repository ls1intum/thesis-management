# proposal-p3 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 4
- **Cost:** 10 calls, 0 tool calls, depth 2, 91129 tokens, 37s
- **Coverage:** 9/9 categories

## Summary

The proposal contains a critical violation of the page limit and several major structural and writing issues, notably overly brief objective subsections and a paragraph that mixes multiple ideas. Minor passive‑voice usage is also noted. Significant revisions are required before the proposal can be submitted.

## Findings

### 1. Proposal exceeds allowed page limit

`CRITICAL` · `structure`

The proposal spans 12 pages, exceeding the maximum of 6 pages specified.

- Page 12, Bibliography: “=== PAGE **12** ===”

### 2. Objective subsections are too short

`MAJOR` · `structure`

Each objective subsection (4.1–4.4) contains only a single paragraph; the guidelines require at least two paragraphs per subsection.

- Page 5, Objective: “4.1 **Unify** Heterogeneous Resources in One Tree”
- Page 7, Objective: “4.2 **Enforce** an Expiry Date on Every Grant”
- Page 8, Objective: “4.3 **Delegate** the Approval of Requests to Maintainers”
- Page 8, Objective: “4.4 **Automate** the Recertifcation of Active Grants”
- Page 5, Objective: “**I model the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems, as the UML object diagram in Figure 2 shows. One approval on a folder covers every resource beneath it, including later additions. A transitive‑closure table stores the tree, and a fan‑out cap keeps an approval on the root from provisioning the whole infrastructure.**”

### 3. Paragraph mixes multiple ideas

`MAJOR` · `writing`

A paragraph in the Problem section combines distinct topics (persistent access, audit consequences, and historical traceability) instead of focusing on a single coherent idea, violating the one‑idea‑per‑paragraph rule.

- Page 3, Problem: “**Access never ends. Nobody records when a person stops needing access, so access piles up, and former students keep login access to the compute hosts long after they leave, a decay that studies of enterprises report as well [Ker+02, SMJ01].** Auditors bear the consequence: the history lies scattered across systems and chat logs, and nobody can say who had access to a host last March and who approved it.”

### 4. Passive voice usage

`MINOR` · `writing`

The sentence uses passive voice ('written after I freeze the framework'), which conflicts with the active voice requirement.

- Page 6, Objective: “I reach this objective when the webhook connector, **written after I freeze the framework**, needs one new class and one registry entry.”
