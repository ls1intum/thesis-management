# Defect Manifest: p4

`p4.pdf` is the deliberately defective proposal of the evaluation corpus. It contains exactly
**N = 12** introduced defects, one per rule, listed below. Every quote is copied from
`pdftotext eval/corpus/proposal/p4.pdf -` with line breaks collapsed to single spaces. `page`
is the number printed on the page; the title page and the AI statement page carry none.
`defects.json` holds the same content for scripts. Each defect is marked in the source with a
`// DEFECT Dk (Rn)` comment (D3 additionally in `references.yml`).

| ID | Rule | Category | Section | Page | Quote | Violation | Conforming version |
|----|------|----------|---------|------|-------|-----------|--------------------|
| D1 | R1 | `structure` | Abstract | 2 | — (absent) | The Abstract is missing entirely: the body starts with "1 Introduction" on page 2, with no Abstract heading and no abstract text. | An unnumbered "Abstract" section of about 0.4 page before "1 Introduction" that summarises the setting, the problem, the platform, and its evaluation. |
| D2 | R14 | `writing-structure` | 1 Introduction | 2 | "1 Introduction 1.1 Setting A university research chair" | Section 1 opens directly with subsection 1.1; there is no introductory text between the section heading and its first subsection. | 1 Introduction This section describes the setting of the thesis and the infrastructure that the chair runs today. 1.1 Setting A university research chair |
| D3 | R7 | `bibliography` | Bibliography | 8 | "[Key26] Keycloak Authors, “Keycloak Server Administration Guide.” Accessed: Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/ latest/server_admin/" | A website (the Keycloak documentation) appears as a regular bibliography entry and is cited as [Key26] in Section 1.2 (page 2) instead of being placed in a footnote. | No [Key26] entry; Section 1.2 ends the sentence with a footnote: "restrict an administrative right to a single group.¹" with footnote "https://www.keycloak.org/docs/latest/server_admin/". |
| D4 | R11 | `writing-style` | 2 Problem | 3 | "The current process doesn’t scale and is the worst bottleneck of the chair." | The sentence uses a contraction ("doesn’t") and a superlative ("the worst"). | Sentence removed; the paragraph ends with "… explain it as a limited compliance budget [BSW08]." |
| D5 | R4 | `problem-motivation-objectives` | 2 Problem | 3 | "A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation." | The Problem section proposes the remedy and names the planned system and its technology (Keycloak workflow engine). | Sentence removed; the paragraph ends with "… because the history sits in scattered chat messages." |
| D6 | R8 | `bibliography` | 3 Motivation | 4 | "temporal model. [BBF01] Joshi et al. generalise this idea to role assignments with a limited duration [Jos+05]." | The citation [BBF01] stands after the full stop of the sentence "Bertino et al. attach enabling and disabling events to roles in their temporal model.", which starts at the foot of page 3. | Bertino et al. attach enabling and disabling events to roles in their temporal model [BBF01]. Joshi et al. generalise this idea to role assignments with a limited duration [Jos+05]. |
| D7 | R6 | `problem-motivation-objectives` | 4 Objective / 4.2 | 4 | "Design and implement connectors with expiry" | The second objective uses a double verb ("Design and implement") in the objective list and in the heading "4.2 Design and Implement Connectors with Expiry" on page 5. | List item "Implement connectors with expiry" and heading "4.2 Implement Connectors with Expiry". |
| D8 | R10 | `figures` | 4.2 Design and Implement Connectors with Expiry | 5 | "Figure 2: System Architecture" | Figure 2 has a two-word caption that does not say what the figure shows or how to read it. | Figure 2: UML component diagram of the deployment. The management API changes Keycloak with the token of the calling user, while the LDAP bridge holds a separate read-only credential and projects users, groups, and SSH keys to the hosts. The bot tokens that reach Slack and GitHub cannot reach Keycloak or the hosts. |
| D9 | R16 | `writing-formatting` | 4.3 Delegate Request Review Without Privilege Escalation | 6 | "no resource owner may hold that role. I will therefore split the approval: in the review step, the resource owner records a decision without changing Keycloak" | Section 4.3 calls the maintainer "resource owner" (three times), while every other section, the objective list, and Figure 1 use "maintainer". | Every "resource owner" in Section 4.3 reads "maintainer", e.g. "no maintainer may hold that role. … in the review step, the maintainer records a decision without changing Keycloak" and "The maintainer completes a request alone …". |
| D10 | R3 | `structure` | 4.4 Automate the Recertification of Grants | 6 | "4.4 Automate the Recertification of Grants I will attach a recertification interval to each resource" | Objective subsection 4.4 consists of a single paragraph instead of at least two. | A second paragraph after the first, holding the audit sentence and the success criterion: "The platform logs every change with the person who made it, and the admin event log of Keycloak keeps a second copy. The objective is met when the platform answers, for any grant, who requested it, who reviewed and granted it, when it ended, and who recertified it in between." |
| D11 | R20 | `schedule` | 5 Schedule | 7 | "Iteration 7 (18–31 March): Write the thesis and prepare the final presentation." | The schedule contains an iteration for thesis writing and presentation preparation. | Buffer (18–31 March): two weeks remain for unplanned work from earlier iterations. |
| D12 | R18 | `ai-transparency` | Transparency in the Use of AI Tools | — | — (absent) | The AI statement (unnumbered last page) names the tool, purpose, and scope but never confirms that the author checked all AI-generated text; the confirmation belongs directly below the table. | Below the table: "I carefully checked all AI-generated text, verified every citation against the original publication, and take full responsibility for the submitted work." |

