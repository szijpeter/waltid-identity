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
