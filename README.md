> [!NOTE]
> Please contact us if you want to get onboarded: **[thesis-management-support.aet@xcit.tum.de](thesis-management-support.aet@xcit.tum.de)**.

# Thesis Management

Thesis Management is a web-based thesis management system designed to streamline the thesis process in academic institutions by integrating essential stages into a single platform.
Developed to address challenges in managing large volumes of theses, it facilitates seamless interactions between students, advisors, and supervisors.
Key features include a centralized application process, guided workflows for thesis writing, automated notifications, and a comprehensive Gantt chart for tracking progress.
By consolidating communication, feedback, and file management, ThesisManagement enhances transparency, reduces administrative burdens, and fosters efficient thesis supervision and assessment.

Thesis Management was developed as part of multiple bachelor's and master's theses, e.g. [Development of a Thesis Management System](documentation/static/files/ba-thesis-fabian-emilius.pdf).

## Documentation

The full documentation lives at **[ls1intum.github.io/thesis-management](https://ls1intum.github.io/thesis-management/)**. It contains role-based guides with embedded video walk-throughs, permission matrices, workflow diagrams, and everything an operator or developer needs to run the platform.

- **[Students](https://ls1intum.github.io/thesis-management/students/)** — apply for a topic, book interview slots, write your thesis, and receive your grade.
- **[Supervisors & Examiners](https://ls1intum.github.io/thesis-management/supervisors/)** — create topics, review applications, run interviews, supervise theses, and issue grades. Includes the full [Permissions](https://ls1intum.github.io/thesis-management/supervisors/permissions) matrix and the [Workflows](https://ls1intum.github.io/thesis-management/supervisors/workflows) flowcharts.
- **[Admins](https://ls1intum.github.io/thesis-management/admin/)** — configuration, production setup, and data retention.
- **[Developers](https://ls1intum.github.io/thesis-management/developer/)** — local development, database migrations, mail templates, release workflow, and the API reference.

> **Just want a demo?** Run `docker compose -f docker-compose.showcase.yml up -d` and open <http://localhost:3100>. See the [Development Setup](https://ls1intum.github.io/thesis-management/developer/development-setup) guide for details.

## Building the documentation locally

The Docusaurus site lives under [`documentation/`](documentation/). See [`documentation/README.md`](documentation/README.md) for local build instructions.

> [!NOTE]
> **Couldn't find what you were looking for?**
> If you need any further help or want to be onboarded to the system, reach out to us at **[thesis-management-support.aet@xcit.tum.de](thesis-management-support.aet@xcit.tum.de)**.
