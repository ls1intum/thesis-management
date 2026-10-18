// Fletcher/CetZ diagrams used in proposal.typ.

#import "@preview/fletcher:0.5.8" as fletcher: node, edge
#import "@preview/cetz:0.4.2"

#let uml-actor(name) = {
  set align(center)
  stack(
    spacing: 3pt,
    cetz.canvas(length: 1pt, {
      import cetz.draw: *
      set-style(stroke: 0.6pt)
      circle((0, 15), radius: 4.0)
      line((0, 11), (0, 2))
      line((-7, 8), (7, 8))
      line((0, 2), (-6, -8))
      line((0, 2), (6, -8))
    }),
    text(size: 8.5pt, name),
  )
}

#let uc(body) = text(size: 8pt, body)

#let usecase-diagram = fletcher.diagram(
  node-stroke: 0.6pt,
  spacing: (15mm, 6mm),
  node-inset: 4pt,
  {
    node((2, -0.9), text(size: 9pt, weight: "bold", "Access Management Platform"), stroke: none, name: <title>)
    node((2, 0), uc("Fill in a request form"), shape: fletcher.shapes.ellipse, name: <uc0>)
    node((2, 1), uc("Request access to a resource"), shape: fletcher.shapes.ellipse, name: <uc1>)
    node((2, 2), uc("Review a request"), shape: fletcher.shapes.ellipse, name: <uc2>)
    node((2, 3), uc("Recertify a grant"), shape: fletcher.shapes.ellipse, name: <uc3>)
    node((2, 4), uc("Grant a reviewed request"), shape: fletcher.shapes.ellipse, name: <uc4>)
    node((2, 5), uc("Maintain the resource tree"), shape: fletcher.shapes.ellipse, name: <uc5>)

    node(
      enclose: (<title>, <uc0>, <uc1>, <uc2>, <uc3>, <uc4>, <uc5>),
      inset: 15pt,
      stroke: 0.6pt,
      fill: none,
      layer: -1,
    )

    node((0, 0.5), uml-actor("Requester"), stroke: none, name: <requester>)
    node((0, 2.5), uml-actor("Maintainer"), stroke: none, name: <maintainer>)
    node((0, 4.6), uml-actor("Administrator"), stroke: none, name: <superadmin>)

    node((4, 3.4), uml-actor("Keycloak"), stroke: none, name: <keycloak>)
    node((4, 5.0), uml-actor("Slack, GitHub"), stroke: none, name: <saas>)

    let assoc = (from, to) => edge(from, to, stroke: 0.6pt)
    assoc(<requester>, <uc0>)
    assoc(<requester>, <uc1>)
    assoc(<maintainer>, <uc2>)
    assoc(<maintainer>, <uc3>)
    assoc(<maintainer>, <uc4>)
    assoc(<superadmin>, <uc4>)
    assoc(<superadmin>, <uc5>)
    assoc(<uc4>, <keycloak>)
    assoc(<uc4>, <saas>)

    edge(
      <uc0>,
      <uc1>,
      "-->",
      text(size: 7pt, "«include»"),
      label-side: right,
      stroke: 0.6pt,
    )
  },
)

#let cmp(name, parts: ()) = {
  set align(center)
  set text(size: 8pt)
  stack(
    spacing: 2pt,
    text(size: 7pt, "«component»"),
    strong(name),
    ..parts.map(p => text(size: 6.8pt, style: "italic", p)),
  )
}

#let ext(name, note) = {
  set align(center)
  set text(size: 8pt)
  stack(spacing: 2pt, strong(name), text(size: 6.8pt, style: "italic", note))
}

#let lbl(body) = text(size: 6.8pt, body)

#let component-diagram = fletcher.diagram(
  node-stroke: 0.6pt,
  node-inset: 6pt,
  spacing: (26mm, 15mm),
  {
    node((0, 0), cmp("mgmt-ui", parts: ("React web application",)), name: <ui>)
    node(
      (0, 1),
      cmp("mgmt-api", parts: ("GrantPlanner", "ConnectorRegistry", "DeprovisionReaper")),
      name: <api>,
    )
    node((-1.1, 1.75), ext("PostgreSQL", "grants, outbox, audit"), name: <db>)
    node((0, 2.5), ext("Slack, GitHub", "managed workspaces"), name: <saas>)

    node((1.35, 0.35), ext("Keycloak", "identity, groups, workflows"), name: <kc>)
    node((1.35, 1.6), cmp("ldap-bridge", parts: ("read-only projection",)), name: <bridge>)
    node((1.35, 2.5), ext("Linux host", "sshd, nslcd, pam_access"), name: <vm>)

    let dep = (from, to, label, ..rest) => edge(
      from,
      to,
      "-->",
      lbl(label),
      stroke: 0.6pt,
      label-sep: 4pt,
      ..rest,
    )

    dep(<ui>, <kc>, "«login»")
    dep(<ui>, <api>, "«calls with user token»")
    dep(<api>, <kc>, "«changes groups as the caller»", label-side: left, label-angle: auto)
    dep(<api>, <db>, "«stores»")
    dep(<api>, <saas>, "«calls with bot token»")
    dep(<bridge>, <kc>, "«reads users and groups»")
    dep(<vm>, <bridge>, "«directory lookups»")
  },
)

#let obj(name, stereotype: none) = {
  set align(center)
  stack(
    spacing: 2pt,
    text(size: 7.6pt, underline(name)),
    ..if stereotype == none { () } else { (text(size: 6.6pt, style: "italic", stereotype),) },
  )
}

#let tree-diagram = fletcher.diagram(
  node-stroke: 0.6pt,
  node-inset: 5pt,
  spacing: (6mm, 13mm),
  {
    node((1.5, 0), obj("chair : Resource", stereotype: "kind = folder"), name: <root>)
    node((0.5, 1), obj("compute-hosts : Resource", stereotype: "kind = folder"), name: <gpu>)
    node((2.5, 1), obj("project-alpha : Resource", stereotype: "kind = folder"), name: <alpha>)

    node((0, 2), obj("host-01 : Resource", stereotype: "connector = keycloak-group"), name: <g1>)
    node((1, 2), obj("host-02 : Resource", stereotype: "connector = keycloak-group"), name: <g2>)
    node((2, 2), obj("alpha-dev : Resource", stereotype: "connector = slack-channel"), name: <sl>)
    node((3, 2), obj("alpha-infra : Resource", stereotype: "connector = github-team"), name: <gh>)

    let child = (from, to) => edge(from, to, "-", stroke: 0.6pt)
    child(<root>, <gpu>)
    child(<root>, <alpha>)
    child(<gpu>, <g1>)
    child(<gpu>, <g2>)
    child(<alpha>, <sl>)
    child(<alpha>, <gh>)

    node(
      enclose: (<alpha>, <sl>, <gh>),
      inset: 9pt,
      stroke: (paint: gray, thickness: 0.7pt, dash: "dashed"),
      fill: none,
      layer: -1,
      name: <fanout>,
    )
    node(
      (2.5, 3.05),
      lbl(emph("one approval on project-alpha")),
      stroke: none,
      inset: 2pt,
    )
  },
)
