# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| latest  | :white_check_mark: |

Only the latest version deployed on the main branch is actively supported with security updates. We recommend always running the most recent release.

## Reporting a Vulnerability

We take security issues in Thesis Management seriously. If you discover a security vulnerability, please report it responsibly.

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, report them via one of the following channels:

- **GitHub Security Advisories**: Use the [private vulnerability reporting](https://github.com/ls1intum/thesis-management/security/advisories/new) feature on GitHub
- **Email**: Send a detailed report to [krusche@tum.de](mailto:krusche@tum.de)

### What to Include

Please include as much of the following information as possible:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept
- The affected component (server, client, authentication, etc.)
- Any suggestions for mitigation or fixes

### What to Expect

- **Acknowledgment**: We will acknowledge receipt of your report within **10 business days**
- **Assessment**: We will investigate and provide an initial assessment within **15 business days**
- **Resolution**: We aim to release a fix for confirmed vulnerabilities as quickly as possible, depending on severity and complexity
- **Credit**: We are happy to credit reporters in release notes (unless you prefer to remain anonymous)

## Security Considerations

This application handles academic data and uses the following security mechanisms:

- **Authentication**: Keycloak-based OpenID Connect authentication
- **Authorization**: Role-based access control (RBAC) enforced on both server and client
- **Database**: PostgreSQL with parameterized queries via Spring Data JPA
- **Dependencies**: Regularly updated to address known vulnerabilities

## Scope

The following are considered in scope for security reports:

- Authentication and authorization bypasses
- Injection vulnerabilities (SQL, XSS, CSRF, etc.)
- Insecure direct object references
- Sensitive data exposure
- Server-side request forgery (SSRF)
- Misconfiguration in default deployment settings

The following are out of scope:

- Vulnerabilities in third-party services (e.g., Keycloak itself): please report those to the respective projects
- Issues requiring physical access to the server
- Social engineering attacks
- Denial-of-service attacks
- Issues in the local development environment that do not affect production
