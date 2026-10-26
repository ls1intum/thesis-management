# Documentation Screenshot Capture

This directory contains a Playwright-driven pipeline that (re-)generates every
screenshot referenced from the documentation. Each screenshot is defined as a
_scene_ — a small object that says who to log in as, where to navigate, and
what state the page should be in when the shutter fires.

## Running

From the repo root:

```bash
./capture-screenshots.sh
```

The script starts Docker (Postgres + Keycloak), boots the Spring server on
port 8180, serves the client production bundle on 3100, and then invokes
Playwright with the config in this directory. Every scene produces a PNG in
[`documentation/static/img/screenshots/`](../../documentation/static/img/screenshots).

Handy flags:

| Flag             | Behaviour                                                                                  |
|------------------|--------------------------------------------------------------------------------------------|
| `--stop`         | Shut down the Docker services / server / client started by the script.                     |
| `--ui`           | Run Playwright in interactive UI mode — good for iterating on a single scene.              |
| `--headed`       | Run in a real browser window instead of headless.                                          |
| `--grep <regex>` | Forwarded to Playwright to filter scenes by name. Example: `--grep student-04`.            |

The script leaves services running after it finishes so re-runs are fast.

## Adding a new screenshot

1. **Add a placeholder to the guide.** Pick a stable, descriptive filename in
   the corresponding guide, e.g. `screenshots/student-17-something.png`, with a
   short caption describing what the shot must depict.

2. **Register a scene.** Add a new object to either
   [`scenes/student.ts`](scenes/student.ts) or
   [`scenes/staff.ts`](scenes/staff.ts) (or create a new file and export it
   from [`scenes/index.ts`](scenes/index.ts)):

   ```ts
   {
     filename: 'student-17-something',
     description: 'One-line summary — mirrors the caption in the guide',
     role: 'student',       // 'anonymous' | 'student' | 'supervisor' | 'examiner' | 'admin'
     run: async (page) => {
       await goto(page, '/some-route')
       // open modals, expand accordions, etc.
       await settle(page)
     },
   }
   ```

   Filenames must be unique across all scenes — the registry throws at import
   time if two scenes collide.

3. **Run the capture.** `./capture-screenshots.sh --grep student-17-something`
   produces the single new file so you can preview it before regenerating
   everything.

## Design notes

- **Scenes are data, not tests.** Each entry in `scenes/*.ts` is a plain
  object, so adding a screenshot never touches the driver or the shell script.
- **Auth is reused from e2e.** The Playwright config includes the e2e
  `auth.setup.ts` project as a dependency, so `.auth/<role>.json` is
  regenerated automatically if it's missing.
- **Roles map to storage state.** The driver in `capture.spec.ts` groups
  scenes by role and applies `test.use({ storageState })` per describe block.
  Add a new role by extending the `SceneRole` union in
  [`types.ts`](types.ts) and the `ROLES` array in `capture.spec.ts`.
- **Output is stable.** The Playwright config forces `prefers-reduced-motion`
  and a fixed 1440×900 viewport at 2× DPR so PNGs stay bit-identical across
  runs — that keeps `git diff` on the screenshots meaningful.
- **Reports.** After a run, `client/screenshots/screenshot-report/` contains
  an HTML report with every captured PNG attached. Open it with
  `pnpm exec playwright show-report screenshot-report`.

## Troubleshooting

- **A scene times out on `expectVisible(...)`.** The seed data may have moved.
  Log in manually as the same role and check whether the target element is on
  the page you thought it was; adjust the `run` function.
- **The screenshot is blank / has an empty modal.** Wait a bit longer before
  capturing (`await page.waitForTimeout(500)` after opening the modal). Modals
  render on the next animation frame; `settle(page)` handles most cases but
  can miss Mantine's mount animation on slow machines.
- **Storage state expired.** Delete `client/e2e/.auth/*.json` and re-run the
  script; the setup project will rebuild the auth files.
