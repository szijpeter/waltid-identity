import {
  apiContext,
  attachConsoleLogging,
  buildWalletInitiatePresentationUrl,
  claimOffer,
  createDid,
  defaults,
  fetchVerifier2SessionInfo,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  loginInBrowser,
  makeArtifactDir,
  openAndPresent,
  requireHarnessPreflight,
  registerAndLogin,
  renderVerifier2HarnessPanel,
  saveScreenshot,
  waitForVerifier2TerminalSessionStatus,
  waitForWalletCredentials,
} from "./lib/common.mjs";

const requestShape = process.env.OID4VP_REQUEST_SHAPE ?? "direct";
const presentationFormat = process.env.PRESENTATION_FORMAT ?? "dc+sd-jwt";
const signedClientId = process.env.PR1_SIGNED_CLIENT_ID ?? "x509_san_dns:verifier.example.com";
const requireRequestUriPost = envBool("REQUIRE_REQUEST_URI_POST", false);
const enableTransactionData = envBool("ENABLE_TRANSACTION_DATA", false);

const supportedRequestShapes = new Set([
  "direct",
  "request_uri_get",
  "request_uri_post",
  "request_object_unsigned",
  "request_object_signed",
]);

async function main() {
  const preflight = requireHarnessPreflight({ requireRunningServices: true });
  if (!supportedRequestShapes.has(requestShape)) {
    throw new Error(
      `Unsupported OID4VP_REQUEST_SHAPE "${requestShape}". Expected one of: ${Array.from(supportedRequestShapes).join(", ")}`,
    );
  }

  const scenarioSlug = `verifier2-api-${presentationFormat.replace("+", "-")}-${requestShape.replaceAll("_", "-")}`;
  const artifactDir = await makeArtifactDir(scenarioSlug);
  const api = await apiContext();
  const wallet = await launchBrowserContext(artifactDir, "wallet", defaults);
  const verifier = await launchBrowserContext(artifactDir, "verifier", defaults);
  attachConsoleLogging(wallet.page, "wallet");
  attachConsoleLogging(verifier.page, "verifier");

  let walletId = null;
  let verifier2SessionId = null;
  let walletCredentialCount = 0;
  let metadata = null;

  try {
    const account = await registerAndLogin(api, defaults);
    await loginInBrowser(wallet.page, account, defaults);
    await saveScreenshot(wallet.page, artifactDir, "wallet", "01-wallet-signed-in-pre-claim.png");

    const wallets = await listWallets(api, defaults);
    walletId = wallets.wallets[0]?.id;
    if (!walletId) {
      throw new Error("No wallet found for account.");
    }

    const did = await createDid(api, walletId, "jwk", defaults);
    const offerUrl = await issueCredentialOffer(api, presentationFormat, defaults);
    const claimed = await claimOffer(api, walletId, did, offerUrl, defaults);
    if (!Array.isArray(claimed) || claimed.length === 0) {
      throw new Error("Expected at least one claimed credential.");
    }

    const walletCredentials = await waitForWalletCredentials(api, walletId, 1, defaults);
    walletCredentialCount = walletCredentials.length;
    await wallet.page.goto(`${defaults.walletBaseUrl}/wallet/${walletId}`, { waitUntil: "networkidle" });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "02-wallet-credential-available.png");

    const setup = buildVerifier2SessionSetup({
      presentationFormat,
      requestShape,
      signedClientId,
      enableTransactionData,
    });
    const createSessionResponse = await api.post(`${defaults.verifier2BaseUrl}/verification-session/create`, {
      data: setup,
    });
    if (!createSessionResponse.ok()) {
      throw new Error(
        `create verifier2 session failed: ${createSessionResponse.status()} ${await createSessionResponse.text()}`,
      );
    }

    const creationPayload = await createSessionResponse.json();
    verifier2SessionId = creationPayload.sessionId;
    if (!verifier2SessionId) {
      throw new Error("Missing verifier2 sessionId in /verification-session/create response.");
    }

    const initialSessionInfo = await fetchVerifier2SessionInfo(api, verifier2SessionId, defaults);
    let requestUriPostSupported = null;
    let requestUriPostProbeStatus = null;

    if (requestShape === "request_uri_post") {
      const requestUriPostProbe = await probeRequestUriPostSupport(api, verifier2SessionId);
      requestUriPostSupported = requestUriPostProbe.supported;
      requestUriPostProbeStatus = requestUriPostProbe.status;
    }

    if (requestShape === "request_uri_post" && !requestUriPostSupported) {
      if (requireRequestUriPost) {
        throw new Error(
          `request_uri_method=post scenario required but verifier2 /request endpoint rejected POST (status ${requestUriPostProbeStatus ?? "unknown"})`,
        );
      }
      metadata = {
        scenario: "verifier2-api",
        status: "SKIPPED_UNSUPPORTED",
        artifactDir,
        walletId,
        walletCredentialCount,
        verifier2SessionId,
        requestShape,
        presentationFormat,
        requestUriPostSupported,
        requestUriPostProbeStatus,
        reason: "request_uri_method=post not supported by verifier2 /request endpoint",
        preflight,
      };
      await renderVerifier2HarnessPanel(verifier.page, {
        phase: "Skipped unsupported request_uri_method=post",
        presentationFormat,
        requestShape,
        sessionId: verifier2SessionId,
        verifier2Status: metadata.status,
        requestUriPostSupported,
        requestUriPostProbeStatus,
        details: {
          reason: metadata.reason,
        },
      });
      await saveScreenshot(verifier.page, artifactDir, "verifier", "01-verifier2-skipped-unsupported.png");
      console.log(`Artifacts saved in ${artifactDir}`);
      console.log(JSON.stringify(metadata, null, 2));
      console.log(`RUN_STATUS:${metadata.status}`);
      console.log(`RUN_ARTIFACT_DIR:${artifactDir}`);
      await finalizeRun(artifactDir, [wallet, verifier], metadata);
      return;
    }

    const walletRequestUrl = buildWalletRequestUrl({
      requestShape,
      creationPayload,
      sessionInfo: initialSessionInfo,
    });

    if (!walletRequestUrl) {
      throw new Error(`Could not resolve wallet request URL for request shape "${requestShape}".`);
    }

    await renderVerifier2HarnessPanel(verifier.page, {
      phase: "Authorization request created",
      presentationFormat,
      requestShape,
      sessionId: verifier2SessionId,
      walletRequestUrl,
      verifier2Status: initialSessionInfo.status ?? "CREATED",
      requestUriPostSupported,
      requestUriPostProbeStatus,
      details: {
        requestMode: initialSessionInfo.requestMode ?? null,
        hasSignedAuthorizationRequestJwt: Boolean(initialSessionInfo.signedAuthorizationRequestJwt),
        hasAuthorizationRequest: Boolean(initialSessionInfo.authorizationRequest),
        verifier2CreateResponse: {
          hasBootstrapAuthorizationRequestUrl: Boolean(creationPayload.bootstrapAuthorizationRequestUrl),
          hasFullAuthorizationRequestUrl: Boolean(creationPayload.fullAuthorizationRequestUrl),
        },
      },
    });
    await saveScreenshot(verifier.page, artifactDir, "verifier", "01-verifier2-request-ready.png");

    const walletLaunchUrl = buildWalletInitiatePresentationUrl(defaults.walletBaseUrl, walletId, walletRequestUrl);
    await wallet.page.goto(walletLaunchUrl, { waitUntil: "networkidle" });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "03-wallet-presentation-request.png");
    await openAndPresent(wallet.page, { expectTransactionDetails: enableTransactionData });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "04-wallet-after-presentation.png");

    const terminal = await waitForVerifier2TerminalSessionStatus(api, verifier2SessionId, defaults);
    if (!["SUCCESSFUL", "COMPLETED"].includes(terminal.status)) {
      throw new Error(`Verifier2 session did not complete successfully: ${JSON.stringify(terminal)}`);
    }

    const finalSessionInfo = await fetchVerifier2SessionInfo(api, verifier2SessionId, defaults);
    await renderVerifier2HarnessPanel(verifier.page, {
      phase: "Presentation completed",
      presentationFormat,
      requestShape,
      sessionId: verifier2SessionId,
      walletRequestUrl,
      verifier2Status: finalSessionInfo.status ?? terminal.status,
      requestUriPostSupported,
      requestUriPostProbeStatus,
      details: {
        requestMode: finalSessionInfo.requestMode ?? null,
        signedAuthorizationRequestPresent: Boolean(finalSessionInfo.signedAuthorizationRequestJwt),
        policyResults: finalSessionInfo.policyResults ?? null,
        presentedCredentials: finalSessionInfo.presentedCredentials ?? null,
      },
    });
    await saveScreenshot(verifier.page, artifactDir, "verifier", "02-verifier2-terminal.png");

    metadata = {
      scenario: "verifier2-api",
      status: "SUCCESSFUL",
      artifactDir,
      walletId,
      walletCredentialCount,
      verifier2SessionId,
      requestShape,
      presentationFormat,
      claimedCredentials: claimed.length,
      verifier2Status: finalSessionInfo.status,
      requestUriPostSupported,
      requestUriPostProbeStatus,
      transactionDataEnabled: enableTransactionData,
      requestMode: finalSessionInfo.requestMode ?? null,
      signedAuthorizationRequestPresent: Boolean(finalSessionInfo.signedAuthorizationRequestJwt),
      preflight,
    };

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify(metadata, null, 2));
    console.log(`RUN_STATUS:${metadata.status}`);
    console.log(`RUN_ARTIFACT_DIR:${artifactDir}`);
    await finalizeRun(artifactDir, [wallet, verifier], metadata);
  } catch (error) {
    metadata = {
      scenario: "verifier2-api",
      status: "FAILED",
      artifactDir,
      walletId,
      walletCredentialCount,
      verifier2SessionId,
      requestShape,
      presentationFormat,
      transactionDataEnabled: enableTransactionData,
      error: String(error),
      preflight,
    };
    try {
      await renderVerifier2HarnessPanel(verifier.page, {
        phase: "Failed",
        presentationFormat,
        requestShape,
        sessionId: verifier2SessionId,
        verifier2Status: metadata.status,
        details: metadata,
        error: metadata.error,
      });
      await saveScreenshot(verifier.page, artifactDir, "verifier", "99-verifier2-failed.png");
    } catch {
      // Best effort failure evidence only.
    }
    console.log(`RUN_STATUS:${metadata.status}`);
    console.log(`RUN_ARTIFACT_DIR:${artifactDir}`);
    await finalizeRun(artifactDir, [wallet, verifier], metadata);
    throw error;
  } finally {
    await api.dispose();
  }
}

