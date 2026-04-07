# OID4VP Progress

## Status
- [x] Create neutral steering branch
- [x] Save steering documents
- [x] Review steering pack and confirm before product work
- [x] Start OpenID4VP 1.0 OSS wallet implementation
- [x] Add tests for OSS wallet v1 flow
- [x] Update wallet UIs for v1 request handling
- [x] Start transaction-data implementation
- [x] Add verifier-side transaction-data validation
- [x] Add verifier demo UI flow
- [x] Add wallet transaction authorization UX
- [ ] Record demo outside git
- [x] Prepare feature branches via cherry-picking
- [ ] Push branches or open PRs after confirmation

## Branch Plan
- Current docs branch: `docs/oid4vp-steering`
- Feature branch 1: `feat/wallet-openid4vp-v1`
- Feature branch 2: `feat/transaction-data-support`

## Branch Heads
- `feat/wallet-openid4vp-v1` at `fe1af38cc`
- `feat/transaction-data-support` at `4e1b3e330`

## Feature Branch Status
- `feat/wallet-openid4vp-v1`
  - ready for push/PR preparation from a code and branch-hygiene perspective
  - latest branch-only commits:
    - `fe1af38cc` `refactor: simplify wallet OpenID4VP request handling`
    - `fc314d8aa` `docs: update wallet API protocol support notes`
    - `6e52a04f5` `test: cover OpenID4VP request resolution paths`
    - `e6db492f7` `feat: add OpenID4VP wallet request handling`
- `feat/transaction-data-support`
  - ready for push/PR preparation from a code and branch-hygiene perspective
  - latest branch-only commits on top of task 1:
    - `4e1b3e330` `refactor: simplify transaction data hash validation`
    - `94c7401cf` `refactor: simplify wallet OpenID4VP request handling`
    - `8af30ac50` `fix: allow SD-JWT presentations without transaction data`
    - `dc312d3d8` `fix: validate OpenID4VP transaction data requirements`
    - `a242614df` `docs: update wallet API protocol support notes`
    - `3b0611eed` `test: cover OpenID4VP request resolution paths`
    - `677a72d9f` `feat: add OpenID4VP transaction data support`

## Commit Separation
- Docs commits stay on docs branch.
- Product commits are cherry-picked onto feature branches.
- No unrelated local changes are to be staged.

## Validation Commands
- Wallet API tests:
  - `./gradlew :waltid-services:waltid-wallet-api:test`
- Verifier 2 tests:
  - `./gradlew :waltid-services:waltid-verifier-api2:test`
- Wallet UI app build scope:
  - `cd waltid-applications/waltid-web-wallet && pnpm install && pnpm -r build`
- Verifier portal build scope:
  - `cd waltid-applications/waltid-web-portal && pnpm install && pnpm build`

## Completed Validation
- Task 1 focused validation:
  - `./gradlew --no-build-cache :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`
- Task 2 focused validation:
  - `./gradlew --no-build-cache :waltid-libraries:protocols:waltid-openid4vp:jvmTest --tests 'id.walt.verifier.openid.TransactionDataUtilsTest' :waltid-libraries:credentials:waltid-digital-credentials:jvmTest --tests 'id.walt.credentials.PresentationTest.testDcSdJwtPresentationDeserializesWithoutTransactionDataFields' :waltid-services:waltid-wallet-api:test --tests 'id.walt.webwallet.service.exchange.OpenId4VpPresentationServiceTest'`

## Remaining Before Push
- Run one manual end-to-end smoke flow for task 1 against wallet-api + wallet UI.
- Run one manual end-to-end smoke flow for task 2 against verifier2 + wallet demo + portal.
- Record the transaction-data demo outside git.
- Prepare final PR descriptions from the notes file.
