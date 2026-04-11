# OID4VP PR Notes

## Current Branches
- `feat/wallet-openid4vp-v1`
- `feat/transaction-data-support`
- `feat/wallet-openid4vp-holder-binding-fix`
- `feat/transaction-data-verifier2-verification-followup`

## Current Fork PRs
- PR 1: [Add OpenID4VP 1.0 support to wallet-api](https://github.com/szijpeter/waltid-identity/pull/3)
  - branch: `feat/wallet-openid4vp-v1`
  - base: `main`
- PR 2: [Add transaction data support to verifier and wallet flows](https://github.com/szijpeter/waltid-identity/pull/4)
  - branch: `feat/transaction-data-support`
  - base: `feat/wallet-openid4vp-v1`
- PR 3 (follow-up): [Fix OpenID4VP holder-bound key selection for SD-JWT presentations](https://github.com/szijpeter/waltid-identity/pull/5)
  - branch: `feat/wallet-openid4vp-holder-binding-fix`
  - base: `feat/wallet-openid4vp-v1`
- PR 4 (follow-up): [Follow-up: add transaction-data checks in verifier2 compatibility validators](https://github.com/szijpeter/waltid-identity/pull/6)
  - branch: `feat/transaction-data-verifier2-verification-followup`
  - base: `feat/transaction-data-support`

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
- No SD-JWT holder-binding interop fix in this PR (tracked in follow-up PR #5).
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
- Current branch remains stacked on top of PR1 (`feat/wallet-openid4vp-v1`).
- Current branch contains the feature commit plus targeted review-driven hardening commits and one post-restack fix restoring SD-JWT transaction-data key binding hashing.

### Exclusions
- No steering docs in this PR branch.
- No verifier2 compatibility-validator package additions in this PR branch (`id.walt.verifier2.verification` moved to follow-up PR #6).

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
  - artifacts are now stored under:
    - `$HOME/.waltid-playwright-artifacts/<branch-tag>--<scenario>--<timestamp>/`
  - canonical task-1 scenario names:
    - `main--legacy-verifier-portal--...`
    - `wallet-openid4vp-v1--verifier2-api-dc-sd-jwt-request_uri_get--...`
    - `wallet-openid4vp-v1--verifier2-api-dc-sd-jwt-request_object_unsigned--...`
    - `wallet-openid4vp-v1--verifier2-api-dc-sd-jwt-request_object_signed--...`
- Current branch 2 post-restack checks:
  - `./gradlew --no-build-cache --no-daemon :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.transactiondata.*' :waltid-libraries:credentials:waltid-verification-policies2-vp:jvmTest --tests 'id.walt.policies2.vp.policies.TransactionDataHashCheckSdJwtVPPolicyTest' --tests 'id.walt.policies2.vp.policies.TransactionDataMdocVpPolicyTest' :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest' :waltid-services:waltid-verifier-api2:test --tests 'id.walt.verifier2.sdjwt.IETFSdJwtVcWithDisclosureVerifier2IntegrationTest' --tests 'id.walt.verifier2.mdocs.PidBirthDateIssuerSignedIntegrityReproTest'`
  - browser E2E artifacts:
    - SD-JWT transaction flow: `$HOME/.waltid-playwright-artifacts/transaction-data-support--verifier2-portal-dc-sd-jwt--<timestamp>/`
    - mdoc transaction flow: `$HOME/.waltid-playwright-artifacts/transaction-data-support--verifier2-portal-mso-mdoc--<timestamp>/`

## Remaining Before Publication
- Final manual demo recording is still pending and should stay outside git.
- Final manual review pass is still pending.
- Upstream PRs to `walt-id/waltid-identity` have not been opened yet.
