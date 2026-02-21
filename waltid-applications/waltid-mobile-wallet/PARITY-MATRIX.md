# Phase 1 Parity Matrix

| Capability | Web Wallet Parity | Mobile Wallet Phase 1 Status | Notes |
|---|---|---|---|
| Credential dashboard list | Required | Implemented | Shared state machine + repository mapping |
| Credential detail view model | Required | Implemented | Detail use-case + model mappers |
| Scan/manual request intake | Required | Implemented | `HandleScannedRequestUseCase` + UI state |
| Issuance resolve/accept | Required | Implemented | Wallet API compatibility endpoints |
| Presentation resolve/submit | Required | Implemented | Includes selected credential IDs + disclosures |
| SD disclosure selection model | Required | Implemented | `PresentationSelection.disclosures` |
| DID list/create/import/delete/set-default | Required | Implemented | Phase 1 methods: `did:key`, `did:jwk`, `did:web` |
| Key list/generate/import/export/sign/verify | Required | Implemented | API-backed repository contracts |
| Security settings model | Required | Implemented | Repository abstraction + in-memory baseline |
| Biometric gating hook | Required | Implemented (abstraction) | Host implementation to provide concrete biometric UX |
| Encrypted local state | Required | Implemented (abstraction) | `EncryptedSecureStateStore` + pluggable cipher |
| Shared module JS target | Deferred | Not implemented | Temporarily disabled for phase-1 reliability; mobile targets remain primary |
| OpenID4VP 1.0 preference | Required | Partial | Standards-first classifier and compatibility fallback |
| NFT/web3 screens | Deferred | Not implemented | Out of phase-1 scope |
| Eventlog/history/reporting parity | Deferred | Not implemented | Out of phase-1 scope |
| Advanced DID methods (`cheqd`, `ebsi`, `iota`) | Deferred | Rejected by phase-1 use-case guard | Migration path kept in model enums |
