# Release lifecycle

How a change travels from a PR to a tagged release, and how a fix reaches a
release that is already out. Everything here is automated by workflows in
`.github/workflows/`; the only manual steps are merging PRs and starting two
`workflow_dispatch` jobs.

## Day to day: `microservices` and release-please

1. A PR into `microservices` has a Conventional Commit title (`feat(activity): ...`,
   `fix(gateway): ...`, `ci: ...`). The **PR policy** check
   (`pr-policy.yml`) rejects any other title, a branch outside the naming
   rules in `AGENTS.md`, and a body without a filled `## Verification`
   section. It comments what is wrong and re-runs when the PR is edited.
2. Squash merge with the title as the commit subject.
3. **release-please** (`release-please.yml`) keeps a release PR open on
   `microservices`: it bumps the version in `.release-please-manifest.json`, rewrites
   `CHANGELOG.md` from the merged titles (`feat` → Added, `fix` → Fixed,
   `perf`/`refactor` → Changed; `ci`, `build`, `chore`, `test` are hidden) and
   waits.
4. Merging the release PR creates the tag `vX.Y.Z` and the GitHub release.
   Images are tagged by commit SHA in `ci.yml`, so a release is a pointer to
   an image that already passed CI.

## Maintaining a released line: `release/ms-vX.Y`

Needed when a version in use must receive a fix but `microservices` has moved
on with changes that cannot ship yet. All twelve services share one version,
so a release branch is cut for the whole reactor.

1. Run **Cut release branch** (`cut-release-branch.yml`) with `version =
   X.Y`. It creates `release/ms-vX.Y` from the newest `vX.Y.*` tag (or from
   `microservices` when no tag exists yet) and refuses to overwrite an existing branch.
2. release-please also listens on `release/ms-**`, with `target-branch` set to
   the branch that was pushed, so each maintenance branch gets its own release
   PR and its own `vX.Y.Z` patch tags. The manifest and changelog on that
   branch are the source of truth for that line.

## Hotfixes: fix on `microservices`, backport by cherry-pick

The fix lands on `microservices` first, with tests, so the next minor
release carries it too. If the same behaviour exists on `main`, the fix is
ported there too (branch parity, see `AGENTS.md`). Then:

1. Run **Cherry-pick a merged PR** (`cherry-pick.yml`) with the merged PR
   number and the target branch (`release/ms-vX.Y`).
2. The workflow verifies the PR is merged, cherry-picks its squash commit
   with `-x` onto `hotfix/<pr>-to-release-ms-vX.Y` and opens a PR into the
   release branch whose body links the original.
3. If the pick conflicts it aborts, lists the conflicting files in the job
   summary and prints the exact local commands to finish it by hand.
4. PRs into `release/*` are accepted by the policy check only from
   `hotfix/*` or `revert-*` branches; new features never enter a release
   branch directly.
5. Merge the hotfix PR; release-please on that branch proposes `vX.Y.(Z+1)`.

A hotfix PR opened with the default `GITHUB_TOKEN` does not start other
workflows (GitHub's loop protection). Either set the `RELEASE_BOT_TOKEN`
secret (a fine-grained token with *contents* and *pull requests* write) so
the workflow uses it, or close and reopen the PR to trigger CI.

## Rolling back

- A bad release on `microservices`: open a revert PR (`revert-<pr>-...` branch, title
  `revert: ...`); the policy check allows the branch and release-please cuts
  the next patch.
- A bad hotfix on a release branch: the same revert flow, targeting the
  release branch.
- Deployments pin images by commit SHA, so rolling a service back is a
  matter of pointing at the previous SHA; no tag has to move.

## Why this shape

- One source of truth for versions and changelog (Conventional Commit
  titles), so a release note is never written twice.
- Fix-forward on `microservices` plus cherry-pick backports keeps release branches
  short-lived and free of drift; the alternative, merging `microservices`
  into a release branch, carries unreleased work with it.
- Release branch names carry the `ms-` prefix because `main` (the monolith)
  maintains its own lines in the same repository.
- The policy check runs on edits, not only on push, so fixing a title does
  not need a new commit.
