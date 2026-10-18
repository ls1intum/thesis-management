#import "/figures/diagrams.typ": usecase-diagram, component-diagram, tree-diagram

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

#set heading(numbering: none)
= Abstract

A university research chair grants access to its compute hosts, chat workspace, and code
repositories by hand, while the people who need that access change every semester. Requests
travel by chat message or e-mail, nobody records an end date, and access granted for one project
outlives the time a person spends at the chair. No single record states who holds which access
or who approved it.

In this thesis, I develop a self-service platform on which every grant carries an expiry date
from the moment somebody requests it. The platform organises all resources in one tree,
provisions them through pluggable connectors, and lets maintainers decide on requests without
administrative privileges. I evaluate the platform at the chair on correctness, administrative
effort, and extensibility.

#set heading(numbering: "1.1")

= Introduction

A university research chair, or a team of similar size, runs shared infrastructure: Linux hosts
with graphics processors that its members reach over Secure Shell (SSH), a Slack workspace, a
GitHub organisation, and internal dashboards and databases. Thesis students, external
collaborators, and working students join and leave this infrastructure every semester, on dates
that the chair rarely tracks.

Keycloak#footnote[https://www.keycloak.org] serves as the identity provider of the chair: it
handles OpenID Connect login, groups, and profile attributes such as SSH public keys, and its
Fine-Grained Admin Permissions scope administrative rights to selected groups. The Linux hosts
read users and keys from Keycloak over the Lightweight Directory Access Protocol (LDAP),
`pam_access` and `sudoers` decide who may log in or gain root rights, and an Ansible role
configures each host.

Slack and GitHub manage membership through their own application programming interfaces (APIs)
and tokens#footnote[https://api.slack.com and https://docs.github.com/en/rest], and neither
shares an administrative interface with Keycloak. People ask for access to any of these systems
by chat message or e-mail, and that message is often the only record of the change. The chair
therefore holds identities in one place but grants access in three.

= Problem

*Requesters*, such as thesis students and external collaborators, do not know who manages which
system. A student who needs a host, a channel, and a repository asks several people and waits for
each of them, and some bridge the delay with a shared login that hides who acted. Studies show
that people bypass access control once asking costs more than a workaround @bauer2009real
@beautement2008compliance.

*Maintainers*, the supervisors who know whether a request is legitimate, cannot act on it, while
the *administrators* who can act lack the context to judge it and become a bottleneck. The common
workaround hands maintainers full administrative rights, which violates the principle of least
privilege @saltzer1975protection and exposes every user and every host to a mistake made for a
single project.

*Access never ends.* Nobody records when a person stops needing access, so access piles up, and former
students keep login access to the compute hosts long after they leave, a decay that studies of
enterprises report as well @kern2002observations @schaad2001role. *Auditors* bear the
consequence: the history lies scattered across systems and chat logs, and nobody can say who had
access to a host last March and who approved it.

= Motivation

Expiring access reverses the default of an organisation: access lasts until its expiry date
unless its holder explains the need for more time. Requesters describe their project once and
receive working access on the same day, maintainers decide about their own resources without
privileges over anything else, administrators work through a queue that maintainers have already
reviewed, and auditors receive evidence as a by-product of daily operation.

Research on access control supplies the concepts for this vision. Role-based access control gives
organisations a vocabulary for who may do what @sandhu1996role, temporal extensions bind roles to
time @bertino2001trbac @joshi2005generalized, attribute-based models express conditions
@hu2015attribute, and administrative and delegation models separate the right to decide from the
right to assign @sandhu1999arbac97 @barka2000framework. A platform that applies these concepts
across systems without a shared administrative interface turns them into daily practice.

= Objective

In this thesis, I develop a self-service platform on which requesters ask for time-limited
access, maintainers decide, and access ends on its own; @fig:usecase shows its actors and use
cases as a Unified Modeling Language (UML) use case diagram. Following the object-oriented
software engineering method of Bruegge and
Dutoit#footnote[https://pearson.com/store/p/object-oriented-software-engineering-using-uml-patterns-and-java/P200000003319],
I pursue four objectives:

+ Unify heterogeneous resources in one tree.
+ Enforce an expiry date on every grant.
+ Delegate the approval of requests to maintainers.
+ Automate the recertification of active grants.

#diagram(
  usecase-diagram,
  caption: [
    UML use case diagram of the access management platform. Requesters fill in request forms,
    maintainers review requests and recertify grants, and administrators grant reviewed requests
    and maintain the resource tree; a maintainer grants a reviewed request alone when it touches
    no binding that requires administrative privileges. Keycloak, Slack, and GitHub take part as
    secondary actors on the right, because a grant takes effect only when their state changes.
  ],
  short-caption: "Use case diagram of the access management platform",
) <fig:usecase>

== Unify Heterogeneous Resources in One Tree

I model the resources as a tree whose folders pass maintainership, audit policy, and grants down
to leaves bound to target systems, as the UML object diagram in @fig:tree shows. One approval on a
folder covers every resource beneath it, including later additions. A transitive-closure table
stores the tree, and a fan-out cap keeps an approval on the root from provisioning the whole
infrastructure.

#diagram(
  tree-diagram,
  caption: [
    UML object diagram of a small resource tree at the chair. Folders such as `project-alpha`
    carry no binding of their own, while each leaf names the connector that provisions it. One
    approval on `project-alpha` covers both resources in the dashed box and would cover a third
    resource that an administrator adds to that folder later.
  ],
  short-caption: "Object diagram of a small resource tree",
) <fig:tree>

