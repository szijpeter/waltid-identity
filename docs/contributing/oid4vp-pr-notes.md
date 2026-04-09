# OID4VP PR Notes

## Current Branch Heads
- `feat/wallet-openid4vp-v1` at `44e181fc9`
- `feat/transaction-data-support` at `48d0fe2df`

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
- Current branch contains the feature commit plus targeted review-driven hardening and cleanup commits.
- Latest readiness hardening restored legacy draft signed request-object compatibility without weakening the strict verifier2/v1 path.

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
- Current branch is cleanly restacked on top of the current task-1 head at `117d4a233`.
- Current branch contains the feature commit plus targeted review-driven hardening commits and one post-restack fix restoring SD-JWT transaction-data key binding hashing.

### Exclusions
- No steering docs in this PR branch.

## Review Reminders
- Keep language neutral and technical.
- Mention draft compatibility explicitly.
- Call out that PR 2 is intentionally stacked on PR 1.
- Mention that the manual recording is still pending and is not part of git history.

## Readiness Notes
- Branch 1 readiness checks on current head:
  - `./gradlew --no-daemon --max-workers=3 --rerun-tasks :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
  - `./gradlew --no-daemon --max-workers=3 --rerun-tasks :waltid-services:waltid-integration-tests:test :waltid-services:waltid-e2e-tests:test`
  - `./gradlew clean build cleanAllTests allTests --rerun-tasks --no-daemon --max-workers=3`
- Current branch 1 readiness result:
  - focused wallet-api test suite passed
  - legacy integration and e2e suites passed
  - browser E2E passed for the demo wallet verifier2 flow, direct query-param flow, inline `request` flow, and signed request-object flow
  - the full repo CI-like run still fails in unrelated JS-node test `VcApiTest.testVcApi[js, node]` under `waltid-w3c-credentials`
- Current branch 1 browser artifacts:
  - base verifier2 flow: `/tmp/waltid-playwright/artifacts/base-2026-04-09T19-22-47.927Z`
  - direct flow: `/tmp/waltid-playwright/artifacts/direct-2026-04-09T19-22-47.927Z`
  - inline `request` flow: `/tmp/waltid-playwright/artifacts/request-2026-04-09T19-22-47.928Z`
  - signed request-object flow: `/tmp/waltid-playwright/artifacts/signed-request-2026-04-09T19-31-56.517Z`

## Remaining Before Publication
- Final manual demo recording is still pending and should stay outside git.
- Final manual review pass is still pending.
- Upstream PRs to `walt-id/waltid-identity` have not been opened yet.
