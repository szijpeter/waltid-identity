import fs from "node:fs/promises";
import path from "node:path";
import { createPrivateKey, sign as nodeSign } from "node:crypto";
import { chromium, request as playwrightRequest } from "playwright";

export const defaults = {
  walletBaseUrl: process.env.WALLET_BASE_URL ?? "http://localhost:7101",
  walletApiBaseUrl: process.env.WALLET_API_BASE_URL ?? "http://localhost:7001/wallet-api",
  issuerApiBaseUrl: process.env.ISSUER_API_BASE_URL ?? "http://localhost:7002",
  verifier2BaseUrl: process.env.VERIFIER2_BASE_URL ?? "http://localhost:7004",
  artifactsBaseDir:
    process.env.PLAYWRIGHT_ARTIFACTS_DIR ??
    path.join(process.env.HOME ?? process.cwd(), ".waltid-playwright-artifacts"),
  headless: envBool("HEADLESS", false),
  slowMo: Number(process.env.SLOW_MO ?? 250),
  timeoutMs: Number(process.env.TIMEOUT_MS ?? 120000),
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
  credentialConfigurationId: "identity_credential_vc+sd-jwt",
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

const verifierRequestObjectKey = {
  kty: "EC",
  d: "AEb4k1BeTR9xt2NxYZggdzkFLLUkhyyWvyUOq3qSiwA",
  crv: "P-256",
  kid: "_nd-T2YRYLSmuKkJZlRI641zrCIJLTpiHeqMwXuvdug",
  x: "G_TgBc0BkmMipiQ_6gkamIn3mmp7hcTrZuyrLTmknP0",
  y: "VkRMZdXYXSMff5AJLrnHiN0x5MV6u_8vrAcytGUe4z4",
};

const verifierRequestObjectX5c = [
  "MIIBVzCB/aADAgECAggNKZAvUrtimzAKBggqhkjOPQQDAjAfMR0wGwYDVQQDDBR2ZXJpZmllci5leGFtcGxlLmNvbTAeFw0yNTEwMTQwNjI0MjBaFw0yNjEwMTQwNjI0MjBaMB8xHTAbBgNVBAMMFHZlcmlmaWVyLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEG/TgBc0BkmMipiQ/6gkamIn3mmp7hcTrZuyrLTmknP1WRExl1dhdIx9/kAkuuceI3THkxXq7/y+sBzK0ZR7jPqMjMCEwHwYDVR0RBBgwFoIUdmVyaWZpZXIuZXhhbXBsZS5jb20wCgYIKoZIzj0EAwIDSQAwRgIhAOu0RGM6BjVQUepeLBogw+ZD3MQ9vFppbPIGMPjtn/qdAiEAttfdfyXHfzJ2tr+Pczyckzv3NlM43461cvP96sIzOQA="
];

export function envBool(name, fallback) {
  const value = process.env[name];
  if (value == null) return fallback;
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

export function timestamp() {
  return new Date().toISOString().replace(/[:]/g, "-");
}

export async function makeArtifactDir(prefix, config = defaults) {
  const dir = path.join(config.artifactsBaseDir, `${prefix}-${timestamp()}`);
  await fs.mkdir(path.join(dir, "screenshots"), { recursive: true });
  await fs.mkdir(path.join(dir, "videos"), { recursive: true });
  return dir;
}

export async function launchBrowserContext(artifactDir, config = defaults) {
  const browser = await chromium.launch({
    headless: config.headless,
    slowMo: config.slowMo,
  });
  const context = await browser.newContext({
    recordVideo: {
      dir: path.join(artifactDir, "videos"),
      size: { width: 1440, height: 1024 },
    },
  });
  const page = await context.newPage();
  page.setDefaultTimeout(config.timeoutMs);
  return { browser, context, page };
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

export function buildSdJwtCredentialQuery(issuerApiBaseUrl) {
  return {
    id: "payment_credential",
    format: "dc+sd-jwt",
    meta: {
      vct_values: [`${issuerApiBaseUrl}/identity_credential`],
    },
    claims: [
      { path: ["given_name"] },
      { path: ["family_name"] },
      { path: ["address", "street_address"] },
    ],
    require_cryptographic_holder_binding: true,
  };
}

export function buildMdocCredentialQuery() {
  return {
    id: "payment_credential",
    format: "mso_mdoc",
    meta: {
      doctype_value: "org.iso.18013.5.1.mDL",
    },
    claims: [
      { path: ["org.iso.18013.5.1", "given_name"] },
      { path: ["org.iso.18013.5.1", "family_name"] },
      { path: ["org.iso.18013.5.1", "issuing_country"] },
    ],
    require_cryptographic_holder_binding: true,
  };
}

export async function createVerificationSession(api, setup, config = defaults) {
  const response = await expectOk(
    await api.post(`${config.verifier2BaseUrl}/verification-session/create`, {
      data: setup,
    }),
    "create verification session",
  );
  return normalizeSessionUrls(await response.json(), config);
}

export async function fetchSessionRequest(api, sessionId, config = defaults) {
  const response = await expectOk(
    await api.get(`${config.verifier2BaseUrl}/verification-session/${sessionId}/request`),
    "fetch verification request",
  );
  const contentType = response.headers()["content-type"] ?? "";
  const body = await response.text();
  return {
    contentType,
    body: rewriteVerifier2Port(body, config),
  };
}

export async function fetchSessionInfo(api, sessionId, config = defaults) {
  const response = await expectOk(
    await api.get(`${config.verifier2BaseUrl}/verification-session/${sessionId}/info`),
    "fetch session info",
  );
  return response.json();
}

export async function waitForTerminalSessionStatus(api, sessionId, config = defaults) {
  const deadline = Date.now() + config.timeoutMs;
  while (Date.now() < deadline) {
    const info = await fetchSessionInfo(api, sessionId, config);
    if (["SUCCESSFUL", "FAILED", "COMPLETED"].includes(info.status)) {
      return info;
    }
    await sleep(1500);
  }
  throw new Error(`Timed out waiting for verifier2 session ${sessionId}`);
}

export function buildLaunchUrl(walletBaseUrl, walletId, requestUrl) {
  return `${walletBaseUrl}/wallet/${walletId}/exchange/presentation?request=${encodeBase64Url(requestUrl)}`;
}

export function encodeBase64Url(value) {
  return Buffer.from(value, "utf8").toString("base64url");
}

export function makeUnsignedRequestObject(requestPayload) {
  const header = encodeJsonBase64Url({ alg: "none", typ: "oauth-authz-req+jwt" });
  const payload = encodeJsonBase64Url(requestPayload);
  return `${header}.${payload}.`;
}

export function makeSignedRequestObject(requestPayload, clientId = "x509_san_dns:verifier.example.com") {
  const header = {
    alg: "ES256",
    typ: "oauth-authz-req+jwt",
    kid: verifierRequestObjectKey.kid,
    x5c: verifierRequestObjectX5c,
  };
  const payload = {
    ...requestPayload,
    client_id: clientId,
  };
  const signingInput = `${encodeJsonBase64Url(header)}.${encodeJsonBase64Url(payload)}`;
  const signature = nodeSign(
    "sha256",
    Buffer.from(signingInput),
    createPrivateKey({
      key: {
        kty: "EC",
        crv: "P-256",
        d: verifierRequestObjectKey.d,
        x: verifierRequestObjectKey.x,
        y: verifierRequestObjectKey.y,
      },
      format: "jwk",
    }),
  );
  return `${signingInput}.${signature.toString("base64url")}`;
}

export function requestPayloadToUrl(requestPayload) {
  const url = new URL("openid4vp://authorize");
  for (const [key, value] of Object.entries(normalizeRequestPayload(requestPayload, defaults))) {
    if (value == null) continue;
    url.searchParams.append(
      key,
      typeof value === "string" ? value : JSON.stringify(value),
    );
  }
  return url.toString();
}

export async function openAndPresent(page, launchUrl, { expectTransactionDetails = false } = {}) {
  await page.goto(launchUrl, { waitUntil: "networkidle" });
  await page.locator("h1").filter({ hasText: "Presentation Request" }).waitFor();
  if (expectTransactionDetails) {
    await page.locator("text=Transaction details").waitFor();
  }
  const disclosureCheckbox = page.getByRole("checkbox").first();
  if (await disclosureCheckbox.count()) {
    await disclosureCheckbox.check();
  }
  await page.getByRole("button", { name: /Authorize|Disclose/ }).last().click();
}

export async function saveScreenshot(page, artifactDir, name) {
  const target = path.join(artifactDir, "screenshots", name);
  await page.screenshot({ path: target, fullPage: true });
  return target;
}

export async function finalizeRun(context, browser, artifactDir, metadata) {
  const pages = context.pages();
  for (const page of pages) {
    try {
      await page.close();
    } catch {
      // ignore
    }
  }
  await context.close();
  await browser.close();
  await fs.writeFile(
    path.join(artifactDir, "run-metadata.json"),
    JSON.stringify(metadata, null, 2),
    "utf8",
  );
}

export function attachConsoleLogging(page) {
  page.on("console", (message) => {
    if (message.type() === "error") {
      console.error(`[wallet console:${message.type()}] ${message.text()}`);
    }
  });
}

function encodeJsonBase64Url(value) {
  return Buffer.from(JSON.stringify(value), "utf8").toString("base64url");
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function normalizeSessionUrls(session, config) {
  return {
    ...session,
    bootstrapAuthorizationRequestUrl: session.bootstrapAuthorizationRequestUrl
      ? rewriteVerifier2Port(session.bootstrapAuthorizationRequestUrl, config)
      : session.bootstrapAuthorizationRequestUrl,
    fullAuthorizationRequestUrl: session.fullAuthorizationRequestUrl
      ? rewriteVerifier2Port(session.fullAuthorizationRequestUrl, config)
      : session.fullAuthorizationRequestUrl,
  };
}

function normalizeRequestPayload(requestPayload, config) {
  return Object.fromEntries(
    Object.entries(requestPayload).map(([key, value]) => {
      if ((key === "request_uri" || key === "response_uri") && typeof value === "string") {
        return [key, rewriteVerifier2Port(value, config)];
      }
      return [key, value];
    }),
  );
}

function rewriteVerifier2Port(value, config) {
  const encodedVerifier2BaseUrl = encodeURIComponent(config.verifier2BaseUrl);
  return value
    .replaceAll("http://localhost:7003", config.verifier2BaseUrl)
    .replaceAll("http%3A%2F%2Flocalhost%3A7003", encodedVerifier2BaseUrl);
}
