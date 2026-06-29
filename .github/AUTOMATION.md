# Automation setup — operator notes

This repo runs three lanes of automation, each backed by GitHub Actions
workflows in `.github/workflows/`. This file documents the **one-time
setup** a maintainer must do in the GitHub UI for the automation to work
safely. The workflow files themselves are self-contained; this is the
"flip these switches" companion.

---

## 1. Secrets

Add these to **Settings → Secrets and variables → Actions**:

| Secret                | Used by                              | Notes |
|-----------------------|--------------------------------------|-------|
| `ANTHROPIC_API_KEY`   | dependabot-triage, idea-*, maintenance | Claude API key. Bedrock/Vertex creds work too — see the [Claude Code Action docs](https://github.com/anthropics/claude-code-action). |

`GITHUB_TOKEN` is provisioned automatically by Actions; no setup needed.

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
- `automation` — anything opened by a Claude-powered workflow
- `idea` — PRs touching `ideas/**`

## 5. CODEOWNERS

`.github/CODEOWNERS` is committed but uses `@cap-java/maintainers` as a
placeholder. Before merging this scaffolding, replace it with the real
team handle or a list of user handles. Code-owner enforcement only kicks
in once branch protection has "Require review from Code Owners" enabled
(see §2).

## 6. Workflow inventory

| Workflow                          | Trigger                              | What it does |
|-----------------------------------|--------------------------------------|--------------|
| `auto-merge-deps.yml`             | Dependabot PR opened                 | Auto-approves + enables auto-merge for patch/minor; labels majors `needs-review`. |
| `dependabot-triage.yml`           | PR labeled `major-update`            | Claude fetches changelog, posts a risk summary as PR comment. |
| `dependabot-stuck-sweep.yml`      | Weekly cron (Mon 09:17 UTC)          | Claude attempts to fix Dependabot PRs stuck >7 days. |
| `idea-pr-opened.yml`              | PR opened touching `ideas/**`        | Claude critiques the spec as PR comment. |
| `idea-implement.yml`              | Push to `main` touching `ideas/**`   | Claude implements `status: ready` ideas, opens PR. |
| `maintenance.yml`                 | Weekly cron (Mon 10:23 UTC)          | README drift, snippet compile, stale TODO, `mvn dependency:analyze`. |

## 7. Killswitch

To disable a workflow temporarily, **Settings → Actions → Workflows →
[name] → ⋯ → Disable workflow**. The workflow file stays in the repo.

To disable Claude across the board, revoke `ANTHROPIC_API_KEY`. All Claude
workflows will fail loudly on next run; nothing destructive happens.

## 8. Cost notes

- `auto-merge-deps`: free (no Claude calls).
- `dependabot-triage`: only fires on major bumps (~once a month). Cheap.
- `dependabot-stuck-sweep`: weekly, only if there are stuck PRs. Bounded
  by `--max-turns 200` across all stuck PRs combined.
- `idea-pr-opened`: bounded critique (~25 turns).
- `idea-implement`: highest-cost workflow. Bounded by `--max-turns 300`
  and a 60-minute job timeout. Diff-size cap stops runaway PRs.
- `maintenance`: weekly. Bounded by `--max-turns 100`.

If you want a hard monthly cap, set it on the Anthropic console (Usage
limits) rather than in workflows — it's the only place that enforces it
across all runs.