function buildVerifier2SessionSetup({ presentationFormat, requestShape, signedClientId, enableTransactionData }) {
  return {
    flow_type: "cross_device",
    core_flow: {
      dcql_query: buildDcqlQuery(presentationFormat),
      signed_request: requestShape === "request_object_signed",
      ...(requestShape === "request_object_signed" ? { clientId: signedClientId } : {}),
    },
    ...(enableTransactionData
      ? {
          openid: {
            transactionData: [buildEncodedTransactionData(presentationFormat)],
          },
        }
      : {}),
  };
}

function buildDcqlQuery(presentationFormat) {
  if (presentationFormat === "dc+sd-jwt") {
    return {
      credentials: [
        {
          id: "my_pid",
          format: "dc+sd-jwt",
          meta: {},
        },
      ],
    };
  }

  if (presentationFormat === "jwt_vc_json") {
    return {
      credentials: [
        {
          id: "my_identity",
          format: "jwt_vc_json",
          meta: {},
        },
      ],
    };
  }

  if (presentationFormat === "mso_mdoc") {
    return {
      credentials: [
        {
          id: "my_mdl",
          format: "mso_mdoc",
          meta: {
            doctype_value: "org.iso.18013.5.1.mDL",
          },
          claims: [
            {
              path: ["org.iso.18013.5.1", "given_name"],
            },
            {
              path: ["org.iso.18013.5.1", "family_name"],
            },
            {
              path: ["org.iso.18013.5.1", "issuing_country"],
            },
          ],
        },
      ],
    };
  }

  throw new Error(`Unsupported PRESENTATION_FORMAT "${presentationFormat}".`);
}

