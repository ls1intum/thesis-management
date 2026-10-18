# Construct the Deliberately Defective Proposal (p4)

You write a constructed proposal for the evaluation corpus of an AI review benchmark. This
document is the **deliberately defective** one: it satisfies the rules listed below everywhere
except at exactly **N** defects you introduce on purpose and record in a manifest. It is the one
document of the corpus whose defects are known in full, so the benchmark measures recall against
your manifest. The manifest must therefore be complete and exact: an unlisted defect counts
against every reviewer that finds it, and a listed defect that is not a clear violation counts
against every reviewer that misses it.

**N = 12**

Target folder: `eval/corpus/proposal/p4/`
Target PDF: `eval/corpus/proposal/p4.pdf`

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
`server/src/test/java/de/tum/cit/aet/thesis/feedback/eval/EvalGuidelines.java`. Read that file
once to confirm the list has not changed.

| ID  | Category slug                   | Rule |
|-----|---------------------------------|------|
| R1  | `structure`                     | Required sections: Abstract, Introduction, Problem, Motivation, Objective (with subsections), Schedule, Bibliography, Transparency statement. |
| R2  | `structure`                     | Total proposal text should be at most 4-6 pages; abstract 1/3-1/2 page; introduction < 1 page; problem and motivation each at most 3/4 page. |
| R3  | `structure`                     | Objectives (Section 4) start with a short intro and a bullet list of 3-4 objectives, then one subsection per objective (at least two paragraphs each). |
| R4  | `problem-motivation-objectives` | The Problem section describes only problems and their negative consequences for identified actors — no solutions. |
| R5  | `problem-motivation-objectives` | The Motivation is visionary and does not repeat the Problem. |
| R6  | `problem-motivation-objectives` | All objectives (including headings) are in action form and avoid double verbs (not 'Design and implement'). |
| R7  | `bibliography`                  | Cite at least 6-8 peer-reviewed publications; internet sources go in footnotes, not the bibliography. |
| R8  | `bibliography`                  | Use a consistent citation style (ideally alpha, [ABC12]); place citations before the full stop. |
| R9  | `figures`                       | Include at least two figures/diagrams/mockups; at least one is a suitable UML diagram; no sequence diagrams. |
| R10 | `figures`                       | Every figure is referenced in the text and has a long, informative caption. |
| R11 | `writing-style`                 | Use active voice; avoid contractions, filler words, and superlatives. |
| R12 | `writing-style`                 | Do not start sentences with 'As', 'Since', 'To', 'In order to', or 'Because'; define abbreviations before use. |
| R13 | `writing-structure`             | Write mainly in prose (5-8 line paragraphs, one idea each), not bullet points — except in the Schedule and objective lists. |
| R14 | `writing-structure`             | Every section has introductory text before its first subsection; numbering is at most three digits. |
| R15 | `writing-formatting`            | Headings use title case (respecting the capitalization rules of the heading's language). |
| R16 | `writing-formatting`            | Terminology is consistent for the same concept throughout the author's prose. |
| R17 | `ai-transparency`               | Include a specific, first-person AI transparency statement naming the tools used, their purpose, and the sections involved. |
| R18 | `ai-transparency`               | The statement confirms the author carefully checked all AI-generated text. |
| R19 | `schedule`                      | Divide work into 2-4 week iterations with measurable, vertically-integrated deliverables following agile principles. |
| R20 | `schedule`                      | Do not include a requirements-gathering sprint or thesis-writing/documentation tasks in the schedule. |

## The Baseline: Everything Else Conforms

Read `eval/corpus/proposal/p3-prompt.md`, section "How to Read the Rules Strictly", and apply it
to every part of this proposal that does not carry an introduced defect. Outside the N defect
locations the document must be as clean as the conforming proposal: verified peer-reviewed
references only, no filler words, no forbidden sentence starters, defined abbreviations, title
case, consistent terms, 5–8 line paragraphs, captions of 2–3 sentences, and a schedule of
vertical increments. The template (`p-template/proposal.typ`) is a layout starting point and is
**not** conforming (six objectives, an extra "Evaluation" subsection, an evaluation-only
iteration), so do not copy its content.

## How to Introduce a Defect

Every defect must meet all five conditions:

1. **One rule.** It violates exactly one rule, and a supervisor would call it a violation
   without debate. No borderline cases ("the abstract might be slightly long").
2. **Isolated.** It causes no second violation. Removing or lengthening text must not push
   another length limit over its bound, and a defect in one section must not break a rule that
   another defect already targets.
3. **One location.** It sits in one contiguous passage (a sentence, a paragraph, a heading, a
   caption, a bibliography entry, or a schedule item), and no two defects share a passage.
4. **Visible in text.** The benchmark reviews extracted PDF text without page images, so the
   defect must be detectable from text alone. A figure defect lives in the caption, the figure
   reference, or the prose, never only in the drawing.
5. **Natural.** It reads like a mistake a student makes, not like a planted marker. Mark it in
   the source with a Typst comment (`// DEFECT D3 (R12)`), which does not reach the PDF.

Choose N distinct rules and spread them over the categories: with N ≥ 9, hit every category at
least once. Pick each defect from the catalogue below, which is designed to satisfy the five
conditions, or design an equivalent one that does.

| Rule | Defect to introduce |
|------|---------------------|
| R1  | Omit the Abstract entirely (no heading, no text). Do not combine with an R2 defect on the abstract length. |
| R2  | Let the Problem section run to 1.1–1.3 rendered pages while the total stays within 6 pages. |
| R3  | Give exactly one objective subsection a single paragraph instead of two. |
| R4  | Add one sentence to the Problem section that proposes the remedy and names the planned system or technology. |
| R5  | Open the Motivation with a paragraph that restates two or three Problem sentences almost verbatim. |
| R6  | Phrase one objective, both in the bullet list and in its subsection heading, with a double verb ("Design and Implement …"). |
| R7  | Put one website or blog post into the bibliography as a regular entry and cite it, instead of a footnote. Keep at least 8 peer-reviewed entries besides it. |
| R8  | Place one citation after the full stop of its sentence ("… delays the review. @key"). |
| R9  | Replace the second (non–use-case) diagram with a UML sequence diagram, and say so in its caption ("Sequence diagram of …"). |
| R10 | Give one figure a one- or two-word caption ("System Overview") while it stays referenced in the prose. Do not combine with R9 on the same figure. |
| R11 | Write one sentence with a contraction and a superlative ("The current process doesn't scale and is the worst bottleneck of the chair."). |
| R12 | Start one sentence in the Introduction with "Since". |
| R13 | Replace one prose paragraph of the Motivation with a bullet list of four or five items. Do not combine with R5 on the same paragraph. |
| R14 | Give the Introduction two subsections (1.1, 1.2) with no introductory text before 1.1. |
| R15 | Write one objective subsection heading in sentence case ("Automate the review of submitted requests"). Do not combine with R6 on the same objective. |
| R16 | Switch the term for one central concept in one section and keep the switch for that section only (e.g. "supervisor" becomes "advisor" in the Schedule). |
| R17 | Replace the tool table and its introduction with a generic sentence ("AI tools were used during writing."), keeping the confirmation sentence. |
| R18 | Remove the sentence that confirms the author checked all AI-generated text, keeping the specific tool table. Do not combine with R17. |
| R19 | Split one feature horizontally across two iterations ("Iteration 2: implement the backend", "Iteration 3: implement the frontend"). |
| R20 | Add a final iteration "Write the thesis and prepare the final presentation". |

The "Do not combine" notes are mutually exclusive pairs: for each pair, pick at most one.

## Procedure

1. `cp -R eval/corpus/proposal/p-template eval/corpus/proposal/p4`. Work only inside `p4/`, and
   never edit `p-template/`.
2. Update the metadata block in `p4/proposal.typ` (title, German title, author, examiner, dates)
   to fit the content. Leave the template code (title page, fonts, `diagram()`,
   `ai_usage_table`) unchanged: no margin, font, or spacing changes.
3. Write the full proposal as a conforming document first, with its own `references.yml` and
   diagrams in `figures/diagrams.typ`. Remove every trace of the template's topic.
4. Introduce the N defects, each marked with a `// DEFECT Dk (Rn)` comment.
5. Compile from the repository root:

   ```sh
   typst compile --root eval/corpus/proposal/p4 \
     --font-path eval/corpus/proposal/p4/fonts \
     eval/corpus/proposal/p4/proposal.typ eval/corpus/proposal/p4.pdf
   ```

   Leave no PDF inside `p4/`. The folder holds sources only; `p4.pdf` next to it is the corpus
   document.
6. Check the **rendered PDF**, not the source:
   - render the pages (`typst compile … --format png --ppi 80 "$TMPDIR/p4-{p}.png"`), view them,
     and measure section and paragraph lengths;
   - extract the text (`pdftotext eval/corpus/proposal/p4.pdf -`) and confirm that every defect
     shows up in it, and that nothing outside the defect passages violates a rule (forbidden
     sentence starters, filler words, contractions, undefined abbreviations, unreferenced
     figures or entries).

   Fix and recompile until the PDF contains exactly the N listed defects and nothing else.
7. Write the manifest (see below), copying every quote from the `pdftotext` output so a
   reviewer's quote can be matched against it.

## The Defect Manifest

Write `p4/defects.md` for people and `p4/defects.json` for scripts, with the same content:

```json
[
  {
    "id": "D1",
    "rule": "R12",
    "category": "writing-style",
    "section": "Introduction",
    "page": 2,
    "quote": "verbatim passage from the extracted PDF text that carries the defect",
    "violation": "one sentence: what breaks the rule",
    "conforming": "the passage as it would read without the defect"
  }
]
```

`page` is the number printed on the page (the title page carries none). For a defect that is an
absence (a missing section, a missing confirmation sentence), set `quote` to `null` and describe
where the missing element belongs. Below the table in `defects.md`, add a short section listing
the rules you did **not** target and confirming that the document satisfies them.

## Deliverables

- `eval/corpus/proposal/p4/` with `proposal.typ`, `references.yml`, `figures/diagrams.typ`,
  `defects.md`, and `defects.json`
- `eval/corpus/proposal/p4.pdf`
- A final message with N, the rule each defect targets, and any defect you had to redesign
  because it broke a second rule.
