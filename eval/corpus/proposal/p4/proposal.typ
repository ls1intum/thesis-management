#import "/figures/diagrams.typ": usecase-diagram, component-diagram

// ============================================================================
// Metadata
// ============================================================================

#let title = "Self-Service Management of Time-Limited Access to Heterogeneous Resources"
#let titleGerman = "Self-Service-Verwaltung zeitlich begrenzter Zugriffe auf heterogene Ressourcen"
#let degree = "Bachelor" // "Bachelor" or "Master"
#let program = "Informatics"
#let examiner = "Prof. Dr. Stephan Krusche"
#let supervisors = ()
#let author = "Benjamin Schmitz"
#let startDate = datetime(day: 1, month: 10, year: 2026)
#let submissionDate = datetime(day: 31, month: 3, year: 2027)

// ============================================================================
// Template
// ============================================================================

#let fonts = (
  body: "New Computer Modern",
  sans: "New Computer Modern Sans",
)

// Figure with a short caption for the outline.
#let in-outline = state("in-outline", false)
#let flex-caption(long, short) = context if in-outline.get() { short } else { long }
#let diagram(content, caption: none, short-caption: none) = figure(
  content,
  caption: flex-caption(caption, if short-caption == none { caption } else { short-caption }),
)

// Visible only when compiled with `--input TODO=1`.
#let TODO(body, color: yellow) = {
  if sys.inputs.at("TODO", default: none) != none {
    block(width: 100%, radius: 3pt, stroke: 0.5pt, fill: color, inset: 10pt, body)
  }
}

// Rows for the "Transparency in the Use of AI Tools" table.
// Level: Integrated GenAI Usage Taxonomy (Ucan & Demirel Ucan, 2026):
//   1 Human-authored, 2 Language editing, 3 Ideation and outlining,
//   4 AI-generated drafts, 5 Predominantly AI-generated, 6 AI-orchestrated.
// Report the highest level used; "n/a" for tools that do not contribute text.
#let ai_usage(tool: "", model: "", dates: "", level: "", purpose: "", scope: "") = (
  tool, model, dates, level, purpose, scope,
)

#let ai_usage_table(..entries) = {
  set text(size: 9.5pt, hyphenate: false)
  set par(leading: 0.65em, justify: false, first-line-indent: 0pt)
  table(
    columns: (17%, 18%, 19%, 9%, 20%, 17%),
    align: left + top,
    inset: (x: 3pt, y: 6pt),
    stroke: (x, y) => (top: if y > 1 { 0.4pt } else { 0pt }),
    table.hline(stroke: 0.7pt),
    table.header([*Tool*], [*Model / Version*], [*Date(s)*], [*Level*], [*Purpose*], [*Scope*]),
    table.hline(stroke: 0.7pt),
    ..entries.pos().flatten(),
    table.hline(stroke: 0.7pt),
  )
}

