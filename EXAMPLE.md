# Example Research Group Guidelines

> **How to use this file.** Paste the text below (everything from "General Expectations"
> onward) into *Research Group Settings → AI Review Guidelines*. The preprocessing step
> distills it into per-category rules that drive the AI review; you can then edit the
> distilled rules directly in the structured editor.
>
> Treat this as a starting point, not a standard. Replace every number (page counts,
> citation counts, iteration lengths) with the values your group actually enforces —
> the AI reviewer checks the thresholds literally, so wrong numbers produce wrong findings.
> Delete rules you do not care about: a shorter, honest list beats a long aspirational one.

---

## General Expectations

Students in this group write a proposal before starting the thesis and a final thesis at
the end. A proposal is a concise, high-level document (4–6 pages of text) that describes the
problem, why it matters, the objectives, and a preliminary schedule. A thesis is a full
research artifact (Bachelor: 30–60 pages; Master: 60–100 pages) that presents a research
question, an approach, and a rigorous evaluation. Both are written in clear, direct academic
English and follow the group's LaTeX template.

## 1. Structure and Completeness

* A proposal must contain these sections, in this order: Abstract, Introduction, Problem,
  Motivation, Objectives (with one subsection per objective), Schedule, Bibliography, and an
  AI Transparency Statement.
* A thesis must contain: Abstract, Introduction, Background, Related Work, Approach,
  Implementation, Evaluation, Discussion and Limitations, Conclusion and Future Work,
  Bibliography, and an AI Transparency Statement.
* In a proposal, the abstract is one third to one half of a page, the introduction stays
  below one page, and the Problem and Motivation sections are at most three quarters of a
  page each.
* The Objectives section opens with a short introduction and a bullet list of three to four
  objectives, followed by one subsection per objective of at least two paragraphs.
* Every chapter and section contains introductory text before its first subsection.

## 2. Problem, Motivation, and Objectives

* The Problem section describes problems and their negative consequences for clearly named
  actors. It must not contain solutions, tools, or technology choices.
* Each described problem names who suffers from it and what the concrete consequence is.
* The Motivation section is visionary and forward-looking; it must not restate the Problem
  section.
* Objectives are written in action form, including the headings (for example "Automate the
  review of submitted proposals").
* An objective must not combine two verbs (write "Design a scheduling component", not
  "Design and implement a scheduling component").

## 3. Bibliography and Citations

* A proposal cites at least 6–8 peer-reviewed publications; a thesis cites at least 30
  (Bachelor) to 50 (Master).
* Only peer-reviewed conference papers and journal articles belong in the bibliography.
  Websites, blog posts, and documentation go into footnotes.
* Use the alpha citation style, for example [ABC12], consistently throughout the document.
* Place the citation before the full stop: "…as shown in prior work [ABC12]."
* Every entry in the bibliography is cited at least once in the text, and every in-text
  citation resolves to a bibliography entry.
* Bibliography entries are cleaned up by hand: no duplicates, no truncated author lists, no
  raw Google Scholar exports.

## 4. Figures, Diagrams, and Tables

* A proposal contains at least two figures, at least one of which is a UML diagram or a
  mockup. A thesis contains at least one architecture diagram and at least one figure per
  major contribution.
* Do not use sequence diagrams. Use activity, communication, or component diagrams instead.
* Every figure and table is referenced by number in the text and discussed there — a figure
  that is never mentioned does not belong in the document.
* Captions are long and informative; a caption must be understandable without the
  surrounding text. One-word captions such as "Architecture" are not acceptable.
* All figures must be readable at 100% zoom in the PDF. Use vector graphics (SVG or PDF)
  wherever the source allows it.
* Screenshots are taken in light mode.
* Diagrams use consistent legends, and every axis in every chart is labeled with a unit.

## 5. Writing Style

* Write in active voice and name the actor. Avoid passive constructions, "one", "I", and
  "our". Use "we" sparingly and only when referring to the approach of this work.
* Do not start a sentence with "As", "Since", "To", "In order to", or "Because".
* Avoid filler words such as "additional", "furthermore", "moreover", "also", "actually",
  "clearly", and "obviously".
* Avoid superlatives and vague intensifiers such as "very", "wide", "optimal", and
  "significant" (unless the significance is statistical and reported as such).
* Do not use contractions: write "do not" instead of "don't".
* Define every abbreviation at first use and keep an abbreviation list. Do not introduce
  abbreviations that are used fewer than three times.
* Use the same term for the same concept throughout. Repetition is preferred over synonyms.

## 6. Paragraphs and Section Structure

* Paragraphs are 5–8 lines long and develop exactly one idea.
* Write in prose. Bullet lists are reserved for the schedule, the objective list, and
  enumerations of contributions; each bullet stays within one or two lines.
* Section numbering goes at most three levels deep: 3.1.4 is acceptable, 3.1.4.1 is not.
* Sections shorter than two paragraphs should be merged into an adjacent section or demoted
  to a paragraph.
* Consecutive sections and paragraphs are linked by explicit transitions.

## 7. Formatting and Terminology

* All headings use title case, following the capitalization rules of the heading's language.
* Terminology is consistent across the author's own prose; titles quoted from cited works
  keep their original wording.
* Use the group's LaTeX template unchanged: do not alter margins, fonts, or line spacing to
  reach a page count.

## 8. AI Transparency Statement

* Every submission contains an AI transparency statement.
* The statement is written in the first person and names the concrete tools used (for
  example "ChatGPT", "GitHub Copilot", "DeepL Write").
* For each tool, the statement describes what it was used for and which parts of the
  document or code it touched.
* The statement explicitly confirms that the author reviewed and verified all AI-generated
  text and code and takes full responsibility for it.
* A generic statement such as "AI tools were used during writing" is not sufficient.

## 9. Schedule (Proposal Only)

* The schedule divides the work into iterations of two to four weeks.
* Every iteration lists measurable deliverables. Items such as "research" or "implement
  features" are too vague.
* Deliverables describe vertically integrated features that are demonstrable end to end, not
  horizontal layers such as "build the backend" followed by "build the frontend".
* The schedule contains no dedicated requirements-gathering iteration — requirements are
  refined continuously from the backlog.
* The schedule contains no thesis writing, documentation, or presentation tasks; writing runs
  alongside the development work.
* Iterations reference the objectives they contribute to.

## 10. Evaluation (Thesis Only)

* The Evaluation chapter describes the experimental setup: hardware, software versions,
  datasets, and sample sizes.
* Every reported metric is defined before it is used.
* Comparative claims are backed by a baseline: prior work, a naive approach, or a control
  condition.
* Numerical results report variance or confidence, not single-point measurements.
* The thesis contains a Threats to Validity section covering internal, external, and
  construct validity.
* The thesis states how the results can be reproduced: repository link, dataset access, and
  configuration.
