# Mobile Wallet Architecture (Phase 1 / Android Fast Path)

## Goals

- Shared Kotlin implementation for Android and iOS.
- Standards-first OID4VC/OID4VP integration with compatibility seams.
- Minimal dependencies outside walt.id and kotlinx ecosystems.
- Clean separation between model/domain/data/ui layers.
- Deliver a usable Android runtime slice first; defer iOS host runtime parity.

## Layering

1. `wallet-model`
- Serializable value objects and request/response models.
- Validation utilities and strongly typed IDs.
- No platform code.

2. `wallet-domain`
- Sealed wallet error model and result wrappers.
- Repository interfaces for credentials, exchange, DIDs, keys, and security.
- Use-cases for scan handling, issuance, presentation, credential browsing, DID and key management.

3. `wallet-data`
- Wallet API adapter (`WalletBackendApi`) with Ktor implementation.
- DTO-to-domain mappers and protocol compatibility classification.
- API repository implementations used by domain use-cases.
- Secure state store abstractions (`SecureStateStore`, `StateCipher`).

4. `wallet-ui-compose`
- Shared state machine (`MobileWalletStateMachine`) for:
  - dashboard
  - scan/manual input
  - issuance
  - presentation
  - credential detail
  - DID/key settings
- UI route/state model independent of host platform.

5. Host apps
- `wallet-app-android`: Android runtime host with Compose UI shell and dependency composition root.
- `wallet-app-ios`: iOS shell bootstrap module for shared flow embedding (runtime composition deferred).

## Android fast-path composition root

- Runtime values are provided through Android BuildConfig:
  - `WALLET_BASE_URL`
  - `WALLET_ID`
  - `WALLET_BEARER_TOKEN` (optional)
- `wallet-app-android` validates runtime config at startup and fails fast on missing mandatory values.
- `MobileWalletDependencies` composes:
  - Ktor `HttpClient`
  - `KtorWalletBackendApi`
  - API repositories (`ApiCredentialRepository`, `ApiDidRepository`, `ApiKeyRepository`, `ApiExchangeRepository`)
  - shared use-cases
  - `MobileWalletStateMachine`
- `MainActivity` is a thin route-driven host that binds Compose actions to shared state-machine methods.

## Protocol strategy

- OpenID4VP 1.0 path where available.
- Draft compatibility retained to interoperate with current web wallet API behavior.
- Issuance flow resolves offer metadata and types using wallet API parity endpoints.

## Security strategy

- Security repository abstraction for biometric gate checks and integrity signals.
- Encrypted state persistence abstraction with pluggable cipher (no-op default, host override expected).

## Test strategy

- `wallet-model`: validation tests.
- `wallet-domain`: use-case behavior tests.
- `wallet-data`: contract and parser tests with fake backend API.
- `wallet-ui-compose`: state machine flow tests.
- Android fast-path currently uses manual acceptance gates; additional automated tests are deferred to follow-up.