function buildWalletRequestUrl({ requestShape, creationPayload, sessionInfo }) {
  const full = creationPayload.fullAuthorizationRequestUrl;
  const bootstrap = creationPayload.bootstrapAuthorizationRequestUrl;

  switch (requestShape) {
    case "direct":
      return requireUrl(full, "fullAuthorizationRequestUrl");

    case "request_uri_get":
      return requireUrl(bootstrap, "bootstrapAuthorizationRequestUrl");

    case "request_uri_post": {
      const url = new URL(requireUrl(bootstrap, "bootstrapAuthorizationRequestUrl"));
      url.searchParams.set("request_uri_method", "post");
      return url.toString();
    }

    case "request_object_unsigned": {
      const requestObject = buildUnsignedRequestObjectJwt(sessionInfo.authorizationRequest);
      return `openid4vp://authorize?request=${encodeURIComponent(requestObject)}`;
    }

    case "request_object_signed": {
      const requestObject = sessionInfo.signedAuthorizationRequestJwt;
      if (!requestObject) {
        throw new Error("Signed request-object scenario requested but session has no signedAuthorizationRequestJwt.");
      }
      return `openid4vp://authorize?request=${encodeURIComponent(requestObject)}`;
    }

    default:
      throw new Error(`Unsupported request shape "${requestShape}".`);
  }
}

