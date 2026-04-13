# OID4VP Recording Scenarios (Robust Runbook)

## Purpose
This document defines repeatable manual recording scenarios for the OID4VP workstreams and the minimum runtime checks needed to avoid false failures caused by local stack drift.

## Branch-to-goal mapping
- `main` baseline:
  - prove legacy verifier flow works end-to-end (`/verify`)
- `publish/oid4vp-wallet-v1-combined` (task 1 / PR-B):
  - prove backward compatibility with legacy verifier (`/verify`)
  - prove OpenID4VP 1.0 wallet compatibility with verifier2 (`request` shapes)
- `publish/transaction-data-support` (task 2 / PR-C):
  - prove transaction-data happy path for `dc+sd-jwt` using verifier2 transaction page
  - record current `mso_mdoc` limitation as known gap if reproduced

## Required topology
- Legacy verifier UI path:
  - portal page: `http://localhost:7102/verify`
  - backend dependency: `verifier-api` (`http://localhost:7303`)
- Verifier2 transaction UI path:
  - portal page: `http://localhost:7102/verify/transaction`
  - backend dependency: `verifier-api2` (`http://localhost:7304`)
- Wallet UI:
  - demo wallet: `http://localhost:7101`
- Issuer API:
  - `http://localhost:7002`

## Clean startup checklist
1. Start from the branch under test in a clean worktree.
2. Bring up full recording stack with override:
```bash
docker compose -f docker-compose/docker-compose.yaml -f verification/playwright-oid4vp/docker-compose.override.yaml down --remove-orphans
docker compose -f docker-compose/docker-compose.yaml -f verification/playwright-oid4vp/docker-compose.override.yaml up -d --build postgres caddy issuer-api verifier-api verifier-api2 wallet-api waltid-demo-wallet web-portal vc-repo
```
3. Verify health before opening browsers:
```bash
docker compose -f docker-compose/docker-compose.yaml -f verification/playwright-oid4vp/docker-compose.override.yaml ps
curl -si http://localhost:7303/swagger | head -n 5
curl -si http://localhost:7304/swagger | head -n 5
curl -s http://localhost:7102/api/env
```
4. Hard-refresh portal and wallet tabs (disable stale frontend bundles during retries).

## Recording conventions
- Capture both windows for each run:
  - verifier/portal window
  - wallet window
- Capture at least these states as screenshots:
  - verifier request created
  - wallet request review page
  - wallet post-confirmation state
  - verifier terminal result state
- Suggested artifact naming:
  - `main-<scenario-name>-<timestamp>`
  - `wallet-openid4vp-v1-<scenario-name>-<timestamp>`
  - `transaction-data-support-<scenario-name>-<timestamp>`

## Scenario S1: Legacy verifier compatibility
1. Open portal `http://localhost:7102`.
2. Use legacy verification path (`/verify`) and create request/offer URL.
3. Open wallet deep link, login, and present.
4. Confirm verifier shows successful session.
5. Record verifier + wallet windows across all key states.

Expected:
- offer/request URL generation works
- wallet resolves request without blank page
- presentation completes and verifier shows success

## Scenario S2: OpenID4VP 1.0 wallet compatibility (task 1)
1. Use verifier2 request generation (API or portal transaction page as orchestrator).
2. Run `dc+sd-jwt` request shapes:
  - `direct`
  - `request_uri_get`
  - `request_object_unsigned`
  - `request_object_signed`
3. For each shape, open wallet request URL, present, verify success.
4. Capture verifier and wallet screens for each shape.

Expected:
- wallet resolves all supported shapes
- successful verifier2 terminal status for each shape

## Scenario S3: Transaction-data happy path (`dc+sd-jwt`)
1. Open `http://localhost:7102/verify/transaction`.
2. Select `dc+sd-jwt`, create verification request.
3. Open wallet and proceed to presentation.
4. Verify wallet shows transaction details prior to consent.
5. Authorize and submit.
6. Verify session result is successful in portal.

Expected:
- transaction details rendered in wallet
- successful verifier2 session result

## Scenario S4: Transaction-data `mso_mdoc` status probe
1. Repeat S3 with `mso_mdoc`.
2. If failure occurs, capture exact verifier status and error details.
3. Classify as known limitation only if behavior matches current known device-auth verification issue.

Expected:
- either successful run or reproducible known limitation with evidence

## Known pitfalls and fixes
- Symptom: cannot generate URL from `/verify`.
  - Cause: `verifier-api` not running.
  - Fix: start `verifier-api` and recheck `http://localhost:7303/swagger`.
- Symptom: wallet says no matching credentials for transaction demo.
  - Cause: strict VCT mismatch vs issuer profile.
  - Fix: ensure branch contains dual-VCT query support on transaction page.
- Symptom: wallet presentation page blank or malformed request behavior.
  - Cause: unsafe URI decode/encode roundtrip mutating encoded query parameters.
  - Fix: ensure branch contains direct `fixRequest("openid://" + window.location.search)` handling in demo and dev wallet init pages.
- Symptom: bind/port errors when starting compose.
  - Cause: stale containers or conflicting local services.
  - Fix: run `down --remove-orphans`, free conflicting ports, then retry startup.

## Submission-ready evidence checklist
- One full successful legacy verifier video.
- One full successful verifier2 OID4VP compatibility video.
- One full successful transaction-data (`dc+sd-jwt`) video.
- Paired screenshots for each recorded run (request-ready, review, submitted, terminal result).
- If `mso_mdoc` remains failing, include one explicit failure recording plus concise known-limitation note.
