import fs from "node:fs/promises";
import path from "node:path";
import { chromium, request as playwrightRequest } from "playwright";

export const defaults = {
  artifactBranchTag: process.env.ARTIFACT_BRANCH_TAG ?? "unknown-branch",
  portalBaseUrl: process.env.PORTAL_BASE_URL ?? "http://localhost:7102",
  walletBaseUrl: process.env.WALLET_BASE_URL ?? "http://localhost:7101",
  walletApiBaseUrl: process.env.WALLET_API_BASE_URL ?? "http://localhost:7001/wallet-api",
  issuerApiBaseUrl: process.env.ISSUER_API_BASE_URL ?? "http://localhost:7002",
  verifierBaseUrl: process.env.VERIFIER_BASE_URL ?? "http://localhost:7303",
  verifier2BaseUrl: process.env.VERIFIER2_BASE_URL ?? "http://localhost:7304",
  artifactsBaseDir:
    process.env.PLAYWRIGHT_ARTIFACTS_DIR ??
    path.join(process.env.HOME ?? process.cwd(), ".waltid-playwright-artifacts"),
  headless: envBool("HEADLESS", false),
  slowMo: Number(process.env.SLOW_MO ?? 250),
  timeoutMs: Number(process.env.TIMEOUT_MS ?? 180000),
};

export const issuerKey = {
  type: "jwk",
  jwk: {
    kty: "EC",
    d: "KJ4k3Vcl5Sj9Mfq4rrNXBm2MoPoY3_Ak_PIR_EgsFhQ",
    crv: "P-256",
    x: "G0RINBiF-oQUD3d5DGnegQuXenI29JDaMGoMvioKRBM",
    y: "ed3eFGs2pEtrp7vAZ7BLcbrUtpKkYWAT2JPUQK4lN4E",
  },
};

export const issuerDid =
  "did:jwk:eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6IkcwUklOQmlGLW9RVUQzZDVER25lZ1F1WGVuSTI5SkRhTUdvTXZpb0tSQk0iLCJ5IjoiZWQzZUZHczJwRXRycDd2QVo3QkxjYnJVdHBLa1lXQVQySlBVUUs0bE40RSJ9";

export const sdJwtIssuancePayload = {
  issuerKey,
  issuerDid,
  credentialConfigurationId: "IdentityCredential_vc+sd-jwt",
  credentialData: {
    given_name: "John",
    family_name: "Doe",
    email: "johndoe@example.com",
    phone_number: "+1-202-555-0101",
    address: {
      street_address: "123 Main St",
      locality: "Anytown",
      region: "Anystate",
      country: "US",
    },
    birthdate: "1940-01-01",
    is_over_18: true,
    is_over_21: true,
    is_over_65: true,
  },
  mapping: {
    id: "<uuid>",
    iat: "<timestamp-seconds>",
    nbf: "<timestamp-seconds>",
    exp: "<timestamp-in-seconds:365d>",
  },
  selectiveDisclosure: {
    fields: {
      birthdate: { sd: true },
      family_name: { sd: false },
    },
  },
};

export const jwtVcIssuancePayload = {
  issuerKey: {
    type: "jwk",
    jwk: {
      kty: "OKP",
      d: "fbpXmCh4KkcVIGOnkcjHvWAcaUPvvkBvgMFPE4nAgvA",
      crv: "Ed25519",
      kid: "DJ3X4BZqk4GJsMGZL44hEZrlEy9scbMcSA_QuUi3tGs",
      x: "Y64Ns3aRo6KQgJTtCZKFA78uYvslBcIrOk7xaS1PIZI",
    },
  },
  issuerDid: "did:key:z6MkmANLkdcnbriWeVaqdfrA3MmtXoVPNu98tww6xDeyVnyF",
  credentialConfigurationId: "IdentityCredential_jwt_vc_json",
  credentialData: {
    type: ["VerifiableCredential", "IdentityCredential"],
    given_name: "John",
    family_name: "Doe",
    email: "johndoe@example.com",
    phone_number: "+1-202-555-0101",
    address: {
      street_address: "123 Main St",
      locality: "Anytown",
      region: "Anystate",
      country: "US",
    },
    birthdate: "1940-01-01",
    is_over_18: true,
    is_over_21: true,
    is_over_65: true,
  },
  mapping: {
    id: "<uuid>",
    iat: "<timestamp-seconds>",
    nbf: "<timestamp-seconds>",
    exp: "<timestamp-in-seconds:365d>",
  },
};

