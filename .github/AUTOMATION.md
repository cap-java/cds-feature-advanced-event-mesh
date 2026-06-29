# Automation setup — operator notes

This repo runs three lanes of automation, each backed by GitHub Actions
workflows in `.github/workflows/`. This file documents the **one-time
setup** a maintainer must do in the GitHub UI for the automation to work
safely. The workflow files themselves are self-contained; this is the
"flip these switches" companion.

The LLM-backed steps use [**opencode**](https://github.com/anomalyco/opencode)
(headless CLI mode) routed through **SAP AI Core** via opencode's
built-in `sap-ai-core` provider. No proxy process needed — the provider
does XSUAA OAuth2 client-credentials exchange internally. Pattern lifted
from [cap/sherlock](https://github.tools.sap/cap/sherlock).

---

## 1. Secrets

Add these to **Settings → Secrets and variables → Actions**:

| Secret                | Used by                              | Notes |
|-----------------------|--------------------------------------|-------|
| `AICORE_SERVICE_KEY`  | dependabot-triage, idea-*, maintenance | The SAP AI Core service key JSON, verbatim. The opencode `sap-ai-core` provider parses this and handles token exchange. |

`GITHUB_TOKEN` is provisioned automatically by Actions; no setup needed.

### Model selection

The default model is `sap-ai-core/anthropic--claude-4.6-opus`, configured
in [`.github/bot-config/opencode.json`](./bot-config/opencode.json). Swap
to `claude-4.6-sonnet`, `gpt-5`, or `gemini-2.5-pro` by editing that file
— no workflow changes needed. Which models are actually available depends
on what's deployed in your AI Core tenant.

## 2. Branch protection on `main`

**Settings → Branches → Branch protection rules → `main`:**

- ✅ Require a pull request before merging
  - Require approvals: **1**
  - Dismiss stale approvals on new commits: ✅
  - Require review from Code Owners: ✅ ← this is what enforces `CODEOWNERS`
- ✅ Require status checks to pass before merging
  - Required checks: `Pull Request Voter / build` (the matrix job), plus
    `Blackduck Scan` when it's stable for Dependabot PRs
- ✅ Require conversation resolution before merging
- ✅ Allow auto-merge ← required for `auto-merge-deps.yml` to work
- ❌ Do NOT include administrators (optional, your call)

## 3. Actions permissions

**Settings → Actions → General:**

- **Fork pull request workflows from outside collaborators:**
  "**Require approval for all outside collaborators**" — gates fork PRs
  from triggering any workflow until a maintainer clicks "Approve and run".
- **Workflow permissions:** "Read repository contents and packages
  permissions". Individual workflows opt into write permissions in their
  yaml.
- **Allow GitHub Actions to create and approve pull requests:** ✅
  (required for `idea-implement.yml` to open PRs).

## 4. Labels

Create these labels (Issues → Labels → New label) so the workflows can apply them:

- `needs-review` — Dependabot major bumps; `idea-implement` failures
- `major-update` — Dependabot major bumps (triggers `dependabot-triage`)
- `automation` — anything opened by an LLM-powered workflow
- `idea` — PRs touching `ideas/**`
- `maintenance` — PRs from the weekly sweep
- `upstream-sync` — issues filed by the fork's drift workflow

## 5. CODEOWNERS

`.github/CODEOWNERS` assigns ownership of `/ideas/` and automation files
to `@cap-java/cds-feature-advanced-event-mesh-team`. Code-owner enforcement
only kicks in once branch protection has "Require review from Code Owners"
enabled (see §2), and the team must have write access to the repo.

## 6. Workflow inventory

| Workflow                          | Trigger                              | What it does |
|-----------------------------------|--------------------------------------|--------------|
| `auto-merge-deps.yml`             | Dependabot PR opened                 | Auto-approves + enables auto-merge for patch/minor; labels majors `needs-review`. |
| `dependabot-triage.yml`           | PR labeled `major-update`            | opencode fetches changelog, posts a risk summary as PR comment. |
| `dependabot-stuck-sweep.yml`      | Weekly cron (Mon 09:17 UTC)          | opencode attempts to fix Dependabot PRs stuck >7 days. |
| `idea-pr-opened.yml`              | PR opened touching `ideas/**`        | opencode critiques the spec as PR comment. |
| `idea-implement.yml`              | Push to `main` touching `ideas/**`   | opencode implements `status: ready` ideas, opens PR. |
| `maintenance.yml`                 | Weekly cron (Mon 10:23 UTC)          | README drift, snippet compile, stale TODO, `mvn dependency:analyze`. |
| `upstream-sync.yml`               | Daily cron (06:43 UTC), forks only   | Rebases fork `main` onto upstream `main`; files an issue on conflict. No-op on the upstream repo. |

The `setup-opencode` composite action in `.github/actions/setup-opencode/`
installs the opencode CLI with a pinned version + SHA256 check. Bump the
`version` / `sha256` defaults in that action's `action.yml` when you want
to upgrade — verify the SHA against the official release before changing.

## 7. Killswitch

To disable a workflow temporarily, **Settings → Actions → Workflows →
[name] → ⋯ → Disable workflow**. The workflow file stays in the repo.

To disable LLM access across the board, revoke `AICORE_SERVICE_KEY` or
delete the AI Core deployment. All LLM-backed workflows will fail loudly
on the next run; nothing destructive happens.

## 8. Cost notes

- `auto-merge-deps`: free (no LLM calls).
- `dependabot-triage`: only fires on major bumps (~once a month). Cheap.
- `dependabot-stuck-sweep`: weekly, only if there are stuck PRs.
- `idea-pr-opened`: bounded by job wall-clock; one critique per spec.
- `idea-implement`: highest-cost workflow. 60-minute job timeout, plus
  the diff-size cap (`DIFF_LOC_CAP=1500`) which the agent self-enforces.
- `maintenance`: weekly. Capped at 4 new PRs per run by the prompt.

For a hard monthly cap, set quotas on the AI Core deployment side — it's
the only enforcement point that survives a runaway workflow.
