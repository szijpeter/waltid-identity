import { spawnSync } from "node:child_process";
import { recreateHarnessStack, requireHarnessPreflight } from "./lib/common.mjs";

function runScript(script, extraEnv = {}) {
  const result = spawnSync(process.execPath, [script], {
    stdio: "inherit",
    env: { ...process.env, ...extraEnv },
  });
  if (result.status !== 0) {
    throw new Error(`${script} failed with exit code ${result.status ?? "unknown"}`);
  }
}

const recreateStack = envBool("HARNESS_RECREATE_STACK", true);
if (recreateStack) {
  recreateHarnessStack();
} else {
  requireHarnessPreflight({ requireRunningServices: true });
}

runScript("record-portal-legacy.mjs", {
  ARTIFACT_BRANCH_TAG: process.env.ARTIFACT_BRANCH_TAG ?? "main",
});

function envBool(name, fallback) {
  const value = process.env[name];
  if (value == null) return fallback;
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}
