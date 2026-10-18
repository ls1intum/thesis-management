# proposal-p3 — category-fan-out-unverified

- **Model:** openai/gpt-oss-120b
- **Document type:** PROPOSAL
- **Seed:** 1
- **Assessment:** NEEDS_WORK
- **Score:** 42
- **Findings:** 4
- **Cost:** 10 calls, 0 tool calls, depth 2, 93554 tokens, 67s
- **Coverage:** 9/9 categories

## Summary

The proposal presents a clear research direction but suffers from several structural and formatting violations. Critical issues include exceeding the allowed page limit and insufficient detail in objective subsections, while multiple paragraphs are overly long and an abbreviation is undefined.

## Findings

### 1. Proposal exceeds maximum allowed length

`CRITICAL` · `structure`

The total text length of the proposal (excluding figures, tables, and schedule) spans more than 6 pages, violating the 4‑6 page limit.

- Page 12: “**=== PAGE 12 ===**”

### 2. Objective subsections lack required detail and insufficient paragraph count

`MAJOR` · `structure`

Each objective subsection contains only a single paragraph; the guidelines require at least two paragraphs with detailed descriptions for every objective.

- Page 5, Objective: “I model   the   resources    as a  tree whose    folders  pass   maintainership,      audit  policy, and   grants   down    to  leaves  bound     to target   systems,    as  the  UML     object   diagram   in Figure 2 shows.”
- Page 5, 4.1 Unify Heterogeneous Resources in One Tree: “I model the resources as a tree whose folders pass maintainership, audit policy, and grants down to leaves bound to target systems, as the UML object diagram in Figure 2 shows.”
- Page 6, 4.2 Enforce an Expiry Date on Every Grant: “The platform changes Keycloak only with the token of the calling user and holds no service account, so no background process of the platform can remove a membership.”
- Page 8, 4.3 Delegate the Approval of Requests to Maintainers: “Requesters fll in a declarative request form instead of naming resources, and its rules turn answers into requests for a resource path, an access level, and a duration, which the platform routes to the maintainer of each resource.”
- Page 8, 4.4 Automate the Recertifcation of Active Grants: “Every resource carries a recertifcation interval that its descendants inherit, and each elapsed interval opens a campaign in which the maintainer keeps or revokes every grant with an immutable decision.”

### 3. Paragraphs exceed recommended length

`MAJOR` · `formatting`

Several paragraphs (abstract, introductory sections, problem, motivation, and objective) span more than ten lines, exceeding the 5‑8 line recommendation.

- Page 2, Abstract: “**A university   research    chair  grants   access   to its compute     hosts,  chat   workspace,** and  code   repositories    by  hand,   while  the  people    who   need   that  access  change    every semester.    Requests    travel   by  chat  message     or  e-mail,  nobody     records   an   end  date, and   access  granted    for  one   project   outlives   the  time   a person    spends    at  the  chair.”
- Page 2, Introduction: “**A  university    research    chair,  or  a team    of similar   size,  runs  shared    infrastruc** ture:  Linux    hosts   with   graphics     processors    that   its  members      reach   over   Secure”
- Page 3, Problem: “**Requesters,       such   as thesis  students    and   external   collaborators,     do  not  know** who manages which system. A student who needs a host, a channel, and a”
- Page 3, Motivation: “**Expiring    access   reverses   the   default   of an   organisation:     access   lasts  until  its** expiry   date  unless   its holder   explains    the  need   for more    time.”
- Page 4, Objective: “**In  this  thesis,  I develop    a self-service    platform    on   which    requesters    ask  for** time-limited    access,   maintainers     decide,   and  access   ends  on  its own;   Figure  1  shows”

### 4. Undefined abbreviation "UML" used without prior definition

`MINOR` · `writing`

The abbreviation 'UML' appears in the proposal before it is defined, violating the guideline that abbreviations must be defined before use.

- Page 5: “Figure 1: **UML** use case diagram of the access management platform.”
- Page 6: “Figure 2: **UML** object diagram of a small resource tree at the chair.”
- Page 7: “Figure 3: **UML** component diagram of the platform and its environment.”