Every target system joins the platform through a connector that calls its API behind one
contract: validate a binding, grant, revoke, and optionally list members. I implement the
`keycloak-group`, `slack-channel`, `github-team`, and `webhook` connectors, whose credentials
administrators configure at runtime, and @fig:components shows how they fit into the platform. I
reach this objective when the `webhook` connector, written after I freeze the framework, needs one
new class and one registry entry.

#diagram(
  component-diagram,
  caption: [
    UML component diagram of the platform and its environment. The React web application
    `mgmt-ui` calls `mgmt-api`, which stores grants, the outbox, and the audit log in PostgreSQL
    and changes Keycloak only with the token of the calling user. The `ldap-bridge` component
    holds a separate read-only credential and projects users, groups, and SSH keys to the Linux
    hosts over LDAP, while the bot tokens for Slack and GitHub reach neither Keycloak nor the
    hosts.
  ],
  short-caption: "Component diagram of the access management platform",
) <fig:components>

== Enforce an Expiry Date on Every Grant

The platform changes Keycloak only with the token of the calling user and holds no service
account, so no background process of the platform can remove a membership. For Keycloak bindings,
the platform therefore arms the workflow engine of Keycloak through its API at grant time. For
all other bindings, a reaper without any Keycloak credential revokes expired grants through an
outbox.

A drift check lists every membership that outlives its grant. I evaluate correctness with an
automated scenario that provisions SSH, Slack, and GitHub access from one approval. I reach this
objective when each access disappears after expiry and, in a second run with the workflow engine
disabled on purpose, the drift check reports the remaining SSH membership.

== Delegate the Approval of Requests to Maintainers

Requesters fill in a declarative request form instead of naming resources, and its rules turn
answers into requests for a resource path, an access level, and a duration, which the platform
routes to the maintainer of each resource. The server evaluates the rules, so a submission cannot
request a resource that the form does not offer, and one submission covers everything that a new
thesis student needs.

Keycloak gates its workflow API on a realm-wide administrative role that maintainers must not
hold, so I split each decision into a review step, in which the maintainer records a decision
without changing Keycloak, and a grant step, in which an administrator sets membership and expiry
together. Slack-only requests need no grant step. I reach this objective when administrators
perform fewer manual actions and requesters wait less than in the current process.

== Automate the Recertification of Active Grants

Every resource carries a recertification interval that its descendants inherit, and each elapsed
interval opens a campaign in which the maintainer keeps or revokes every grant with an immutable
decision. Ignored campaigns escalate to an administrator and may revoke undefended grants
automatically, but never when a campaign reached no maintainer, because a missing recipient
signals a configuration error rather than a decision.

The platform writes every change to an audit log with the person who made it, and the admin
event log of Keycloak keeps a second copy, because interactive changes carry the token of that
person. I reach this objective when the platform answers, for any grant, who requested it and why,
who reviewed and granted it, when it ended, and who confirmed it since.

= Schedule

The thesis runs from 1 October 2026 to 31 March 2027. I divide the work into six iterations of
four weeks, each ending with a vertically integrated increment that a user can try in the web
application and that serves the objectives named in parentheses. I reserve the final two weeks,
from 18 to 31 March 2027, as buffer for features that slip from earlier iterations.

- *Iteration 1, 1–28 October (Objective 1):* I deliver the Keycloak realm with OpenID Connect
  login, SSH key self-service, and the resource tree with inherited maintainers. Outcome: an
  administrator builds a tree and delegates one folder to a maintainer.
- *Iteration 2, 29 October – 25 November (Objectives 2, 3):* I deliver requests with a duration,
  the review and grant steps, the `keycloak-group` connector, the LDAP bridge, and the Ansible
  host role. Outcome: an approved SSH session stops working at expiry.
- *Iteration 3, 26 November – 23 December (Objectives 1, 2):* I deliver the connector contract,
  outbox, dispatcher, and reaper, the Slack and GitHub connectors, and their credentials screen.
  Outcome: one folder approval provisions a Slack channel and a GitHub team until expiry.
- *Iteration 4, 24 December – 20 January (Objective 3):* I deliver the form schema, the form
  builder with live preview, and server-side evaluation. Outcome: one onboarding submission
  yields requests across three connectors.
- *Iteration 5, 21 January – 17 February (Objectives 2, 4):* I deliver recertification
  campaigns with escalation, the drift report, and e-mail notifications. Outcome: an ignored
  campaign escalates, and the drift report lists a membership that outlives its grant.
- *Iteration 6, 18 February – 17 March (Objectives 1, 2):* I deliver the `webhook` connector
  and the deployment at the chair with the correctness scenario. Outcome: the connector needs
  one new class and one registry entry, and the scenario passes without the workflow engine.
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
The level refers to the six-level taxonomy of generative AI use in student writing by Ucan and
Demirel Ucan @ucan2026generative.

#ai_usage_table(
  ai_usage(
    tool: "Anthropic Claude Code",
    model: "Claude Opus 5.5 (claude-opus-5-5)",
    dates: "18.10.2026",
    level: "5",
    purpose: "Drafting text, authoring diagrams, researching and verifying references",
    scope: "Abstract, Sections 1-5, Figures 1-3, bibliography",
  ),
)

#v(6pt)

I used Claude Code to draft the text of the abstract and of Sections 1 to 5, to author the three
diagrams in Typst, and to search for the cited publications and verify their bibliographic data.
I carefully checked all AI-generated text, revised it where necessary, verified every citation
against the original publication, and compiled and inspected every diagram. I take full
responsibility for the content of this proposal.
