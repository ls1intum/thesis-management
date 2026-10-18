#import "/figures/diagrams.typ": usecase-diagram, component-diagram, tree-diagram

// ============================================================================
// Metadata
// ============================================================================

#let title = "Self-Service Management of Time-Limited Access to Heterogeneous Resources"
#let titleGerman = "Self-Service-Verwaltung zeitlich begrenzter Zugriffe auf heterogene Ressourcen"
#let degree = "Bachelor" // "Bachelor" or "Master"
#let program = "Information Systems"
#let examiner = "Prof. Dr. Stephan Krusche"
#let supervisors = ("(Supervisor)",)
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

A research chair runs Linux hosts reached over SSH, a Slack workspace, and a GitHub
organisation, and grants access to each of them by hand. A student joining a three-month
project asks three different people, who edit three different systems. When the project ends,
nobody edits them back. Access meant to last a semester outlives the person's association with
the chair, and no single record states who holds which permission today.

This proposal describes Elapsia, a platform in which access carries an expiry date from the
moment somebody requests it. Elapsia models hosts, chat channels, and repositories as one
resource tree, so an approval on a folder covers every resource beneath it, including resources
added later. A pluggable connector provisions each resource behind a uniform grant and revoke
contract, which keeps the request, approval, expiry, and audit workflow identical across systems
that share no administrative interface.

Two constraints shape the design. Elapsia mutates the identity provider with the calling user's
own token instead of a service account, so every change is attributable to a person. Keycloak in
turn gates its expiry-timer API on a role maintainers must not hold, which splits the grant into
a judgement step that needs no privilege and a mechanical step that does.

I will implement the resource tree, four connectors, expiry for every resource type, delegated
two-step approval, declarative request forms, and periodic recertification, and evaluate the
result on a deployment that provisions SSH, Slack, and GitHub access.

#set heading(numbering: "1.1")

= Introduction

A university research chair, or an engineering team of comparable size, operates a small
estate of shared infrastructure: Linux hosts with GPUs reached over SSH, a Slack workspace for
day-to-day coordination, a GitHub organisation holding the code, and a handful of internal
dashboards and databases. Membership of this estate turns over every semester. Students arrive
for a thesis, external collaborators join for the length of a funded project, and working
student contracts start and end on dates nobody records outside the payroll system.

Identity is already centralised. Keycloak acts as the identity provider: it authenticates
users through OpenID Connect, holds the group structure, and stores the profile attributes
that downstream systems read, including each user's SSH public keys. Administrative
permissions inside Keycloak can be scoped with Fine-Grained Admin Permissions, so a given
administrator may be authorised over one group and not another.

The Linux hosts consume that directory rather than keeping local accounts. The name service
daemon `nslcd` resolves users and groups over LDAP, `sshd` fetches a user's public key through
`AuthorizedKeysCommand`, and `pam_access` together with `sudoers` decides which group may open
a session and which may escalate to root. An Ansible role configures this once per host, so
adding a host to the estate means running a playbook, and granting a person access to that
host means adding them to a group.

The systems that are not Linux hosts share no such common denominator. Slack administers
channel membership through its own API and a bot token; GitHub administers team membership
through a separate API and an organisation token. Each exposes its own notion of membership,
its own administrative roles, and its own audit log. Requests to join any of them travel by
chat message or e-mail, and that exchange is the only record that the change ever happened.

= Problem

Four groups of people work around the arrangement described above, and each pays a different
cost.

*Requesters* — students, doctoral researchers, and external collaborators — do not know who
administers what. Somebody who needs a GPU host, a Slack channel, and a repository for one
project asks in three places and waits on three people; while waiting, they borrow a colleague's
session or work under a shared account. Bauer et al. observed this in organisations with far
more mature tooling: people route around access control once the cost of asking exceeds the cost
of a workaround @bauer2009real. Beautement et al. describe the same effect as a finite compliance
budget that security friction spends down @beautement2008compliance.

*Maintainers* hold the context but not the authority. The supervisor who knows whether a request
is legitimate cannot act on it, and the central administrator who can act lacks the context to
judge it. The practical compromise hands the supervisor a fully privileged administrative role,
which confers authority over every user and every host rather than the ones they maintain. That
contradicts least privilege, which has underpinned protection design since Saltzer and Schroeder
stated it @saltzer1975protection, and it is the delegation problem administrative RBAC models
were formulated to address @sandhu1999arbac97 @barka2000framework.

