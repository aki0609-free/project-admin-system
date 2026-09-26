#!/usr/bin/env node

import { createHash } from "node:crypto";
import {
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  renameSync,
  writeFileSync,
} from "node:fs";
import { dirname, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(scriptDirectory, "../..");
const catalogPath = resolve(projectRoot, "report-design/workspace-assets.json");
const statePath = resolve(projectRoot, "report-design/workspace-state.json");
const backupRoot = resolve(projectRoot, "report-design/.workspace-backups");

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function writeJsonAtomic(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  const temporaryPath = `${path}.tmp`;
  writeFileSync(temporaryPath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
  renameSync(temporaryPath, path);
}

function sha256(path) {
  return createHash("sha256").update(readFileSync(path)).digest("hex");
}

function absolute(path) {
  const resolved = resolve(projectRoot, path);
  if (!resolved.startsWith(`${projectRoot}/`)) {
    throw new Error(`プロジェクト外のパスは利用できません: ${path}`);
  }
  return resolved;
}

function loadCatalog() {
  const catalog = readJson(catalogPath);
  if (catalog.version !== 1 || !Array.isArray(catalog.assets)) {
    throw new Error("workspace-assets.json の形式が不正です。");
  }
  return catalog;
}

function loadState() {
  if (!existsSync(statePath)) {
    return { version: 1, updatedAt: null, assets: {} };
  }
  const state = readJson(statePath);
  if (state.version !== 1 || typeof state.assets !== "object") {
    throw new Error("workspace-state.json の形式が不正です。");
  }
  return state;
}

function validateAsset(asset, path) {
  const content = readFileSync(path, "utf8");
  if (!content.trim()) {
    throw new Error(`${asset.name} が空です: ${relative(projectRoot, path)}`);
  }
  if (/^(<<<<<<<|=======|>>>>>>>)/m.test(content)) {
    throw new Error(`${asset.name} に未解決の競合マーカーがあります。`);
  }
  if (asset.kind === "jasper") {
    if (!content.includes("<jasperReport")) {
      throw new Error(`${asset.name} はJasperテンプレートとして認識できません。`);
    }
    if (
      /Jaspersoft Studio version 7\./.test(content) ||
      /<element\s+kind=/.test(content)
    ) {
      throw new Error(
        `${asset.name} はJasperReports 7形式で保存されています。\n` +
          "BackendはJasperReports 6.21.3を使用しているため、6.21.x互換形式で保存してください。",
      );
    }
  }
}

function initialize() {
  const catalog = loadCatalog();
  const state = loadState();
  let changed = false;

  for (const asset of catalog.assets) {
    const target = absolute(asset.targetPath);
    const working = absolute(asset.workingPath);
    if (!existsSync(target)) {
      throw new Error(`正式ファイルがありません: ${asset.targetPath}`);
    }
    mkdirSync(dirname(working), { recursive: true });
    if (!existsSync(working)) {
      copyFileSync(target, working);
      changed = true;
    }
    validateAsset(asset, target);
    validateAsset(asset, working);

    const key = asset.workingPath;
    if (!state.assets[key]) {
      const targetHash = sha256(target);
      const workingHash = sha256(working);
      if (targetHash !== workingHash) {
        throw new Error(
          `初期化前から内容が異なります: ${asset.name}\n` +
            "どちらを正とするか確認してから、同じ内容に揃えて再実行してください。",
        );
      }
      state.assets[key] = {
        targetPath: asset.targetPath,
        baseSha256: targetHash,
      };
      changed = true;
    }
  }

  if (changed || !existsSync(statePath)) {
    state.updatedAt = new Date().toISOString();
    writeJsonAtomic(statePath, state);
  }
  return { catalog, state };
}

function classify(asset, state) {
  const working = absolute(asset.workingPath);
  const target = absolute(asset.targetPath);
  const entry = state.assets[asset.workingPath];
  if (!entry) {
    return { asset, status: "未初期化" };
  }
  const workingHash = sha256(working);
  const targetHash = sha256(target);
  const baseHash = entry.baseSha256;

  if (workingHash === targetHash && targetHash === baseHash) {
    return { asset, status: "変更なし", workingHash, targetHash, baseHash };
  }
  if (workingHash === targetHash) {
    return { asset, status: "同期済み", workingHash, targetHash, baseHash };
  }
  if (targetHash === baseHash) {
    return { asset, status: "作業側を変更", workingHash, targetHash, baseHash };
  }
  if (workingHash === baseHash) {
    return { asset, status: "正式側を変更", workingHash, targetHash, baseHash };
  }
  return { asset, status: "競合", workingHash, targetHash, baseHash };
}

function printStatus(records) {
  const labels = {
    "変更なし": "OK       ",
    "同期済み": "SYNCED   ",
    "作業側を変更": "WORKING  ",
    "正式側を変更": "SOURCE   ",
    競合: "CONFLICT ",
    未初期化: "UNSET    ",
  };
  for (const record of records) {
    console.log(`${labels[record.status]} ${record.asset.kind.padEnd(6)} ${record.asset.name}`);
  }
}

function timestamp() {
  return new Date().toISOString().replaceAll(":", "-").replaceAll(".", "-");
}

function sync({ dryRun }) {
  const { catalog, state } = initialize();
  const records = catalog.assets.map((asset) => classify(asset, state));
  const conflicts = records.filter((record) => record.status === "競合");
  printStatus(records);
  if (conflicts.length > 0) {
    throw new Error(
      "正式ファイルと作業ファイルの両方が変更されています。自動反映を停止しました。\n" +
        conflicts.map((record) => `- ${record.asset.name}`).join("\n"),
    );
  }
  if (dryRun) {
    console.log("\n事前確認のみです。ファイルは変更していません。");
    return;
  }

  const backupDirectory = resolve(backupRoot, timestamp());
  let copiedToTarget = 0;
  let fastForwardedWorking = 0;

  for (const record of records) {
    const { asset } = record;
    const working = absolute(asset.workingPath);
    const target = absolute(asset.targetPath);

    if (record.status === "作業側を変更") {
      validateAsset(asset, working);
      const backup = resolve(backupDirectory, asset.targetPath);
      mkdirSync(dirname(backup), { recursive: true });
      copyFileSync(target, backup);
      copyFileSync(working, target);
      copiedToTarget += 1;
    } else if (record.status === "正式側を変更") {
      copyFileSync(target, working);
      fastForwardedWorking += 1;
    }

    const synchronizedHash = sha256(target);
    if (sha256(working) !== synchronizedHash) {
      throw new Error(`同期後の内容が一致しません: ${asset.name}`);
    }
    state.assets[asset.workingPath] = {
      targetPath: asset.targetPath,
      baseSha256: synchronizedHash,
    };
  }

  state.updatedAt = new Date().toISOString();
  writeJsonAtomic(statePath, state);
  console.log(`\n正式ファイルへ反映: ${copiedToTarget}件`);
  console.log(`正式ファイルから作業側を更新: ${fastForwardedWorking}件`);
  if (copiedToTarget > 0) {
    console.log(`反映前バックアップ: ${relative(projectRoot, backupDirectory)}`);
  }
}

function listAssets(kind, format) {
  const catalog = loadCatalog();
  for (const asset of catalog.assets.filter((item) => !kind || item.kind === kind)) {
    if (format === "resource") {
      const prefix = "backend/src/main/resources/";
      if (!asset.targetPath.startsWith(prefix)) {
        throw new Error(`resources配下ではありません: ${asset.targetPath}`);
      }
      console.log(asset.targetPath.slice(prefix.length));
    } else if (format === "working") {
      console.log(asset.workingPath);
    } else {
      console.log(asset.targetPath);
    }
  }
}

function main() {
  const [command = "status", ...args] = process.argv.slice(2);
  if (command === "init") {
    const { catalog } = initialize();
    console.log(`帳票作業領域を準備しました: ${catalog.assets.length}件`);
    return;
  }
  if (command === "status") {
    const { catalog, state } = initialize();
    const records = catalog.assets.map((asset) => classify(asset, state));
    printStatus(records);
    if (records.some((record) => record.status === "競合")) {
      process.exitCode = 2;
    }
    return;
  }
  if (command === "sync") {
    sync({ dryRun: args.includes("--dry-run") });
    return;
  }
  if (command === "list") {
    const kind = args.find((value) => value.startsWith("--kind="))?.split("=")[1];
    const format = args.find((value) => value.startsWith("--format="))?.split("=")[1];
    listAssets(kind, format);
    return;
  }
  throw new Error(`未対応のコマンドです: ${command}`);
}

try {
  main();
} catch (error) {
  console.error(`\n帳票作業領域エラー: ${error.message}`);
  process.exitCode = 1;
}