export const mdocIssuancePayload = {
  issuerKey: {
    type: "jwk",
    jwk: {
      kty: "EC",
      d: "-wSIL_tMH7-mO2NAfHn03I8ZWUHNXVzckTTb96Wsc1s",
      crv: "P-256",
      kid: "sW5yv0UmZ3S0dQuUrwlR9I3foREBHHFwXhGJGqGEVf0",
      x: "Pzp6eVSAdXERqAp8q8OuDEhl2ILGAaoaQXTJ2sD2g5U",
      y: "6dwhUAzKzKUf0kNI7f40zqhMZNT0c40O_WiqSLCTNZo",
    },
  },
  credentialConfigurationId: "org.iso.18013.5.1.mDL",
  mdocData: {
    "org.iso.18013.5.1": {
      family_name: "Doe",
      given_name: "John",
      birth_date: "1986-03-22",
      issue_date: "2019-10-20",
      expiry_date: "2029-10-20",
      issuing_country: "AT",
      issuing_authority: "AT DMV",
      document_number: "123456789",
    },
  },
  x5Chain: [
    "-----BEGIN CERTIFICATE-----\nMIICCTCCAbCgAwIBAgIUfqyiArJZoX7M61/473UAVi2/UpgwCgYIKoZIzj0EAwIwKDELMAkGA1UEBhMCQVQxGTAXBgNVBAMMEFdhbHRpZCBUZXN0IElBQ0EwHhcNMjUwNjAyMDY0MTEzWhcNMjYwOTAyMDY0MTEzWjAzMQswCQYDVQQGEwJBVDEkMCIGA1UEAwwbV2FsdGlkIFRlc3QgRG9jdW1lbnQgU2lnbmVyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPzp6eVSAdXERqAp8q8OuDEhl2ILGAaoaQXTJ2sD2g5Xp3CFQDMrMpR/SQ0jt/jTOqExk1PRzjQ79aKpIsJM1mqOBrDCBqTAfBgNVHSMEGDAWgBTxCn2nWMrE70qXb614U14BweY2azAdBgNVHQ4EFgQUx5qkOLC4lpl1xpYZGmF9HLxtp0gwDgYDVR0PAQH/BAQDAgeAMBoGA1UdEgQTMBGGD2h0dHBzOi8vd2FsdC5pZDAVBgNVHSUBAf8ECzAJBgcogYxdBQECMCQGA1UdHwQdMBswGaAXoBWGE2h0dHBzOi8vd2FsdC5pZC9jcmwwCgYIKoZIzj0EAwIDRwAwRAIgHTap3c6yCUNhDVfZWBPMKj9dCWZbrME03kh9NJTbw1ECIAvVvuGll9O21eR16SkJHHAA1pPcovhcTvF9fz9cc66M\n-----END CERTIFICATE-----\n",
  ],
};

export function envBool(name, fallback) {
  const value = process.env[name];
  if (value == null) return fallback;
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

export function timestamp() {
  return new Date().toISOString().replaceAll(":", "-");
}

export async function makeArtifactDir(prefix, config = defaults) {
  const scenarioSlug = slugify(prefix);
  const branchSlug = slugify(config.artifactBranchTag);
  const dir = path.join(config.artifactsBaseDir, `${branchSlug}--${scenarioSlug}--${timestamp()}`);
  await fs.mkdir(path.join(dir, "screenshots", "wallet"), { recursive: true });
  await fs.mkdir(path.join(dir, "screenshots", "verifier"), { recursive: true });
  await fs.mkdir(path.join(dir, "videos", "wallet"), { recursive: true });
  await fs.mkdir(path.join(dir, "videos", "verifier"), { recursive: true });
  return dir;
}

export async function launchBrowserContext(artifactDir, actor, config = defaults) {
  const browser = await chromium.launch({
    headless: config.headless,
    slowMo: config.slowMo,
  });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1024 },
    recordVideo: {
      dir: path.join(artifactDir, "videos", actor),
      size: { width: 1440, height: 1024 },
    },
  });
  const page = await context.newPage();
  page.setDefaultTimeout(config.timeoutMs);
  return { actor, browser, context, page };
}

export async function apiContext() {
  return playwrightRequest.newContext({
    extraHTTPHeaders: {
      Accept: "application/json, text/plain;q=0.9, */*;q=0.8",
    },
  });
}

export function randomAccount() {
  const suffix = Math.random().toString(36).slice(2, 10);
  return {
    name: `Playwright ${suffix}`,
    email: `playwright-${suffix}@example.com`,
    password: "password",
  };
}