## Rules Not Targeted

The document satisfies the eight rules that carry no defect:

- **R2 (length):** the body runs from page 2 to page 7, six pages including both figures. Rendered
  lengths: Introduction ≈ 0.69 page, Problem ≈ 0.69 page (including the D4 and D5 sentences),
  Motivation ≈ 0.55 page. The abstract length is not assessable because the abstract is absent (D1).
- **R5 (motivation):** the Motivation describes what becomes possible for each actor and which
  research it builds on; it restates no Problem sentence.
- **R9 (figures):** two figures; Figure 1 is a UML use case diagram and Figure 2 a UML component
  diagram. There is no sequence diagram and no caption uses the word "sequence".
- **R12 (sentence starters, abbreviations):** no sentence starts with As, Since, To, In order to, or
  Because. SSH, OIDC, LDAP, and API are defined at first use in the body and used at least three
  times; AI is defined in the first sentence of the statement. Other capitals occur only in
  bibliography entries.
- **R13 (prose):** every body paragraph renders at 5–8 lines and develops one idea. Bullets appear
  only in the objective list and the schedule.
- **R15 (title case):** every heading, including the four objective headings, is in title case.
- **R17 (AI statement):** the statement is first person and names the tool (Anthropic Claude Code,
  Claude Opus 5.5), the purpose (drafting text, authoring Typst sources and diagrams, researching
  references), and the scope (Sections 1–5, Figures 1–2, bibliography entries).
- **R19 (iterations):** iterations 1–6 last four weeks each, carry explicit dates, name the
  objective they serve, and end with a demonstrable, vertically integrated outcome. Iteration 7
  (two weeks) is the D11 defect.

## Notes for Scoring

- **D1** is listed under R1 only. A reviewer may phrase it as an abstract-length issue (R2); match it to D1.
- **D11** is listed under R20 only. A writing iteration is not a vertical increment, so a reviewer may
  file it under R19; match it to D11.
- **D7** occupies the objective bullet (page 4) and the heading of Section 4.2 (page 5); both are one defect.
- **D3** is visible both as the bibliography entry (page 8) and as the citation [Key26] (page 2).
- The other bibliography entries are peer-reviewed journal or conference papers whose existence and
  metadata were checked against Crossref. The Slack and GitHub links are footnotes, as R7 requires.
- Section 4 names the object-oriented software engineering method of Bruegge and Dutoit without a
  citation. That is not a violation of any rule; a finding on it is an over-report.
