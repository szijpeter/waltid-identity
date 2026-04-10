import {
  apiContext,
  attachConsoleLogging,
  buildSdJwtCredentialQuery,
  claimOffer,
  createDid,
  createVerificationSession,
  defaults,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  loginInBrowser,
  makeArtifactDir,
  recordVerifierArtifacts,
  registerAndLogin,
  saveScreenshot,
  saveVerifierInfoScreenshot,
} from "./lib/common.mjs";

async function main() {
  const artifactDir = await makeArtifactDir("smoke-main");
  const api = await apiContext();
  const { browser, context, page } = await launchBrowserContext(artifactDir, defaults);
  attachConsoleLogging(page);

  let walletId = null;
  let sessionId = null;

  try {
    const account = await registerAndLogin(api, defaults);
    await loginInBrowser(page, account, defaults);

    const wallets = await listWallets(api, defaults);
    walletId = wallets.wallets[0]?.id;
    if (!walletId) {
      throw new Error("No wallet found for account.");
    }

    const did = await createDid(api, walletId, "jwk", defaults);
    const offerUrl = await issueCredentialOffer(api, "dc+sd-jwt", defaults);
    const claimed = await claimOffer(api, walletId, did, offerUrl, defaults);

    if (!Array.isArray(claimed) || claimed.length === 0) {
      throw new Error("Expected at least one claimed credential.");
    }

    const session = await createVerificationSession(api, {
      flow_type: "cross_device",
      core_flow: {
        dcql_query: {
          credentials: [buildSdJwtCredentialQuery(defaults.issuerApiBaseUrl)],
        },
      },
    }, defaults);
    sessionId = session.sessionId;

    const verifierInitial = await recordVerifierArtifacts(api, sessionId, artifactDir, defaults, "initial");
    await saveVerifierInfoScreenshot(context, sessionId, artifactDir, defaults, "initial");

    await page.goto(`${defaults.walletBaseUrl}/wallet/${walletId}`, { waitUntil: "networkidle" });
    await saveScreenshot(page, artifactDir, "01-wallet-overview.png");

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify({
      walletId,
      sessionId,
      claimedCredentials: claimed.length,
      verifierInitialStatus: verifierInitial.status,
      status: "SUCCESSFUL",
    }, null, 2));

    await finalizeRun(context, browser, artifactDir, {
      walletId,
      sessionId,
      claimedCredentials: claimed.length,
      verifierInitialStatus: verifierInitial.status,
      status: "SUCCESSFUL",
      artifactDir,
    });
  } catch (error) {
    await finalizeRun(context, browser, artifactDir, {
      walletId,
      sessionId,
      status: "FAILED",
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
