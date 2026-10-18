# proposal-p4 — agentic

- **Model:** Qwen/Qwen3.8-27B
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 55
- **Findings:** 15
- **Cost:** 9 calls, 36 tool calls, depth 9, 205275 tokens, 262s
- **Coverage:** 9/9 categories

## Summary

The proposal is well organized: a clearly scoped problem for named actors (requester, maintainer, super admin, auditor), a visionary motivation grounded in 12 peer-reviewed access-control publications, four measurable objectives with a two-paragraph subsection each (except 4.4), two properly referenced UML diagrams, and a plausible four-week iteration schedule with demonstrable outcomes. However, one required section is missing entirely (Abstract — critical) and several rule violations must be fixed before submission: the Problem section prescribes a solution, objective 2 uses the prohibited double verb "Design and implement", subsection 4.4 is only one paragraph, the online Keycloak documentation sits in the bibliography instead of a footnote, Bruegge and Dutoit are named in the text without a citation or reference, the AI transparency statement lacks the required author-check confirmation, and the schedule ends with an iteration devoted to writing the thesis and preparing the presentation. Minor fixes: add introductory text under "1 Introduction", move [BBF01] before the full stop, expand Figure 2's one-line caption, remove the contraction "doesn't" and the superlative "worst bottleneck", reduce the pervasive first-person "I", and unify "maintainer"/"resource owner".

## Findings

### 1. Required Abstract section is missing

`CRITICAL` · `STRUCTURE`

The proposal has no Abstract section at all (the word "Abstract" does not occur anywhere). The required sections are Abstract, Introduction, Problem, Motivation, Objective, Schedule, Bibliography, and AI Transparency Statement. Add an abstract of 1/3 to 1/2 page summarizing the problem, the proposed platform, and the four objectives, placed before the Introduction.

- Page 1, Abstract

### 2. Problem section presents a solution

`MAJOR` · `STRUCTURE`

The Problem section must describe only problems and their negative consequences, not solutions. The final sentence of the Problem section prescribes the solution (a self-service platform arming the Keycloak workflow engine with an expiry date). Move this sentence — and any solution design — into Motivation or Objective, and end the Problem section with the negative consequences (stale access, no audit trail) instead.

- Page 3, Problem: “A self-service platform that arms the workfow engine of Keycloak with an expiry date at grant time would end this accumulation.”

### 3. Objective 2 uses a double verb ("Design and implement")

`MAJOR` · `WRITING`

Objective 2 (bullet in Section 4 and heading of 4.2) uses a double verb, exactly the pattern the guidelines prohibit ("Design and implement" is the cited bad example). Pick a single verb per objective, e.g. "Implement connectors with expiry" or "Design connectors with expiry", in both the bullet list and the subsection heading.

- Page 4, Objective: “Design   and   implement      connectors     with   expiry”

### 4. Objective subsection 4.4 has only one paragraph

`MAJOR` · `STRUCTURE`

Each objective subsection must be at least two paragraphs long, but Section 4.4 contains only a single paragraph (four sentences that continue across the page break to page 7). Subsections 4.1, 4.2, and 4.3 each have two paragraphs. Split 4.4 into at least two paragraphs — for example, one on how recertification intervals and campaigns work, and one on escalation, automatic revocation, and the audit record.

- Page 6, 4.4 Automate the Recertifcation of Grants: “I will attach a recertifcation interval to each resource, which its descendants inherit, and open a campaign whenever the interval elapses.”

### 5. Section 1 has no introductory text before its first subsection

`MINOR` · `STRUCTURE`

Section 1 (Introduction) jumps directly from the section heading to subsection 1.1 "Setting" without any introductory text. Every chapter must have introductory prose before its first subsection. Add a short paragraph under "1 Introduction" that frames the thesis topic and previews the Setting and Existing Infrastructure subsections.

- Page 2, Introduction: “1.1 Setting”

### 6. Bruegge and Dutoit referenced in text but missing from the bibliography

`MAJOR` · `CITATION`

The text attributes a method to Bruegge and Dutoit without a citation marker, and there is no Bruegge/Dutoit entry in the bibliography. Add a citation (e.g. [BD11] for "Software Engineering: An Object-Oriented Case Study") immediately after the mention and add the corresponding reference entry.

- Page 4, Objective: “I follow the object-oriented software engineering method of Bruegge and Dutoit, and Figure 1 shows the actors and use cases of the platform”

