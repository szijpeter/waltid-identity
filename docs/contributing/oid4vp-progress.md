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
- Follow-up branch: `feat/wallet-openid4vp-holder-binding-fix`

## Active Branches
- `feat/wallet-openid4vp-v1`
- `feat/transaction-data-support`
- `feat/wallet-openid4vp-holder-binding-fix`

## Feature Branch Status
- `feat/wallet-openid4vp-v1`
  - pushed to fork and under review
  - fork PR: [https://github.com/szijpeter/waltid-identity/pull/3](https://github.com/szijpeter/waltid-identity/pull/3)
  - current branch evolved through review-driven hardening and cleanup after the initial feature commit
  - latest review-driven hardening also restored legacy draft compatibility for signed request-object flows that still use plain `http` / `https` client IDs
  - key change areas:
    - OpenID4VP 1.0 request resolution and routing in wallet-api
    - wallet presentation flow hardening and request-object handling
    - legacy draft compatibility fallback for signed request-object edge cases
- `feat/transaction-data-support`
  - pushed to fork and under review
  - fork PR: [https://github.com/szijpeter/waltid-identity/pull/4](https://github.com/szijpeter/waltid-identity/pull/4)
  - PR base is `feat/wallet-openid4vp-v1`
  - branch was restacked onto the current task-1 head after the latest task-1 review round
  - key change areas:
    - shared transaction-data parsing and validation utilities
    - verifier2 transaction-data policy checks for SD-JWT and mdoc
    - wallet and portal transaction-data UX wiring
- `feat/wallet-openid4vp-holder-binding-fix`
  - pushed to fork and under review
  - fork PR: [https://github.com/szijpeter/waltid-identity/pull/5](https://github.com/szijpeter/waltid-identity/pull/5)
  - PR base is `feat/wallet-openid4vp-v1`
  - this branch intentionally carries only the SD-JWT holder-binding interop mitigation that was split out of PR1 scope

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
  - readiness rerun on current branch head:
    - `./gradlew --no-daemon --max-workers=3 --rerun-tasks :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
  - legacy compatibility rerun on current branch head:
    - `./gradlew --no-daemon --max-workers=3 --rerun-tasks :waltid-services:waltid-integration-tests:test :waltid-services:waltid-e2e-tests:test`
- Task 2 focused validation:
  - `./gradlew --no-build-cache :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.TransactionDataUtilsTest' :waltid-libraries:credentials:waltid-verification-policies2-vp:jvmTest --tests 'id.walt.policies2.vp.policies.TransactionDataHashCheckSdJwtVPPolicyTest' --tests 'id.walt.policies2.vp.policies.TransactionDataMdocVpPolicyTest' :waltid-services:waltid-verifier-api2:test --tests 'id.walt.verifier2.sdjwt.IETFSdJwtVcWithDisclosureVerifier2IntegrationTest' --tests 'id.walt.verifier2.mdocs.PidBirthDateIssuerSignedIntegrityReproTest'`
  - `./gradlew --no-build-cache :waltid-services:waltid-issuer-api:compileKotlin :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
  - post-restack validation:
    - `./gradlew --no-build-cache :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.TransactionDataUtilsTest' :waltid-libraries:credentials:waltid-verification-policies2-vp:jvmTest --tests 'id.walt.policies2.vp.policies.TransactionDataHashCheckSdJwtVPPolicyTest' --tests 'id.walt.policies2.vp.policies.TransactionDataMdocVpPolicyTest' :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
- Production Docker validation:
  - `docker build -t waltid-web-portal-local -f waltid-applications/waltid-web-portal/Dockerfile .`
  - `docker build -t waltid-demo-wallet-local -f waltid-applications/waltid-web-wallet/apps/waltid-demo-wallet/Dockerfile .`
  - `docker build -t waltid-dev-wallet-local -f waltid-applications/waltid-web-wallet/apps/waltid-dev-wallet/Dockerfile .`
- Automated end-to-end validation:
  - Playwright base OpenID4VP 1.0 wallet flow completed successfully for task 1 on current branch head
  - Playwright direct query-parameter launch flow completed successfully on current branch head
  - Playwright inline `request` launch flow completed successfully on current branch head
  - Playwright signed request-object flow completed successfully on current branch head when verifier2 is started with the `x509_san_dns:verifier.example.com` profile
  - Playwright transaction flow completed successfully for `dc+sd-jwt`
  - Playwright transaction flow completed successfully for `mso_mdoc`
  - latest task-1 base artifact set: `/tmp/waltid-playwright/artifacts/base-2026-04-09T19-22-47.927Z`
  - latest task-1 direct artifact set: `/tmp/waltid-playwright/artifacts/direct-2026-04-09T19-22-47.927Z`
  - latest task-1 inline-request artifact set: `/tmp/waltid-playwright/artifacts/request-2026-04-09T19-22-47.928Z`
  - latest task-1 signed-request artifact set: `/tmp/waltid-playwright/artifacts/signed-request-2026-04-09T19-31-56.517Z`
  - latest task-2 SD-JWT artifact set: `/tmp/waltid-playwright/artifacts/2026-04-09T19-39-35.888Z`
  - latest task-2 mdoc artifact set: `/tmp/waltid-playwright/artifacts/2026-04-09T19-39-47.994Z`
- Full repo CI-like validation:
  - `./gradlew clean build cleanAllTests allTests --rerun-tasks --no-daemon --max-workers=3`
  - current outcome: branch-related wallet/integration/e2e suites are green; the full run still fails in unrelated JS-node test `VcApiTest.testVcApi[js, node]` under `waltid-libraries/credentials/waltid-w3c-credentials`

## Recent Learnings
- Strict OpenID4VP 1.0 request routing and legacy draft compatibility need a narrow escape hatch, not a broad fallback.
  - Real verifier2/v1 requests should still fail hard when request resolution or signed request-object validation fails.
  - Legacy draft request-object flows in the older test suites still use plain `http` / `https` client IDs, so wallet-api now falls back only for `UnsupportedPrefix(http|https)` when that failure came from signed request-object validation.
- Preserving signed Request Objects end to end was the right correctness choice, but it made the frontend read-side slightly heavier because the UI sometimes has to inspect a preserved `request=<jwt>` payload rather than flattened query params.
- The local Playwright harness is worth keeping, but it now clearly behaves like verification tooling rather than product code.
  - signed request-object browser verification requires the verifier2 temp config to use `clientId: "x509_san_dns:verifier.example.com"`
  - the demo wallet browser path is the most reliable verification target today
- Restacking PR 2 after the late PR 1 hardening pass was mostly a request-resolution integration exercise.
  - the important re-check after that restack was not just the focused Kotlin suite but also a fresh SD-JWT and mdoc browser run against rebuilt images

## Remaining Before Upstream Publication
- Record the final manual demo outside git.
- Do the final manual code review.
- Prepare upstream PR descriptions from the notes file.
- Rebase on latest upstream `main` only when ready to open upstream PRs.
