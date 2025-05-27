# 🛠 Release Workflow Documentation

This document outlines the release workflow for the Thesis Management project. It is intended for all developers
and team members involved in managing or triggering releases.

## 👥 Role Groups

Two key roles exist:

- **Thesis Management Developers**
- **Thesis Management Maintainers**

> Maintainers have elevated permissions, such as the ability to merge small PRs directly and create or bump releases.

---

## 🔁 Git Branch Workflow

### Feature Branches & Pull Requests

All work (features, bug fixes, improvements) must be done in dedicated branches derived from `develop`:

- Use clear and consistent naming like `feature/xyz`, `bugfix/abc`, or `chore/some-task`.

Once a task is complete:

1. Open a Pull Request (PR) targeting `develop`.
2. The PR may contain multiple commits but must be **well-documented**.
   > ⚠️ **Pull Request Size Warning:**  
   > Keep PRs small and easy to review. As a general guideline, limit PRs to a maximum of **600 changed lines**.  
   > Large PRs are harder to review and more prone to delays or missed issues.
3. At least one Maintainer must approve the PR.
4. Merge using **Squash Merge** — this automatically triggers a **test server deployment**.

After thorough testing of the new features on the test server:

- A Maintainer should be contacted to merge the current state of `develop` into `main`.
- This must be done via **merge commit** (not squash), making `main` a mirror of `develop`, with no additional commits.

### Automated Releases

| Target                 | Branch    | Server            |
|------------------------|-----------|-------------------|
| Development Deployment | `develop` | Test Server       |
| Production Deployment  | `main`    | Production Server |

### 🌐 Deployment URLs

- **Test Server**: [https://thesis-dev.aet.cit.tum.de](https://thesis-dev.aet.cit.tum.de)
    - **Note**: This is a test server and not intended for production use.
    - **Credentials**: Use your TUM credentials to log in.
- **Production Server**: [https://thesis.aet.cit.tum.de](https://thesis.aet.cit.tum.de)
    - **Note**: This is the production server and should be used for live operations.
    - **Credentials**: Use your TUM credentials to log in.

**Automation:**

- Merges into `develop` → automatically trigger a deployment to the **Test Environment** (⚠️ this is **not** considered
  a release)
- Merges into `main` → automatically trigger a deployment to **Production**, *provided* that version numbers in Client
  and Server were updated and are identical
- Manual deployments to **Production** from `main` or `develop` remain possible

### ⚠️ Production Release Behavior

Once something is pushed to `main` and GitHub detects a new version in the application properties:

- A new GitHub Release is automatically created.
- It points to the `main` branch and uses the version inside the properties as the title (e.g. `v4.0.0`).
- The description is initially empty and **must be updated manually**.

➕ Please refer to [Creating a New Release](#-releasing-to-production) for versioning and tag details.  
➕ Look at previous PRs and releases for description style and structure.

---

## 📦 Release and Repository Rules

- Releases are always created from the `main` branch.
- `main` is updated only via merge commits from `develop`.
- Never squash or rebase onto `main`.
- Only Maintainers are allowed to perform merges/pushes into `main`.

---

## 🚀 Releasing to Production

### Prerequisites

- Ensure client and server version numbers have been incremented

### Steps

1. **Ensure `develop` is up to date and contains all relevant changes**

2. **Switch to main and pull latest changes**

```bash
git checkout main
git pull origin main
```

3. **Merge develop into main**

```bash
git merge develop --no-ff -m "Merge Release vX.Y.Z to main"
```

4. **Push changes to main**

```bash
git push origin main
```

### 🔖 Version Naming Convention

Use semantic versioning: `vX.Y.Z`

- `X` = **Major** – for breaking changes or architecture overhauls
- `Y` = **Minor** – for backward-compatible features
- `Z` = **Patch** – for bugfixes and small improvements

---

## 🛑 Important Notes

- Only Maintainers are allowed to perform production releases
- Every production deployment via `main` must be associated with a new version and GitHub release.
- Test your changes thoroughly before merging into `main`

---

For questions, please contact the Thesis Management Maintainer team.

*Updated: May 2025*