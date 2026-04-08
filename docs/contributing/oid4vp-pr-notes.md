# OID4VP PR Notes

## Current Branch Heads
- `feat/wallet-openid4vp-v1` at `b9611fda2`
- `feat/transaction-data-support` at `b1388dc15`

## Current Fork PRs
- PR 1: [Add OpenID4VP 1.0 support to wallet-api](https://github.com/szijpeter/waltid-identity/pull/3)
  - branch: `feat/wallet-openid4vp-v1`
  - base: `main`
- PR 2: [Add transaction data support to verifier and wallet flows](https://github.com/szijpeter/waltid-identity/pull/4)
  - branch: `feat/transaction-data-support`
  - base: `feat/wallet-openid4vp-v1`

## PR 1 Draft Notes

### Working Title
- Add OpenID4VP 1.0 support to wallet-api

### Scope
- Integrate OSS `wallet-api` with the shared v1 holder library.
- Keep draft OpenID4VP behavior intact.
- Update wallet UI only where needed for v1 request handling.
- Add tests for the new v1 OSS wallet path.
- Current branch contains the feature commit plus targeted review-driven hardening commits.

### Exclusions
- No transaction-data support in this PR.
- No steering docs in this PR branch.

## PR 2 Draft Notes

### Working Title
- Add transaction data support to verifier and wallet flows

### Scope
- Complete shared v1 `transaction_data` support.
- Validate transaction-data binding in verifier2.
- Add minimal verifier demo flow and wallet authorization UX.
- Support both `dc+sd-jwt` and `mso_mdoc` transaction-data presentation paths.
- Align verifier validation with the policy-based direction described in issue `#1583`.
- Current branch is cleanly restacked on top of the current task-1 head.
- Current branch contains the feature commit plus targeted review-driven hardening commits.

### Exclusions
- No steering docs in this PR branch.

## Review Reminders
- Keep language neutral and technical.
- Mention draft compatibility explicitly.
- Call out that PR 2 is intentionally stacked on PR 1.
- Mention that the manual recording is still pending and is not part of git history.

## Remaining Before Publication
- Final manual demo recording is still pending and should stay outside git.
- Final manual review pass is still pending.
- Upstream PRs to `walt-id/waltid-identity` have not been opened yet.
