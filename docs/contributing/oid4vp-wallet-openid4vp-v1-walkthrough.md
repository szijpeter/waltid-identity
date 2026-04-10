# OpenID4VP 1.0 Wallet Support Walkthrough

## Branch and PR
- Branch: `feat/wallet-openid4vp-v1`
- Fork PR: [https://github.com/szijpeter/waltid-identity/pull/3](https://github.com/szijpeter/waltid-identity/pull/3)

## Task and Intent
The goal of this branch is to add OpenID4VP 1.0 holder support to the OSS `wallet-api` and wallet UI without breaking the older draft-based presentation flow that the repo already had.

The original request was:
- keep the existing draft protocol support in place
- add support for the newer `openid4vp-holder` library / v1 holder flow
- make the OSS wallet work with the OSS `verifier-api2`, which already speaks OpenID4VP 1.0
- update the wallet UI if needed

In practice, that means the OSS wallet has to understand both of these request families side by side:
- draft-oriented requests centered on `presentation_definition`
- 1.0-oriented requests centered on `dcql_query`, Request Objects, `request_uri`, and `response_uri`

## Standards and Reference Material

### Primary standards
- OpenID for Verifiable Presentations 1.0:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0.html](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)
- OpenID for Verifiable Presentations draft 20:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0-20.html](https://openid.net/specs/openid-4-verifiable-presentations-1_0-20.html)

### Relevant spec delta for this branch
- Draft-oriented flows commonly use `presentation_definition` / `presentation_definition_uri` and `presentation_submission`.
- OpenID4VP 1.0 adds the final `dcql_query` model, Request Objects, `request_uri`, `request_uri_method`, and `response_uri` / `direct_post`.
- That is the key reason this branch adds a parallel v1 path instead of rewriting the existing draft path.

The sections that mattered most during implementation were:
- OpenID4VP 1.0, general request model and authorization request parameters
- OpenID4VP 1.0, Request Object / `request_uri` handling
- OpenID4VP 1.0, `response_uri` and `direct_post`
- OpenID4VP draft 20, legacy `presentation_definition` request shape

### Upstream repo references
- `waltid-openid4vp-wallet` was already added publicly in [PR #1254](https://github.com/walt-id/waltid-identity/pull/1254)
- OSS verifier2/OpenID4VP 1.0 work landed in [PR #1274](https://github.com/walt-id/waltid-identity/pull/1274)
- additional wallet-side v1 support landed in [PR #1492](https://github.com/walt-id/waltid-identity/pull/1492)

Those upstream PRs are important because this branch is not inventing a new protocol implementation; it is wiring existing OSS v1 libraries into the OSS wallet service and UI.

## What the Repo Already Had Before This Branch

### In the wallet service
Before this branch, the OSS wallet path in `waltid-services/waltid-wallet-api` was still effectively draft-oriented:
- it used `id.walt.oid4vc` request parsing and presentation processing
- it assumed a `presentation_definition`-based request shape
- it did not yet route OpenID4VP 1.0 requests into `waltid-openid4vp-wallet`

Relevant existing files:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/SSIKit2WalletService.kt`
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/web/controllers/exchange/ExchangeController.kt`

## Notable review-driven hardening after the initial implementation
The branch moved beyond the first feature commit during fork review rounds. The most important follow-up changes were:
- shared request resolution in `waltid-openid4vp-wallet` so wallet-api and wallet library use the same `request` / `request_uri` logic
- shared authorization request parameter parsing and encoding so plain scalar query parameters stay plain scalars while structured JSON values stay JSON
- stricter wallet-api submission handling so only successful v1 submissions log `Credential.Present`
- clearer request matching/submission flow in wallet-api and the web wallet composable

Those changes did not change the intended feature scope; they made the final branch more correct and more internally consistent with the repo's protocol handling.

### In the shared libraries
The repo already had:
- `waltid-libraries/protocols/waltid-openid4vp`
- `waltid-libraries/protocols/waltid-openid4vp-wallet`
- `waltid-libraries/protocols/waltid-openid4vp-clientidprefix`

So the v1 holder implementation already existed, but the OSS wallet service did not yet consume it.

### In the wallet UI
The wallet UI already had a presentation page, but it assumed the resolved request would expose a `presentation_definition` and legacy matching semantics:
- `waltid-applications/waltid-web-wallet/libs/composables/presentation.ts`
- `waltid-applications/waltid-web-wallet/apps/waltid-demo-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`
- `waltid-applications/waltid-web-wallet/apps/waltid-dev-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`

### In verifier2
The OSS repo already had a v1 verifier service:
- `waltid-services/waltid-verifier-api2`

That meant the main missing piece was wallet-side support for the requests verifier2 already generated.

## What Changed

## High-level design
This branch adds a protocol-aware split in the OSS wallet:
- if the request is a v1/OpenID4VP request, resolve it via the v1 library path and match credentials against `dcql_query`
- otherwise keep using the existing draft path

That design was chosen because it is the smallest change that:
- preserves draft compatibility
- unlocks verifier2 interoperability
- stays consistent with the repo’s existing split between `openid4vc` and `openid4vp` libraries

## Most Important Review-driven Hardening
The initial feature implementation was correct in broad shape, but the fork review rounds surfaced a few places where the final branch is now materially better:
- request resolution is now shared between wallet-api and the v1 wallet library instead of duplicated
- query-parameter parsing/encoding now follows one explicit rule everywhere:
  - plain scalar request parameters stay plain strings
  - only explicitly JSON-encoded values are parsed as JSON
- signed Request Objects are preserved through resolve and final submit instead of being flattened into loose query parameters
- wallet event logging now only records actual successful presentations
- the final review pass restored a narrow legacy draft fallback for signed request-object flows that still use plain `http` / `https` client IDs in the old suites, without weakening strict handling of real verifier2/v1 requests

That last point is worth calling out because it reflects the eventual intended layering:
- strict v1 requests should still fail hard if request resolution or signed request-object validation fails
- only the legacy draft compatibility edge gets the fallback

SD-JWT note for reviewers:
- The holder-key-binding failure (`signature_sd-jwt-vc`) seen in some legacy runs is an interop issue in the stack, not a spec-level ban on SD-JWT.
- The wallet-side binding fix is tracked separately in follow-up branch `feat/wallet-openid4vp-holder-binding-fix` (fork PR [#5](https://github.com/szijpeter/waltid-identity/pull/5)) so PR1 can stay in-scope.
- Related upstream issue history:
  - [https://github.com/walt-id/waltid-identity/issues/713](https://github.com/walt-id/waltid-identity/issues/713)
  - [https://github.com/walt-id/waltid-identity/issues/1272](https://github.com/walt-id/waltid-identity/issues/1272)

## File-by-file walkthrough

### 1. Wallet API dependencies
File:
- `waltid-services/waltid-wallet-api/build.gradle.kts`

Why:
- the wallet service needs the v1 holder library and related protocol modules on its classpath

What changed:
- added the required `waltid-openid4vp-wallet` / v1 dependency wiring

Why this matters:
- without this, the OSS wallet service cannot call the shared v1 holder code that already exists elsewhere in the repo

### 2. New OpenID4VP-specific wallet service
File:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationService.kt`

This is the main new service introduced by the branch.

What it does:
- detects and resolves OpenID4VP 1.0 presentation requests
- supports:
  - inline authorization-request parameters
  - `request`
  - `request_uri`
  - `request_uri_method`
- supports signed Request Objects with client-id-prefix validation
- resolves requests into the shared `AuthorizationRequest` model from `waltid-openid4vp`
- matches wallet credentials against `dcql_query`
- validates request-side transaction-data constraints via the shared utility layer

Why it exists:
- the old wallet-api flow was tightly coupled to draft/OpenID4VC parsing
- v1 request handling has enough different semantics that it is clearer and safer to keep it in a dedicated service instead of sprinkling conditional parsing throughout the legacy path

Important implementation points:
- `resolveAuthorizationRequestFromRequestUri(...)`
  - handles both JWT Request Objects and JSON authorization requests based on content type
- `resolveAuthorizationRequestFromRequestObject(...)`
  - authenticates signed request objects through `ClientIdPrefixAuthenticator`
- `matchCredentialsForPresentationRequest(...)`
  - matches against `dcql_query`, not DIF `presentation_definition`
- `parseAuthorizationRequestParameters(...)`
  - now preserves raw scalar query parameters as strings unless the value is actually JSON-encoded

That last point was added later during review because OpenID parameter scalars like `nonce` and `state` must not be turned into JSON booleans/numbers by accident.

### 3. Wallet service routing
File:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/SSIKit2WalletService.kt`

What changed:
- added protocol-aware branching:
  - draft path stays on the existing `openid4vc` wallet flow
  - v1 path goes through `OpenId4VpPresentationService` and `WalletPresentFunctionality2`
- added `resolvePresentationRequest(...)` support for both draft and v1
- added v1 credential matching and submission
- ensured presentation event logging still happens on the v1 path
- later hardened the v1 path so:
  - failed `transmissionSuccess == false` is treated as failure
  - only actually matched credentials are logged as presented
  - `response_mode=form_post` is rejected explicitly because wallet-api does not support returning HTML form-post content to the UI

Why:
- this is the main OSS wallet integration point
- keeping the split here makes the overall service easy to reason about during review

### 4. Wallet service interface and controller endpoints
Files:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/WalletService.kt`
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/web/controllers/exchange/ExchangeController.kt`
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/web/controllers/exchange/openapi/ExchangeDocs.kt`

What changed:
- exposed a request-oriented matching endpoint that can accept the full presentation request, not just a raw Presentation Definition
- kept the legacy endpoints in place
- mapped new v1-specific failures to clearer client-facing errors
- updated the OpenAPI docs accordingly

Why:
- the old matching API was structurally tied to `presentation_definition`
- for OpenID4VP 1.0, the wallet needs to match against `dcql_query`

### 5. Event logging
File:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/usecase/event/EventLogUseCase.kt`

What changed:
- adjusted event logging support so the v1 path can emit the same `Credential.Present` history entries with verifier metadata

Why:
- the feature should not regress wallet activity/history behavior just because the protocol route changed

### 6. Wallet UI composable
File:
- `waltid-applications/waltid-web-wallet/libs/composables/presentation.ts`

What changed:
- resolves presentation requests through wallet-api before rendering
- distinguishes between:
  - legacy `presentation_definition`
  - v1 `dcql_query`
- calls the correct backend matching route for each
- on the v1 path, reuses the already resolved request for matching and submission instead of sending the original `request_uri` payload again
- validates that the resolved request actually contains the expected payload (`presentation_definition` or `dcql_query`)

Why:
- this is the UI bridge between the raw verifier request and the wallet-api behavior
- reusing the resolved request avoids re-dereferencing one-time or short-lived `request_uri` endpoints

### 7. Wallet presentation pages
Files:
- `waltid-applications/waltid-web-wallet/apps/waltid-demo-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`
- `waltid-applications/waltid-web-wallet/apps/waltid-dev-wallet/src/pages/wallet/[wallet]/exchange/presentation.vue`

What changed:
- minimal updates so both pages can work with the new composable contract and v1 request shape

Why:
- task 1 asked for UI updates if necessary
- the goal here was to touch the pages lightly and keep the existing UX intact

### 8. README and tests
Files:
- `waltid-services/waltid-wallet-api/README.md`
- `waltid-services/waltid-wallet-api/src/test/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationServiceTest.kt`

What changed:
- README no longer incorrectly says OpenID4VP 1.0 is unsupported
- added focused tests for:
  - request resolution
  - Request Object / `request_uri` handling
  - DCQL matching
  - preserving scalar query parameters as strings

Why:
- the README was stale after the implementation landed
- this feature needs direct tests because the interoperability risk is mostly in request parsing and routing, not deep business logic

## Why These Changes Match the Task

This branch does what the task asked for:
- it adds support for the `openid4vp-holder` / v1 holder flow to the OSS wallet service
- it keeps the old draft flow intact
- it updates the wallet UI only where needed

It also matches the repo’s direction:
- verifier2 already used the v1 libraries
- this branch brings the OSS wallet service up to the same protocol family rather than inventing a parallel custom implementation

## Request and Data Flows

The main flows in this branch are request resolution, credential matching, and presentation submission.

### 1. Request resolution flow
The wallet starts from an incoming OpenID4VP request URL, usually via the shared wallet UI composable:
- `waltid-applications/waltid-web-wallet/libs/composables/presentation.ts`

That request is sent to wallet-api, which routes it through:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/SSIKit2WalletService.kt`
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationService.kt`

The actual protocol parsing and request retrieval live in:
- `waltid-libraries/protocols/waltid-openid4vp-wallet/src/commonMain/kotlin/id/waltid/openid4vp/wallet/AuthorizationRequestResolver.kt`

That resolver handles three request shapes:
- direct authorization-request query parameters
- inline `request` Request Objects
- `request_uri`, including `request_uri_method=post`

Relevant OpenID4VP 1.0 references:
- request parameters and authorization-request structure:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5)
- Request Objects and `request_uri`:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5.10.1](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5.10.1)
- `request_uri_method=post` retrieval requirements:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-request-uri-method-post](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-request-uri-method-post)

Important behavior:
- if the request uses a signed Request Object, the branch now preserves the original `request` JWT instead of flattening it into plain query parameters
- that matters because signature and client-id-prefix validation only make sense if the original Request Object survives the flow

### 2. Authorization-request parameter encoding and decoding
A subtle but important flow in this branch is how authorization-request parameters move between URL form and typed model form.

Parsing happens in:
- `waltid-libraries/protocols/waltid-openid4vp-wallet/src/commonMain/kotlin/id/waltid/openid4vp/wallet/AuthorizationRequestParameterCodec.kt`
- used by `AuthorizationRequestResolver.kt`

Encoding back into the normalized wallet URL happens in:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationService.kt`

The convention is:
- plain scalar query parameters stay plain strings
- only explicitly JSON-encoded values are parsed as JSON

That means fields like:
- `client_id`
- `state`
- `nonce`
- `response_uri`

stay protocol strings even if they look like JSON scalars such as `true` or `12345`.

Structured values like:
- `dcql_query={...}`
- `client_metadata={...}`

still round-trip as JSON.

This was an important review-driven hardening step because blindly JSON-parsing every parameter can accidentally re-type protocol strings into booleans or numbers.

### 3. Frontend interpretation flow
After wallet-api resolves the request, the wallet UI still needs to inspect it to decide whether it is handling:
- a legacy draft `presentation_definition` flow, or
- an OpenID4VP 1.0 `dcql_query` flow

That logic lives in:
- `waltid-applications/waltid-web-wallet/libs/composables/presentation.ts`

Because the normalized request may now preserve a signed `request` JWT, the frontend extracts parameters from either:
- direct query parameters, or
- the JWT payload inside `request`

That keeps the UI working for:
- unsigned direct-parameter requests
- `request_uri` requests that resolve to JSON
- signed Request Objects preserved as `request=<jwt>`

### 4. Credential matching flow
Once the request is resolved, wallet-api matches local wallet credentials against the OpenID4VP 1.0 `dcql_query`.

That logic lives in:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/exchange/OpenId4VpPresentationService.kt`

The flow is:
1. load wallet credentials from storage
2. convert them into `RawDcqlCredential`
3. run `DcqlMatcher.match(...)`
4. map the matched DCQL credentials back to the real wallet credentials by ID

This is intentionally not delegated to `DcqlHolderPolicyCheck`, because wallet-api needs:
- the exact wallet credentials that matched
- the real wallet credential IDs
- per-query match results

not just a boolean “policy passed” answer.

Relevant standard reference:
- DCQL-based request model in OpenID4VP 1.0:
  - [https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6)

### 5. Presentation submission flow
When the user accepts, the frontend posts the resolved request and selected credential IDs back to wallet-api:
- `waltid-applications/waltid-web-wallet/libs/composables/presentation.ts`

wallet-api then hands the flow to the v1 holder library through:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/SSIKit2WalletService.kt`
- `waltid-libraries/protocols/waltid-openid4vp-wallet/src/commonMain/kotlin/id/waltid/openid4vp/wallet/WalletPresentFunctionality2.kt`

Important detail:
- if the original verifier request was signed, the final submission path still uses the original `request` JWT
- that prevents a client-side tamper between resolve and submit from changing fields like `response_uri`

This was one of the most important correctness fixes added during review.

### 6. Draft vs v1 routing flow
The branch keeps both protocol families alive side by side:
- draft flow continues to use `presentation_definition`
- OpenID4VP 1.0 flow uses `dcql_query` and the newer v1 holder library

The protocol-selection logic is centered in:
- `waltid-services/waltid-wallet-api/src/main/kotlin/id/walt/webwallet/service/SSIKit2WalletService.kt`

Requests that clearly look like OpenID4VP 1.0 are routed into the v1 path; legacy requests continue through the old draft path.

This matches the task requirement to support draft protocols and OpenID4VP 1.0 alongside each other rather than replacing one with the other.

One subtle point discovered during readiness work:
- `request_uri` is a strong v1 signal, but some legacy draft suites still reach wallet-api through signed request-object handling and use plain `http` / `https` client IDs
- wallet-api now falls back only for the specific `UnsupportedPrefix(http|https)` failure that comes from signed request-object validation
- this keeps verifier2/OpenID4VP 1.0 strict while preserving old draft compatibility

## What This Branch Does Not Try To Do
- it does not remove the draft flow
- it does not implement `transaction_data`
- it does not change verifier2 behavior
- it does not add an entirely new wallet UX

That separation is intentional so PR 1 stays focused and reviewable.

## Automated Verification Used

### Focused backend test
```bash
./gradlew --no-build-cache :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'
```

### Current readiness reruns on the latest branch head
```bash
./gradlew --no-daemon --max-workers=3 --rerun-tasks :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'
./gradlew --no-daemon --max-workers=3 --rerun-tasks :waltid-services:waltid-integration-tests:test :waltid-services:waltid-e2e-tests:test
./gradlew clean build cleanAllTests allTests --rerun-tasks --no-daemon --max-workers=3
```

Readiness result:
- focused wallet-api suite passed
- legacy integration and e2e suites passed
- the full repo CI-like run still fails in unrelated JS-node test `VcApiTest.testVcApi[js, node]` under `waltid-libraries/credentials/waltid-w3c-credentials`

### Browser E2E
The branch was also validated by:
- rebuilding the wallet-api image with `jibDockerBuild`
- rebuilding the wallet web apps in Docker
- running the local compose stack
- driving browser E2E scenarios against verifier2 and the wallet UI

Current successful task-1 browser artifacts:
- base verifier2 `request_uri` flow:
  - `/tmp/waltid-playwright/artifacts/base-2026-04-09T19-22-47.927Z`
- direct query-parameter flow:
  - `/tmp/waltid-playwright/artifacts/direct-2026-04-09T19-22-47.927Z`
- inline `request` Request Object flow:
  - `/tmp/waltid-playwright/artifacts/request-2026-04-09T19-22-47.928Z`
- signed request-object flow:
  - `/tmp/waltid-playwright/artifacts/signed-request-2026-04-09T19-31-56.517Z`

Important note about the signed-request artifact:
- the verifier2 temp config must use `clientId: "x509_san_dns:verifier.example.com"` for that scenario
- if verifier2 is running with its default `clientId: "verifier2"` profile, the wallet correctly produces a KB-JWT audience for `x509_san_dns:verifier.example.com`, and verifier2 correctly rejects that as an audience mismatch
- that is a harness/config alignment issue, not a production bug in this branch

## Manual Verification Guide

### Goal
Verify that the OSS wallet can successfully handle a verifier2/OpenID4VP 1.0 request that uses `dcql_query` and `request_uri`.

### Recommended local setup
1. Start Docker Desktop.
2. From repo root, build the service images:
   ```bash
   ./gradlew --no-build-cache :waltid-services:waltid-wallet-api:jibDockerBuild :waltid-services:waltid-verifier-api2:jibDockerBuild
   docker tag waltid/wallet-api:1.0.0-SNAPSHOT waltid/wallet-api:stable
   docker tag waltid/verifier-api2:1.0.0-SNAPSHOT waltid/verifier-api2:stable
   ```
3. Build the demo wallet:
   ```bash
   docker compose -f docker-compose/docker-compose.yaml build waltid-demo-wallet
   ```
4. Start the relevant stack:
   ```bash
   docker compose -f docker-compose/docker-compose.yaml up -d postgres issuer-api verifier-api2 wallet-api waltid-demo-wallet
   ```

### Manual smoke options

#### Option A: reuse the task-2 transaction demo page
This is the easiest practical smoke because it exercises the same OpenID4VP 1.0 wallet path. See the task-2 walkthrough for the portal flow.

#### Option B: create a bare verifier2 session manually
1. Create a wallet account and obtain a matching credential in the demo wallet.
2. Create a verifier2 session whose request contains:
   - `dcql_query`
   - `request_uri`
   - `response_uri`
3. Open the returned `openid4vp://authorize?...request_uri=...` deep link in the web wallet.
4. Confirm that:
   - the wallet page loads
   - a matching credential is shown
   - the presentation succeeds
   - verifier2 marks the session as successful

#### Option C: signed request-object smoke
1. Start verifier2 with a client-id-prefix profile, for example:
   - `clientId: "x509_san_dns:verifier.example.com"`
2. Create a verifier2 session and fetch its authorization request.
3. Wrap the authorization request into an ES256-signed Request Object with:
   - `typ: "oauth-authz-req+jwt"`
   - an `x5c` chain whose SAN DNS matches `verifier.example.com`
4. Open the resulting `openid4vp://authorize?request=<jwt>` link in the wallet.
5. Confirm that:
   - wallet-api resolves the Request Object successfully
   - the wallet displays the matching credential
   - the presentation succeeds
   - verifier2 accepts the KB-JWT audience and marks the session successful

### What to specifically watch for
- the wallet should not fail on JSON `request_uri` responses from verifier2
- the wallet should not fall back to draft parsing for a real v1 request
- the request should be matched against `dcql_query`, not `presentation_definition`
- successful presentations should show up in the wallet event/history log
- old draft signed request-object flows that still use plain `http` / `https` client IDs should still work through the legacy path

## Key Learnings
- Preserving signed Request Objects end to end was the right correctness tradeoff even though it made the frontend request-inspection path a little heavier.
- The real protocol boundary is between:
  - semantic request resolution and matching in wallet-api / shared v1 libraries
  - display-only interpretation in the web-wallet composable
- The final compatibility fix belongs in wallet-api routing, not in the shared v1 library:
  - the v1 library should stay strict
  - wallet-api is the right place to decide when to fall back to the draft path
- The local Playwright harness is useful enough to keep, but it should probably live on a separate verification branch if we want it preserved in git without turning it into product code.

## Review Questions Worth Asking During Manual Review
- Is the draft/v1 split placed in the right layer, or should more of it live deeper in the shared libraries?
- Are the v1 request parsing rules strict enough without being incompatible with verifier2?
- Is the controller/API surface still readable after adding the request-oriented matching path?
- Do the UI changes stay minimal and consistent with the rest of the web wallet?