function buildUnsignedRequestObjectJwt(payload) {
  if (!payload || typeof payload !== "object") {
    throw new Error("Cannot build unsigned request object: missing authorizationRequest payload.");
  }

  const encodedHeader = base64UrlEncodeObject({
    alg: "none",
    typ: "oauth-authz-req+jwt",
  });
  const encodedPayload = base64UrlEncodeObject(payload);
  return `${encodedHeader}.${encodedPayload}.`;
}

async function probeRequestUriPostSupport(api, verifier2SessionId) {
  const response = await api.post(
    `${defaults.verifier2BaseUrl}/verification-session/${encodeURIComponent(verifier2SessionId)}/request`,
    {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/oauth-authz-req+jwt, application/json, */*",
      },
      data: "",
    },
  );

  const status = response.status();
  return {
    supported: status >= 200 && status < 300,
    status,
  };
}

function requireUrl(url, fieldName) {
  if (!url) {
    throw new Error(`Missing ${fieldName} in verifier2 session creation response.`);
  }
  return url;
}

function base64UrlEncodeObject(value) {
  return Buffer.from(JSON.stringify(value))
    .toString("base64")
    .replaceAll("=", "")
    .replaceAll("+", "-")
    .replaceAll("/", "_");
}

function envBool(name, fallback) {
  const value = process.env[name];
  if (value == null) return fallback;
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

function buildEncodedTransactionData(presentationFormat) {
  const credentialId = presentationFormat === "mso_mdoc" ? "my_mdl" : "my_pid";
  const payload = {
    type: "org.waltid.transaction-data.payment-authorization",
    credential_ids: [credentialId],
    require_cryptographic_holder_binding: true,
    amount: "42.00",
    currency: "EUR",
    payee: "ACME Corp",
    reference: "INV-2026-042",
    ...(presentationFormat === "dc+sd-jwt" ? { transaction_data_hashes_alg: ["sha-256"] } : {}),
  };

  return Buffer.from(JSON.stringify(payload))
    .toString("base64")
    .replaceAll("=", "")
    .replaceAll("+", "-")
    .replaceAll("/", "_");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
