import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const moduleDir = path.dirname(fileURLToPath(import.meta.url));
const harnessRoot = path.resolve(moduleDir, "..");
const repoRoot = path.resolve(harnessRoot, "..", "..");
const composeWorkingDir = path.join(repoRoot, "docker-compose");
const composeFiles = [
  path.join(repoRoot, "docker-compose", "docker-compose.yaml"),
  path.join(repoRoot, "verification", "playwright-oid4vp", "docker-compose.override.yaml"),
];

const rootContextServices = [
  "wallet-api",
  "verifier-api",
  "verifier-api2",
  "web-portal",
  "waltid-demo-wallet",
  "waltid-dev-wallet",
];

const requiredServices = [
  "caddy",
  "postgres",
  "wallet-api",
  "issuer-api",
  "verifier-api",
  "verifier-api2",
  "web-portal",
  "waltid-demo-wallet",
  "waltid-dev-wallet",
];

const trackedProvenanceServices = [
  "wallet-api",
  "verifier-api",
  "verifier-api2",
  "web-portal",
];

export function resolveHarnessEnvironment() {
  const composeProjectName = process.env.HARNESS_COMPOSE_PROJECT ?? `waltid-oid4vp-${slugify(path.basename(repoRoot))}`;
  return {
    moduleDir,
    harnessRoot,
    repoRoot,
    composeWorkingDir,
    composeFiles,
    composeProjectName,
    requiredServices: [...requiredServices],
    trackedProvenanceServices: [...trackedProvenanceServices],
  };
}

export function requireHarnessPreflight({ requireRunningServices = true } = {}) {
  const env = resolveHarnessEnvironment();
  const git = getGitInfo(env.repoRoot);
  const composeConfig = getComposeConfig(env);
  const issues = [];

  if (canonicalPath(git.repoRoot) !== canonicalPath(env.repoRoot)) {
    issues.push(
      `active git root mismatch: expected "${env.repoRoot}", got "${git.repoRoot}"`,
    );
  }

  issues.push(...validateBuildContexts(composeConfig));

  let running = [];
  if (requireRunningServices) {
    running = listComposeServices(env);
    issues.push(...validateRunningServices(env, running));
  }

  if (issues.length > 0) {
    const recovery = [
      "Harness preflight failed:",
      ...issues.map((issue) => `- ${issue}`),
      "",
      "Suggested recovery:",
      `- cd ${env.repoRoot}`,
      `- HARNESS_COMPOSE_PROJECT=${env.composeProjectName} docker compose -p ${env.composeProjectName} -f docker-compose/docker-compose.yaml -f verification/playwright-oid4vp/docker-compose.override.yaml down --remove-orphans --volumes`,
      `- HARNESS_COMPOSE_PROJECT=${env.composeProjectName} docker compose -p ${env.composeProjectName} -f docker-compose/docker-compose.yaml -f verification/playwright-oid4vp/docker-compose.override.yaml up -d --build ${env.requiredServices.join(" ")}`,
    ].join("\n");
    throw new Error(recovery);
  }

  return {
    checkedAt: new Date().toISOString(),
    git,
    compose: {
      projectName: env.composeProjectName,
      configName: composeConfig.name ?? null,
      composeFiles: env.composeFiles,
      composeWorkingDir: env.composeWorkingDir,
    },
    runningServices: requireRunningServices ? summarizeRunningServices(running) : [],
  };
}

export function recreateHarnessStack() {
  const env = resolveHarnessEnvironment();
  runCommand("docker", [...composeBaseArgs(env), "down", "--remove-orphans", "--volumes"], {
    cwd: env.repoRoot,
    allowFailure: true,
    inheritStdio: true,
  });
  runCommand("docker", [...composeBaseArgs(env), "up", "-d", "--build", ...env.requiredServices], {
    cwd: env.repoRoot,
    inheritStdio: true,
  });
  return requireHarnessPreflight({ requireRunningServices: true });
}

export function collectRuntimeProvenance() {
  const env = resolveHarnessEnvironment();
  const git = getGitInfo(env.repoRoot);
  const composeConfig = getComposeConfig(env);
  const running = listComposeServices(env);
  const runningByService = new Map(running.map((row) => [row.Service, row]));
  const containers = {};

  for (const service of env.trackedProvenanceServices) {
    const row = runningByService.get(service);
    if (!row) {
      containers[service] = null;
      continue;
    }

    containers[service] = {
      containerId: row.ID ?? null,
      containerName: row.Name ?? null,
      image: row.Image ?? null,
      imageId: row.ID ? lookupImageId(row.ID, env.repoRoot) : null,
      state: row.State ?? null,
      status: row.Status ?? null,
      composeProject: row.Project ?? null,
      composeWorkingDir: extractComposeLabel(row.Labels, "com.docker.compose.project.working_dir"),
    };
  }

  return {
    capturedAt: new Date().toISOString(),
    git,
    compose: {
      projectName: env.composeProjectName,
      configName: composeConfig.name ?? null,
      composeFiles: env.composeFiles,
      composeWorkingDir: env.composeWorkingDir,
    },
    containers,
    missingTrackedServices: env.trackedProvenanceServices.filter((service) => containers[service] == null),
  };
}

