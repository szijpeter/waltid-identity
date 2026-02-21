# walt.id Mobile Wallet (Phase 1)

This directory contains the new Kotlin Multiplatform mobile wallet modules:

- `wallet-model`: shared pure models and validation
- `wallet-domain`: shared use-cases and orchestration contracts
- `wallet-data`: shared repository and protocol adapter implementations
- `wallet-ui-compose`: shared UI state/navigation contracts for Compose-based apps
- `wallet-app-android`: Android host app shell
- `wallet-app-ios`: iOS host framework shell

Phase 1 focuses on core credential, DID, key, issuance, and presentation flows.

## Module architecture

- `wallet-model`: Value objects and serializable protocol/domain models with local validation.
- `wallet-domain`: Repository contracts, error model (`WalletError`), and orchestration use-cases.
- `wallet-data`: Wallet API adapters, protocol compatibility parsers, repository implementations, and secure-state abstractions.
- `wallet-ui-compose`: Shared flow state machine, navigation route model, and UI-facing state.
- `wallet-app-android`: Android host entry point for shared mobile wallet flows.
- `wallet-app-ios`: iOS host bridge for shared mobile wallet flows.

## Standards direction

- Primary presentation protocol target: OpenID4VP 1.0.
- Compatibility mode retained for current parity flows used by the existing web wallet backend.
- Issuance flow keeps a migration seam for full OpenID4VCI wallet-side operation.
- Supported credential formats in phase 1:
  - W3C VC / JWT VC
  - SD-JWT VC
  - mdoc (`mso_mdoc`)

## Additional docs

- `ARCHITECTURE.md`
- `PARITY-MATRIX.md`
