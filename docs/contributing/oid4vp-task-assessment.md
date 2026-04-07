# OID4VP Task Assessment

## Scope Readout

### Task 1
- Add support for the `openid4vp-holder` library to the existing wallet service.
- In this repo that corresponds to integrating the OSS wallet path with `waltid-openid4vp-wallet`, which already exists but is not wired into `waltid-wallet-api`.
- Support must coexist with the current draft protocol handling based on `waltid-openid4vc`.
- Wallet UI updates are likely required because the current OSS UI only knows how to render `presentation_definition`-driven requests.

### Task 2
- Complete `transaction_data` support by finishing the shared v1 libraries and wiring the feature through `verifier-api2` and the updated wallet flow.
- Add a simple verifier and wallet demo flow.
- Record a demo outside git.

## What Is Already Present
- `waltid-openid4vp-wallet` provides `WalletPresentFunctionality2` for v1 request handling.
- `verifier-api2` already creates v1 authorization requests and can include `transactionData` in session setup.
- The v1 request model in `waltid-openid4vp` already includes `transaction_data`.
- `transaction_data` does not appear to be consumed in the OSS wallet flow yet.
- There is no implementation yet for response-side transaction-data binding in the v1 wallet library.

## What Is Missing

### For Task 1
- OSS `wallet-api` does not currently branch between draft and v1 presentation flows.
- OSS credential matching is still DIF Presentation Definition specific.
- OSS wallet UI does not handle `dcql_query`.
- Existing integration helpers assert `presentation_definition=` in resolved requests.

### For Task 2
- Shared wallet-side support for `transaction_data` parsing and validation.
- Response-side `transaction_data_hashes` and `transaction_data_hashes_alg`.
- Verifier-side validation of transaction-data binding.
- Minimal demo UI on the verifier side.
- Wallet-side UI rendering of transaction details before consent.

## Upstream Research Notes
- PR `#1254`
  - Introduced `waltid-openid4vp-wallet` and explicitly notes it is used by the Enterprise Wallet.
  - Strong evidence that OSS wallet integration is the intended follow-up.
- PR `#1274`
  - Added verifier2 compliance work and v1 protocol plumbing in OSS.
  - Good source for expected v1 request behavior and test style.
- PR `#1492`
  - Added more wallet-side request handling such as signed request support, client-id prefixes, encrypted responses, and wallet presentation result modeling.
  - Useful as a baseline for what the v1 wallet library already supports.
- Issue `#1583`
  - Still open and labeled `On hold`.
  - Confirms public awareness that `transaction_data` is only partially wired today.
  - No linked branches or PRs, so there is no public in-flight OSS fix to align with.

## Feasibility Assessment
- Task 1 is feasible as an OSS integration task, not a greenfield protocol build.
- Task 2 is feasible, but it is broader and should explicitly build on Task 1's new OSS v1 wallet path.
- The cleanest incremental split is:
  - PR 1: OSS wallet support for OpenID4VP 1.0
  - PR 2: `transaction_data` support plus demo additions

## Main Risks
- Breaking existing draft flows while adding v1 routing.
- Over-coupling new v1 matching to old presentation-definition APIs.
- Letting UI assumptions about `presentation_definition` leak into the v1 path.
- Implementing only UI rendering for `transaction_data` without response binding and verifier validation.
- Allowing docs/progress commits to leak into later feature branches.

## Recommended Defaults
- Keep public HTTP endpoint names stable where possible and add v1-aware behavior behind them.
- Add new unified wallet matching endpoints only where the old presentation-definition contract is too constraining.
- Build the verifier demo in `waltid-web-portal`, not a brand-new demo app.
- Start `transaction_data` with one simple concrete type for the demo while keeping the library shape extensible.
