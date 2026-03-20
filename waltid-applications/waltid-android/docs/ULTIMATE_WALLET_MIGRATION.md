# Ultimate Wallet Migration (Fork-First)

## Goal
Evolve `waltid-android` into the ultimate mobile wallet while staying **walt-libraries-first**:
- No parallel protocol stack.
- No custom domain model layer duplicating walt models.
- Reuse `waltid-core-wallet`, `waltid-did`, `waltid-crypto-*`, and OpenID libraries directly.

## What This First Migration Slice Delivers
- New wallet dashboard UI shell (tabbed exchange/DID/key/history/settings layout).
- Android deep-link ingestion for OpenID offer/presentation schemes.
- DID and key actions wired directly to walt libraries:
  - `DidService`
  - `AndroidKey`
  - `CoreWalletOpenId4VCI` (offer resolution)

This slice ports the most useful UI interaction patterns from the prior KMP prototype into the upstream fork app without introducing a second backend/protocol implementation.

## Next Migration Steps
1. Add full OpenID4VCI issuance flow using `CoreWalletOpenId4VCI` end-to-end.
2. Add full OpenID4VP flow using `waltid-openid4vp-wallet` / `CoreWalletOpenId4VP`.
3. Replace in-memory UI state with wallet persistence driven by walt wallet data models/services.
4. Build iOS companion app module under `waltid-applications` that reuses the same walt library flow definitions.
5. Align screen parity to `waltid-web-wallet` exchange/settings/history behavior.
