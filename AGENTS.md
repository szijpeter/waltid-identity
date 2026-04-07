# Repository Working Notes

## Purpose
- This repository is being worked on as a normal upstream contribution flow.
- Keep contribution metadata neutral in naming and wording.

## Branching
- Planning and steering work stays on a docs-oriented branch.
- Product work should use focused feature branches.
- Keep planning commits and product commits separate so product branches can be created by cherry-picking only the product commits.

## Naming
- Do not use personal-process or interview-oriented wording in:
  - branch names
  - commit messages
  - pull request titles
  - pull request descriptions
- Prefer technical, contribution-style names only.

## Pull Requests
- Do not open pull requests without explicit user confirmation.
- If two related feature branches are active, expect the second PR to temporarily overlap the first until the first is merged and the second is restacked.

## Planning Artifacts
- Steering, assessment, progress tracking, and PR notes may live in `docs/contributing/`.
- These artifacts are allowed on planning branches, but they should stay out of product PR branches unless explicitly requested.

## Current OID4VP Workstream
- OSS wallet work lives primarily in:
  - `waltid-services/waltid-wallet-api`
  - `waltid-applications/waltid-web-wallet`
- Shared OpenID4VP 1.0 logic lives primarily in:
  - `waltid-libraries/protocols/waltid-openid4vp`
  - `waltid-libraries/protocols/waltid-openid4vp-wallet`
  - `waltid-libraries/protocols/waltid-openid4vp-verifier`
- Verifier demo work is most likely to touch:
  - `waltid-services/waltid-verifier-api2`
  - `waltid-applications/waltid-web-portal`

## Local Hygiene
- Local-only workstation files should be hidden using local git mechanisms when possible.
- Prefer `.git/info/exclude` for untracked local files.
- For tracked machine-local files, use local index flags carefully and document the choice in the working notes if relevant.
