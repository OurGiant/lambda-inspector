---
name: ship-issue
description: The standard workflow for shipping a bug fix or feature to Lambda Inspector — file a GitHub issue, branch off main, implement, verify, bump the patch version, and open a PR. Use whenever picking up a bug fix or feature for this repo.
---

# Shipping a change to Lambda Inspector

Follow `java-swing-ship-issue` (the generic workflow shared across the
Java Swing project family) with these Lambda Inspector specifics:

- **Project path**: `/projects/lambda-inspector` inside the build container.
- **Verify**: use this repo's own `.claude/skills/verify/SKILL.md` for
  build/launch mechanics.
- **AWS-call surfaces need extra care**: any change to `core/` that adds
  a new AWS API call (Lambda, CloudWatch Logs, STS) should note the new
  IAM permission it requires in README's IAM Permissions section (see
  dynamodb-client's for the pattern) — not just implement it silently.
- **Destructive/mutating actions get a confirmation dialog**: this
  project's scope is deliberately browse/invoke/tail, not deploy or
  delete — if a change would add a mutating Lambda action (e.g. updating
  function config), treat that as a scope question to raise, not a
  routine feature add.
- No repo-specific branch-naming or extra PR-checklist step beyond the
  generic workflow has been established here yet; follow
  `java-swing-ship-issue` as-is until one is.