#let titlepage() = {
  assert(degree in ("Bachelor", "Master"), message: "The degree must be either 'Bachelor' or 'Master'")
  set page(margin: (x: 20mm, y: 30mm), numbering: none)
  set par(leading: 0.5em)

  v(1cm)
  align(center, image("/figures/tum_logo.png", width: 26%))
  v(5mm)
  align(center, text(font: fonts.sans, 2em, weight: 700, "Technical University of Munich"))
  v(5mm)
  align(center, text(font: fonts.sans, 1.5em, weight: 100, "School of Computation, Information and Technology \n -- Informatics --"))
  v(15mm)
  align(center, text(font: fonts.sans, 1.3em, weight: 100, degree + "’s Thesis in " + program))
  v(8mm)
  align(center, text(font: fonts.sans, 2em, weight: 700, title))
  align(center, text(font: fonts.sans, 2em, weight: 500, titleGerman))

  let entries = (("Author", author), ("Examiner", examiner))
  if supervisors.len() > 0 {
    entries.push(("Supervisor" + if supervisors.len() > 1 [s], supervisors.join(", ")))
  }
  entries.push(("Start Date", startDate.display("[day].[month].[year]")))
  entries.push(("Submission Date", submissionDate.display("[day].[month].[year]")))

  v(1cm)
  align(center, grid(
    columns: 2,
    gutter: 1em,
    align: left,
    ..for (term, desc) in entries { ([*#term:*], desc) },
  ))
}

#set document(title: title, author: author)
#set text(font: fonts.body, size: 12pt, lang: "en")

#titlepage()
#pagebreak()

#set page(margin: (x: 30mm, y: 40mm), numbering: "1", number-align: center)
#show math.equation: set text(weight: 400)
#show heading: set block(below: 0.85em, above: 1.75em)
#show heading: it => {
  it
  h(1.45em)
}
#set par(leading: 1em, justify: true, first-line-indent: 2em)
#set cite(style: "alphanumeric")
#show figure: set text(size: 0.85em)
#show figure.caption: set text(size: 9.2pt)
#show figure.caption: set par(leading: 0.5em)

// ============================================================================
// Content
// ============================================================================

// DEFECT D1 (R1): the Abstract section is missing entirely (no heading, no text).

#set heading(numbering: "1.1")

= Introduction

// DEFECT D2 (R14): no introductory text between the Introduction heading and Section 1.1.

== Setting

A university research chair, or a team of similar size, operates shared infrastructure: Linux
hosts with graphics processors reached over Secure Shell (SSH), a Slack workspace, a GitHub
organisation, and a handful of internal dashboards and databases. Its people change every
semester, because thesis students, external collaborators, and working students join and leave
on their own dates. Whoever needs access to any part of this infrastructure asks for it by chat
message or e-mail.

== Existing Infrastructure

// DEFECT D3 (R7): cites the Keycloak guide, a website listed as a regular bibliography entry.
Keycloak serves as the identity provider of the chair. It handles OpenID Connect (OIDC) login,
groups, and profile attributes such as SSH public keys, and its Fine-Grained Admin Permissions
restrict an administrative right to a single group @keycloak2026admin. The Linux hosts read this
directory over the Lightweight Directory Access Protocol (LDAP) through `nslcd`, `sshd`,
`pam_access`, and `sudoers`, and an Ansible role configures each host. Slack and GitHub manage
membership through their own application programming interfaces
(APIs)#footnote[https://api.slack.com/ and https://docs.github.com/en/rest] and tokens, and
neither shares an administrative interface with Keycloak.

= Problem

// DEFECT D4 (R11): the last sentence of this paragraph carries a contraction and a superlative.
*Requesters* do not know who manages which resource. A student who needs a host, a channel, and a
repository asks several people and meanwhile borrows the session of a colleague or works under
a shared account. Bauer et al. observed this workaround in far more mature organisations
@bauer2009real, and Beautement et al. explain it as a
limited compliance budget @beautement2008compliance. The current
process doesn't scale and is the worst bottleneck of the chair.

*Maintainers*, the staff who look after a project, know whether a request is legitimate
but lack the rights to act on it, while the super admin holds the rights but lacks the context.
The usual workaround hands the maintainer full administrative rights over every user and every
host. It contradicts the protection principles of Saltzer and Schroeder
@saltzer1975protection and leaves open the delegation problem of administrative role models
@sandhu1999arbac97 @barka2000framework.

// DEFECT D5 (R4): the last sentence of this paragraph proposes the remedy and names Keycloak.
*Access outlives its purpose and leaves no trace.* No grant carries an expiry date, so access
piles up and former students keep SSH access months after they leave, the same decay that Schaad
et al. @schaad2001role and Kern et al. @kern2002observations report from industry. *Auditors*
cannot reconstruct who held access when, or who approved it, because the history sits in
scattered chat messages. A self-service platform that arms the workflow engine of Keycloak with
an expiry date at grant time would end this accumulation.

= Motivation

Access that ends on its own changes the question an organisation answers: it no longer asks who
still needs a grant, but only who wants more time. The requester describes a need once, in terms
of a project, and receives working access within minutes. The maintainer keeps the judgement over
a request without holding any privilege in the identity provider, and the super admin works
through a queue of vetted requests instead of interruptions. The auditor receives evidence as a
by-product of normal operation.

// DEFECT D6 (R8): the citation of Bertino et al. stands after the full stop of its sentence.
Research on access control supplies the building blocks for this vision. Role-based access
control gave organisations a shared vocabulary for who may do what @sandhu1996role, and Ferraiolo
et al. consolidated its models into a reference standard @ferraiolo2001proposed. Bertino et al.
attach enabling and disabling events to roles in their temporal model. @bertino2001trbac Joshi
et al. generalise this idea to role assignments with a limited duration @joshi2005generalized.

= Objective

I will build a platform on which any member of the chair requests time-limited access to any
resource, a maintainer decides, and the access ends on its own. I follow the object-oriented
software engineering method of Bruegge and Dutoit, and @fig:usecase shows the actors and use
cases of the platform as a use case diagram in the Unified Modeling Language. I pursue four
objectives:

// DEFECT D7 (R6): the second objective uses a double verb, here and in its subsection heading.
- Structure resources as an inheriting tree
- Design and implement connectors with expiry
- Delegate request review without privilege escalation
- Automate the recertification of grants

#diagram(
  usecase-diagram,
  caption: [
    Use case diagram of the access platform. Requesters fill in forms and request access,
    maintainers review, grant, and recertify, and the super admin maintains the resource tree and
    completes every grant that needs privileged rights. Keycloak, Slack, and GitHub take part as
    secondary actors, because a grant takes effect only once their state changes.
  ],
) <fig:usecase>

== Structure Resources as an Inheriting Tree

I will replace the flat list of hosts and channels with a resource tree whose inner nodes are
folders and whose leaves each carry a binding to one target system. Maintainership, audit policy,
and grants pass down from a folder to every resource beneath it. One approval on a project folder
therefore covers its Slack channel and its GitHub team, and it covers a resource that a
maintainer adds to the folder next month without a second decision.

