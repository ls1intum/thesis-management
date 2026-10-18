# p3 Conformance Evidence

p3 is the **conforming** proposal of the corpus. Every rule R1–R20 of
`server/src/test/java/de/tum/cit/aet/thesis/feedback/eval/EvalGuidelines.java` holds plainly on
the rendered `eval/corpus/proposal/p3.pdf`. Page numbers refer to that PDF (page 1 is the title
page). A finding against p3 counts as over-reporting.

Rendered layout: Abstract p. 2; 1 Introduction p. 2–3; 2 Problem p. 3; 3 Motivation p. 3–4;
4 Objective p. 4–8 (Figures 1–3 on p. 5–7); 5 Schedule p. 9; Bibliography p. 10–11;
Transparency in the Use of AI Tools p. 12.

| Rule | Where and how p3 satisfies it |
|------|-------------------------------|
| R1 | Top-level headings in order: Abstract, 1 Introduction, 2 Problem, 3 Motivation, 4 Objective (subsections 4.1–4.4), 5 Schedule, Bibliography (p. 10), "Transparency in the Use of AI Tools" (p. 12). Nothing else at top level. |
| R2 | Measured on the rendered pages (about 26 body lines per page): Abstract 10 lines ≈ 0.4 page; Introduction 16 lines ≈ 0.65 page; Problem 16 lines ≈ 0.65 page; Motivation 13 lines ≈ 0.55 page; total body text, excluding title page, figures, bibliography and AI statement, ≈ 5.25 pages. |
| R3 | Section 4 opens with a two-sentence paragraph ending "I pursue four objectives:" and a numbered list of four one-line objectives, followed by exactly one subsection per objective (4.1–4.4), each with two full prose paragraphs. |
| R4 | Problem (p. 3) contains only actors and their negative consequences: *Requesters* "do not know who manages which system", *Maintainers* "cannot act on it", *administrators* "become a bottleneck", former students keep login access, *Auditors* cannot say "who had access to a host last March and who approved it". It names no tool, product, or remedy and contains no "should" or "could". |
| R5 | Motivation (p. 3–4) looks forward: "Expiring access reverses the default of an organisation …", then what each actor gains and how prior research enables this vision. No Problem sentence is repeated. |
| R6 | Objectives and identical headings each start with one imperative verb: "Unify heterogeneous resources in one tree", "Enforce an expiry date on every grant", "Delegate the approval of requests to maintainers", "Automate the recertification of active grants". No double verbs. |
| R7 | Bibliography has 12 entries, all peer-reviewed journal articles or conference papers, each checked by web search (title, authors, venue, year; DOIs where confirmed): Saltzer & Schroeder 1975; Sandhu et al. 1996; Sandhu et al. 1999 (ARBAC97); Barka & Sandhu 2000; Bertino et al. 2001 (TRBAC, pp. 191–223); Joshi et al. 2005; Schaad et al. 2001; Kern et al. 2002; Beautement et al. 2008; Bauer et al. 2009; Hu et al. 2015; Ucan & Demirel Ucan 2026. Websites (Keycloak, Slack and GitHub APIs, the Bruegge & Dutoit publisher page) appear only as footnotes 1–3. Every entry is cited at least once. |
| R8 | `#set cite(style: "alphanumeric")` throughout ([SS75], [Bau+09, BSW08]). Every citation sits inside its sentence before the full stop, e.g. "… than a workaround [Bau+09, BSW08]." |
| R9 | Three figures (Fletcher, `figures/diagrams.typ`): Figure 1 UML use case diagram, Figure 2 UML object diagram, Figure 3 UML component diagram. No sequence diagram; the word "sequence" does not occur. |
| R10 | Each figure is referenced in the prose before it appears ("Figure 1 shows …" p. 4, "Figure 2 shows" p. 5, "Figure 3 shows" p. 6), and each caption has 2–3 sentences that say what the figure shows and how to read it. |
| R11 | Active voice with named actors ("I implement …", "the maintainer records …", "Keycloak gates …"). A scan of the extracted text finds no contractions in the body (the only apostrophe is "Bachelor’s" on the title page), none of the listed filler words or superlatives, and no "is/are … -ed" passives. |
| R12 | A scan of every extracted sentence, including bullets and captions, finds none starting with As, Since, To, In order to, or Because. Abbreviations are defined at their first use in the body and used at least three times: Secure Shell (SSH) ×7, Lightweight Directory Access Protocol (LDAP) ×3, application programming interfaces (APIs) ×4, Unified Modeling Language (UML) ×5, artificial intelligence (AI) in the statement. The abstract uses no abbreviations. |
| R13 | Prose everywhere except the objective list and the schedule. Every rendered paragraph has 5–7 lines and develops one idea (e.g. the Problem has one paragraph per affected group). Run-in bold labels sit inside prose paragraphs. |
| R14 | Only Section 4 has subsections, and its introductory paragraph and list come before 4.1. Numbering is at most two levels deep (4.1–4.4). |
| R15 | All headings use Chicago title case, e.g. "Delegate the Approval of Requests to Maintainers", "Transparency in the Use of AI Tools". |
| R16 | One term per concept: request, grant, expiry, resource tree/folder, connector, binding, requester, maintainer (defined once as "the supervisors who …", then only "maintainer"), administrator, auditor, recertification campaign, review step / grant step, web application. "Permission" appears only in the product name "Fine-Grained Admin Permissions". |
| R17 | First-person statement (p. 12) with the template table: Anthropic Claude Code, Claude Opus 5.5 (claude-opus-5-5), 18.10.2026, level 5, purpose "Drafting text, authoring diagrams, researching and verifying references", scope "Abstract, Sections 1-5, Figures 1-3, bibliography", plus prose that repeats tools, purposes, and sections. |
| R18 | "I carefully checked all AI-generated text, revised it where necessary, verified every citation against the original publication, and compiled and inspected every diagram. I take full responsibility for the content of this proposal." |
| R19 | Six four-week iterations with explicit dates (1 Oct – 17 Mar), followed by two weeks of buffer up to 31 March 2027. Each iteration names the objectives it serves and a measurable, demonstrable outcome (e.g. "an approved SSH session stops working at expiry"). |
| R20 | No iteration covers requirements gathering, literature research, writing, documentation, presentation, or evaluation alone. Iteration 6 delivers the `webhook` connector and the deployment at the chair. The buffer is reserved "for features that slip from earlier iterations", not for writing. |
