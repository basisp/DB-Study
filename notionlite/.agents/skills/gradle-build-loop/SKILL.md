---
name: gradle-build-loop
description: |
  After code changes, run ./scripts/gradle test then bootJar.
  If anything fails, fix minimally and rerun only the failing step first.
---

## Steps
1) Read repo root AGENTS.md and follow its commands strictly.
2) Run:
   - ./scripts/gradle test
   - ./scripts/gradle bootJar
3) On failure:
   - show relevant error excerpt
   - apply smallest fix
   - rerun only failing test via:
     ./scripts/gradle test --tests "<pattern>"
4) Summarize commands + results + next steps.
