# OID4VP PR Notes

## PR 1 Draft Notes

### Working Title
- Add OpenID4VP 1.0 support to wallet-api

### Scope
- Integrate OSS `wallet-api` with the shared v1 holder library.
- Keep draft OpenID4VP behavior intact.
- Update wallet UI only where needed for v1 request handling.
- Add tests for the new v1 OSS wallet path.

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

### Exclusions
- No steering docs in this PR branch.

## Review Reminders
- Keep language neutral and technical.
- Mention draft compatibility explicitly.
- Call out any temporary overlap between PR 1 and PR 2 if PR 2 is opened before PR 1 merges.