*Grants never end.* No step in the chain carries a date, so revocation is a separate act somebody
has to remember, and access accumulates instead. Schaad et al. found in a European bank that
reviewing who held what became the dominant administrative cost once assignments outlived their
purpose @schaad2001role, and Kern et al. describe the same life-cycle decay @kern2002observations.
At chair scale the shape is identical: former students keep SSH access to GPU hosts months after
they graduate.

*Auditors* cannot reconstruct a decision. Each system holds a fragment of the history and the
justification lives in a chat log that has since been pruned, so the question "who could log in
to `gpu-01` in March, and who approved it?" has no answer the chair can produce on demand.

= Motivation

Access that expires on its own changes the question an organisation has to answer. Instead of
asking periodically who still needs a permission — which nobody can answer without
reconstructing intent from memory — it asks only who is willing to say they need more time. The
burden moves from the administrator who must justify taking access away to the person who wants
to keep it, and the estate drifts towards fewer standing privileges rather than more.

The scientific ingredients exist. Role-based access control gave organisations a vocabulary for
who may do what @sandhu1996role @ferraiolo2001proposed, and temporal extensions have been in the
literature for two decades: TRBAC attaches enabling and disabling events to roles
@bertino2001trbac, and GTRBAC generalises this to duration-constrained assignments
@joshi2005generalized. Zero-trust guidance now recommends granting privilege per session and
re-evaluating it rather than holding it @rose2020zero, and attribute-based models supply the
vocabulary for the conditions under which it should be granted @hu2014guide. What these models
leave open is operational: who performs the revocation, with which credential, in an estate whose
identity provider, chat system, and code host share no administrative interface. A system that
answers this concretely contributes a design the models themselves do not supply.

Each actor gains something specific. The requester describes a situation once, in terms of the
project rather than the infrastructure, and receives access in minutes. The maintainer keeps the
judgement call without acquiring privilege, because reviewing a request touches no state in the
identity provider at all — the separation administrative RBAC has argued for since ARBAC97
@sandhu1999arbac97. The central administrator works a queue of already vouched-for requests
instead of fielding interruptions. The auditor receives evidence as a by-product of normal
operation.

For a chair, the end state is an estate where a departing student loses access without anyone
filing a ticket, and where the answer to "who can reach this host today?" is a page rather than
an investigation.

= Objective

I will design, implement, and evaluate Elapsia, a platform on which any member of an
organisation requests time-limited access to any resource, a maintainer decides, and the access
ends on its own. @fig:usecase shows the actors and what the system offers each of them. My
goals are to:

+ Structure the estate as a resource tree with inheriting grants.
+ Provision every target system through a pluggable connector behind one contract.
+ Give every grant an expiry that fires without a privileged background actor.
+ Delegate the approval decision without escalating anyone's privilege.
+ Reduce a request to a form phrased in the requester's own terms.
+ Produce audit evidence continuously rather than on demand.

#diagram(
  usecase-diagram,
  caption: [
    Use case diagram of Elapsia. A maintainer may grant a reviewed request whenever it touches
    no system that demands a privileged caller; otherwise a super admin completes it. Keycloak,
    Slack, and GitHub participate as secondary actors, because a grant is only real once their
    state changes.
  ],
  short-caption: "Use case diagram of Elapsia",
) <fig:usecase>

== Structure the Estate as a Resource Tree with Inheriting Grants

I will replace the flat list of hosts with a tree whose inner nodes are folders and whose leaves
carry bindings to concrete target systems, as @fig:tree illustrates. Maintainership, audit
policy, and grants all inherit downwards, so one approval on `project-alpha` covers the Slack
channel and the GitHub team beneath it, and covers a resource added to that folder next month
without anyone revisiting the decision.

Inheritance has to be cheap, because the fan-out of a grant and every authorisation check run on
it constantly. I will maintain the transitive closure of the tree as an explicit table of
ancestor--descendant pairs, and cap the fan-out so that an approval on the root cannot silently
provision the whole estate. The goal is met when a grant on a folder with $n$ bindable
descendants produces exactly $n$ provisioning commands, and a resource added afterwards is
covered without a second approval.

