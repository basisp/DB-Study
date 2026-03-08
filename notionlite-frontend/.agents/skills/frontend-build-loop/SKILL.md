---
name: frontend-build-loop
description: |
  After frontend code changes, run `npm run build`.
  If it fails, fix minimally and re-run until it passes.
---

## Steps
1) Read repo root AGENTS.md and follow its Standard commands.
2) Run: npm run build
3) If it fails:
   - show the key error excerpt
   - apply the smallest fix
   - re-run npm run build
4) Summarize: files changed, commands executed, results, remaining risks
