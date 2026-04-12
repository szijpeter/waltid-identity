import fs from "node:fs/promises";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { defaults, recreateHarnessStack, requireHarnessPreflight, timestamp } from "./lib/common.mjs";

const artifactBranchTag = process.env.ARTIFACT_BRANCH_TAG ?? "transaction-data-support";
const includeMdocPortal = envBool("INCLUDE_MDOC", true);
const includeApiMatrix = envBool("PR2_INCLUDE_API_MATRIX", true);
const matrixFormats = parseCsv(process.env.PR2_MATRIX_FORMATS ?? "dc+sd-jwt,mso_mdoc");
const baseShapes = parseCsv(
  process.env.PR2_REQUEST_SHAPES ??
    "direct,request_uri_get,request_object_unsigned,request_object_signed",
);
const checkRequestUriPost = envBool("CHECK_REQUEST_URI_POST", true);
const requireRequestUriPost = envBool("REQUIRE_REQUEST_URI_POST", false);
const recreateStack = envBool("HARNESS_RECREATE_STACK", true);

async function main() {
  const preflight = recreateStack
    ? recreateHarnessStack()
    : requireHarnessPreflight({ requireRunningServices: true });

  const summaryDir = path.join(
    defaults.artifactsBaseDir,
    `${slugify(artifactBranchTag)}--pr2-matrix-summary--${timestamp()}`,
  );
  await fs.mkdir(summaryDir, { recursive: true });

  const requestShapes = [...new Set(checkRequestUriPost ? [...baseShapes, "request_uri_post"] : baseShapes)];
  const results = [];

  results.push(
    runScript("record-portal-legacy.mjs", {
      ARTIFACT_BRANCH_TAG: artifactBranchTag,
      LEGACY_FORMAT: process.env.LEGACY_FORMAT ?? "JWT + W3C VC",
      LEGACY_DISABLE_SIGNATURE_POLICY: process.env.LEGACY_DISABLE_SIGNATURE_POLICY ?? "false",
    }, {
      id: "legacy-jwt-w3c",
      kind: "legacy",
      required: true,
    }),
  );

  results.push(
    runScript("record-portal-verifier2.mjs", {
      ARTIFACT_BRANCH_TAG: artifactBranchTag,
      PRESENTATION_FORMAT: "dc+sd-jwt",
    }, {
      id: "verifier2-portal-dc+sd-jwt",
      kind: "verifier2-portal",
      required: true,
    }),
  );

  if (includeMdocPortal) {
    results.push(
      runScript("record-portal-verifier2.mjs", {
        ARTIFACT_BRANCH_TAG: artifactBranchTag,
        PRESENTATION_FORMAT: "mso_mdoc",
      }, {
        id: "verifier2-portal-mso_mdoc",
        kind: "verifier2-portal",
        required: true,
      }),
    );
  }

  if (includeApiMatrix) {
    for (const presentationFormat of matrixFormats) {
      for (const requestShape of requestShapes) {
        const required = requestShape !== "request_uri_post" || requireRequestUriPost;
        results.push(
          runScript("record-verifier2-api.mjs", {
            ARTIFACT_BRANCH_TAG: artifactBranchTag,
            PRESENTATION_FORMAT: presentationFormat,
            OID4VP_REQUEST_SHAPE: requestShape,
            REQUIRE_REQUEST_URI_POST: requireRequestUriPost ? "true" : "false",
            ENABLE_TRANSACTION_DATA: "true",
            PR1_SIGNED_CLIENT_ID: process.env.PR1_SIGNED_CLIENT_ID ?? "x509_san_dns:verifier.example.com",
          }, {
            id: `verifier2-api-${presentationFormat}-${requestShape}-transaction-data`,
            kind: "verifier2-api",
            required,
          }),
        );
      }
    }
  }

  const mandatoryFailures = results.filter((result) => result.required && result.status !== "SUCCESSFUL");
  const summary = {
    scenario: "pr2-suite",
    status: mandatoryFailures.length === 0 ? "SUCCESSFUL" : "FAILED",
    generatedAt: new Date().toISOString(),
    artifactBranchTag,
    matrix: {
      includeMdocPortal,
      includeApiMatrix,
      formats: matrixFormats,
      requestShapes,
      checkRequestUriPost,
      requireRequestUriPost,
    },
    preflight,
    results,
    mandatoryFailures: mandatoryFailures.map(({ id, status, exitCode, error }) => ({
      id,
      status,
      exitCode,
      error,
    })),
  };

  const summaryPath = path.join(summaryDir, "run-summary.json");
  await fs.writeFile(summaryPath, JSON.stringify(summary, null, 2), "utf8");
  console.log(`PR2 matrix summary saved to ${summaryPath}`);
  console.log(JSON.stringify(summary, null, 2));

  if (mandatoryFailures.length > 0) {
    throw new Error(
      `PR2 suite has ${mandatoryFailures.length} mandatory failure(s): ${mandatoryFailures.map((it) => it.id).join(", ")}`,
    );
  }
}

function runScript(script, extraEnv, scenarioMeta) {
  const result = spawnSync(process.execPath, [script], {
    env: { ...process.env, ...extraEnv },
    encoding: "utf8",
  });

  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);

  const status = parseStatus(result.stdout) ?? (result.status === 0 ? "SUCCESSFUL" : "FAILED");
  const artifactDir = parseArtifactDir(result.stdout);

  return {
    id: scenarioMeta.id,
    kind: scenarioMeta.kind,
    required: scenarioMeta.required,
    script,
    status,
    artifactDir,
    exitCode: result.status ?? null,
    error: result.status === 0 ? null : `${script} failed with exit code ${result.status ?? "unknown"}`,
  };
}

function parseStatus(stdout) {
  const statusMatch = stdout?.match(/RUN_STATUS:([A-Z_]+)/);
  return statusMatch?.[1] ?? null;
}

function parseArtifactDir(stdout) {
  const explicitArtifact = stdout?.match(/RUN_ARTIFACT_DIR:(.+)/)?.[1]?.trim();
  if (explicitArtifact) return explicitArtifact;

  const artifactFromLog = stdout?.match(/Artifacts saved in (.+)/)?.[1]?.trim();
  return artifactFromLog ?? null;
}

function parseCsv(value) {
  return String(value)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function envBool(name, fallback) {
  const value = process.env[name];
  if (value == null) return fallback;
  return ["1", "true", "yes", "on"].includes(value.toLowerCase());
}

function slugify(value) {
  return String(value)
    .trim()
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, "-")
    .replaceAll(/^-+|-+$/g, "");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
