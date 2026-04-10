# Playwright OID4VP Harness

This harness records full browser-driven OpenID4VP sessions with both sides captured:

- verifier portal browser session
- wallet portal browser session

It is intentionally kept outside product branches and is not meant to be merged directly.

Run the target stack from the branch you want to validate (`main`, PR1, PR2, etc.). The harness only drives browser/API flows; it does not force a specific product branch revision.

Bring up the local stack with the harness override:

```bash
docker compose \
  -f docker-compose/docker-compose.yaml \
  -f verification/playwright-oid4vp/docker-compose.override.yaml \
  up -d --build
```

## Scenario model

- `legacy` scenario:
  - verifier portal route: `/verify`
  - backend: legacy verifier (`NEXT_PUBLIC_VERIFIER`, default `http://localhost:7003`)
  - main-compatible baseline
- `verifier2-portal` scenario:
  - verifier portal route: `/verify/transaction`
  - backend: verifier2 (`NEXT_PUBLIC_VERIFIER2`, default `http://localhost:7004`)
  - format selectable with `PRESENTATION_FORMAT=dc+sd-jwt|mso_mdoc`
- `verifier2-api` scenario:
  - no portal dependency
  - creates verifier2 sessions via `/verification-session/create`
  - drives wallet presentation with one request shape at a time:
    - `direct`
    - `request_uri_get`
    - `request_object_unsigned`
    - `request_object_signed`
    - optional probe: `request_uri_post`
  - verifier-side evidence is captured from a harness-local verifier UI panel (PR2-style state view) populated from verifier2 API responses

## Branch-oriented suites

- `main` suite:
  - runs legacy scenario only
- `pr1` suite:
  - runs legacy scenario (`JWT + W3C VC`) as mandatory backward-compat baseline
  - runs verifier2 API matrix over request shapes and formats (`dc+sd-jwt`, `jwt_vc_json` by default)
  - optionally probes `request_uri_method=post` support and records `SKIPPED_UNSUPPORTED` if endpoint-level POST retrieval is not supported
- `pr2` suite:
  - keeps the portal-based verifier2 transaction flow (`/verify/transaction`)
  - artifact branch tag defaults to `transaction-data-support`

## Install

```bash
cd verification/playwright-oid4vp
npm install
npx playwright install chromium
```

## Run

```bash
npm run record:scenario:legacy
npm run record:scenario:verifier2
npm run record:scenario:verifier2-api
PRESENTATION_FORMAT=mso_mdoc npm run record:scenario:verifier2

npm run record:suite:main
npm run record:suite:pr1
npm run record:suite:pr1:matrix
npm run record:suite:pr1:portal
npm run record:suite:pr2
INCLUDE_MDOC=true npm run record:suite:pr1:portal
```

Compatibility aliases:

```bash
npm run record:smoke-main
npm run record:base
npm run record:transaction
```

## Evidence output

Artifacts are written to:

```text
$HOME/.waltid-playwright-artifacts/<branch-tag>--<scenario>--<timestamp>/
```

Examples:
- `main--legacy-verifier-portal--...`
- `wallet-openid4vp-v1--verifier2-portal-dc-sd-jwt--...`
- `wallet-openid4vp-v1--verifier2-api-dc-sd-jwt-request-object-signed--...`
- `transaction-data-support--verifier2-portal-mso-mdoc--...`

Each run contains:

- verifier evidence:
  - `screenshots/verifier/*.png`
  - `videos/verifier/*.webm`
- wallet evidence:
  - `screenshots/wallet/*.png`
  - `videos/wallet/*.webm`
- `run-metadata.json`

The run flow now explicitly verifies wallet credential presence via wallet-api before the presentation step.

The PR1 matrix suite also writes:
- `$HOME/.waltid-playwright-artifacts/<branch-tag>--pr1-matrix-summary--<timestamp>/run-summary.json`

## Required local stack behavior

- start services from the branch under test (especially portal and wallet-api)
- keep verifier and verifier2 ports distinct:
  - `7303 -> verifier-api` (direct service port)
  - `7304 -> verifier-api2` (direct service port)
- wallet, portal, and harness should use `7303/7304` for verifier backends to avoid proxy ambiguity
- verifier2 `urlPrefix` is override-mounted in this harness so generated request/response URLs also target `7304`

The harness-specific Caddy override in this directory is configured with this separation.

## Useful environment variables

```bash
HEADLESS=true
SLOW_MO=0
TIMEOUT_MS=180000
PLAYWRIGHT_ARTIFACTS_DIR=/absolute/path/for/artifacts

PORTAL_BASE_URL=http://localhost:7102
WALLET_BASE_URL=http://localhost:7101
WALLET_API_BASE_URL=http://localhost:7001/wallet-api
ISSUER_API_BASE_URL=http://localhost:7002
VERIFIER_BASE_URL=http://localhost:7303
VERIFIER2_BASE_URL=http://localhost:7304

LEGACY_CREDENTIAL_ID=IdentityCredential
LEGACY_FORMAT=JWT + W3C VC
LEGACY_DISABLE_SIGNATURE_POLICY=false
PRESENTATION_FORMAT=dc+sd-jwt
INCLUDE_MDOC=false
ARTIFACT_BRANCH_TAG=main

PR1_MATRIX_FORMATS=dc+sd-jwt,jwt_vc_json
PR1_REQUEST_SHAPES=direct,request_uri_get,request_object_unsigned,request_object_signed
OID4VP_REQUEST_SHAPE=direct
CHECK_REQUEST_URI_POST=true
REQUIRE_REQUEST_URI_POST=false
PR1_SIGNED_CLIENT_ID=x509_san_dns:verifier.example.com
ENABLE_LEGACY_SDJWT_PROBE=false
```

## Failure expectations

- On `main`, the legacy verifier flow is recorded with `LEGACY_FORMAT=JWT + W3C VC` by default. This avoids the known SD-JWT holder-binding verification failure in current legacy verifier defaults.
- On stacks where the legacy verifier endpoint `/openid4vc/verify` is unavailable, `record:scenario:legacy` fails fast with a clear legacy-verifier requirement message.
- On branches where `/verify/transaction` is not implemented in the portal, `record:scenario:verifier2` fails fast with a clear message.
- On branches where wallet-api does not yet support OpenID4VP 1.0 request resolution, `record:scenario:verifier2` fails with a clear wallet compatibility message.
- `record:suite:main` should remain valid on `main` as long as legacy verifier and wallet are healthy.
- `record:suite:pr1` fails if any mandatory compatibility scenario fails:
  - legacy verifier compatibility
  - verifier2 direct path
  - verifier2 `request_uri` path
  - verifier2 inline unsigned request-object path
  - verifier2 inline signed request-object path
- `record:suite:pr1` treats `request_uri_method=post` as optional by default:
  - if verifier2 `/request` POST retrieval is unsupported, the probe is recorded as `SKIPPED_UNSUPPORTED`
  - set `REQUIRE_REQUEST_URI_POST=true` to make that scenario mandatory