Inheritance runs on every authorisation check, so it has to stay cheap. I will store the tree with
a transitive-closure table of ancestor--descendant pairs and cap the fan-out, so that an approval
on the root never reaches the whole infrastructure by accident. The objective is met when a grant
on a folder with $n$ bindable descendants yields exactly $n$ provisioning commands and a resource
added later inherits the grant without a new request.

== Design and Implement Connectors with Expiry

A Slack channel, a GitHub team, and a Keycloak group share only the idea of membership. I will
define one connector contract that validates a binding, grants and revokes access, and optionally
lists the current members, and I will implement the `keycloak-group`, `slack-channel`,
`github-team`, and `webhook` connectors on it. The super admin configures their credentials at
runtime, and @fig:components shows how the components of the platform connect.

// DEFECT D8 (R10): this figure has a two-word caption.
#diagram(
  component-diagram,
  caption: [System Architecture],
) <fig:components>

The platform changes Keycloak only with the token of the calling user, so it arms the workflow
engine of Keycloak at grant time. A deprovisioning reaper revokes the grants of the other
connectors through an outbox, and a drift check reports memberships that outlive their grants. An
automated scenario checks that SSH, Slack, and GitHub access from one approval disappears after
expiry, even when the workflow engine fails. I measure extensibility by the size of the change
that the `webhook` connector needs after I freeze the framework.

// DEFECT D9 (R16): this subsection calls the maintainer "resource owner".
== Delegate Request Review Without Privilege Escalation

A requester should not need to know which resources a project needs. I will build declarative
request forms whose fields, visibility conditions, and rules turn answers into requests for a
resource path, a level, and a duration. The server evaluates the answers itself, so a submission cannot
request a resource that the form does not offer. One onboarding submission thereby replaces several conversations.

Keycloak requires a realm-wide administrative role for its workflow API, and no resource owner may
hold that role. I will therefore split the approval: in the review step, the resource owner
records a decision without changing Keycloak, and in the grant step, the super admin sets
membership and expiry atomically, in bulk for a vetted queue. The resource owner completes a
request alone when it touches only non-privileged bindings, such as a Slack channel. I will compare
the manual administrative actions and the time to working access with the current process.

// DEFECT D10 (R3): this objective subsection has a single paragraph.
== Automate the Recertification of Grants

I will attach a recertification interval to each resource, which its descendants inherit, and
open a campaign whenever the interval elapses. The maintainer decides to keep or revoke each
grant, and the platform stores every decision as an immutable record. Ignored campaigns escalate
and may revoke grants automatically, but never in a campaign that reached nobody. The platform
logs every change with the person who made it, and the admin event log of Keycloak keeps a second
copy.

= Schedule

The thesis runs from 1 October 2026 to 31 March 2027, and I divide it into iterations of two to
four weeks. Each iteration ends with an increment that a user can try in the web interface, and
each item names the objective it serves and the outcome that I demonstrate.
Later iterations extend the platform that the earlier iterations deliver.

- *Iteration 1 (1--28 October), Objective 1:* Keycloak realm, OIDC login, SSH key
  self-service, and resource tree. Outcome: a super admin delegates a folder to a maintainer.
- *Iteration 2 (29 October -- 25 November), Objectives 2 and 3:* requests, review and grant steps,
  `keycloak-group` connector, LDAP bridge, and Ansible role. Outcome: an SSH login ends at expiry.
- *Iteration 3 (26 November -- 23 December), Objective 2:* connector framework with outbox and
  reaper, Slack and GitHub connectors. Outcome: one approval grants and revokes both.
- *Iteration 4 (24 December -- 20 January), Objective 3:* request forms with builder and
  server-side evaluation. Outcome: one submission yields requests for several connectors.
- *Iteration 5 (21 January -- 17 February), Objective 4:* recertification campaigns, drift check,
  and e-mail notifications. Outcome: an ignored campaign escalates on schedule.
- *Iteration 6 (18 February -- 17 March), Objectives 2 and 3:* `webhook` connector and deployment
  at the chair. Outcome: the correctness scenarios pass, and I measure the effort.
// DEFECT D11 (R20): a final iteration for writing the thesis and preparing the presentation.
- *Iteration 7 (18--31 March):* Write the thesis and prepare the final presentation.

// ============================================================================
// Bibliography and AI transparency
// ============================================================================

#pagebreak()
#bibliography("/references.yml")

#pagebreak()
#set page(numbering: none)
#set par(first-line-indent: 0pt)
#align(left, text(font: fonts.sans, 20pt, weight: 700, "Transparency in the Use of AI Tools"))
#v(12pt)

I used the following generative artificial intelligence (AI) tool while preparing this proposal.
The level refers to the six-level taxonomy of generative AI use in writing by Uçan and Demirel
Uçan @ucan2026generative.

#ai_usage_table(
  ai_usage(
    tool: "Anthropic Claude Code",
    model: "Claude Opus 5.5 (claude-opus-5-5)",
    dates: "18.10.2026",
    level: "5",
    purpose: "Drafting text, authoring the Typst sources and diagrams, researching references",
    scope: "Sections 1-5, Figures 1-2, bibliography entries",
  ),
)

// DEFECT D12 (R18): the sentence confirming that I checked all AI-generated text is missing.
