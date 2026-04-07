# OID4VP PR Notes

## Current Branch Heads
- `feat/wallet-openid4vp-v1` at `fe1af38cc`
- `feat/transaction-data-support` at `4e1b3e330`

## PR 1 Draft Notes

### Working Title
- Add OpenID4VP 1.0 support to wallet-api

### Scope
- Integrate OSS `wallet-api` with the shared v1 holder library.
- Keep draft OpenID4VP behavior intact.
- Update wallet UI only where needed for v1 request handling.
- Add tests for the new v1 OSS wallet path.
- Current branch also includes a small cleanup commit to simplify the new wallet OpenID4VP request-handling path.

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
- Attach demo recording outside git.
- Current branch also carries the task-1 cleanup commit plus a small `transaction_data` utility cleanup.

### Exclusions
- No steering docs in this PR branch.

## Review Reminders
- Keep language neutral and technical.
- Mention draft compatibility explicitly.
- Call out any temporary overlap between PR 1 and PR 2 if PR 2 is opened before PR 1 merges.

## Remaining Before Publication
- Manual smoke validation is still recommended for both branches.
- The transaction-data demo recording is still outstanding.