export async function expectOk(response, label) {
  if (!response.ok()) {
    throw new Error(`${label} failed: ${response.status()} ${await response.text()}`);
  }
  return response;
}

export async function registerAndLogin(api, config = defaults) {
  const account = randomAccount();
  await expectOk(
    await api.post(`${config.walletApiBaseUrl}/auth/register`, {
      data: {
        name: account.name,
        email: account.email,
        password: account.password,
        type: "email",
      },
    }),
    "register account",
  );
  await expectOk(
    await api.post(`${config.walletApiBaseUrl}/auth/login`, {
      data: {
        email: account.email,
        password: account.password,
        type: "email",
      },
    }),
    "login account",
  );
  return account;
}

export async function loginInBrowser(page, account, config = defaults) {
  await page.goto(config.walletBaseUrl, { waitUntil: "networkidle" });
  await page.locator("input[name='email']").last().fill(account.email);
  await page.locator("input[name='password']").last().fill(account.password);
  await page.getByRole("button", { name: /Sign in/i }).last().click();
  await page.waitForURL(/\/wallet\//);
  await page.waitForLoadState("networkidle");
}

export async function listWallets(api, config = defaults) {
  const response = await expectOk(
    await api.get(`${config.walletApiBaseUrl}/wallet/accounts/wallets`),
    "list wallets",
  );
  return response.json();
}

export async function createDid(api, walletId, method = "jwk", config = defaults) {
  const response = await expectOk(
    await api.post(`${config.walletApiBaseUrl}/wallet/${walletId}/dids/create/${method}`),
    `create did:${method}`,
  );
  return response.text();
}

export async function issueCredentialOffer(api, presentationFormat = "dc+sd-jwt", config = defaults) {
  if (presentationFormat === "jwt_vc_json") {
    const response = await expectOk(
      await api.post(`${config.issuerApiBaseUrl}/openid4vc/jwt/issue`, {
        data: jwtVcIssuancePayload,
      }),
      "issue jwt-vc offer",
    );
    return response.text();
  }

  if (presentationFormat === "mso_mdoc") {
    const response = await expectOk(
      await api.post(`${config.issuerApiBaseUrl}/openid4vc/mdoc/issue`, {
        data: mdocIssuancePayload,
      }),
      "issue mdoc offer",
    );
    return response.text();
  }

  const response = await expectOk(
    await api.post(`${config.issuerApiBaseUrl}/openid4vc/sdjwt/issue`, {
      data: sdJwtIssuancePayload,
    }),
    "issue sd-jwt offer",
  );
  return response.text();
}

export async function claimOffer(api, walletId, did, offerUrl, config = defaults) {
  const url = new URL(`${config.walletApiBaseUrl}/wallet/${walletId}/exchange/useOfferRequest`);
  if (did) {
    url.searchParams.set("did", did);
  }
  const response = await expectOk(
    await api.post(url.toString(), {
      headers: { "Content-Type": "text/plain" },
      data: offerUrl,
    }),
    "claim credential offer",
  );
  return response.json();
}

export async function waitForWalletCredentials(
  api,
  walletId,
  minCount = 1,
  config = defaults,
) {
  const deadline = Date.now() + config.timeoutMs;
  while (Date.now() < deadline) {
    const response = await expectOk(
      await api.get(`${config.walletApiBaseUrl}/wallet/${walletId}/credentials`),
      "list wallet credentials",
    );
    const payload = await response.json();
    if (Array.isArray(payload) && payload.length >= minCount) {
      return payload;
    }
    await sleep(1000);
  }
  throw new Error(`Timed out waiting for wallet ${walletId} to contain at least ${minCount} credential(s).`);
}

function encodeRequestForWallet(requestUrl) {
  return Buffer.from(requestUrl, "utf-8")
    .toString("base64")
    .replaceAll("=", "")
    .replaceAll("+", "-")
    .replaceAll("/", "_");
}

export function buildWalletInitiatePresentationUrl(walletBaseUrl, walletId, requestUrl) {
  const encodedRequest = encodeRequestForWallet(requestUrl);
  return `${walletBaseUrl}/wallet/${walletId}/exchange/presentation?request=${encodedRequest}`;
}

export async function openAndPresent(page, { expectTransactionDetails = false } = {}) {
  try {
    await page.locator("h1").filter({ hasText: /Presentation Request/i }).first().waitFor({
      timeout: 15000,
    });
  } catch {
    const urlPreview = page.url().length > 240 ? `${page.url().slice(0, 240)}...` : page.url();
    const bodyText = (await page.locator("body").innerText()).slice(0, 600).replaceAll(/\s+/g, " ");
    throw new Error(
      `Wallet presentation page did not load. This usually means wallet-api could not resolve the request. Current URL: ${urlPreview} Body snippet: ${bodyText}`,
    );
  }
  if (expectTransactionDetails) {
    await page.getByText("Transaction details", { exact: true }).first().waitFor();
  }
  const disclosureCheckbox = page.getByRole("checkbox").first();
  if (await disclosureCheckbox.count()) {
    await disclosureCheckbox.check();
  }
  await page.getByRole("button", { name: /Authorize|Disclose|Present/i }).last().click();
  await page.waitForLoadState("networkidle");
}

export async function saveScreenshot(page, artifactDir, actor, name) {
  await scrollToTop(page);
  const target = path.join(artifactDir, "screenshots", actor, name);
  await page.screenshot({ path: target, fullPage: true });
  return target;
}

export async function waitForLegacyVerificationResult(api, state, config = defaults) {
  const deadline = Date.now() + config.timeoutMs;
  while (Date.now() < deadline) {
    const response = await expectOk(
      await api.get(`${config.verifierBaseUrl}/openid4vc/session/${encodeURIComponent(state)}`),
      "fetch legacy verifier session",
    );
    const payload = await response.json();
    if (typeof payload.verificationResult === "boolean") {
      return payload;
    }
    await sleep(1000);
  }
  throw new Error(`Timed out waiting for legacy verifier session ${state}`);
}

export async function fetchVerifier2SessionInfo(api, sessionId, config = defaults) {
  const response = await expectOk(
    await api.get(`${config.verifier2BaseUrl}/verification-session/${sessionId}/info`),
    "fetch verifier2 session info",
  );
  return response.json();
}

export async function waitForVerifier2TerminalSessionStatus(api, sessionId, config = defaults) {
  const deadline = Date.now() + config.timeoutMs;
  while (Date.now() < deadline) {
    const info = await fetchVerifier2SessionInfo(api, sessionId, config);
    if (["SUCCESSFUL", "FAILED", "COMPLETED"].includes(info.status)) {
      return info;
    }
    await sleep(1000);
  }
  throw new Error(`Timed out waiting for verifier2 session ${sessionId}`);
}

export function parseStateFromRequestUrl(requestUrl) {
  const normalized = requestUrl.replace(/^openid4vp:/, "https:");
  const parsed = new URL(normalized);
  return parsed.searchParams.get("state");
}

export async function ensureTransactionPageAvailable(page, config = defaults) {
  const response = await page.goto(`${config.portalBaseUrl}/verify/transaction`, {
    waitUntil: "networkidle",
  });
  if (response && response.status() === 404) {
    throw new Error("Verifier2 portal route /verify/transaction is not available on this branch.");
  }
  if (await page.getByText("This page could not be found").count()) {
    throw new Error("Verifier2 portal route /verify/transaction is not available on this branch.");
  }
  await page.locator("h1").filter({ hasText: "Transaction Verification" }).first().waitFor();
}

export async function renderVerifier2HarnessPanel(
  page,
  {
    phase,
    presentationFormat,
    requestShape,
    sessionId = null,
    walletRequestUrl = null,
    verifier2Status = null,
    requestUriPostSupported = null,
    requestUriPostProbeStatus = null,
    details = null,
    error = null,
  },
) {
  const statusClass = error ? "status-failed" : verifier2Status === "SUCCESSFUL" || verifier2Status === "COMPLETED"
    ? "status-success"
    : "status-progress";
  const statusLabel = error ? "FAILED" : verifier2Status ?? "IN_PROGRESS";
  const detailsText = details == null
    ? "{}"
    : JSON.stringify(details, null, 2);
  const requestUrlText = walletRequestUrl ?? "pending";
  const requestUriPostText = requestUriPostSupported == null
    ? "not checked"
    : requestUriPostSupported
      ? `supported (status ${requestUriPostProbeStatus ?? "n/a"})`
      : `not supported (status ${requestUriPostProbeStatus ?? "n/a"})`;

  await page.setContent(
    `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>Verifier2 Harness</title>
    <style>
      :root {
        color-scheme: light;
      }
      body {
        margin: 0;
        background: #f8fafc;
        color: #0f172a;
        font-family: "Inter", "Segoe UI", sans-serif;
      }
      .layout {
        min-height: 100vh;
        padding: 36px;
      }
      .card {
        max-width: 1220px;
        margin: 0 auto;
        background: #ffffff;
        border-radius: 28px;
        box-shadow: 0 24px 48px rgba(15, 23, 42, 0.08);
        overflow: hidden;
      }
      .hero {
        padding: 28px 34px 20px 34px;
        border-bottom: 1px solid #e2e8f0;
      }
      .eyebrow {
        font-size: 12px;
        letter-spacing: 0.14em;
        text-transform: uppercase;
        color: #64748b;
        font-weight: 600;
      }
      h1 {
        margin: 10px 0 6px;
        font-size: 34px;
        line-height: 1.2;
      }
      .subtitle {
        margin: 0;
        color: #475569;
        font-size: 15px;
      }
      .content {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 24px;
        padding: 24px 34px 34px 34px;
      }
      .panel {
        border: 1px solid #e2e8f0;
        border-radius: 20px;
        padding: 18px 20px;
        background: #ffffff;
      }
      .panel h2 {
        margin: 0 0 12px 0;
        font-size: 15px;
      }
      .kv {
        display: grid;
        grid-template-columns: 180px 1fr;
        gap: 8px;
        font-size: 14px;
      }
      .kv dt {
        color: #64748b;
      }
      .kv dd {
        margin: 0;
        word-break: break-word;
      }
      .status {
        display: inline-flex;
        align-items: center;
        border-radius: 999px;
        padding: 6px 12px;
        font-size: 12px;
        font-weight: 700;
        letter-spacing: 0.02em;
      }
      .status-progress {
        background: #eff6ff;
        color: #1d4ed8;
      }
      .status-success {
        background: #ecfdf3;
        color: #15803d;
      }
      .status-failed {
        background: #fef2f2;
        color: #b91c1c;
      }
      pre {
        margin: 0;
        padding: 14px;
        border-radius: 14px;
        background: #020617;
        color: #e2e8f0;
        font-size: 12px;
        line-height: 1.45;
        overflow: auto;
        max-height: 400px;
      }
      .error {
        margin-top: 12px;
        border: 1px solid #fecaca;
        border-radius: 14px;
        background: #fff1f2;
        color: #9f1239;
        padding: 10px 12px;
        font-size: 13px;
      }
    </style>
  </head>
  <body>
    <main class="layout">
      <article class="card">
        <header class="hero">
          <div class="eyebrow">Verifier2 Harness</div>
          <h1>OpenID4VP 1.0 Request Matrix</h1>
          <p class="subtitle">PR1 compatibility recording view modeled after the transaction verifier UX.</p>
        </header>
        <section class="content">
          <div class="panel">
            <h2>Session State</h2>
            <p><span class="status ${statusClass}">${escapeHtml(statusLabel)}</span></p>
            <dl class="kv">
              <dt>Phase</dt><dd>${escapeHtml(phase ?? "Unknown")}</dd>
              <dt>Format</dt><dd>${escapeHtml(presentationFormat ?? "n/a")}</dd>
              <dt>Request shape</dt><dd>${escapeHtml(requestShape ?? "n/a")}</dd>
              <dt>Session ID</dt><dd>${escapeHtml(sessionId ?? "pending")}</dd>
              <dt>request_uri POST</dt><dd>${escapeHtml(requestUriPostText)}</dd>
              <dt>Wallet request URL</dt><dd>${escapeHtml(requestUrlText)}</dd>
            </dl>
            ${error ? `<div class="error">${escapeHtml(error)}</div>` : ""}
          </div>
          <div class="panel">
            <h2>Live Details</h2>
            <pre>${escapeHtml(detailsText)}</pre>
          </div>
        </section>
      </article>
    </main>
  </body>
</html>`,
    { waitUntil: "load" },
  );
}

export async function finalizeRun(artifactDir, browserContexts, metadata) {
  for (const item of browserContexts) {
    try {
      await item.context.close();
    } catch {
      // ignore
    }
    try {
      await item.browser.close();
    } catch {
      // ignore
    }
  }

  await fs.writeFile(
    path.join(artifactDir, "run-metadata.json"),
    JSON.stringify(metadata, null, 2),
    "utf8",
  );
}

export function attachConsoleLogging(page, actor) {
  page.on("console", (message) => {
    if (message.type() === "error") {
      console.error(`[${actor} console:${message.type()}] ${message.text()}`);
    }
  });
}

async function scrollToTop(page) {
  await page.evaluate(() => window.scrollTo(0, 0));
}

function slugify(value) {
  return String(value)
    .trim()
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, "-")
    .replaceAll(/^-+|-+$/g, "");
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
