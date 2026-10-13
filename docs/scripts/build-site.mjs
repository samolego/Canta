#!/usr/bin/env node
/**
 * Builds the full GitHub Pages site: WASM app + VitePress docs.
 *
 * Usage (run from anywhere, paths resolve relative to this file):
 *   node ./scripts/build-site.mjs              # wasm + docs + merge into dist/app
 *   node ./scripts/build-site.mjs --no-wasm    # docs + merge cached wasm dist (no Gradle)
 *   SKIP_WASM=1 node ./scripts/build-site.mjs  # same as --no-wasm (useful in CI)
 *
 * Layout:
 *   wasm dist : <repo>/webApp/build/dist/wasmJs/productionExecutable
 *   docs dist : <repo>/docs/.vitepress/dist
 *   app dest  : <repo>/docs/.vitepress/dist/app  -> served as /Canta/app/
 *
 * Gradle is invoked with --no-daemon (CI-friendly, no lingering daemon).
 */
import { spawnSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const docsDir = resolve(scriptDir, "..");
const repoRoot = resolve(docsDir, "..");
const wasmDist = resolve(
  repoRoot,
  "webApp/build/dist/wasmJs/productionExecutable",
);
const docsDist = resolve(docsDir, ".vitepress/dist");
const appDest = resolve(docsDist, "app");

const args = new Set(process.argv.slice(2));
const skipWasm =
  args.has("--no-wasm") ||
  args.has("--skip-wasm") ||
  process.env.SKIP_WASM === "1" ||
  process.env.SKIP_WASM === "true";

function run(cmd, runArgs, cwd) {
  console.log(`\n$ ${cmd} ${runArgs.join(" ")} (cwd: ${cwd})`);
  const res = spawnSync(cmd, runArgs, { cwd, stdio: "inherit", shell: false });
  if (res.status !== 0) {
    console.error(`Command failed with exit code ${res.status}: ${cmd} ${runArgs.join(" ")}`);
    process.exit(res.status ?? 1);
  }
}

function buildWasm() {
  const gradleCmd =
    process.platform === "win32"
      ? resolve(repoRoot, "gradlew.bat")
      : resolve(repoRoot, "gradlew");
  const gradleArgs = [":webApp:wasmJsBrowserDistribution", "--no-daemon"];
  const cmd = process.platform === "win32" ? gradleCmd : "sh";
  const cmdArgs = process.platform === "win32" ? gradleArgs : [gradleCmd, ...gradleArgs];
  run(cmd, cmdArgs, repoRoot);
  if (!existsSync(wasmDist)) {
    console.error(`WASM build finished but dist not found: ${wasmDist}`);
    process.exit(1);
  }
}

function buildDocs() {
  // Reuses docs/package.json -> "docs:build": "vitepress build"
  const npmCmd = process.platform === "win32" ? "npm.cmd" : "npm";
  run(npmCmd, ["run", "docs:build", "--silent"], docsDir);
}

function mergeApp() {
  mkdirSync(appDest, { recursive: true });
  if (existsSync(wasmDist)) {
    console.log(`\nCopying WASM dist -> ${appDest}`);
    cpSync(wasmDist, appDest, { recursive: true });
  } else if (skipWasm) {
    console.warn(
      `Warning: WASM dist not found (${wasmDist}). Deploying docs only — /app/ will be missing. ` +
        `Run without --no-wasm once to populate it.`,
    );
  } else {
    console.error(`WASM dist not found: ${wasmDist}`);
    process.exit(1);
  }
  // GitHub Pages: don't let Jekyll ignore underscore-prefixed asset dirs.
  writeFileSync(resolve(docsDist, ".nojekyll"), "");
  console.log(`\nSite ready: ${docsDist} (app at ${appDest})`);
}

console.log(`skipWasm=${skipWasm}`);
if (!skipWasm) buildWasm();
buildDocs();
mergeApp();
