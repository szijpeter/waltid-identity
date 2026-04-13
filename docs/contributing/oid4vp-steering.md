# OID4VP Steering

## Objective
- Add OpenID4VP 1.0 holder support to OSS `wallet-api` without regressing draft OpenID4VP flows.
- Follow up with end-to-end `transaction_data` support across the shared v1 libraries, `verifier-api2`, and the OSS wallet flow.
- Keep planning artifacts committed on a neutral docs branch and out of eventual feature PR branches via cherry-picking.

## Working Rules
- Use neutral, contribution-style naming in branches, commits, and any future PR metadata.
- Do not open pull requests without explicit confirmation.
- Keep steering and progress artifacts separate from product commits.
- Treat draft and v1 support as parallel capabilities, not a migration that removes draft behavior.

## Additional Baseline Fix Required
- `main` currently has a wallet registration blocker unrelated to the OID4VP feature scope:
  - runtime classpath collision in `wallet-api` caused by duplicate `id.walt.webwallet.usecase.exchange.*` classes coming from `waltid-core-wallet`
  - observed failure mode: `NoSuchMethodError` during `WalletServiceManager` init, then `NoClassDefFoundError` and HTTP 500 on register/create-wallet flows
- Related upstream tracking:
  - issue: [https://github.com/walt-id/waltid-identity/issues/1608](https://github.com/walt-id/waltid-identity/issues/1608)
  - prior fix attempt (closed, unmerged): [https://github.com/walt-id/waltid-identity/pull/1609](https://github.com/walt-id/waltid-identity/pull/1609)
- Repo strategy:
  - keep this as a separate minimal hotfix PR in addition to the original task PRs
  - do not fold this baseline bugfix into PR1/PR2 feature scope
  - mention the related issue explicitly in the upstream PR description (`Resolves #1608` or equivalent)

## Current Repo Map

### Wallet API and Current OSS Wallet Flow
- `waltid-services/waltid-wallet-api`
- Current presentation flow is still centered on `id.walt.oid4vc` request parsing and `presentation_definition`.
- Main controller entrypoints:
  - `web/controllers/exchange/ExchangeController.kt`
  - `web/controllers/exchange/ExchangeExternalSignaturesController.kt`
- Main service entrypoint:
  - `service/SSIKit2WalletService.kt`
- Current wallet-side helper classes:
  - `service/oidc4vc/TestCredentialWallet.kt`
  - `service/oidc4vc/VPresentationSession.kt`
- Current matching logic is presentation-definition specific:
  - `usecase/exchange/MatchPresentationDefinitionCredentialsUseCase.kt`
  - `usecase/exchange/NoMatchPresentationDefinitionCredentialsUseCase.kt`

### Shared OpenID4VP 1.0 Libraries
- `waltid-libraries/protocols/waltid-openid4vp`
  - v1 authorization request model including `dcql_query` and `transaction_data`
- `waltid-libraries/protocols/waltid-openid4vp-wallet`
  - holder-side processing through `WalletPresentFunctionality2`
- `waltid-libraries/protocols/waltid-openid4vp-verifier`
  - verifier-side v1 flow logic reused by `verifier-api2`
- `waltid-libraries/protocols/waltid-openid4vp-clientidprefix`
  - signed request and client-id prefix handling used by the v1 wallet flow

### Verifier 2
- `waltid-services/waltid-verifier-api2`
- Thin OSS service around the shared verifier library.
- Key entrypoints:
  - `OSSVerifier2Service.kt`
  - `OSSVerifier2Manager.kt`
- Session creation and request generation:
  - `waltid-openid4vp-verifier/.../VerificationSessionCreator.kt`
- The verifier-side setup types already expose `transactionData`.

### Wallet UIs
- `waltid-applications/waltid-web-wallet`
- Shared presentation composable:
  - `libs/composables/presentation.ts`
- Relevant pages:
  - `apps/waltid-demo-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`
  - `apps/waltid-dev-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`
- Current UI assumes the resolved request contains `presentation_definition`.

### Verifier Demo UI
- `waltid-applications/waltid-web-portal`
- Existing verification flow is legacy-verifier based:
  - `pages/verify/index.tsx`
  - `components/sections/VerificationSection.tsx`
- This app is the best candidate for a minimal verifier demo rather than inventing a new UI surface.

## Known Constraints
- Working tree already contains unrelated local changes:
  - modified `local.properties`
  - untracked `gradle/gradle-daemon-jvm.properties`
- These files must stay out of docs commits and later product commits.
- Existing tests and helpers strongly assume draft request resolution via `presentation_definition`, so v1 support must add a parallel path instead of replacing that assumption globally in one step.

## Known SD-JWT Interop Note
- The legacy verifier SD-JWT path can fail on `signature_sd-jwt-vc` with holder key-binding verification errors in some wallet/verifier combinations.
- This is not a standards limitation of SD-JWT itself; it is an implementation-interoperability risk in the current mainline stack.
- That wallet-side mitigation is tracked out of PR1 scope in follow-up branch `feat/wallet-openid4vp-holder-binding-fix` and fork PR [#5](https://github.com/szijpeter/waltid-identity/pull/5), so PR1 can stay focused on OpenID4VP 1.0 wallet integration.
- Related public issue context:
  - [https://github.com/walt-id/waltid-identity/issues/713](https://github.com/walt-id/waltid-identity/issues/713)
  - [https://github.com/walt-id/waltid-identity/issues/1272](https://github.com/walt-id/waltid-identity/issues/1272)
  - [https://github.com/walt-id/waltid-identity/issues/779](https://github.com/walt-id/waltid-identity/issues/779)

## Known PR1 Verification Finding (`jwt_vc_json`)
- In verifier2 matrix runs on PR1, `dc+sd-jwt` succeeds across implemented request shapes, while `jwt_vc_json` fails across those same shapes.
- This points to a format-specific integration gap, not to request-shape support being broken.
- `main` does not expose the same verifier2-compatible wallet path, so the exact failure cannot be compared one-to-one there.
- Treat this as an out-of-scope follow-up for PR1 unless the PR scope is explicitly expanded to include fixing verifier2 `jwt_vc_json` end-to-end behavior.

## Known A/B Verification Finding (PR1 vs PR5 holder binding)
- Focused A/B harness runs (`dc+sd-jwt`, request shapes `direct` + `request_object_signed`) show:
  - PR1 (`feat/wallet-openid4vp-v1`): verifier2 `dc+sd-jwt` scenarios fail
  - PR5 (`feat/wallet-openid4vp-holder-binding-fix`): the same scenarios succeed
- This is the expected split behavior and confirms PR5 contains the holder-binding fix that is intentionally out of PR1 scope.
- Optional legacy SD-JWT portal probes were also added, but in the measured environment the verifier portal did not expose the `SD-JWT VC` option, so those probe failures are not used as branch-delta evidence.

## Late-stage Findings (2026-04-13)
- Verifier UI split is intentional and matters for manual verification:
  - `http://localhost:7102/verify` is legacy verifier UI (depends on `verifier-api`, port `7303`)
  - `http://localhost:7102/verify/transaction` is verifier2 transaction demo UI (depends on `verifier-api2`, port `7304`)
  - there is no full separate verifier2 "dev app" equivalent to the legacy `/verify` page in the current OSS portal
- Two production-facing compatibility fixes were confirmed as required and are not harness-only:
  - wallet deep-link handling in demo/dev wallet must avoid `encodeURI(decodeURI(...))` roundtrip before base64url request wrapping, because this can mutate already-encoded OpenID4VP query values
  - verifier transaction demo `dc+sd-jwt` query should include both known VCT identifiers used in OSS flows:
    - `<issuer>/identity_credential`
    - `<issuer>/draft13/IdentityCredential`
- Practical operator finding:
  - if `verifier-api` is not started, the legacy verifier flow cannot generate offer/request URLs from `/verify`, even when verifier2 flow is healthy
  - this is a runtime stack prerequisite issue, not a protocol issue

## Recording Runbook
- A dedicated, robust manual recording runbook is now maintained at:
  - `docs/contributing/oid4vp-recording-scenarios.md`
- Use that file as the source of truth for:
  - required service topology
  - scenario matrix per branch objective
  - expected states to capture for wallet and verifier windows
  - known troubleshooting patterns during recording sessions

## Readiness Snapshot (2026-04-12)
- PR1 required readiness matrix is green on the current PR1+PR5 stack:
  - summary: `$HOME/.waltid-playwright-artifacts/wallet-openid4vp-pr1pr5-final--pr1-matrix-summary--2026-04-12T17-25-37.216Z/run-summary.json`
  - required scenarios passed:
    - `legacy-jwt-w3c`
    - verifier2 `dc+sd-jwt` with `direct`, `request_uri_get`, `request_object_unsigned`, `request_object_signed`
  - non-blocking probe: `request_uri_post` is `SKIPPED_UNSUPPORTED` (verifier2 `/request` POST probe returns `404`).
- PR2 matrix on PR2+PR5 stack is partially green:
  - summary: `$HOME/.waltid-playwright-artifacts/transaction-data-pr2pluspr5-fix--pr2-matrix-summary--2026-04-12T17-10-31.643Z/run-summary.json`
  - successful required paths:
    - legacy verifier compatibility (`legacy-jwt-w3c`)
    - verifier2 portal `dc+sd-jwt`
    - verifier2 API `dc+sd-jwt` request-shape matrix (`direct`, `request_uri_get`, `request_object_unsigned`, `request_object_signed`)
  - failing required paths:
    - verifier2 portal `mso_mdoc`
    - verifier2 API `mso_mdoc` request-shape matrix (`direct`, `request_uri_get`, `request_object_unsigned`, `request_object_signed`)
- mdoc failure classification:
  - main baseline mdoc verifier2 run fails earlier in wallet request resolution (`resolvePresentationRequest` 500): `$HOME/.waltid-playwright-artifacts/main-baseline--verifier2-api-mso-mdoc-direct--2026-04-12T15-22-34.275Z`
  - PR1+PR5 baseline reproduces the same early failure: `$HOME/.waltid-playwright-artifacts/pr1pr5-baseline--verifier2-api-mso-mdoc-direct--2026-04-12T15-28-59.885Z`
  - PR2 reaches verifier2 policy execution, but fails at `mso_mdoc/device-auth` (`Device authentication signature failed to verify.`)
  - PR2 with `ENABLE_TRANSACTION_DATA=false` still fails at the same `device-auth` step while `mso_mdoc/transaction-data-hash-check` passes with `expected_transaction_data_items=0` and `embedded_transaction_data_items=0`
  - conclusion: current mdoc E2E blocker is not caused by PR2 transaction-data logic.

## Delivery Strategy
- Docs branch:
  - `docs/oid4vp-steering`
- Later product branches:
  - `feat/wallet-openid4vp-v1`
  - `feat/transaction-data-support`
- Cherry-pick only product commits from this docs branch onto future feature branches.

## Immediate Checkpoint
- Finish and commit the docs in `docs/contributing/`.
- Stop and confirm before any product-code implementation starts.
