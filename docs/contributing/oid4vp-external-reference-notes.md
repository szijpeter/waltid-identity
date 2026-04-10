# External Reference Notes

## Purpose
This file answers two side questions for later manual review:
- what other open source implementations are useful comparison points?
- is there any public way to verify what walt.id Enterprise already does?

## Other Open Source Repos Worth Comparing Against

## 1. OpenWallet Foundation `oid4vc-ts`
- Repo: [https://github.com/openwallet-foundation-labs/oid4vc-ts](https://github.com/openwallet-foundation-labs/oid4vc-ts)

Useful files:
- request resolution:
  - [resolve-authorization-request.ts](https://github.com/openwallet-foundation-labs/oid4vc-ts/blob/main/packages/openid4vp/src/authorization-request/resolve-authorization-request.ts)
- authorization request schema:
  - [z-authorization-request.ts](https://github.com/openwallet-foundation-labs/oid4vc-ts/blob/main/packages/openid4vp/src/authorization-request/z-authorization-request.ts)
- transaction-data schema:
  - [z-transaction-data.ts](https://github.com/openwallet-foundation-labs/oid4vc-ts/blob/main/packages/openid4vp/src/transaction-data/z-transaction-data.ts)
- transaction-data verification:
  - [verify-transaction-data.ts](https://github.com/openwallet-foundation-labs/oid4vc-ts/blob/main/packages/openid4vp/src/transaction-data/verify-transaction-data.ts)
- parsing tests:
  - [parse-authorization-request-params.test.mts](https://github.com/openwallet-foundation-labs/oid4vc-ts/blob/main/packages/openid4vp/src/authorization-request/__tests__/parse-authorization-request-params.test.mts)

Why it is useful:
- it is a modern public OpenID4VP implementation with explicit support for:
  - `dcql_query`
  - request resolution
  - transaction-data parsing and verification

How to use it as a comparison:
- compare request parsing behavior
- compare transaction-data schema validation rules
- compare omission/strictness rules

## 2. EUDI JVM OpenID4VP library
- Repo: [https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vp-kt](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vp-kt)

Useful files:
- resolver implementation:
  - [DefaultAuthorizationRequestResolver.kt](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vp-kt/blob/main/src/main/kotlin/eu/europa/ec/eudi/openid4vp/internal/request/DefaultAuthorizationRequestResolver.kt)
- public API / request model:
  - [AuthorizationRequestResolver.kt](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vp-kt/blob/main/src/main/kotlin/eu/europa/ec/eudi/openid4vp/AuthorizationRequestResolver.kt)
- spec-level constants:
  - [OpenId4VPSpec.kt](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vp-kt/blob/main/src/main/kotlin/eu/europa/ec/eudi/openid4vp/OpenId4VPSpec.kt)
- README:
  - [README.md](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vp-kt/blob/main/README.md)

Why it is useful:
- it is another Kotlin OpenID4VP implementation
- it is especially useful for comparing request-resolution and holder-facing request handling concepts

Important limitation:
- it is not a drop-in architectural match for this repo
- it is more useful as a protocol behavior reference than as a full-stack OSS wallet/verifier reference

## What These External Repos Are Good For
- validating standards interpretation
- checking whether our request parsing and validation are unusually lax or unusually strict
- checking how other projects model transaction data

## What They Are Not Good For
- proving that our service/controller layering should match theirs
- proving that our verifier policy architecture should be identical
- proving exact parity with walt.id Enterprise

## Can We Verify Against the Enterprise Stack?
Short answer: not directly, not from public artifacts alone.

## What is publicly visible
- [PR #1254](https://github.com/walt-id/waltid-identity/pull/1254) states that the v1 wallet library is used by the Enterprise Wallet
- [Issue comment 4071924774](https://github.com/walt-id/waltid-identity/issues/1583#issuecomment-4071924774) describes the intended verifier architecture for transaction data
- the OSS repo already contains the same family of shared `openid4vp-*` libraries that Enterprise is described as using

## What is not publicly visible
- the private service integration code in Enterprise
- exact private UI behavior
- exact internal deployment configuration
- exact private tests

## Best available proxy for Enterprise parity
Without private repo access, the best available proxy is:
1. match the public standards
2. match the public issue guidance
3. stay consistent with the repo’s shared protocol libraries
4. make verifier2 and wallet interoperate end to end
5. let a walt maintainer confirm or reject any remaining design deltas during review

That is exactly why the current implementation was evaluated against:
- OpenID4VP 1.0
- [Issue #1583](https://github.com/walt-id/waltid-identity/issues/1583)
- verifier2/browser E2E flows

## Practical Review Advice
If the goal is confidence during manual review, use this order:
1. Review the two walkthrough docs in this directory.
2. Check the standards links and issue comment.
3. Compare our behavior to `oid4vc-ts` for request parsing and transaction-data strictness.
4. Compare our request-resolution approach to the EUDI JVM library.
5. Treat Enterprise parity as something to confirm with walt reviewers rather than something that can be proven from public code alone.