function getGitInfo(cwd) {
  const repoRoot = runCommand("git", ["rev-parse", "--show-toplevel"], { cwd }).stdout.trim();
  const branch = runCommand("git", ["branch", "--show-current"], { cwd }).stdout.trim();
  const sha = runCommand("git", ["rev-parse", "HEAD"], { cwd }).stdout.trim();
  return { repoRoot, branch, sha };
}

function getComposeConfig(env) {
  const output = runCommand("docker", [...composeBaseArgs(env), "config", "--format", "json"], {
    cwd: env.repoRoot,
  }).stdout;
  return JSON.parse(output);
}

function listComposeServices(env) {
  const output = runCommand("docker", [...composeBaseArgs(env), "ps", "--format", "json"], {
    cwd: env.repoRoot,
    allowFailure: true,
  }).stdout;
  return parseJsonLines(output);
}

function validateBuildContexts(composeConfig) {
  const issues = [];

  for (const service of rootContextServices) {
    const serviceConfig = composeConfig.services?.[service];
    const build = serviceConfig?.build;
    const context = typeof build === "string" ? build : build?.context;
    if (!context) {
      issues.push(`service "${service}" has no build context in compose config`);
      continue;
    }

    const resolvedContext = canonicalPath(path.resolve(repoRoot, context));
    const expectedContext = canonicalPath(repoRoot);
    if (resolvedContext !== expectedContext) {
      issues.push(
        `service "${service}" build context mismatch: expected "${expectedContext}", got "${resolvedContext}"`,
      );
    }
  }

  return issues;
}

function validateRunningServices(env, runningRows) {
  const issues = [];
  const runningByService = new Map(runningRows.map((row) => [row.Service, row]));

  for (const service of env.requiredServices) {
    const row = runningByService.get(service);
    if (!row) {
      issues.push(`required service "${service}" is not running in compose project "${env.composeProjectName}"`);
      continue;
    }

    if ((row.State ?? "").toLowerCase() !== "running") {
      issues.push(`required service "${service}" is not running (state="${row.State ?? "unknown"}")`);
    }

    const projectLabel = extractComposeLabel(row.Labels, "com.docker.compose.project");
    if (projectLabel && projectLabel !== env.composeProjectName) {
      issues.push(
        `service "${service}" belongs to compose project "${projectLabel}", expected "${env.composeProjectName}"`,
      );
    }

    const workingDirLabel = extractComposeLabel(row.Labels, "com.docker.compose.project.working_dir");
    if (workingDirLabel && canonicalPath(workingDirLabel) !== canonicalPath(env.composeWorkingDir)) {
      issues.push(
        `service "${service}" working_dir mismatch: expected "${env.composeWorkingDir}", got "${workingDirLabel}"`,
      );
    }
  }

  return issues;
}

function summarizeRunningServices(rows) {
  return rows.map((row) => ({
    service: row.Service ?? null,
    state: row.State ?? null,
    status: row.Status ?? null,
    containerId: row.ID ?? null,
    image: row.Image ?? null,
    project: row.Project ?? null,
  }));
}

function extractComposeLabel(rawLabels, key) {
  if (!rawLabels) return null;
  const matcher = new RegExp(`(?:^|,)${escapeRegExp(key)}=([^,]*)`);
  const match = String(rawLabels).match(matcher);
  return match?.[1] ?? null;
}

function lookupImageId(containerId, cwd) {
  const result = runCommand("docker", ["inspect", "--format", "{{.Image}}", containerId], {
    cwd,
    allowFailure: true,
  });
  return result.status === 0 ? result.stdout.trim() : null;
}

function composeBaseArgs(env) {
  return [
    "compose",
    "-p",
    env.composeProjectName,
    "-f",
    env.composeFiles[0],
    "-f",
    env.composeFiles[1],
  ];
}

function parseJsonLines(value) {
  return String(value)
    .split(/\r?\n/g)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => JSON.parse(line));
}

function runCommand(command, args, { cwd, allowFailure = false, inheritStdio = false } = {}) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: "utf8",
    stdio: inheritStdio ? "inherit" : "pipe",
  });

  if (result.error) {
    throw result.error;
  }

  if (!allowFailure && result.status !== 0) {
    const stdout = result.stdout ? `\nstdout:\n${result.stdout}` : "";
    const stderr = result.stderr ? `\nstderr:\n${result.stderr}` : "";
    throw new Error(`Command failed (${result.status}): ${command} ${args.join(" ")}${stdout}${stderr}`);
  }

  return {
    status: result.status ?? 0,
    stdout: result.stdout ?? "",
    stderr: result.stderr ?? "",
  };
}

function canonicalPath(value) {
  try {
    return fs.realpathSync(value);
  } catch {
    return path.resolve(value);
  }
}

function slugify(value) {
  return String(value)
    .trim()
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, "-")
    .replaceAll(/^-+|-+$/g, "");
}

function escapeRegExp(value) {
  return String(value).replaceAll(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
