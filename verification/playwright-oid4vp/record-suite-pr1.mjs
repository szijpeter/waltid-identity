import { spawnSync } from "node:child_process";

const includeMdoc = ["1", "true", "yes"].includes((process.env.INCLUDE_MDOC ?? "").toLowerCase());
const artifactBranchTag = process.env.ARTIFACT_BRANCH_TAG ?? "wallet-openid4vp-v1";

function runScript(script, extraEnv = {}) {
  const result = spawnSync(process.execPath, [script], {
    stdio: "inherit",
    env: { ...process.env, ...extraEnv },
  });
  if (result.status !== 0) {
    throw new Error(`${script} failed with exit code ${result.status ?? "unknown"}`);
  }
}

runScript("record-portal-legacy.mjs", { ARTIFACT_BRANCH_TAG: artifactBranchTag });
runScript("record-portal-verifier2.mjs", {
  ARTIFACT_BRANCH_TAG: artifactBranchTag,
  PRESENTATION_FORMAT: "dc+sd-jwt",
});
if (includeMdoc) {
  runScript("record-portal-verifier2.mjs", {
    ARTIFACT_BRANCH_TAG: artifactBranchTag,
    PRESENTATION_FORMAT: "mso_mdoc",
  });
}
