# proposal-p4 — agentic

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 3
- **Assessment:** NEEDS_WORK
- **Score:** 62
- **Findings:** 11
- **Cost:** 7 calls, 33 tool calls, depth 7, 102203 tokens, 23s
- **Coverage:** 9/9 categories

## Summary

The proposal has a coherent problem context, four objectives, two referenced diagrams, a detailed schedule, and a specific AI-tools table. However, it needs substantial revision: the required Abstract is absent; the Problem section proposes the solution; one objective uses two verbs and one objective subsection does not meet the two-paragraph requirement; a documentation page is incorrectly included in the bibliography; the AI statement lacks the required confirmation that generated text was checked; the schedule includes thesis writing and presentation preparation; and at least one citation, a contraction, and the Figure 2 caption violate the stated rules.

## Findings

### 1. Required Abstract Is Missing

`CRITICAL` · `COMPLETENESS`

Add the required Abstract section before the Introduction, with a proposal summary of approximately one-third to one-half page.

- Page 1, Abstract

### 2. Introduction Lacks Text Before Its First Subsection

`MINOR` · `STRUCTURE`

Add a short introductory paragraph under Section 1 before subsection 1.1, explaining what the introduction covers; the section currently jumps directly from the chapter heading to a subsection.

- Page 2, Introduction

### 3. Problem Section Proposes a Solution

`MAJOR` · `LOGIC`

Keep the Problem section limited to the problem and its consequences. Move the proposed platform and expiry-based solution to Motivation or Objective, or rewrite this sentence as a consequence without proposing a system.

- Page 3, Problem: “A self-service platform that arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 4. Objective Uses Two Verbs

`MAJOR` · `METHODOLOGY`

Rewrite the objective with one action verb, for example by choosing either designing or implementing, and apply the same correction to the corresponding subsection heading and objective bullet.

- Page 4, Objective: “•   Design   and   implement      connectors     with   expiry”

### 5. Citation Follows the Full Stop

`MINOR` · `CITATION`

Move the citation before the sentence-ending full stop so the citation is integrated into the sentence, e.g. “temporal model [BBF01].”

- Page 4, Motivation: “temporal    model.    [BBF01]     Joshi  et  al. generalise   this  idea  to  role  assignments”

### 6. Documentation Is Listed as a Bibliography Entry

`MAJOR` · `CITATION`

Remove the Keycloak administration guide from the bibliography and retain it as a footnote or other non-bibliography web reference, because the bibliography should contain peer-reviewed publications only.

- Page 8, Bibliography: “[Key26]       Keycloak     Authors,    “Keycloak     Server    Administration      Guide. ”   Accessed:”

### 7. AI Statement Lacks Review Confirmation

`MAJOR` · `COMPLETENESS`

Add an explicit first-person confirmation that you carefully checked all AI-generated text for correctness and coherence, as required by the transparency statement.

- Page 10, Transparency in the Use of AI Tools: “I used  the   following   generative    artifcial   intelligence    (AI)  tool  while   preparing    this proposal.”

### 8. Schedule Includes Thesis Writing and Presentation Tasks

`MAJOR` · `SCHEDULE`

Remove thesis writing and final-presentation preparation from the development schedule. Replace this iteration with research or implementation work tied to the objectives, and schedule thesis writing separately if needed outside the agile development plan.

- Page 7, Schedule: “•   Iteration     7 (18–31      March):      Write   the   thesis  and   prepare    the  fnal   presen­ tation.”

### 9. Objective Subsection Has Only One Paragraph

`MAJOR` · `STRUCTURE`

Split subsection 4.1 into at least two paragraphs, while retaining the detailed content. It currently presents the subsection as one continuous paragraph, unlike the required two-paragraph minimum for every objective subsection.

- Page 5, Objective — Structure Resources as an Inheriting Tree: “Maintainership,      audit  policy,  and   grants  pass  down    from   a  folder  to  every  resource beneath    it. One   approval    on  a project   folder   therefore   covers   its Slack  channel    and                                                                                    its   GitHub     team,    and   it covers   a  resource    that  a  maintainer     adds   to  the   folder   next   month    without    a   second   decision.”

### 10. Contraction Used in Formal Prose

`MINOR` · `WRITING`

Replace the contraction with its expanded form (“does not”). Apply the same check throughout the proposal because contractions are disallowed by the writing rules.

- Page 3, Problem: “The    current   process    doesn’t                                                                                    scale”

### 11. Figure 2 Caption Is Not Informative Enough

`MINOR` · `FIGURES`

Expand the Figure 2 caption to explain the components, trust boundaries, and data/control flows shown, rather than providing only a generic label.

- Page 5, Objective — Design and Implement Connectors with Expiry: “Figure  2:  System  Architecture”
