# Ideas

This directory is the entry point for requirements and feature ideas in
this repo. Drop a markdown file describing what you want, open a PR, and
the automation will help shape and (once ready) implement it.

## Convention

One file per idea. Filename is a kebab-case slug: `support-cross-subaccount-queues.md`.

Each file starts with frontmatter:

```yaml
---
status: draft       # draft | ready | in-progress | done
priority: med       # low | med | high
created: 2026-06-29 # ISO date — manual, since CI can't read git dates reliably
---
```

Followed by free-form markdown. The structure isn't enforced — it's a spec,
not a form — but the critique bot will nudge you toward including:

- **Problem.** What's broken or missing today.
- **Proposed behavior.** What the user-visible change looks like.
- **API sketch.** Method signatures, config keys, or YAML changes — even
  a rough draft. Reduces ambiguity for the implementer.
- **Acceptance criteria.** Bullet list of testable conditions.
- **Out of scope.** What you're explicitly *not* asking for.

## Lifecycle

```
draft → (you iterate the spec, critique bot comments)
  ↓
ready → (you merge the idea PR to main)
  ↓
in-progress → (idea-implement.yml runs, opens a code PR)
  ↓
done → (you merge the code PR; flip the status manually or let the
        implementation PR do it)
```

### Status meanings

- **`draft`** — spec is being shaped. The critique bot will comment on
  PRs touching this file. `idea-implement.yml` ignores drafts.
- **`ready`** — spec is approved. Once merged to main, `idea-implement.yml`
  will pick it up and try to implement it.
- **`in-progress`** — implementation has started or there's an open code
  PR. The implementer sets this automatically.
- **`done`** — implemented and merged. Kept for history.

## Who can submit ideas

Anyone can open a PR, but per `.github/CODEOWNERS` ideas need code-owner
review to merge. Fork PRs are inert until a maintainer approves them via
GitHub's "Approve and run" gate.

## What gets implemented automatically

`idea-implement.yml` only runs on `push` to `main` for `ideas/**` files
with `status: ready`. That means:

1. A human (you) merges the idea PR.
2. The implementation job picks it up and opens a follow-up code PR.
3. The code PR goes through the normal review process — it is **not**
   auto-merged.

The implementation agent runs with a diff-size cap (~1500 LOC). If your
idea is bigger than that, split it before marking `ready`.

## Examples

See `ideas/_example.md` for a worked example.
