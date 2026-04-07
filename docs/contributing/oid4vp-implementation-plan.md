# OID4VP Implementation Plan

## Phase 1: OSS Wallet OpenID4VP 1.0 Support

### Backend
- Add the required v1 wallet library dependencies to `waltid-wallet-api`.
- Introduce protocol-aware request handling in `SSIKit2WalletService`:
  - keep draft requests on the existing `id.walt.oid4vc` path
  - route v1 requests to `waltid-openid4vp-wallet`
- Add a unified request-resolution path in `ExchangeController` so the wallet UI can resolve both draft and v1 presentation requests.
- Add a unified credential-matching path for full presentation requests, not only raw Presentation Definitions.
- Implement credential selection for `dcql_query` from wallet storage using the shared digital-credential/DCQL types.
- Preserve operation-history logging and extend it to the v1 path.

### UI
- Update `libs/composables/presentation.ts` to detect and handle:
  - legacy `presentation_definition`
  - v1 `dcql_query`
- Keep current user flow intact for draft requests.
- For v1 requests, render the resolved verifier host and matched credentials without assuming a Presentation Definition JSON blob exists.
- Touch both demo and dev wallet pages only as needed to support the new composable contract.

### Tests
- Add wallet-api tests for resolving verifier2-generated v1 requests.
- Add wallet-api tests for matching credentials from `dcql_query`.
- Add wallet-api tests for successful v1 presentation submission.
- Keep draft regression coverage intact.
- Update integration helpers that currently hardcode `presentation_definition=` so they can validate either draft or v1 expectations.

## Phase 2: Transaction Data Support

### Shared v1 Libraries
- Add decoding and validation helpers for `transaction_data` objects.
- Enforce the spec behavior for unsupported transaction-data types and malformed values.
- Add response-side generation of:
  - `transaction_data_hashes`
  - `transaction_data_hashes_alg`

### Verifier 2
- Validate the returned transaction-data binding before final session success.
- Add focused tests around:
  - session creation with `transactionData`
  - successful bound response
  - failed response where transaction-data binding is missing or invalid

### Wallet UI
- Decode and display transaction details before consent.
- Make the action wording reflect transaction authorization, not just credential presentation.

### Verifier Demo
- Extend `waltid-web-portal` with a minimal verifier2-backed flow that:
  - creates a verification session with `dcql_query`
  - includes one concrete `transaction_data` payload
  - renders the QR/deep-link
  - follows the result to success/failure

### Demo Recording
- Record a short end-to-end run outside git.
- Attach it later when preparing the final review material.

## Delivery Sequence
1. Steering docs and checkpoint
2. Phase 1 implementation
3. Phase 1 local validation
4. Phase 2 implementation
5. Phase 2 local validation
6. Prepare feature branches via cherry-pick
7. Push or open PRs only after confirmation
