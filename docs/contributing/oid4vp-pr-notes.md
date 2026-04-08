# OID4VP PR Notes

## Current Branch Heads
- `feat/wallet-openid4vp-v1` at `426feec27`
- `feat/transaction-data-support` at `53636c926`

## PR 1 Draft Notes

### Working Title
- Add OpenID4VP 1.0 support to wallet-api

### Scope
- Integrate OSS `wallet-api` with the shared v1 holder library.
- Keep draft OpenID4VP behavior intact.
- Update wallet UI only where needed for v1 request handling.
- Add tests for the new v1 OSS wallet path.
- Current branch is collapsed to a single commit for local review convenience.

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
- Current branch is restacked as a single task-2 commit on top of the final task-1 head.

### Exclusions
- No steering docs in this PR branch.

## Review Reminders
- Keep language neutral and technical.
- Mention draft compatibility explicitly.
- Call out any temporary overlap between PR 1 and PR 2 if PR 2 is opened before PR 1 merges.

## Remaining Before Publication
- Final manual demo recording is still pending and should stay outside git.
- Manual smoke validation is optional, not blocking, after the completed automated E2E runs.
