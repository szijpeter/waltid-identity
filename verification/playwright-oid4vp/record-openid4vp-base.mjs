import path from "node:path";
import {
  apiContext,
  attachConsoleLogging,
  buildLaunchUrl,
  buildSdJwtCredentialQuery,
  claimOffer,
  createDid,
  createVerificationSession,
  defaults,
  fetchSessionInfo,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  loginInBrowser,
  makeArtifactDir,
  openAndPresent,
  recordVerifierArtifacts,
  registerAndLogin,
  saveScreenshot,
  saveVerifierInfoScreenshot,
  waitForTerminalSessionStatus,
} from "./lib/common.mjs";

async function main() {
  const artifactDir = await makeArtifactDir("base");
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
    const claimed = await claimOffer(api, walletId, did, offerUrl, defaults);
    console.log(`Wallet now has ${claimed.length} credential(s)`);

    const session = await createVerificationSession(api, {
      flow_type: "cross_device",
      core_flow: {
        dcql_query: {
          credentials: [buildSdJwtCredentialQuery(defaults.issuerApiBaseUrl)],
        },
      },
    }, defaults);
    sessionId = session.sessionId;
    console.log(`Verifier session created: ${sessionId}`);
    await recordVerifierArtifacts(api, sessionId, artifactDir, defaults, "initial");
    await saveVerifierInfoScreenshot(context, sessionId, artifactDir, defaults, "initial");

    const launchUrl = buildLaunchUrl(
      defaults.walletBaseUrl,
      walletId,
      session.bootstrapAuthorizationRequestUrl ?? session.fullAuthorizationRequestUrl,
    );

    await page.goto(launchUrl, { waitUntil: "networkidle" });
    await saveScreenshot(page, artifactDir, "01-wallet-loaded.png");
    await openAndPresent(page, launchUrl);

    const terminal = await waitForTerminalSessionStatus(api, sessionId, defaults);
    const verifierFinal = await recordVerifierArtifacts(api, sessionId, artifactDir, defaults, "final");
    await saveVerifierInfoScreenshot(context, sessionId, artifactDir, defaults, "final");
    await saveScreenshot(page, artifactDir, "02-wallet-after-submit.png");

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify({ sessionId, walletId, status: terminal.status }, null, 2));

    await finalizeRun(context, browser, artifactDir, {
      sessionId,
      walletId,
      terminalStatus: terminal.status,
      verifierFinalStatus: verifierFinal.status,
      artifactDir,
    });
  } catch (error) {
    await finalizeRun(context, browser, artifactDir, {
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
