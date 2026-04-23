#!/usr/bin/env node
/**
 * Validates schema/style-tokens.json.
 *
 * Every entry under ContainerVariant / ImageVariant must have:
 *   - a non-empty description and intent
 *   - ios tiers: 26+, 17-25, <17
 *   - android tiers: 15+, 12-14, <12
 *   - web tiers: modern, fallback
 *   - every tier has non-empty light and dark specs
 *   - an overrideMatrix whose axes are a subset of KNOWN_AXES and values are "allow", "lock", or a per-platform object
 *   - an evidence block with composers, iosRefApp, androidRefApp fields
 *
 * Exits non-zero on any validation failure so CI can gate on it.
 */

const fs = require('fs');
const path = require('path');

const REGISTRY = path.join(__dirname, '..', 'schema', 'style-tokens.json');

const REQUIRED_TIERS = {
  ios: ['26+', '17-25', '<17'],
  android: ['15+', '12-14', '<12'],
  web: ['modern', 'fallback'],
};

const KNOWN_AXES = new Set([
  'padding',
  'cornerRadius',
  'background',
  'shadow',
  'color',
  'gap',
  'opacity',
  'border',
]);

const KNOWN_PLATFORMS = new Set(['ios', 'android', 'web']);

const errors = [];

function fail(pathStr, message) {
  errors.push(`${pathStr}: ${message}`);
}

function validateTier(pathStr, tier) {
  if (tier == null || typeof tier !== 'object') {
    fail(pathStr, 'tier entry must be an object');
    return;
  }
  for (const mode of ['light', 'dark']) {
    if (typeof tier[mode] !== 'string' || tier[mode].trim() === '') {
      fail(`${pathStr}.${mode}`, 'must be a non-empty string');
    }
  }
}

function validatePlatformTiers(pathStr, platform, entry) {
  if (entry == null || typeof entry !== 'object') {
    fail(`${pathStr}.${platform}`, 'missing or not an object');
    return;
  }
  for (const tierKey of REQUIRED_TIERS[platform]) {
    if (!(tierKey in entry)) {
      fail(`${pathStr}.${platform}`, `missing required tier "${tierKey}"`);
      continue;
    }
    validateTier(`${pathStr}.${platform}.${tierKey}`, entry[tierKey]);
  }
}

function validateOverrideMatrix(pathStr, matrix) {
  if (matrix == null || typeof matrix !== 'object') {
    fail(pathStr, 'overrideMatrix missing or not an object');
    return;
  }
  for (const [axis, policy] of Object.entries(matrix)) {
    if (!KNOWN_AXES.has(axis)) {
      fail(`${pathStr}.${axis}`, `unknown axis (known axes: ${[...KNOWN_AXES].join(', ')})`);
      continue;
    }
    if (typeof policy === 'string') {
      if (policy !== 'allow' && policy !== 'lock') {
        fail(`${pathStr}.${axis}`, `must be "allow" or "lock", got "${policy}"`);
      }
    } else if (policy != null && typeof policy === 'object') {
      for (const [plat, val] of Object.entries(policy)) {
        if (!KNOWN_PLATFORMS.has(plat)) {
          fail(`${pathStr}.${axis}.${plat}`, `unknown platform key`);
        }
        if (val !== 'allow' && val !== 'lock') {
          fail(`${pathStr}.${axis}.${plat}`, `must be "allow" or "lock", got "${val}"`);
        }
      }
    } else {
      fail(`${pathStr}.${axis}`, 'must be a string or per-platform object');
    }
  }
}

function validateEvidence(pathStr, evidence) {
  if (evidence == null || typeof evidence !== 'object') {
    fail(pathStr, 'evidence block missing or not an object');
    return;
  }
  for (const key of ['composers', 'iosRefApp', 'androidRefApp']) {
    if (!(key in evidence)) {
      fail(`${pathStr}.${key}`, 'missing');
    }
  }
}

function validateVariant(variantFamily, variantName, entry) {
  const pathStr = `${variantFamily}.${variantName}`;
  if (entry == null || typeof entry !== 'object') {
    fail(pathStr, 'variant entry must be an object');
    return;
  }
  for (const field of ['description', 'intent']) {
    if (typeof entry[field] !== 'string' || entry[field].trim() === '') {
      fail(`${pathStr}.${field}`, 'must be a non-empty string');
    }
  }
  for (const platform of Object.keys(REQUIRED_TIERS)) {
    validatePlatformTiers(pathStr, platform, entry[platform]);
  }
  validateOverrideMatrix(`${pathStr}.overrideMatrix`, entry.overrideMatrix);
  validateEvidence(`${pathStr}.evidence`, entry.evidence);
}

function main() {
  if (!fs.existsSync(REGISTRY)) {
    console.error(`style-tokens.json not found at ${REGISTRY}`);
    process.exit(1);
  }
  let registry;
  try {
    registry = JSON.parse(fs.readFileSync(REGISTRY, 'utf8'));
  } catch (err) {
    console.error(`Failed to parse ${REGISTRY}: ${err.message}`);
    process.exit(1);
  }

  for (const family of ['ContainerVariant', 'ImageVariant']) {
    if (!(family in registry)) {
      fail(family, 'missing variant family');
      continue;
    }
    const entries = registry[family];
    if (Object.keys(entries).length === 0) {
      fail(family, 'variant family has no entries');
    }
    for (const [name, entry] of Object.entries(entries)) {
      validateVariant(family, name, entry);
    }
  }

  if (!registry.diagnostics || typeof registry.diagnostics !== 'object') {
    fail('diagnostics', 'missing diagnostics block');
  } else {
    for (const diag of ['variant_override_blocked', 'variant_resolver_missing']) {
      if (!(diag in registry.diagnostics)) {
        fail(`diagnostics.${diag}`, 'missing');
      }
    }
  }

  if (errors.length > 0) {
    console.error(`style-tokens.json validation FAILED with ${errors.length} error(s):\n`);
    for (const err of errors) {
      console.error(`  - ${err}`);
    }
    process.exit(1);
  }

  const containerCount = Object.keys(registry.ContainerVariant || {}).length;
  const imageCount = Object.keys(registry.ImageVariant || {}).length;
  console.log(
    `style-tokens.json OK: ${containerCount} ContainerVariant entries, ${imageCount} ImageVariant entries.`,
  );
}

main();
