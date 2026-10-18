# Construct the Conforming Proposal (p3)

You write a constructed proposal for the evaluation corpus of an AI review benchmark. This
document is the **conforming** one: it satisfies every rule listed below, so a reviewer that
reports findings on it is over-reporting. A defect you leave in by accident turns into a false
"over-report" in the benchmark, so conformance is the whole point of this task.

Target folder: `eval/corpus/proposal/p3/`
Target PDF: `eval/corpus/proposal/p3.pdf`

## Proposal Content

The subject matter of the proposal is given below. Build the proposal around it. Use it as the
source of facts about the system, the setting, and the actors. You may add plausible detail
that is consistent with it, but do not contradict it and do not claim measured results.

<content>
### Thesis Proposal: Project Content

#### Metadata

- Title: *Self-Service Management of Time-Limited Access to Heterogeneous Resources*
- German title: *Self-Service-Verwaltung zeitlich begrenzter Zugriffe auf heterogene Ressourcen*
- Degree: Bachelor in Informatics. Examiner: Prof. Dr. Stephan Krusche
- Author: Benjamin Schmitz. Period: 1 October 2026 – 31 March 2027

#### Project Summary

**Setting.** A university research chair (or a team of similar size) runs shared infrastructure: Linux GPU hosts reached over SSH, a Slack workspace, a GitHub organisation, and some internal dashboards and databases. Its people change every semester (thesis students, external collaborators, working students).

**Existing infrastructure.** Keycloak is the identity provider. It handles OIDC login, groups, and profile attributes such as SSH public keys, and it can scope admin rights with Fine-Grained Admin Permissions. The Linux hosts use the directory through `nslcd` over LDAP, `sshd` `AuthorizedKeysCommand`, `pam_access` and `sudoers`, and an Ansible role configures each host. Slack and GitHub manage membership through their own APIs and tokens and share no admin interface with Keycloak. People ask for access by chat or e-mail.

**Problem (by stakeholder).**

- *Requesters* don't know who manages what. They ask several people, then wait, or work around the process with shared accounts.
- *Maintainers* (supervisors) know whether a request is legitimate but can't act on it. The usual workaround gives them full admin rights, which breaks least privilege.
- *Grants never end.* No grant has an expiry date, access piles up, and former students keep SSH access.
- *Auditors* can't reconstruct who had access when, or who approved it.

**Solution: the tool.** On this platform, access has an expiry date from the moment someone requests it.

1. **Resource tree.** Folders are inner nodes and leaves are bound to target systems. Maintainership, audit policy and grants are inherited downwards. An approval on a folder covers every resource under it, including resources added later. The tree is stored with a transitive-closure (ancestor–descendant) table, and fan-out is capped.
2. **Pluggable connectors** share one contract: validate binding, grant, revoke, and optionally list members. There are four connectors: `keycloak-group` (SSH hosts), `slack-channel`, `github-team`, and `webhook`. Credentials are configured at runtime.
3. **Expiry without a privileged background actor.** The tool changes Keycloak only with the calling user's own token, never with a service account. For Keycloak bindings it arms Keycloak's own workflow engine at grant time. For the other connectors, a deprovisioning reaper revokes through an outbox. A drift check reports memberships that outlive their grants.
4. **Delegated two-step approval.** Keycloak's workflow API requires a realm-wide admin role that maintainers must not have. The flow is therefore split: a *review* step, where the maintainer records a decision without changing Keycloak, and a *grant* step, where membership and expiry are set atomically by a privileged admin. A request that touches only non-privileged bindings, such as Slack-only, can be completed by the maintainer. Bulk granting lets admins clear a queue that maintainers have already vetted.
5. **Declarative request forms.** The form has fields, visibility conditions and rules that turn answers into requests (resource path, level, duration). The server evaluates the answers, so a submission can't request resources the form doesn't offer.
6. **Recertification and audit.** Recertification intervals are inherited. Campaigns ask maintainers to keep or revoke each grant, and decisions are immutable. Ignored campaigns escalate, with optional auto-revoke, which never applies to campaigns that reached nobody. Every change is logged with the person who made it, and Keycloak's admin event log keeps a second copy.

