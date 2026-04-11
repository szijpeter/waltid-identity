import fs from "node:fs/promises";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { defaults, timestamp } from "./lib/common.mjs";

const artifactBranchTag = process.env.ARTIFACT_BRANCH_TAG ?? "wallet-openid4vp-v1";
const matrixFormats = parseCsv(process.env.PR1_MATRIX_FORMATS ?? "dc+sd-jwt,jwt_vc_json");
const baseShapes = parseCsv(
  process.env.PR1_REQUEST_SHAPES ??
    "direct,request_uri_get,request_object_unsigned,request_object_signed",
);
const checkRequestUriPost = envBool("CHECK_REQUEST_URI_POST", true);
const requireRequestUriPost = envBool("REQUIRE_REQUEST_URI_POST", false);
const enableLegacySdJwtProbe = envBool("ENABLE_LEGACY_SDJWT_PROBE", true);
const enableLegacySdJwtSignatureProbe = envBool("ENABLE_LEGACY_SDJWT_SIGNATURE_PROBE", true);
const requireLegacySdJwtProbe = envBool("REQUIRE_LEGACY_SDJWT_PROBE", false);
const legacySdJwtLabel = process.env.LEGACY_FORMAT_SD_JWT_LABEL ?? "SD-JWT VC";

async function main() {
  const summaryDir = path.join(
    defaults.artifactsBaseDir,
    `${slugify(artifactBranchTag)}--pr1-matrix-summary--${timestamp()}`,
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

  if (enableLegacySdJwtProbe) {
    results.push(
      runScript("record-portal-legacy.mjs", {
        ARTIFACT_BRANCH_TAG: artifactBranchTag,
        LEGACY_FORMAT: legacySdJwtLabel,
        LEGACY_DISABLE_SIGNATURE_POLICY: process.env.LEGACY_DISABLE_SIGNATURE_POLICY ?? "true",
      }, {
        id: "legacy-sd-jwt-signature-disabled",
        kind: "legacy",
        required: requireLegacySdJwtProbe,
      }),
    );
  }

  if (enableLegacySdJwtSignatureProbe) {
    results.push(
      runScript("record-portal-legacy.mjs", {
        ARTIFACT_BRANCH_TAG: artifactBranchTag,
        LEGACY_FORMAT: legacySdJwtLabel,
        LEGACY_DISABLE_SIGNATURE_POLICY: "false",
      }, {
        id: "legacy-sd-jwt-signature-enabled",
        kind: "legacy",
        required: false,
      }),
    );
  }

  for (const presentationFormat of matrixFormats) {
    for (const requestShape of requestShapes) {
      const required = requestShape !== "request_uri_post" || requireRequestUriPost;
      results.push(
        runScript("record-verifier2-api.mjs", {
          ARTIFACT_BRANCH_TAG: artifactBranchTag,
          PRESENTATION_FORMAT: presentationFormat,
          OID4VP_REQUEST_SHAPE: requestShape,
          REQUIRE_REQUEST_URI_POST: requireRequestUriPost ? "true" : "false",
          PR1_SIGNED_CLIENT_ID: process.env.PR1_SIGNED_CLIENT_ID ?? "x509_san_dns:verifier.example.com",
        }, {
          id: `verifier2-${presentationFormat}-${requestShape}`,
          kind: "verifier2",
          required,
        }),
      );
    }
  }

  const mandatoryFailures = results.filter((result) => result.required && result.status !== "SUCCESSFUL");
  const summary = {
    scenario: "pr1-matrix-suite",
    status: mandatoryFailures.length === 0 ? "SUCCESSFUL" : "FAILED",
    generatedAt: new Date().toISOString(),
    artifactBranchTag,
    matrix: {
      formats: matrixFormats,
      requestShapes,
      checkRequestUriPost,
      requireRequestUriPost,
      enableLegacySdJwtProbe,
      enableLegacySdJwtSignatureProbe,
      requireLegacySdJwtProbe,
    },
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
  console.log(`PR1 matrix summary saved to ${summaryPath}`);
  console.log(JSON.stringify(summary, null, 2));

  if (mandatoryFailures.length > 0) {
    throw new Error(
      `PR1 matrix has ${mandatoryFailures.length} mandatory failure(s): ${mandatoryFailures.map((it) => it.id).join(", ")}`,
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
