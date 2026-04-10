import {
  apiContext,
  attachConsoleLogging,
  buildWalletInitiatePresentationUrl,
  claimOffer,
  createDid,
  defaults,
  ensureTransactionPageAvailable,
  fetchVerifier2SessionInfo,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  loginInBrowser,
  makeArtifactDir,
  openAndPresent,
  registerAndLogin,
  saveScreenshot,
  waitForVerifier2TerminalSessionStatus,
  waitForWalletCredentials,
} from "./lib/common.mjs";

const presentationFormat = process.env.PRESENTATION_FORMAT ?? "dc+sd-jwt";
const isMdoc = presentationFormat === "mso_mdoc";

async function main() {
  const artifactDir = await makeArtifactDir(`verifier2-portal-${presentationFormat.replace("+", "-")}`);
  const api = await apiContext();
  const wallet = await launchBrowserContext(artifactDir, "wallet", defaults);
  const verifier = await launchBrowserContext(artifactDir, "verifier", defaults);
  attachConsoleLogging(wallet.page, "wallet");
  attachConsoleLogging(verifier.page, "verifier");

  let walletId = null;
  let verifier2SessionId = null;
  let walletCredentialCount = 0;

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

    await ensureTransactionPageAvailable(verifier.page, defaults);
    await saveScreenshot(verifier.page, artifactDir, "verifier", "01-verifier2-form.png");

    if (isMdoc) {
      await verifier.page.locator("select").first().selectOption("mso_mdoc");
    }

    const createSessionPromise = verifier.page.waitForResponse((response) =>
      response.request().method() === "POST" && response.url().includes("/verification-session/create"),
    );

    await verifier.page.getByRole("button", { name: /Create verification request/i }).click();
    const createSessionResponse = await createSessionPromise;
    const createSessionPayload = await createSessionResponse.json();
    verifier2SessionId = createSessionPayload.sessionId;

    const walletRequestUrl =
      createSessionPayload.fullAuthorizationRequestUrl ??
      createSessionPayload.bootstrapAuthorizationRequestUrl;

    if (!walletRequestUrl) {
      throw new Error("Verifier2 portal did not return a wallet request URL.");
    }

    await saveScreenshot(verifier.page, artifactDir, "verifier", "02-verifier2-request-ready.png");

    const walletLaunchUrl = buildWalletInitiatePresentationUrl(defaults.walletBaseUrl, walletRequestUrl);
    await wallet.page.goto(walletLaunchUrl, { waitUntil: "networkidle" });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "03-wallet-presentation-request.png");
    await openAndPresent(wallet.page, { expectTransactionDetails: true });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "04-wallet-after-presentation.png");

    const terminal = await waitForVerifier2TerminalSessionStatus(api, verifier2SessionId, defaults);
    if (!["SUCCESSFUL", "COMPLETED"].includes(terminal.status)) {
      throw new Error(`Verifier2 session did not complete successfully: ${JSON.stringify(terminal)}`);
    }

    await verifier.page.getByText(/SUCCESSFUL|COMPLETED/i).first().waitFor();
    await saveScreenshot(verifier.page, artifactDir, "verifier", "03-verifier2-terminal.png");

    const finalSessionInfo = await fetchVerifier2SessionInfo(api, verifier2SessionId, defaults);

    const metadata = {
      scenario: "verifier2-portal",
      status: "SUCCESSFUL",
      artifactDir,
      walletId,
      walletCredentialCount,
      verifier2SessionId,
      presentationFormat,
      claimedCredentials: claimed.length,
      verifier2Status: finalSessionInfo.status,
    };

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify(metadata, null, 2));
    console.log(`RUN_STATUS:${metadata.status}`);
    console.log(`RUN_ARTIFACT_DIR:${artifactDir}`);
    await finalizeRun(artifactDir, [wallet, verifier], metadata);
  } catch (error) {
    const metadata = {
      scenario: "verifier2-portal",
      status: "FAILED",
      artifactDir,
      walletId,
      walletCredentialCount,
      verifier2SessionId,
      presentationFormat,
      error: String(error),
    };
    console.log(`RUN_STATUS:${metadata.status}`);
    console.log(`RUN_ARTIFACT_DIR:${artifactDir}`);
    await finalizeRun(artifactDir, [wallet, verifier], metadata);
    throw error;
  } finally {
    await api.dispose();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