**Architecture components:** `mgmt-ui` (React SPA, OIDC PKCE), `mgmt-api` (GrantPlanner, ConnectorRegistry, DeprovisionReaper; PostgreSQL for grants, outbox and audit), `ldap-bridge` (read-only projection of Keycloak to LDAPS for the hosts), plus Keycloak, Slack/GitHub and the Linux hosts.

**Actors:** Requester, Maintainer, Super admin. Secondary actors: Keycloak, Slack and GitHub.

**Evaluation.** Use Bruegge & Dutoit's OOSE method throughout. Evaluate on three axes:

- *Correctness:* an automated scenario provisions SSH, Slack and GitHub access from one approval and checks that each is removed after expiry, including when the workflow engine fails.
- *Effort:* count manual admin actions and measure the time from request to working access, compared with the current chat and e-mail process.
- *Extensibility:* measure the size of the change needed to add a connector written after the framework is frozen.

**Schedule.** Six iterations of four weeks plus two weeks of buffer. Each iteration ends with something a user can try in the web UI:

1. Keycloak realm, OIDC login, SSH key self-service, resource tree and maintainer inheritance
2. Requests, review/grant flow, `keycloak-group` connector with expiry, LDAP bridge, Ansible host role
3. Connector framework (contract, outbox, dispatcher, reaper), Slack and GitHub connectors, credentials screen
4. Request forms: schema, builder with live preview, server-side evaluation, submission tracking
5. Recertification: policies, campaigns, escalation, attestation, drift report, e-mail notifications
6. Evaluation: deploy at the chair, correctness scenarios, effort measurement, `webhook` connector
</content>

## Requirements (the Benchmark Rules)

These are the 20 rules the benchmark judges every document against, verbatim from
`server/src/test/java/de/tum/cit/aet/thesis/feedback/eval/EvalGuidelines.java`. The proposal
must satisfy **every one** of them. Read that file once to confirm the list has not changed.

Group overview: a proposal is a high-level overview (about 4–6 text pages) describing the
problem, the motivation, the objectives, and a preliminary schedule. It must cite at least 6–8
peer-reviewed publications and include at least two diagrams/mockups (at least one suitable UML
diagram, no sequence diagrams). Expected sections: Abstract, Introduction, Problem, Motivation,
Objective (with subsections), Schedule, Bibliography, and an AI Transparency Statement.

