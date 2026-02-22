# walt.id Mobile Wallet (Phase 1 / Fast Path)

This directory contains the new Kotlin Multiplatform mobile wallet modules:

- `wallet-model`: shared pure models and validation
- `wallet-domain`: shared use-cases and orchestration contracts
- `wallet-data`: shared repository and protocol adapter implementations
- `wallet-ui-compose`: shared UI state/navigation contracts for Compose-based apps
- `wallet-app-android`: Android host app (fast-path delivery target)
- `wallet-app-ios`: iOS host framework shell (deferred runtime parity)

Phase 1 focuses on core credential, DID, key, issuance, and presentation flows.

## Current delivery status

- Shared core (`wallet-model`, `wallet-domain`, `wallet-data`, `wallet-ui-compose`) is implemented.
- Android host is now the active fast-path integration target and is being wired for real backend runtime operation.
- iOS host runtime parity is deferred until Android fast-path manual acceptance is complete.
- Automated test expansion is deferred in this cycle; manual acceptance scenarios are tracked in `.ai/mobile-wallet/STATUS.yaml`.

## Module architecture

- `wallet-model`: Value objects and serializable protocol/domain models with local validation.
- `wallet-domain`: Repository contracts, error model (`WalletError`), and orchestration use-cases.
- `wallet-data`: Wallet API adapters, protocol compatibility parsers, repository implementations, and secure-state abstractions.
- `wallet-ui-compose`: Shared flow state machine, navigation route model, and UI-facing state.
- `wallet-app-android`: Android host entry point and runtime composition root for fast-path delivery.
- `wallet-app-ios`: iOS host bridge for shared mobile wallet flows (runtime wiring deferred in this cycle).

## Standards direction

- Primary presentation protocol target: OpenID4VP 1.0.
- Compatibility mode retained for current parity flows used by the existing web wallet backend.
- Issuance flow keeps a migration seam for full OpenID4VCI wallet-side operation.
- Supported credential formats in phase 1:
  - W3C VC / JWT VC
  - SD-JWT VC
  - mdoc (`mso_mdoc`)

## Build targets

- Shared mobile-wallet modules currently target JVM (and iOS when `enableIosBuild=true`).
- JavaScript targets are temporarily disabled for `wallet-model`, `wallet-domain`, `wallet-data`, and `wallet-ui-compose` to keep mobile-wallet builds reliable in phase 1.

## Additional docs

- `ARCHITECTURE.md`
- `PARITY-MATRIX.md`
