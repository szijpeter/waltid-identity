import {
  apiContext,
  attachConsoleLogging,
  buildWalletInitiatePresentationUrl,
  claimOffer,
  createDid,
  defaults,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  loginInBrowser,
  makeArtifactDir,
  openAndPresent,
  parseStateFromRequestUrl,
  registerAndLogin,
  saveScreenshot,
  waitForLegacyVerificationResult,
  waitForWalletCredentials,
} from "./lib/common.mjs";

const legacyCredentialId = process.env.LEGACY_CREDENTIAL_ID ?? "IdentityCredential";
const legacyFormat = process.env.LEGACY_FORMAT ?? "JWT + W3C VC";
const disableSignaturePolicy = !["0", "false", "no"].includes(
  (process.env.LEGACY_DISABLE_SIGNATURE_POLICY ?? "false").toLowerCase(),
);

async function main() {
  const artifactDir = await makeArtifactDir("legacy-verifier-portal");
  const api = await apiContext();
  const wallet = await launchBrowserContext(artifactDir, "wallet", defaults);
  const verifier = await launchBrowserContext(artifactDir, "verifier", defaults);
  attachConsoleLogging(wallet.page, "wallet");
  attachConsoleLogging(verifier.page, "verifier");

  let walletId = null;
  let legacySessionId = null;
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
    const legacyIssuanceFormat =
      legacyFormat === "JWT + W3C VC" ? "jwt_vc_json" : "dc+sd-jwt";
    const offerUrl = await issueCredentialOffer(api, legacyIssuanceFormat, defaults);
    const claimed = await claimOffer(api, walletId, did, offerUrl, defaults);
    if (!Array.isArray(claimed) || claimed.length === 0) {
      throw new Error("Expected at least one claimed credential.");
    }
    const walletCredentials = await waitForWalletCredentials(api, walletId, 1, defaults);
    walletCredentialCount = walletCredentials.length;
    await wallet.page.goto(`${defaults.walletBaseUrl}/wallet/${walletId}`, { waitUntil: "networkidle" });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "02-wallet-credential-available.png");

    await verifier.page.goto(defaults.portalBaseUrl, { waitUntil: "networkidle" });
    await verifier.page.getByText(legacyCredentialId).first().waitFor();
    await saveScreenshot(verifier.page, artifactDir, "verifier", "01-verifier-home.png");

    await verifier.page.locator("h6", { hasText: legacyCredentialId }).first().click();
    await verifier.page.getByRole("button", { name: /^Start$/i }).click();
    await verifier.page.waitForURL(/\/credentials\?/);
    await verifier.page.getByRole("button", { name: /^Verify$/i }).click();
    await verifier.page.getByRole("heading", { name: /Customise Verification/i }).waitFor();

    const formatDropdown = verifier.page.locator("[id^='headlessui-listbox-button-']").first();
    await formatDropdown.click();
    await verifier.page.getByRole("listbox").getByText(legacyFormat, { exact: true }).first().click();

    if (disableSignaturePolicy) {
      const signatureUnchecked = await verifier.page.evaluate(() => {
        const textElement = Array.from(document.querySelectorAll("*")).find(
          (el) => el.textContent?.trim() === "Signature Policy",
        );
        if (!textElement) {
          return false;
        }

        const row =
          textElement.closest("label, li, div, tr") ??
          textElement.parentElement ??
          textElement;
        const checkbox = row.querySelector("input[type='checkbox']");
        if (!checkbox) {
          return false;
        }

        if (checkbox.checked) {
          checkbox.click();
        }

        return !checkbox.checked;
      });

      if (!signatureUnchecked) {
        throw new Error("Could not disable Signature Policy in verifier customize screen.");
      }
    }

    await saveScreenshot(verifier.page, artifactDir, "verifier", "02-verifier-customized.png");

    const verifyRequestBodyPromise = verifier.page.waitForRequest((request) =>
      request.method() === "POST" && request.url().includes("/openid4vc/verify"),
    );
    const verifyRequestPromise = verifier.page.waitForResponse((response) =>
      response.request().method() === "POST" && response.url().includes("/openid4vc/verify"),
    );
    await verifier.page.getByRole("button", { name: /^Verify$/i }).last().click();
    const verifyRequestBody = await verifyRequestBodyPromise;
    const verifyRequestText = verifyRequestBody.postData() ?? "";
    if (disableSignaturePolicy && verifyRequestText.toLowerCase().includes("signature")) {
      throw new Error(
        `Signature Policy still present in /openid4vc/verify payload despite disable request: ${verifyRequestText}`,
      );
    }
    const verifyResponse = await verifyRequestPromise;
    const verifyResponseText = (await verifyResponse.text()).trim();

    if (verifyResponse.status() === 404) {
      throw new Error("Legacy verifier endpoint /openid4vc/verify is unavailable (HTTP 404). Run the stack with the draft verifier API for the legacy scenario.");
    }

    if (!verifyResponseText.startsWith("openid4vp://")) {
      throw new Error(
        `Legacy verifier did not return an OpenID4VP request URL (HTTP ${verifyResponse.status()}): ${verifyResponseText}`,
      );
    }
    const verifyRequestUrl = verifyResponseText;

    legacySessionId = parseStateFromRequestUrl(verifyRequestUrl);
    if (!legacySessionId) {
      throw new Error("Could not extract legacy verifier session state from request URL.");
    }

    await saveScreenshot(verifier.page, artifactDir, "verifier", "03-verifier-request-ready.png");

    const walletLaunchUrl = buildWalletInitiatePresentationUrl(defaults.walletBaseUrl, verifyRequestUrl);
    await wallet.page.goto(walletLaunchUrl, { waitUntil: "networkidle" });
    await saveScreenshot(wallet.page, artifactDir, "wallet", "03-wallet-presentation-request.png");
    await openAndPresent(wallet.page);
    await saveScreenshot(wallet.page, artifactDir, "wallet", "04-wallet-after-presentation.png");

    const legacyResult = await waitForLegacyVerificationResult(api, legacySessionId, defaults);
    if (legacyResult.verificationResult !== true) {
      throw new Error(`Legacy verifier session ended without success: ${JSON.stringify(legacyResult)}`);
    }

    try {
      await verifier.page.waitForURL(/\/success\/[^/]+/, { timeout: 90000 });
    } catch {
      await verifier.page.goto(`${defaults.portalBaseUrl}/success/${legacySessionId}`, { waitUntil: "networkidle" });
    }
    await verifier.page.locator("h1").filter({ hasText: /Presented Credentials/i }).waitFor();
    await saveScreenshot(verifier.page, artifactDir, "verifier", "04-verifier-success.png");

    const metadata = {
      scenario: "legacy-portal",
      status: "SUCCESSFUL",
      artifactDir,
      walletId,
      walletCredentialCount,
      legacySessionId,
      claimedCredentials: claimed.length,
      legacyVerificationResult: legacyResult.verificationResult,
    };

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify(metadata, null, 2));
    console.log(`RUN_STATUS:${metadata.status}`);
    console.log(`RUN_ARTIFACT_DIR:${artifactDir}`);
    await finalizeRun(artifactDir, [wallet, verifier], metadata);
  } catch (error) {
    const metadata = {
      scenario: "legacy-portal",
      status: "FAILED",
      artifactDir,
      walletId,
      walletCredentialCount,
      legacySessionId,
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
