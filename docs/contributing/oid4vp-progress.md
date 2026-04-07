# OID4VP Progress

## Status
- [x] Create neutral steering branch
- [x] Save steering documents
- [ ] Review steering pack and confirm before product work
- [ ] Start OpenID4VP 1.0 OSS wallet implementation
- [ ] Add tests for OSS wallet v1 flow
- [ ] Update wallet UIs for v1 request handling
- [ ] Start transaction-data implementation
- [ ] Add verifier-side transaction-data validation
- [ ] Add verifier demo UI flow
- [ ] Add wallet transaction authorization UX
- [ ] Record demo outside git
- [ ] Prepare feature branches via cherry-picking
- [ ] Push branches or open PRs after confirmation

## Branch Plan
- Current docs branch: `docs/oid4vp-steering`
- Future feature branch 1: `feat/wallet-openid4vp-v1`
- Future feature branch 2: `feat/transaction-data-support`

## Commit Separation
- Docs commits stay on docs branch.
- Product commits are cherry-picked onto future feature branches.
- No unrelated local changes are to be staged.

## Validation Commands
- Wallet API tests:
  - `./gradlew :waltid-services:waltid-wallet-api:test`
- Verifier 2 tests:
  - `./gradlew :waltid-services:waltid-verifier-api2:test`
- Wallet UI app build scope:
  - `cd waltid-applications/waltid-web-wallet && pnpm install && pnpm -r build`
- Verifier portal build scope:
  - `cd waltid-applications/waltid-web-portal && pnpm install && pnpm build`

## Notes
- Current dry-run checks confirm the Gradle task graph for wallet-api and verifier-api2 resolves successfully from this checkout.
- Existing wallet test helpers are draft-oriented and will need a parallel v1 path instead of a blanket rewrite.