#diagram(
  tree-diagram,
  caption: [
    Object diagram of a small estate. Folders carry no binding of their own; leaves name the
    connector that provisions them. One approval on `project-alpha` fans out to both resources
    beneath it, and would reach a third added to that folder later.
  ],
  short-caption: "Object diagram of a small resource tree",
) <fig:tree>

== Provision Every Target System Through a Pluggable Connector

A Slack channel, a GitHub team, and a Keycloak group have nothing in common except that somebody
can be a member. I will define a connector interface that captures exactly that much: validate a
binding when it is created, grant a subject at a level, revoke a subject, and, where the target
permits it, list the members it actually holds. Everything above that interface — requesting,
reviewing, granting, expiring, recertifying — stays unaware of which system it talks to.

I will implement four connectors: `keycloak-group` for SSH hosts, `slack-channel`,
`github-team`, and a generic `webhook`. @fig:components shows the resulting structure.
Credentials for external systems are configured through the platform rather than baked into a
deployment, so rotating a token needs no redeploy. I will measure this goal by the cost of the
fourth connector: one class and one registry entry, with no edit to the request, approval, or
expiry code.

#diagram(
  component-diagram,
  caption: [
    Component diagram of the deployment. The API mutates Keycloak with the calling user's own
    token; the LDAP bridge holds a separate read-only credential and projects users, groups, and
    SSH keys to the hosts. The credentials that reach Slack and GitHub cannot reach Keycloak,
    LDAP, or SSH.
  ],
  short-caption: "Component diagram of the Elapsia deployment",
) <fig:components>

== Give Every Grant an Expiry That Fires Without a Privileged Background Actor

Elapsia holds no service account that may mutate the identity provider, so it has no background
actor that could remove a group membership on a timer. For resources bound to Keycloak I will
therefore arm Keycloak's own workflow engine at grant time, with the approving user's token and
the requested duration as the delay; Keycloak removes the membership itself, on its own
schedule. For every other connector, a scheduled reaper revokes expired grants through the same
outbox a live request uses — a component that holds no Keycloak credential of any kind.

I will treat disagreement between the two mechanisms as a first-class case: the reaper marks a
Keycloak-bound grant expired on trust, never confirming or repairing it, and a drift check
surfaces memberships that outlived their grant. The goal is met when an automated run shows an
SSH session that works after approval, stops resolving after expiry, and produces a drift entry
when the removal is suppressed on purpose.

== Delegate the Approval Decision Without Escalating Privilege

Keycloak can delegate the right to add a member to one specific group, but it gates its workflow
API on a realm-wide administrative role no maintainer should hold. A maintainer's token can
therefore create a membership but cannot arm the timer that removes it, and access that never
expires is the outcome this system exists to prevent. I will split the decision along that line:
a review step that records the maintainer's judgement and touches no state in Keycloak, and a
grant step that performs the membership and arms the expiry together or not at all.

The split applies only where it must. When a request's fan-out touches no binding that needs a
privileged caller, nothing in the plan can outlive its expiry, so the resource's own maintainer
completes it directly, and bulk granting lets the privileged administrator clear a queue somebody
else has vouched for. The goal is met when a maintainer's token completes a Slack-only request end
to end and is refused on a host request, with the refusal coming from Keycloak rather than from
Elapsia.

== Reduce a Request to a Form Phrased in the Requester's Own Terms

A requester should not need to know that "onboard a master's student on project alpha" means
three resources at two privilege levels. I will build a declarative form schema in which an
administrator defines fields, visibility conditions between them, and rules that turn answers
into concrete requests for resource paths, levels, and durations.

The published schema is the authorisation boundary: a submission carries answers only, never a
list of requests, and the server evaluates the rules, so a hand-crafted submission cannot smuggle
in a resource the form never offered. The goal is met when one submission produces the set of
grants that currently takes several separate conversations.

== Produce Audit Evidence Continuously Rather Than on Demand