| ID  | Category                         | Rule |
|-----|----------------------------------|------|
| R1  | Structure & Completeness         | Required sections: Abstract, Introduction, Problem, Motivation, Objective (with subsections), Schedule, Bibliography, Transparency statement. |
| R2  | Structure & Completeness         | Total proposal text should be at most 4-6 pages; abstract 1/3-1/2 page; introduction < 1 page; problem and motivation each at most 3/4 page. |
| R3  | Structure & Completeness         | Objectives (Section 4) start with a short intro and a bullet list of 3-4 objectives, then one subsection per objective (at least two paragraphs each). |
| R4  | Problem, Motivation & Objectives | The Problem section describes only problems and their negative consequences for identified actors — no solutions. |
| R5  | Problem, Motivation & Objectives | The Motivation is visionary and does not repeat the Problem. |
| R6  | Problem, Motivation & Objectives | All objectives (including headings) are in action form and avoid double verbs (not 'Design and implement'). |
| R7  | Bibliography & Citations         | Cite at least 6-8 peer-reviewed publications; internet sources go in footnotes, not the bibliography. |
| R8  | Bibliography & Citations         | Use a consistent citation style (ideally alpha, [ABC12]); place citations before the full stop. |
| R9  | Figures & Diagrams               | Include at least two figures/diagrams/mockups; at least one is a suitable UML diagram; no sequence diagrams. |
| R10 | Figures & Diagrams               | Every figure is referenced in the text and has a long, informative caption. |
| R11 | Writing Style                    | Use active voice; avoid contractions, filler words, and superlatives. |
| R12 | Writing Style                    | Do not start sentences with 'As', 'Since', 'To', 'In order to', or 'Because'; define abbreviations before use. |
| R13 | Paragraph Structure              | Write mainly in prose (5-8 line paragraphs, one idea each), not bullet points — except in the Schedule and objective lists. |
| R14 | Paragraph Structure              | Every section has introductory text before its first subsection; numbering is at most three digits. |
| R15 | Formatting & Terminology         | Headings use title case (respecting the capitalization rules of the heading's language). |
| R16 | Formatting & Terminology         | Terminology is consistent for the same concept throughout the author's prose. |
| R17 | AI Transparency Statement        | Include a specific, first-person AI transparency statement naming the tools used, their purpose, and the sections involved. |
| R18 | AI Transparency Statement        | The statement confirms the author carefully checked all AI-generated text. |
| R19 | Schedule Quality                 | Divide work into 2-4 week iterations with measurable, vertically-integrated deliverables following agile principles. |
| R20 | Schedule Quality                 | Do not include a requirements-gathering sprint or thesis-writing/documentation tasks in the schedule. |

## How to Read the Rules Strictly

A reviewer model looks for anything it can call a violation, so stay clear of every borderline
case rather than just inside the line. The template (`p-template/proposal.typ`) is a layout
starting point, **not** a conforming example: it has six objectives, an extra "Evaluation"
subsection, and an evaluation-only iteration, all of which you must not copy.

- **R1, R14:** use exactly these top-level headings, in this order: Abstract, Introduction,
  Problem, Motivation, Objective, Schedule, then the bibliography, then "Transparency in the Use
  of AI Tools". Under Objective, add one subsection per objective and **nothing else**. Every
  heading that has subsections opens with prose before the first one. No numbering deeper than
  `x.y.z`.
- **R2:** aim for about 5 text pages in total (abstract to schedule, excluding the title page,
  figures, bibliography, and AI statement). Abstract 0.4 page, introduction 0.6–0.8 page,
  problem and motivation 0.5–0.7 page each. Measure on the rendered PDF, not by estimation.
- **R3, R6:** exactly 3 or 4 objectives, introduced by one or two sentences. Each bullet fits on
  one line. Each objective, and the subsection heading that repeats it, starts with **one**
  imperative verb ("Model …", "Automate …"), never two joined by "and" and never a noun phrase.
  Bullet text and subsection heading use identical wording. Each subsection has at least two
  full paragraphs.
- **R4:** the Problem section names actors (for example students, supervisors, administrators)
  and the concrete negative consequence each one suffers. It names no tool, technology, system,
  or remedy, and contains no "should", "could be solved", or "a system that …".
- **R5:** the Motivation looks forward (what becomes possible, and for whom) and restates no
  problem sentence. It may cite related research.
- **R7:** 8–12 bibliography entries, **all** peer-reviewed journal articles or conference papers
  that really exist. Verify each one (title, authors, venue, year, DOI) with a web search before
  adding it, and never invent a reference. No books, theses, websites, standards, or grey
  literature in the bibliography. Put websites and tool documentation in footnotes
  (`#footnote[https://…]`). Every entry is cited at least once, and every citation resolves.
- **R8:** keep `#set cite(style: "alphanumeric")`. Every citation sits inside its sentence, before
  the full stop: `… as prior work shows @key.`
- **R9, R10:** at least two figures, built with Fletcher in `figures/diagrams.typ`: one UML use
  case diagram, plus a UML component, activity, class, or object diagram. No sequence diagram,
  and no word "sequence" in any caption. Each caption is 2–3 sentences saying what the figure
  shows and how to read it. The prose references every figure by label *before* it appears
  ("@fig:usecase shows …"). The benchmark reviews extracted text only, so a caption must carry
  its meaning without the image.
- **R11:** active voice with a named actor ("I implement …", "The supervisor approves …"). No
  contractions. None of these words: very, really, actually, clearly, obviously, basically,
  simply, just, quite, additional(ly), furthermore, moreover, also, optimal, best, perfect,
  significant, huge, extremely, seamless, state-of-the-art, cutting-edge.
- **R12:** no sentence, including bullets, captions, and the schedule, starts with "As",
  "Since", "To", "In order to", or "Because". Define every abbreviation at its first use in the
  body text ("Role-Based Access Control (RBAC)"), even if the abstract defined it already, and
  introduce no abbreviation used fewer than three times.
- **R13:** prose everywhere except the objective list and the schedule. Each paragraph is 5–8
  rendered lines and develops one idea. No bullet lists in the problem, motivation, or objective
  subsections. Bold run-in labels inside a prose paragraph (`*Supervisors* lose …`) are fine.
- **R15:** every heading uses title case (Chicago style: capitalize every word except articles,
  coordinating conjunctions, and short prepositions, unless first or last).
- **R16:** pick one term per concept and keep it ("request" never becomes "application" or
  "ticket"; "supervisor" never becomes "advisor"). Write a short glossary for yourself before
  drafting and check the final text against it.
- **R17, R18:** the AI statement is first person and names the tool and model you actually are
  (for example "Anthropic Claude Code, Claude Opus 5.5"). It states what the tool did (drafting
  text, authoring diagrams, researching references) and which sections it touched, and it
  explicitly confirms that the author checked all AI-generated text and takes responsibility for
  it. Keep the template's table and prose layout.
- **R19, R20:** 4–6 iterations of 2–4 weeks with explicit dates between the start and submission
  dates. Each iteration delivers one vertically integrated, demonstrable feature with a
  measurable outcome and names the objective(s) it serves. No iteration for requirements
  gathering, literature research, writing, documentation, presentation, or evaluation alone, and
  no buffer filled with writing tasks.

## Procedure

1. `cp -R eval/corpus/proposal/p-template eval/corpus/proposal/p3`. Work only inside `p3/`, and
   never edit `p-template/`.
2. Update the metadata block in `p3/proposal.typ` (title, German title, author, examiner, dates)
   to fit the content. Leave the template code (title page, fonts, `diagram()`,
   `ai_usage_table`) unchanged: no margin, font, or spacing changes.
3. Replace all body content, `p3/references.yml`, and the diagrams in `p3/figures/diagrams.typ`
   with material for this proposal. Remove every trace of the template's topic (Elapsia,
   Keycloak, and so on) unless the content asks for it.
