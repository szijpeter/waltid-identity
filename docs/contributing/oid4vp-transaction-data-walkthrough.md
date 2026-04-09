# Transaction Data Walkthrough

## Branch and PR
- Branch: `feat/transaction-data-support`
- Current head: `48d0fe2df`
- Fork PR: [https://github.com/szijpeter/waltid-identity/pull/4](https://github.com/szijpeter/waltid-identity/pull/4)
- Base branch: `feat/wallet-openid4vp-v1`

## Current stacked shape
This branch is currently restacked on top of task 1 at `117d4a233`.

Current task-2-only commits on top of task 1:
- `f3dcf6221` `feat: add transaction data support`
- `c19b398cc` `fix: tighten transaction data response validation`
- `6e31d7273` `fix: tighten transaction data validation and demo config`
- `bace9a200` `fix: tighten transaction data response validation`
- `48d0fe2df` `fix: restore transaction data key binding handling after restack`

## Task and Intent
The goal of this branch is to complete `transaction_data` support end to end in OSS:
- shared `openid4vp-*` libraries
- verifier2 service and validation
- wallet service and wallet UI
- simple verifier demo UI
- one demoable use case

The issue that best captures the intended direction is:
- [Issue #1583: Transaction data authorization support with Verifiable Presentation](https://github.com/walt-id/waltid-identity/issues/1583)
- especially the guidance comment:
  - [https://github.com/walt-id/waltid-identity/issues/1583#issuecomment-4071924774](https://github.com/walt-id/waltid-identity/issues/1583#issuecomment-4071924774)

That comment says the missing half of the feature is:
1. save the request transaction data
2. look it up again during presentation verification
3. verify the returned binding via a `VerificationPolicy`
4. make the verification format-specific
5. for mdoc, use the `DeviceSigned`-embedded approach

This branch was explicitly aligned to that direction.

## Standards and Reference Material

### Primary standard
- OpenID for Verifiable Presentations 1.0:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0.html](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)

### Most relevant parts
- request-side `transaction_data`
- format-specific handling of transaction authorization
- mdoc-specific format guidance
- SD-JWT VC transaction-data hash binding

The most relevant spec areas for this branch were:
- OpenID4VP 1.0, `transaction_data` request semantics
- OpenID4VP 1.0, Appendix B.2.1 for the mdoc-oriented model
- OpenID4VP 1.0, Appendix B.3.3 for SD-JWT VC transaction-data hash binding

### Upstream repo references
- [Issue #1583](https://github.com/walt-id/waltid-identity/issues/1583)
- [Issue comment 4071924774](https://github.com/walt-id/waltid-identity/issues/1583#issuecomment-4071924774)
- [PR #1597: Annex C Extensions](https://github.com/walt-id/waltid-identity/pull/1597)

The issue comment matters more than the issue title alone because it describes the intended architecture:
- policy-based verification
- saved request data looked up again during presentation
- format-specific behavior for SD-JWT vs mdoc

## What the Repo Already Had Before This Branch

### Shared models and partial wiring
Before this branch:
- the v1 request model already had `transaction_data`
- verifier2 setup types already exposed `transactionData`
- the OSS wallet/verifier path did not yet complete the second half of the feature

In other words, the repo already had some of the request surface, but not the full end-to-end enforcement and demo path.

### What was missing
Before this branch, OSS was still missing:
- request-side decoding and strict validation of `transaction_data`
- wallet response-side transaction binding
- verifier-side validation of the returned binding
- policy-based format-specific verification
- OSS demo UI for creating a transaction-data verifier2 session
- wallet-side UI to show transaction details before consent

## Supported Formats in This Branch
This branch supports transaction data for:
- `dc+sd-jwt`
- `mso_mdoc`

That is intentional.

Why only those two:
- they are the two formats for which the spec and the issue guidance provide a concrete transaction-binding story
- SD-JWT uses `transaction_data_hashes` / `transaction_data_hashes_alg`
- mdoc uses embedded transaction data in the `DeviceSigned` structure with authorization through key authorization

Formats not supported for transaction data in this branch:
- `jwt_vc_json`
- `ldp_vc`
- `ac_vp`

Why not:
- there is no equivalent, standards-grounded transaction-binding implementation path for those formats in this repo today
- claiming support there would be speculative and weaker than the task asked for

## High-level design
This branch completes the feature in four layers:

1. Shared request/validation utility layer
- parse and validate request-side `transaction_data`
- enforce supported types and request constraints

2. Wallet response generation
- SD-JWT: emit transaction-data hashes in the KB-JWT
- mdoc: embed transaction data in the `DeviceSigned` namespace when the credential authorizes it

3. Verifier verification
- save transaction data with the session
- pass it into VP verification context
- validate it through format-specific VP policies

4. Demo surfaces
- verifier portal page to create a transaction-data verifier2 session
- wallet UI that renders the transaction details before consent

## File-by-file walkthrough

### 1. Shared transaction-data utility layer
Files:
- `waltid-libraries/protocols/waltid-openid4vp/src/commonMain/kotlin/id/walt/verifier/openid/TransactionDataUtils.kt`
- `waltid-libraries/protocols/waltid-openid4vp/src/commonMain/kotlin/id/walt/verifier/openid/models/authorization/TransactionDataItem.kt`
- `waltid-libraries/protocols/waltid-openid4vp/src/commonTest/kotlin/id/walt/verifier/openid/TransactionDataUtilsTest.kt`

What changed:
- added the core parsing and validation helper for request-side and response-side transaction-data processing
- defined the supported type set in one place
- validated:
  - type
  - `credential_ids`
  - `transaction_data_hashes_alg`
  - `require_cryptographic_holder_binding`
  - omission rules when no transaction data was requested
- added focused tests for valid and invalid cases

Why:
- the repo needed one shared authority for transaction-data rules instead of duplicating logic in wallet UI, wallet service, and verifier service
- the supported type set is intentionally centralized so wallet and verifier use the same rules

### 2. SD-JWT response handling
Files:
- `waltid-libraries/protocols/waltid-openid4vp-wallet/src/commonMain/kotlin/id/waltid/openid4vp/wallet/WalletPresentFunctionality2.kt`
- `waltid-libraries/protocols/waltid-openid4vp-wallet/src/commonMain/kotlin/id/waltid/openid4vp/wallet/presentation/SdJwtVcPresenter.kt`
- `waltid-libraries/credentials/waltid-digital-credentials/src/commonMain/kotlin/id/walt/credentials/presentations/formats/DcSdJwtPresentation.kt`
- `waltid-libraries/credentials/waltid-digital-credentials/src/commonMain/kotlin/id/walt/credentials/presentations/PresentationValidationExceptions.kt`
- `waltid-libraries/credentials/waltid-digital-credentials/src/jvmTest/kotlin/id/walt/credentials/PresentationTest.kt`

What changed:
- wallet-side SD-JWT presentations now attach:
  - `transaction_data_hashes`
  - `transaction_data_hashes_alg`
- parsing/validation on the digital-credentials side is strict about malformed/non-string values

Why:
- SD-JWT VC transaction authorization is a hash-binding problem
- the branch needed both:
  - correct generation on the holder side
  - strict deserialization/validation on the verifier side

### 3. mdoc response handling
Files:
- `waltid-libraries/protocols/waltid-openid4vp-wallet/src/commonMain/kotlin/id/waltid/openid4vp/wallet/presentation/MdocPresenter.kt`
- `waltid-services/waltid-issuer-api/src/main/kotlin/id/walt/issuer/issuance/CIProvider.kt`

What changed:
- the wallet-side mdoc presenter now embeds transaction data into the appropriate response path and requires key authorization support
- the issuer side now emits transaction-data-related key authorizations for mdocs so the wallet can legally use them in a transaction-authorizing presentation

Why:
- the issue comment explicitly called out that the mdoc path is format-specific and should use the `DeviceSigned`-embedded model
- without issuer-side key authorizations, an mdoc E2E demo would not actually be possible

### 4. Verifier-side policy architecture
Files:
- `waltid-libraries/credentials/waltid-verification-policies2-vp/src/commonMain/kotlin/id/walt/policies2/vp/policies/VPVerificationContext.kt`
- `waltid-libraries/credentials/waltid-verification-policies2-vp/src/commonMain/kotlin/id/walt/policies2/vp/policies/VPVerificationPolicyManager.kt`
- `waltid-libraries/credentials/waltid-verification-policies2-vp/src/commonMain/kotlin/id/walt/policies2/vp/policies/dc_sd_jwt/TransactionDataHashCheckSdJwtVPPolicy.kt`
- `waltid-libraries/credentials/waltid-verification-policies2-vp/src/commonMain/kotlin/id/walt/policies2/vp/policies/mso_mdoc/TransactionDataMdocVpPolicy.kt`
- tests under:
  - `.../dc_sd_jwt/TransactionDataHashCheckSdJwtVPPolicyTest.kt`
  - `.../mso_mdoc/TransactionDataMdocVpPolicyTest.kt`

What changed:
- transaction data verification is now policy-based and format-specific
- expected transaction data is added to the VP verification context
- default VP policy wiring includes:
  - an SD-JWT-specific transaction hash policy
  - an mdoc-specific transaction-data policy

Why:
- this directly follows the architecture described in [issue comment 4071924774](https://github.com/walt-id/waltid-identity/issues/1583#issuecomment-4071924774)
- this is cleaner than burying format-specific transaction verification inside a single generic validator

### 5. Verifier runtime integration
Files:
- `waltid-libraries/protocols/waltid-openid4vp-verifier/src/commonMain/kotlin/id/walt/verifier2/verification2/PresentationVerificationEngine.kt`
- `waltid-libraries/protocols/waltid-openid4vp-verifier/src/jvmMain/kotlin/id/walt/verifier2/handlers/sessioncreation/VerificationSessionCreator.kt`
- `waltid-libraries/protocols/waltid-openid4vp-verifier/src/commonMain/kotlin/id/walt/verifier2/verification/Verifier2PresentationValidator.kt`
- `waltid-libraries/protocols/waltid-openid4vp-verifier/src/commonMain/kotlin/id/walt/verifier2/verification/SdJwtVcPresentationValidator.kt`
- `waltid-libraries/protocols/waltid-openid4vp-verifier/src/commonMain/kotlin/id/walt/verifier2/verification/MdocPresentationValidator.kt`
- `waltid-libraries/protocols/waltid-openid4vp-verifier/src/commonMain/kotlin/id/walt/verifier2/verification/W3CPresentationValidator.kt`
- `waltid-libraries/protocols/waltid-openid4vp-verifier/README.md`

What changed:
- verifier2 now stores the request transaction data in the session and passes it into verification
- the live verifier path uses the VP policy architecture
- older public validator entry points were kept as compatibility shims instead of being removed outright
- obsolete internal wiring was removed, including:
  - `Verifier2SessionPresentationValidation.kt`

Why:
- we wanted the live verifier architecture to follow the issue guidance
- but we explicitly avoided breaking public APIs that already existed in the OSS library

That compatibility decision matters:
- internally, verifier2 uses the new policy-based path
- externally, previously available validator classes still exist so the branch does not introduce a gratuitous library API break

### 6. Wallet service integration
Files:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/SSIKit2WalletService.kt`
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationService.kt`
- `waltid-services/waltid-wallet-api/src/test/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationServiceTest.kt`

What changed:
- task-2-specific transaction-data support was wired through the already updated v1 wallet path
- request validation uses the shared transaction-data utility layer
- the later review fixes were kept compatible with task 1:
  - no duplicate `request_uri` resolution
  - strict matching/submission semantics

Why:
- transaction data should be an extension of the v1 path, not an entirely separate wallet route

### 7. Wallet UI changes
Files:
- `waltid-applications/waltid-web-wallet/libs/composables/presentation.ts`
- `waltid-applications/waltid-web-wallet/apps/waltid-demo-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`
- `waltid-applications/waltid-web-wallet/apps/waltid-dev-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`
- `waltid-applications/waltid-web-wallet/apps/waltid-demo-wallet/src/components/CredentialDisclosure.vue`

What changed:
- the wallet composable decodes and validates request-side `transaction_data`
- both wallet UIs display transaction details before consent
- CTA wording changes from a pure disclosure action to an authorization-oriented action when transaction data is present
- credential disclosure rendering is more robust for wrapped/scalar payloads

Why:
- the task explicitly asked for a demoable wallet UI flow
- transaction authorization needs to be visible to the holder before consent

### 8. Verifier demo UI and deployment wiring
Files:
- `waltid-applications/waltid-web-portal/pages/verify/transaction.tsx`
- `waltid-applications/waltid-web-portal/pages/index.tsx`
- `waltid-applications/waltid-web-portal/pages/api/env.ts`
- `waltid-applications/waltid-web-portal/next.config.js`
- `waltid-applications/waltid-web-portal/.env.example`
- `docker-compose/docker-compose.yaml`
- `helm-charts/portal/templates/configmap.yaml`
- `helm-charts/portal/values.yaml`

What changed:
- added a minimal verifier2-backed transaction demo page
- the page can create either:
  - `dc+sd-jwt`
  - `mso_mdoc`
  transaction requests
- environment wiring now properly exposes `NEXT_PUBLIC_VERIFIER2`
- stale session data is cleared when creating or editing requests

Why:
- the task asked for a simple demo UI for the verifier side
- the verifier portal was the smallest existing UI surface to extend

## Why This Branch Matches the Task

This branch does the core things the task asked for:
- enhances the shared `openid4vp-*` libraries
- implements the missing verifier2 transaction-data handling
- implements the missing wallet-side handling
- adds a simple verifier UI and wallet UI experience
- supports a recorded/demoable use case

It also matches the direction in [issue comment 4071924774](https://github.com/walt-id/waltid-identity/issues/1583#issuecomment-4071924774):
- request data is saved with the verifier session
- looked up again during presentation
- verified by `VerificationPolicy`
- format-specific for SD-JWT and mdoc

## What Is Deliberately Out of Scope
- transaction-data support for formats that do not have a clear standards-backed binding path in this repo
- a dedicated issuer UI for the mdoc issuance payload; mdoc demo issuance still uses the issuer API directly
- a committed demo video file

## Automated Verification Used

### Focused backend and library checks
```bash
./gradlew --no-build-cache \
  :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.TransactionDataUtilsTest' \
  :waltid-libraries:credentials:waltid-verification-policies2-vp:jvmTest --tests 'id.walt.policies2.vp.policies.TransactionDataHashCheckSdJwtVPPolicyTest' --tests 'id.walt.policies2.vp.policies.TransactionDataMdocVpPolicyTest' \
  :waltid-services:waltid-verifier-api2:test --tests 'id.walt.verifier2.sdjwt.IETFSdJwtVcWithDisclosureVerifier2IntegrationTest' --tests 'id.walt.verifier2.mdocs.PidBirthDateIssuerSignedIntegrityReproTest'
```

### Additional checks used during review rounds
```bash
./gradlew --no-build-cache :waltid-services:waltid-issuer-api:compileKotlin
./gradlew --no-build-cache :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'
```

### Browser-level E2E
Validated with a local Playwright harness against the branch-backed Docker stack for:
- SD-JWT transaction flow
- mdoc transaction flow

Latest successful artifact sets:
- SD-JWT: `/tmp/waltid-playwright/artifacts/2026-04-08T16-25-47.193Z`
- mdoc: `/tmp/waltid-playwright/artifacts/2026-04-08T16-26-16.190Z`

### Additional post-restack verification
After restacking the branch onto the latest task-1 head, the following focused validation was rerun to confirm the shared wallet path still compiles and the transaction-data validation path still behaves correctly:

```bash
./gradlew --no-build-cache \
  :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.TransactionDataUtilsTest' \
  :waltid-libraries:credentials:waltid-verification-policies2-vp:jvmTest --tests 'id.walt.policies2.vp.policies.TransactionDataHashCheckSdJwtVPPolicyTest' --tests 'id.walt.policies2.vp.policies.TransactionDataMdocVpPolicyTest' \
  :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'
```

## Manual Verification Guide

### Recommended stack startup
From repo root:
```bash
./gradlew --no-build-cache :waltid-services:waltid-wallet-api:jibDockerBuild :waltid-services:waltid-verifier-api2:jibDockerBuild
docker tag waltid/wallet-api:1.0.0-SNAPSHOT waltid/wallet-api:stable
docker tag waltid/verifier-api2:1.0.0-SNAPSHOT waltid/verifier-api2:stable
docker compose -f docker-compose/docker-compose.yaml build waltid-demo-wallet web-portal
docker compose -f docker-compose/docker-compose.yaml up -d postgres issuer-api verifier-api2 wallet-api waltid-demo-wallet web-portal
```

Relevant local URLs:
- wallet API: `http://localhost:7001`
- issuer API: `http://localhost:7002`
- verifier2: `http://localhost:7004`
- demo wallet: `http://localhost:7101`
- web portal: `http://localhost:7102`

### Manual SD-JWT verification
1. Open the demo wallet and create/login to a wallet.
2. Issue or claim a matching SD-JWT VC.
3. Open `http://localhost:7102/verify/transaction`.
4. Choose `dc+sd-jwt`.
5. Create a transaction verification request.
6. Open the wallet via the generated deep-link or “Open web wallet”.
7. Confirm that:
   - the wallet displays the transaction details
   - the confirmation button is authorization-oriented
   - the presentation succeeds
   - the verifier page reaches a successful final state

### Manual mdoc verification
The branch supports an end-to-end mdoc flow, but the mdoc issuance step is API-driven rather than portal-driven.

Recommended approach:
1. Start the same stack as above.
2. Issue an mdoc through the issuer API using the same kind of payload used in automated verification:
   - endpoint: `POST http://localhost:7002/openid4vc/mdoc/issue`
   - payload must include:
     - `credentialConfigurationId`
     - `issuerKey`
     - `x5Chain`
     - `mdocData`
3. Claim the returned credential offer in the demo wallet.
4. In the portal transaction page, choose `mso_mdoc`.
5. Create the transaction verification request.
6. Open it in the wallet and verify:
   - transaction details are shown
   - the presentation succeeds
   - verifier2 reaches success

### What to inspect manually
- Does the wallet show transaction details for both formats?
- Does the verifier success depend on the correct transaction binding?
- Do malformed or unsupported transaction-data cases fail closed?
- Does the demo page clear stale session/QR state when the form changes?

## Review Questions Worth Asking During Manual Review
- Is the policy-based verifier architecture the cleanest place for transaction-data checks?
- Are the supported transaction-data formats exactly the ones the repo can defend with standards-backed behavior?
- Was keeping the public validator compatibility layer the right non-breaking tradeoff?
- Are the issuer-side mdoc authorization changes narrow enough for a feature branch like this?