I will attach a recertification interval to a resource, inherited by its descendants, and open a
campaign when that interval elapses. The maintainer decides *keep* or *revoke* per grant, the
decisions are timestamped and immutable, and ignored campaigns escalate — optionally revoking
what nobody defended, but never for a campaign that reached nobody in the first place.

Every state change enters an audit log alongside the acting identity, and because interactive
changes carry the caller's own token, the identity provider's admin event log holds an
independent second copy. The goal is met when the system answers, for an arbitrary grant, who
requested it and why, who reviewed it, who granted it, when it ended, and who re-attested it in
between.

== Evaluation

I will evaluate along three axes, following the object-oriented analysis and design method of
Bruegge and Dutoit throughout @bruegge2004object. *Correctness*: an automated scenario provisions
SSH, Slack, and GitHub access from one approval and asserts each is gone after expiry, including
under induced failure of the workflow engine. *Effort*: the manual administrative actions and the
elapsed time from request to working access, against the current chat-and-e-mail process.
*Extensibility*: the change set a target system not anticipated during design requires, measured
on a connector written after the framework is frozen.

= Schedule

The thesis starts on 1 October 2026 and ends on 31 March 2027. I divide the implementation into
six iterations of four weeks, each ending with a vertically integrated increment a user can
exercise through the web interface. The remaining two weeks are held as buffer.

*Iteration 1 (1--28 October): identity foundation and resource tree.* Set up the Keycloak realm
with fine-grained administrative permissions and OIDC login for the single-page application, and
implement self-service management of SSH public keys. Implement creation, movement, and deletion
of resources with the transitive closure kept current, and maintainer assignment that descendants
inherit. Demonstrable outcome: a super admin builds a tree and delegates one subtree to a
maintainer.

*Iteration 2 (29 October -- 25 November): request, approval, and SSH access.* Implement access
requests with a duration, the two-step review-and-grant flow, and the `keycloak-group` connector
including the arming of the native expiry workflow. Implement the LDAP bridge that projects
users, groups, and SSH keys to the hosts, and the Ansible role that configures a host to consume
it. Demonstrable outcome: an approved request yields a working SSH session that stops working
when the grant expires.

*Iteration 3 (26 November -- 23 December): connector framework and external targets.* Extract the
connector contract, the provisioning outbox, the dispatcher, and the deprovisioning reaper, then
implement the `slack-channel` and `github-team` connectors and the screen holding their
credentials. Demonstrable outcome: one approval on a folder provisions a Slack channel and a
GitHub team and revokes both on expiry.

*Iteration 4 (24 December -- 20 January): self-service request forms.* Implement the form
schema, the builder with live preview of the emitted requests, server-side evaluation of
answers, and submission tracking. Demonstrable outcome: one onboarding submission produces
several requests across different connectors.

*Iteration 5 (21 January -- 17 February): recertification and evidence.* Implement inherited
audit policies, scheduled campaigns with escalation and optional auto-revocation, per-grant
attestation, the drift report, and e-mail notification of pending work. Demonstrable outcome:
a campaign opens on schedule, escalates when ignored, and records immutable decisions.

*Iteration 6 (18 February -- 17 March): evaluation.* Deploy on the chair's estate, run the
correctness scenarios including induced failure of the workflow engine, measure the effort and
latency of a request against the current process, and implement the `webhook` connector against
the frozen framework. Demonstrable outcome: measurement results for all three evaluation axes.

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

I used the following generative AI tools while preparing this proposal. The levels refer to the
six-level taxonomy of generative AI use in writing by Ucan and Demirel Ucan @ucan2026generative.

#ai_usage_table(
  ai_usage(
    tool: "Anthropic Claude Code",
    model: "Claude Opus 5 (claude-opus-5)",
    dates: "27.09.2026",
    level: "4",
    purpose: "Drafting from the existing code base and documentation, Typst and diagram authoring",
    scope: "Abstract, Sections 1-6, Figures 1-3, bibliography entries",
  ),
)

#v(6pt)

I reviewed and revised all AI-generated or AI-modified content. I verified the factual claims
about the system against the implementation and its documentation, and I checked every citation
and its bibliographic metadata against the original publication. I inspected and compiled the
generated Typst sources and diagrams. I remain fully responsible for the submitted work.
