# Playwright OID4VP Harness

This branch stores local verification tooling that was used to validate the OpenID4VP 1.0 wallet work and the transaction-data follow-up.

It is intentionally kept outside the product branches and is not meant to be merged as-is.

## What it covers

- base OpenID4VP 1.0 `request_uri` flow
- alternate request shapes:
  - direct query parameters
  - inline `request` object
  - signed `request` object
- transaction-data flow:
  - `dc+sd-jwt`
  - `mso_mdoc`
- main-compatible smoke flow:
  - wallet register/login
  - DID creation
  - credential issue + claim
  - verifier2 session creation

## Assumptions

- local stack is running
- default local ports:
  - demo wallet: `http://localhost:7101`
  - wallet-api: `http://localhost:7001/wallet-api`
  - issuer-api: `http://localhost:7002`
  - verifier2: `http://localhost:7004`
- scripts target the demo wallet by default

The transaction-data script verifies feature-branch behavior. It is expected to be run against a local stack built from the relevant feature branch, not from this verification branch alone.

## Install

```bash
cd verification/playwright-oid4vp
npm install
npx playwright install chromium
```

## Run

```bash
npm run record:base
npm run record:shape -- --shape=direct
npm run record:shape -- --shape=request
npm run record:shape -- --shape=signed-request
npm run record:transaction
PRESENTATION_FORMAT=mso_mdoc npm run record:transaction
npm run record:smoke-main
```

Artifacts are written to:

```text
$HOME/.waltid-playwright-artifacts/<run-name>-<timestamp>/
```

You can override the output root with `PLAYWRIGHT_ARTIFACTS_DIR`.

Each run now stores both sides:
- wallet evidence:
  - screenshots
  - browser videos
- verifier evidence:
  - `verifier-session-info-{initial|final}.json`
  - `verifier-request-{initial|final}.txt`
  - `verifier-request-{initial|final}.meta.json`
  - `verifier-info-{initial|final}.png`

## Useful environment variables

```bash
HEADLESS=true
SLOW_MO=0
TIMEOUT_MS=120000
WALLET_BASE_URL=http://localhost:7101
WALLET_API_BASE_URL=http://localhost:7001/wallet-api
ISSUER_API_BASE_URL=http://localhost:7002
VERIFIER2_BASE_URL=http://localhost:7004
PRESENTATION_FORMAT=dc+sd-jwt
PLAYWRIGHT_ARTIFACTS_DIR=/absolute/path/for/artifacts
```

## Signed request-object note

The signed request-object script assumes verifier2 is configured with a client-id-prefix profile compatible with:

```text
x509_san_dns:verifier.example.com
```

If verifier2 is still using the default plain `verifier2` client ID, the signed-request scenario is expected to fail.

## Main-branch sanity expectations

`record:smoke-main` is the baseline check that should remain valid on `main`.

The OpenID4VP 1.0 and transaction scenarios are feature checks and may fail on `main` depending on branch state.
