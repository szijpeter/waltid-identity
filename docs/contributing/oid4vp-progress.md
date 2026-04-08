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
- [x] Collapse feature branches to one commit each
- [ ] Push branches or open PRs after confirmation

## Branch Plan
- Current docs branch: `docs/oid4vp-steering`
- Feature branch 1: `feat/wallet-openid4vp-v1`
- Feature branch 2: `feat/transaction-data-support`

## Branch Heads
- `feat/wallet-openid4vp-v1` at `426feec27`
- `feat/transaction-data-support` at `53636c926`

## Feature Branch Status
- `feat/wallet-openid4vp-v1`
  - ready for push/PR preparation from a code and branch-hygiene perspective
  - collapsed to a single branch-only commit:
    - `426feec27` `feat: add OpenID4VP wallet request handling`
- `feat/transaction-data-support`
  - ready for push/PR preparation from a code and branch-hygiene perspective
  - restacked onto the current `feat/wallet-openid4vp-v1` head as a single task-2 commit
  - latest branch-only commit on top of task 1:
    - `53636c926` `feat: add transaction data support`

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
  - Playwright transaction flow completed successfully for `dc+sd-jwt`
  - Playwright transaction flow completed successfully for `mso_mdoc`
  - latest mdoc artifact set: `/tmp/waltid-playwright/artifacts/2026-04-08T12-30-38.986Z`
  - latest SD-JWT artifact set: `/tmp/waltid-playwright/artifacts/2026-04-08T12-30-52.961Z`

## Remaining Before Push
- Prepare final PR descriptions from the notes file.
- Record the final manual demo outside git.
- Optional final manual browser smoke run before pushing.