### 7. Citation [BBF01] placed after the full stop

`MINOR` · `CITATION`

The citation [BBF01] is placed after the full stop ("temporal model. [BBF01]"), whereas all other citations in the proposal are placed before the punctuation. Move the marker to before the period: "... in their temporal model [BBF01]."

- Page 4, Motivation: “temporal    model.    [BBF01]     Joshi  et  al. generalise   this  idea  to  role  assignments  with a limited   duration     [Jos+05].”

### 8. Figure 2 caption is short and generic

`MINOR` · `FIGURES`

Figure 2's caption is just "System Architecture" — short and generic, while Figure 1's caption is a full, informative description. Expand the caption to describe the components (mgmt-ui, mgmt-api with GrantPlanner/ConnectorRegistry/DeprovisionReaper, PostgreSQL, ldap-bridge), the role of Keycloak, and why Slack/GitHub/Linux hosts are secondary actors, matching the style of Figure 1's caption.

- Page 5, 4.2 Design and Implement Connectors with Expiry: “Figure 2:  System  Architecture”

### 9. Contraction "doesn't" in the Problem section

`MINOR` · `WRITING`

Contractions are not allowed in the proposal. Rewrite "doesn't" as "does not".

- Page 3, Problem: “The    current   process    doesn’t scale  and   is the  worst   bottleneck     of the  chair.”

### 10. Documentation source [Key26] belongs in a footnote, not the bibliography

`MAJOR` · `CITATION`

[Key26] is a product documentation page (Keycloak Server Administration Guide, accessed online). Internet/documentation sources must be footnotes, not bibliography entries, and the bibliography may contain only peer-reviewed publications. Move this URL into a footnote (as the author already does for the Slack/GitHub API docs) and keep the peer-reviewed entries in the bibliography.

- Page 8, Bibliography: “Oct. 18, 2026. [Online]. Available: https://www.keycloak.org/docs/”

### 11. Superlative "worst bottleneck" in the Problem section

`MINOR` · `WRITING`

"worst bottleneck" is a superlative/hyperbole in a factual problem statement. State the concrete negative consequence instead (e.g., access requests wait for days and block onboarding), without superlative wording.

- Page 3, Problem: “scale  and   is the  worst   bottleneck     of the  chair.”

### 12. Systematic use of "I" in Objectives and Schedule

`MINOR` · `WRITING`

The style guide asks to avoid "I" and to use "we" only sparingly for the thesis approach, but Section 4 (Objective) and the Schedule use first-person singular in nearly every sentence ("I will build", "I will replace", "I will store", "I will define", "I measure", "I will compare", "I will attach", "I divide it", "I demonstrate"). Reformulate the thesis approach with "we" (or a subject such as "the platform") so "I" does not dominate the text.

- Page 4, Objective: “I will build  a  platform    on  which   any  member     of  the  chair  requests   time-limited access   to  any  resource,    a maintainer      decides,    and  the  access   ends   on  its  own.”

### 13. Inconsistent terminology: "maintainer" vs "resource owner"

`MINOR` · `FORMATTING`

The same actor is called "maintainer" throughout (Introduction, Problem, Motivation, Figure 1, Sections 4.1 and 4.4) but "resource owner" in Section 4.3. Pick one term — the diagrams and most of the prose use "maintainer" — and replace "resource owner" in 4.3 for consistency.

- Page 6, 4.3 Delegate Request Review Without Privilege Escalation: “in  the   review   step,  the  resource   owner    records   a decision   without    changing     Keycloak,    and”

### 14. AI transparency statement lacks the author-check confirmation

`MAJOR` · `OTHER`

The AI transparency statement names the tool, model, version, date, level, purposes, and sections, which is good, but it lacks the required confirmation sentence stating that the author has carefully checked all AI-generated text. Add a sentence such as: "I have carefully checked all texts created with these tools to ensure that they are correct and make sense."

- Page 10, Transparency in the Use of AI Tools

### 15. Iteration 7 contains thesis writing and presentation preparation

`MAJOR` · `OTHER`

The schedule must focus on development/research work and must not include thesis-writing or presentation-preparation tasks. Iteration 7 is exactly that. Remove it and redistribute the 18-31 March window into the development iterations (e.g., extend iteration 6 or add a hardening/deployment iteration).

- Page 7, Schedule: “Iteration     7  (18–31      March):      Write   the   thesis  and   prepare    the”
