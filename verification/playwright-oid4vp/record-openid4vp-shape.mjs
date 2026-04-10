import {
  apiContext,
  attachConsoleLogging,
  buildLaunchUrl,
  buildSdJwtCredentialQuery,
  claimOffer,
  createDid,
  createVerificationSession,
  defaults,
  fetchSessionRequest,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  loginInBrowser,
  makeArtifactDir,
  makeSignedRequestObject,
  makeUnsignedRequestObject,
  openAndPresent,
  recordVerifierArtifacts,
  registerAndLogin,
  requestPayloadToUrl,
  saveScreenshot,
  saveVerifierInfoScreenshot,
  waitForTerminalSessionStatus,
} from "./lib/common.mjs";

function getShape() {
  const arg = process.argv.find((value) => value.startsWith("--shape="));
  return arg?.split("=")[1] ?? process.env.SHAPE ?? "direct";
}

async function main() {
  const shape = getShape();
  const artifactDir = await makeArtifactDir(shape);
  const api = await apiContext();
  const { browser, context, page } = await launchBrowserContext(artifactDir, defaults);
  attachConsoleLogging(page);

  let sessionId = null;
  let walletId = null;

  try {
    const account = await registerAndLogin(api, defaults);
    await loginInBrowser(page, account, defaults);
    const wallets = await listWallets(api, defaults);
    walletId = wallets.wallets[0].id;
    const did = await createDid(api, walletId, "jwk", defaults);

    const offerUrl = await issueCredentialOffer(api, "dc+sd-jwt", defaults);
    await claimOffer(api, walletId, did, offerUrl, defaults);

    const session = await createVerificationSession(api, {
      flow_type: "cross_device",
      core_flow: {
        dcql_query: {
          credentials: [buildSdJwtCredentialQuery(defaults.issuerApiBaseUrl)],
        },
      },
    }, defaults);
    sessionId = session.sessionId;
    await recordVerifierArtifacts(api, sessionId, artifactDir, defaults, "initial");
    await saveVerifierInfoScreenshot(context, sessionId, artifactDir, defaults, "initial");

    const fetched = await fetchSessionRequest(api, sessionId, defaults);
    const requestPayload = JSON.parse(fetched.body);

    let launchRequestUrl;
    if (shape === "request") {
      launchRequestUrl = `openid4vp://authorize?request=${encodeURIComponent(makeUnsignedRequestObject(requestPayload))}`;
    } else if (shape === "signed-request") {
      launchRequestUrl = `openid4vp://authorize?request=${encodeURIComponent(makeSignedRequestObject(requestPayload))}`;
    } else {
      launchRequestUrl = requestPayloadToUrl(requestPayload);
    }

    const launchUrl = buildLaunchUrl(defaults.walletBaseUrl, walletId, launchRequestUrl);
    await page.goto(launchUrl, { waitUntil: "networkidle" });
    await saveScreenshot(page, artifactDir, "01-wallet-loaded.png");
    await openAndPresent(page, launchUrl);

    const terminal = await waitForTerminalSessionStatus(api, sessionId, defaults);
    const verifierFinal = await recordVerifierArtifacts(api, sessionId, artifactDir, defaults, "final");
    await saveVerifierInfoScreenshot(context, sessionId, artifactDir, defaults, "final");
    await saveScreenshot(page, artifactDir, "02-wallet-after-submit.png");

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify({ shape, sessionId, walletId, status: terminal.status }, null, 2));

    await finalizeRun(context, browser, artifactDir, {
      shape,
      sessionId,
      walletId,
      terminalStatus: terminal.status,
      verifierFinalStatus: verifierFinal.status,
      artifactDir,
    });
  } catch (error) {
    await finalizeRun(context, browser, artifactDir, {
      shape,
      sessionId,
      walletId,
      error: String(error),
      artifactDir,
    });
    throw error;
  } finally {
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
