# proposal-p4 — agentic

- **Model:** gpt-5.6-luna
- **Document type:** PROPOSAL
- **Seed:** 2
- **Assessment:** NEEDS_WORK
- **Score:** 68
- **Findings:** 9
- **Cost:** 6 calls, 38 tool calls, depth 6, 96740 tokens, 26s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a coherent and technically concrete access-management project, with four actionable objectives, two diagrams, a detailed schedule, and an adequate peer-reviewed research base. Before acceptance, add the missing Abstract, remove the solution proposal from the Problem section, revise the double-verb objective, move the Keycloak documentation to a footnote, expand Figure 2's caption, eliminate the contraction, add explicit AI-output review confirmation, and remove thesis-writing/presentation tasks from the development schedule. Also add introductory prose before subsection 1.1 and correct the citation placed after a full stop.

## Findings

### 1. Required Abstract is missing

`CRITICAL` · `COMPLETENESS`

Add the required Abstract section before the Introduction. It should summarise the problem, motivation, objectives, and planned approach in roughly one-third to one-half page.

- Page 1, Abstract

### 2. Problem section proposes a solution

`MAJOR` · `LOGIC`

Remove this proposed solution from the Problem section. Keep the section limited to the current process, its stakeholders, and negative consequences; move the platform proposal to Motivation or Objective.

- Page 3, Problem: “A self-service platform that arms the workflow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Objective combines two verbs

`MAJOR` · `LOGIC`

Use one action per objective, for example “Implement connectors with expiry” or “Specify connectors with expiry,” rather than combining two verbs in one objective.

- Page 4, Objective: “Design and implement connectors with expiry”

### 4. Figure 2 has a generic caption

`MINOR` · `FIGURES`

Expand the caption to explain the architecture’s components, their responsibilities, and the principal connections shown. “System Architecture” alone is not an informative caption.

- Page 5, Objective: “Figure 2: System Architecture”

### 5. Contraction used in formal prose

`MINOR` · `WRITING`

Replace the contraction with its non-contracted form (“does not”). Search for and remove any other contractions throughout the proposal.

- Page 3, Problem: “The current process doesn’t scale and is the worst bottleneck of the chair.”

### 6. AI statement lacks review confirmation

`MAJOR` · `COMPLETENESS`

Add a review-confirmation sentence to the AI transparency statement explicitly stating that you carefully checked all AI-generated text for correctness and sense.

- Page 10, Transparency in the Use of AI Tools: “I used the following generative artificial intelligence (AI) tool while preparing this proposal.”

### 7. Citation appears after the full stop

`MINOR` · `CITATION`

Move the citation before the preceding full stop so the sentence follows the required placement style: “...temporal model [BBF01].” Check the document for the same pattern elsewhere.

- Page 4, Motivation: “temporal    model.    [BBF01]     Joshi  et  al. generalise   this  idea  to  role  assignments”

### 8. Internet documentation is listed in the bibliography

`MAJOR` · `CITATION`

Move this documentation source out of the bibliography and place it in a footnote at its first use. The bibliography should contain scientific peer-reviewed publications only.

- Page 8, Bibliography: “[Key26]       Keycloak     Authors,    “Keycloak     Server    Administration      Guide. ”   Accessed:”

### 9. Introduction begins directly with a subsection

`MINOR` · `STRUCTURE`

Add a short introductory paragraph immediately after the Introduction heading and before subsection 1.1, explaining the purpose and scope of the chapter.

- Page 2, Introduction
