#!/usr/bin/env node
/**
 * Validates schema/color-tokens.json.
 *
 * Palette primitives must have a 6- or 8-digit hex light + dark value.
 * Semantic tokens must alias a palette primitive (or another semantic
 * token that ultimately resolves to a palette primitive). Alias cycles
 * and dangling references fail the check.
 *
 * Exits non-zero on any validation failure.
 */

const fs = require('fs');
const path = require('path');

const REGISTRY = path.join(__dirname, '..', 'schema', 'color-tokens.json');
const HEX_RE = /^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/;
const NAME_RE = /^[A-Za-z0-9][A-Za-z0-9_.-]*$/;

const errors = [];

function fail(pathStr, message) {
  errors.push(`${pathStr}: ${message}`);
}

function validatePaletteEntry(name, entry) {
  if (!NAME_RE.test(name)) {
    fail(`palette.${name}`, `name must match ${NAME_RE}`);
  }
  if (entry == null || typeof entry !== 'object') {
    fail(`palette.${name}`, 'entry must be an object');
    return;
  }
  for (const mode of ['light', 'dark']) {
    const val = entry[mode];
    if (typeof val !== 'string' || !HEX_RE.test(val)) {
      fail(`palette.${name}.${mode}`, `must be a 6- or 8-digit hex color, got ${JSON.stringify(val)}`);
    }
  }
}

function resolveAlias(name, semantic, palette, seen) {
  if (seen.has(name)) {
    return { error: `alias cycle detected at "${name}"` };
  }
  seen.add(name);

  if (palette[name]) return { resolved: name };
  const entry = semantic[name];
  if (!entry || typeof entry !== 'object') {
    return { error: `unresolved alias "${name}"` };
  }
  if (typeof entry.aliasOf !== 'string') {
    return { error: `semantic token "${name}" has no aliasOf` };
  }
  return resolveAlias(entry.aliasOf, semantic, palette, seen);
}

function validateSemanticEntry(name, entry, semantic, palette) {
  if (!NAME_RE.test(name)) {
    fail(`semantic.${name}`, `name must match ${NAME_RE}`);
  }
  if (entry == null || typeof entry !== 'object') {
    fail(`semantic.${name}`, 'entry must be an object');
    return;
  }
  if (typeof entry.aliasOf !== 'string') {
    fail(`semantic.${name}.aliasOf`, 'must be a string');
    return;
  }
  const result = resolveAlias(entry.aliasOf, semantic, palette, new Set([name]));
  if (result.error) {
    fail(`semantic.${name}`, result.error);
  }
}

function main() {
  if (!fs.existsSync(REGISTRY)) {
    console.error(`color-tokens.json not found at ${REGISTRY}`);
    process.exit(1);
  }
  let registry;
  try {
    registry = JSON.parse(fs.readFileSync(REGISTRY, 'utf8'));
  } catch (err) {
    console.error(`Failed to parse ${REGISTRY}: ${err.message}`);
    process.exit(1);
  }

  const palette = registry.palette ?? {};
  const semantic = registry.semantic ?? {};

  if (Object.keys(palette).length === 0) {
    fail('palette', 'registry must declare at least one palette primitive');
  }
  for (const [name, entry] of Object.entries(palette)) {
    validatePaletteEntry(name, entry);
  }
  for (const [name, entry] of Object.entries(semantic)) {
    validateSemanticEntry(name, entry, semantic, palette);
  }

  if (!registry.diagnostics || typeof registry.diagnostics !== 'object') {
    fail('diagnostics', 'missing diagnostics block');
  } else if (!('token_resolver_missing' in registry.diagnostics)) {
    fail('diagnostics.token_resolver_missing', 'missing');
  }

  if (errors.length > 0) {
    console.error(`color-tokens.json validation FAILED with ${errors.length} error(s):\n`);
    for (const err of errors) {
      console.error(`  - ${err}`);
    }
    process.exit(1);
  }

  console.log(
    `color-tokens.json OK: ${Object.keys(palette).length} palette primitives, ${Object.keys(semantic).length} semantic tokens.`,
  );
}

main();
