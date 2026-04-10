import { spawnSync } from "node:child_process";

const includeMdoc = ["0", "false", "no"].includes((process.env.INCLUDE_MDOC ?? "").toLowerCase())
  ? "false"
  : "true";

const result = spawnSync(process.execPath, ["record-suite-pr1-portal.mjs"], {
  stdio: "inherit",
  env: {
    ...process.env,
    ARTIFACT_BRANCH_TAG: process.env.ARTIFACT_BRANCH_TAG ?? "transaction-data-support",
    INCLUDE_MDOC: process.env.INCLUDE_MDOC ?? includeMdoc,
  },
});

if (result.status !== 0) {
  throw new Error(`record-suite-pr1-portal.mjs failed with exit code ${result.status ?? "unknown"}`);
}
