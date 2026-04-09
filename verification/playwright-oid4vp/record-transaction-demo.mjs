import {
  apiContext,
  attachConsoleLogging,
  buildLaunchUrl,
  buildMdocCredentialQuery,
  buildSdJwtCredentialQuery,
  claimOffer,
  createDid,
  createVerificationSession,
  defaults,
  encodeBase64Url,
  finalizeRun,
  issueCredentialOffer,
  launchBrowserContext,
  listWallets,
  makeArtifactDir,
  openAndPresent,
  registerAndLogin,
  saveScreenshot,
  waitForTerminalSessionStatus,
} from "./lib/common.mjs";

const presentationFormat = process.env.PRESENTATION_FORMAT ?? "dc+sd-jwt";
const transactionDataType = "org.waltid.transaction-data.payment-authorization";

function buildTransactionPreview() {
  return {
    type: transactionDataType,
    credential_ids: ["payment_credential"],
    ...(presentationFormat === "dc+sd-jwt" ? { transaction_data_hashes_alg: ["sha-256"] } : {}),
    require_cryptographic_holder_binding: true,
    amount: "42.00",
    currency: "EUR",
    payee: "ACME Corp",
    reference: "INV-2026-042",
  };
}

async function main() {
  const artifactDir = await makeArtifactDir("transaction");
  const api = await apiContext();
  const { browser, context, page } = await launchBrowserContext(artifactDir, defaults);
  attachConsoleLogging(page);

  let sessionId = null;
  let walletId = null;

  try {
    await registerAndLogin(api, defaults);
    const wallets = await listWallets(api, defaults);
    walletId = wallets.wallets[0].id;
    const did = await createDid(api, walletId, "jwk", defaults);

    const offerUrl = await issueCredentialOffer(api, presentationFormat, defaults);
    const claimed = await claimOffer(api, walletId, did, offerUrl, defaults);
    console.log(`Wallet now has ${claimed.length} credential(s)`);

    const transactionPreview = buildTransactionPreview();
    const session = await createVerificationSession(api, {
      flow_type: "cross_device",
      core_flow: {
        dcql_query: {
          credentials: [
            presentationFormat === "mso_mdoc"
              ? buildMdocCredentialQuery()
              : buildSdJwtCredentialQuery(defaults.issuerApiBaseUrl),
          ],
        },
      },
      openid: {
        transactionData: [encodeBase64Url(JSON.stringify(transactionPreview))],
      },
    }, defaults);
    sessionId = session.sessionId;
    console.log(`Verifier session created: ${sessionId}`);

    const launchUrl = buildLaunchUrl(
      defaults.walletBaseUrl,
      walletId,
      session.bootstrapAuthorizationRequestUrl ?? session.fullAuthorizationRequestUrl,
    );

    await page.goto(launchUrl, { waitUntil: "networkidle" });
    await saveScreenshot(page, artifactDir, "01-wallet-loaded.png");
    await page.locator("text=Transaction details").waitFor();
    await saveScreenshot(page, artifactDir, "02-transaction-details.png");
    await openAndPresent(page, launchUrl, { expectTransactionDetails: true });

    const terminal = await waitForTerminalSessionStatus(api, sessionId, defaults);
    await saveScreenshot(page, artifactDir, "03-wallet-after-submit.png");

    console.log(`Artifacts saved in ${artifactDir}`);
    console.log(JSON.stringify({ presentationFormat, sessionId, walletId, status: terminal.status }, null, 2));

    await finalizeRun(context, browser, artifactDir, {
      presentationFormat,
      sessionId,
      walletId,
      terminalStatus: terminal.status,
      artifactDir,
    });
  } catch (error) {
    await finalizeRun(context, browser, artifactDir, {
      presentationFormat,
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
