# OID4VP Progress

## Status
- [x] Create neutral steering branch
- [x] Save steering documents
- [x] Review steering pack and confirm before product work
- [x] Start OpenID4VP 1.0 OSS wallet implementation
- [x] Add tests for OSS wallet v1 flow
- [x] Update wallet UIs for v1 request handling
- [x] Start transaction-data implementation
- [x] Add verifier-side transaction-data validation
- [x] Add verifier demo UI flow
- [x] Add wallet transaction authorization UX
- [ ] Record demo outside git
- [x] Prepare feature branches via cherry-picking
- [x] Push feature branches to fork
- [x] Open fork-local draft PRs for review
- [x] Restack PR 2 cleanly on top of PR 1
- [ ] Open upstream PRs after confirmation

## Branch Plan
- Current docs branch: `docs/oid4vp-steering`
- Feature branch 1: `feat/wallet-openid4vp-v1`
- Feature branch 2: `feat/transaction-data-support`

## Branch Heads
- `feat/wallet-openid4vp-v1` at `b9611fda2`
- `feat/transaction-data-support` at `b1388dc15`

## Feature Branch Status
- `feat/wallet-openid4vp-v1`
  - pushed to fork and under review
  - fork PR: [https://github.com/szijpeter/waltid-identity/pull/3](https://github.com/szijpeter/waltid-identity/pull/3)
  - current commits on top of `origin/main`:
    - `426feec27` `feat: add OpenID4VP wallet request handling`
    - `b495467b0` `fix: harden OpenID4VP wallet request handling`
    - `2025654f9` `fix: tighten OpenID4VP wallet request handling`
    - `b9611fda2` `fix: harden OpenID4VP request parsing and submission`
- `feat/transaction-data-support`
  - pushed to fork and under review
  - fork PR: [https://github.com/szijpeter/waltid-identity/pull/4](https://github.com/szijpeter/waltid-identity/pull/4)
  - PR base is `feat/wallet-openid4vp-v1`
  - current task-2-only commits on top of task 1:
    - `e39328932` `feat: add transaction data support`
    - `6dc4808c1` `fix: tighten transaction data response validation`
    - `228954e40` `fix: tighten transaction data validation and demo config`
    - `b1388dc15` `fix: tighten transaction data response validation`

## Commit Separation
- Docs commits stay on docs branch.
- Product commits are cherry-picked onto feature branches.
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

## Completed Validation
- Task 1 focused validation:
  - `./gradlew --no-build-cache :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
- Task 2 focused validation:
  - `./gradlew --no-build-cache :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.TransactionDataUtilsTest' :waltid-libraries:credentials:waltid-verification-policies2-vp:jvmTest --tests 'id.walt.policies2.vp.policies.TransactionDataHashCheckSdJwtVPPolicyTest' --tests 'id.walt.policies2.vp.policies.TransactionDataMdocVpPolicyTest' :waltid-services:waltid-verifier-api2:test --tests 'id.walt.verifier2.sdjwt.IETFSdJwtVcWithDisclosureVerifier2IntegrationTest' --tests 'id.walt.verifier2.mdocs.PidBirthDateIssuerSignedIntegrityReproTest'`
  - `./gradlew --no-build-cache :waltid-services:waltid-issuer-api:compileKotlin :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
- Production Docker validation:
  - `docker build -t waltid-web-portal-local -f waltid-applications/waltid-web-portal/Dockerfile .`
  - `docker build -t waltid-demo-wallet-local -f waltid-applications/waltid-web-wallet/apps/waltid-demo-wallet/Dockerfile .`
  - `docker build -t waltid-dev-wallet-local -f waltid-applications/waltid-web-wallet/apps/waltid-dev-wallet/Dockerfile .`
- Automated end-to-end validation:
  - Playwright base OpenID4VP 1.0 wallet flow completed successfully for task 1
  - Playwright transaction flow completed successfully for `dc+sd-jwt`
  - Playwright transaction flow completed successfully for `mso_mdoc`
  - latest task-1 artifact set: `/tmp/waltid-playwright/artifacts/base-2026-04-08T16-13-47.561Z`
  - latest task-2 SD-JWT artifact set: `/tmp/waltid-playwright/artifacts/2026-04-08T16-25-47.193Z`
  - latest task-2 mdoc artifact set: `/tmp/waltid-playwright/artifacts/2026-04-08T16-26-16.190Z`

## Remaining Before Upstream Publication
- Record the final manual demo outside git.
- Do the final manual code review.
- Prepare upstream PR descriptions from the notes file.
- Rebase on latest upstream `main` only when ready to open upstream PRs.