4. Compile from the repository root:

   ```sh
   typst compile --root eval/corpus/proposal/p3 \
     --font-path eval/corpus/proposal/p3/fonts \
     eval/corpus/proposal/p3/proposal.typ eval/corpus/proposal/p3.pdf
   ```

   Leave no PDF inside `p3/`. The folder holds sources only; `p3.pdf` next to it is the corpus
   document.
5. Check every rule against the **rendered PDF**, not the source:
   - render the pages (`typst compile … --format png --ppi 80 "$TMPDIR/p3-{p}.png"`), view them,
     and measure the section lengths for R2 and the paragraph lengths for R13;
   - extract the text (`pdftotext eval/corpus/proposal/p3.pdf -`) and search it for the
     forbidden sentence starters, filler words, contractions, and undefined abbreviations;
   - confirm that every figure label and every bibliography key is referenced in the prose.

   Fix and recompile until every rule holds.
6. Write `p3/conformance.md` with one row per rule R1–R20, stating where and how the proposal
   satisfies it (section, page, short quote). The evaluation cites this file as the evidence
   that p3 contains no known defects. If a rule holds only arguably, rewrite the text until it
   holds plainly.

## Deliverables

- `eval/corpus/proposal/p3/` with `proposal.typ`, `references.yml`, `figures/diagrams.typ`, and
  `conformance.md`
- `eval/corpus/proposal/p3.pdf`
- A final message listing the rendered length of each section and any rule that was hard to
  satisfy.
