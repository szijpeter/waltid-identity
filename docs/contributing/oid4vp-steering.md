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
