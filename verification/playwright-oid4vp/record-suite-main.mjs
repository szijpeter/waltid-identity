import { spawnSync } from "node:child_process";

function runScript(script, extraEnv = {}) {
  const result = spawnSync(process.execPath, [script], {
    stdio: "inherit",
    env: { ...process.env, ...extraEnv },
  });
  if (result.status !== 0) {
    throw new Error(`${script} failed with exit code ${result.status ?? "unknown"}`);
  }
}

runScript("record-portal-legacy.mjs", {
  ARTIFACT_BRANCH_TAG: process.env.ARTIFACT_BRANCH_TAG ?? "main",
});
