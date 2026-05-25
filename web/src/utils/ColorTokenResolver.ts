/**
 * ColorTokenResolver — converts SDUI color values to concrete CSS color
 * strings.
 *
 * Input shapes, per the schema's `ColorToken` definition:
 *   - literal hex: `#RRGGBB` or `#RRGGBBAA` — returned unchanged.
 *   - semantic token: `token:<dot.separated.path>` — looked up against
 *     the bundled registry (palette primitives + semantic aliases) and
 *     resolved to a light or dark hex based on the caller's current
 *     `ColorScheme`.
 *
 * The registry below is a snapshot of `schema/color-tokens.json`.
 * Re-transcribe when that file changes. Do not fetch the JSON at
 * runtime — the snapshot is deliberately inlined so the bundle has
 * no network or file dependency at render time.
 */

import { useEffect, useState } from 'react';
import { resolveTeamColor } from '../tokens/TeamColorRegistry';

export type ColorScheme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'sdui-color-scheme';
const THEME_CHANGE_EVENT = 'sdui-color-scheme-change';

interface PaletteEntry {
  light: string;
  dark: string;
}

/** Palette primitives + UI mode-aware tokens from schema/color-tokens.json. */
const PALETTE: Record<string, PaletteEntry> = {
  // blue
  'nba.color.blue.0': { light: '#000000', dark: '#000000' },
  'nba.color.blue.10': { light: '#051C2D', dark: '#051C2D' },
  'nba.color.blue.20': { light: '#132A59', dark: '#132A59' },
  'nba.color.blue.30': { light: '#1D428A', dark: '#1D428A' },
  'nba.color.blue.40': { light: '#0064FF', dark: '#0064FF' },
  'nba.color.blue.50': { light: '#1A81FF', dark: '#1A81FF' },
  'nba.color.blue.60': { light: '#4D9DFF', dark: '#4D9DFF' },
  'nba.color.blue.70': { light: '#66ABFF', dark: '#66ABFF' },
  'nba.color.blue.80': { light: '#99C7FF', dark: '#99C7FF' },
  'nba.color.blue.90': { light: '#CCE3FF', dark: '#CCE3FF' },
  'nba.color.blue.95': { light: '#E5F1FF', dark: '#E5F1FF' },
  'nba.color.blue.99': { light: '#F5F9FF', dark: '#F5F9FF' },
  'nba.color.blue.100': { light: '#FFFFFF', dark: '#FFFFFF' },
  // green
  'nba.color.green.0': { light: '#000000', dark: '#000000' },
  'nba.color.green.10': { light: '#103514', dark: '#103514' },
  'nba.color.green.20': { light: '#206A28', dark: '#206A28' },
  'nba.color.green.30': { light: '#317E44', dark: '#317E44' },
  'nba.color.green.40': { light: '#3B8A4A', dark: '#3B8A4A' },
  'nba.color.green.50': { light: '#45A057', dark: '#45A057' },
  'nba.color.green.60': { light: '#4FB664', dark: '#4FB664' },
  'nba.color.green.70': { light: '#30D158', dark: '#30D158' },
  'nba.color.green.80': { light: '#64D879', dark: '#64D879' },
  'nba.color.green.90': { light: '#97E09B', dark: '#97E09B' },
  'nba.color.green.95': { light: '#CBE8BD', dark: '#CBE8BD' },
  'nba.color.green.99': { light: '#F0F2DE', dark: '#F0F2DE' },
  'nba.color.green.100': { light: '#FFFFFF', dark: '#FFFFFF' },
  // grey
  'nba.color.grey.0': { light: '#000000', dark: '#000000' },
  'nba.color.grey.10': { light: '#191C23', dark: '#191C23' },
  'nba.color.grey.20': { light: '#2B2F37', dark: '#2B2F37' },
  'nba.color.grey.30': { light: '#3C414A', dark: '#3C414A' },
  'nba.color.grey.40': { light: '#4E525C', dark: '#4E525C' },
  'nba.color.grey.50': { light: '#838A96', dark: '#838A96' },
  'nba.color.grey.60': { light: '#A4A7AD', dark: '#A4A7AD' },
  'nba.color.grey.70': { light: '#BFC2C6', dark: '#BFC2C6' },
  'nba.color.grey.80': { light: '#DADDDE', dark: '#DADDDE' },
  'nba.color.grey.90': { light: '#E7E9EA', dark: '#E7E9EA' },
  'nba.color.grey.95': { light: '#F3F4F5', dark: '#F3F4F5' },
  'nba.color.grey.99': { light: '#F9FAFA', dark: '#F9FAFA' },
  'nba.color.grey.100': { light: '#FFFFFF', dark: '#FFFFFF' },
  // orange
  'nba.color.orange.0': { light: '#000000', dark: '#000000' },
  'nba.color.orange.10': { light: '#1F0D02', dark: '#1F0D02' },
  'nba.color.orange.20': { light: '#3A1805', dark: '#3A1805' },
  'nba.color.orange.30': { light: '#5A2508', dark: '#5A2508' },
  'nba.color.orange.40': { light: '#7A340B', dark: '#7A340B' },
  'nba.color.orange.50': { light: '#9A450F', dark: '#9A450F' },
  'nba.color.orange.60': { light: '#BB5814', dark: '#BB5814' },
  'nba.color.orange.70': { light: '#E66E1A', dark: '#E66E1A' },
  'nba.color.orange.80': { light: '#F18F45', dark: '#F18F45' },
  'nba.color.orange.90': { light: '#F6B679', dark: '#F6B679' },
  'nba.color.orange.95': { light: '#FAD5A6', dark: '#FAD5A6' },
  'nba.color.orange.99': { light: '#FEF2E6', dark: '#FEF2E6' },
  'nba.color.orange.100': { light: '#FFFFFF', dark: '#FFFFFF' },
  // red
  'nba.color.red.0': { light: '#000000', dark: '#000000' },
  'nba.color.red.10': { light: '#410E0B', dark: '#410E0B' },
  'nba.color.red.20': { light: '#601410', dark: '#601410' },
  'nba.color.red.30': { light: '#8C1D18', dark: '#8C1D18' },
  'nba.color.red.40': { light: '#B3261E', dark: '#B3261E' },
  'nba.color.red.50': { light: '#DC362E', dark: '#DC362E' },
  'nba.color.red.60': { light: '#FE6F67', dark: '#FE6F67' },
  'nba.color.red.70': { light: '#EC928E', dark: '#EC928E' },
  'nba.color.red.80': { light: '#F2B8B5', dark: '#F2B8B5' },
  'nba.color.red.90': { light: '#F9DEDC', dark: '#F9DEDC' },
  'nba.color.red.95': { light: '#FCEEEE', dark: '#FCEEEE' },
  'nba.color.red.99': { light: '#FFFBF9', dark: '#FFFBF9' },
  'nba.color.red.100': { light: '#FFFFFF', dark: '#FFFFFF' },
  // t-black
  'nba.color.t-black.0': { light: '#00000000', dark: '#00000000' },
  'nba.color.t-black.5': { light: '#0000000D', dark: '#0000000D' },
  'nba.color.t-black.10': { light: '#0000001A', dark: '#0000001A' },
  'nba.color.t-black.15': { light: '#00000026', dark: '#00000026' },
  'nba.color.t-black.20': { light: '#00000033', dark: '#00000033' },
  'nba.color.t-black.25': { light: '#00000040', dark: '#00000040' },
  'nba.color.t-black.30': { light: '#0000004D', dark: '#0000004D' },
  'nba.color.t-black.40': { light: '#00000066', dark: '#00000066' },
  'nba.color.t-black.50': { light: '#00000080', dark: '#00000080' },
  'nba.color.t-black.60': { light: '#00000099', dark: '#00000099' },
  'nba.color.t-black.70': { light: '#000000B2', dark: '#000000B2' },
  'nba.color.t-black.75': { light: '#000000BF', dark: '#000000BF' },
  'nba.color.t-black.80': { light: '#000000CC', dark: '#000000CC' },
  'nba.color.t-black.85': { light: '#000000D9', dark: '#000000D9' },
  'nba.color.t-black.90': { light: '#000000E5', dark: '#000000E5' },
  'nba.color.t-black.95': { light: '#000000F2', dark: '#000000F2' },
  // t-white
  'nba.color.t-white.0': { light: '#FFFFFF00', dark: '#FFFFFF00' },
  'nba.color.t-white.5': { light: '#FFFFFF0D', dark: '#FFFFFF0D' },
  'nba.color.t-white.10': { light: '#FFFFFF1A', dark: '#FFFFFF1A' },
  'nba.color.t-white.15': { light: '#FFFFFF26', dark: '#FFFFFF26' },
  'nba.color.t-white.20': { light: '#FFFFFF33', dark: '#FFFFFF33' },
  'nba.color.t-white.25': { light: '#FFFFFF40', dark: '#FFFFFF40' },
  'nba.color.t-white.30': { light: '#FFFFFF4D', dark: '#FFFFFF4D' },
  'nba.color.t-white.40': { light: '#FFFFFF66', dark: '#FFFFFF66' },
  'nba.color.t-white.50': { light: '#FFFFFF80', dark: '#FFFFFF80' },
  'nba.color.t-white.60': { light: '#FFFFFF99', dark: '#FFFFFF99' },
  'nba.color.t-white.70': { light: '#FFFFFFB2', dark: '#FFFFFFB2' },
  'nba.color.t-white.75': { light: '#FFFFFFBF', dark: '#FFFFFFBF' },
  'nba.color.t-white.80': { light: '#FFFFFFCC', dark: '#FFFFFFCC' },
  'nba.color.t-white.85': { light: '#FFFFFFD9', dark: '#FFFFFFD9' },
  'nba.color.t-white.90': { light: '#FFFFFFE5', dark: '#FFFFFFE5' },
  'nba.color.t-white.95': { light: '#FFFFFFF2', dark: '#FFFFFFF2' },
  // yellow
  'nba.color.yellow.0': { light: '#000000', dark: '#000000' },
  'nba.color.yellow.10': { light: '#281E01', dark: '#281E01' },
  'nba.color.yellow.20': { light: '#644B02', dark: '#644B02' },
  'nba.color.yellow.30': { light: '#967103', dark: '#967103' },
  'nba.color.yellow.40': { light: '#E1AA05', dark: '#E1AA05' },
  'nba.color.yellow.50': { light: '#F1BE27', dark: '#F1BE27' },
  'nba.color.yellow.60': { light: '#FBCD44', dark: '#FBCD44' },
  'nba.color.yellow.70': { light: '#FCD769', dark: '#FCD769' },
  'nba.color.yellow.80': { light: '#FCDE82', dark: '#FCDE82' },
  'nba.color.yellow.90': { light: '#FEF2CD', dark: '#FEF2CD' },
  'nba.color.yellow.95': { light: '#FEF9E6', dark: '#FEF9E6' },
  'nba.color.yellow.99': { light: '#FFFEFA', dark: '#FFFEFA' },
  'nba.color.yellow.100': { light: '#FFFFFF', dark: '#FFFFFF' },

  // UI tokens (mode-aware) — values reference semantic/primitive names
  'nba.bg.primary': { light: 'nba.color.primary.95', dark: 'nba.color.primary.0' },
  'nba.bg.secondary': { light: 'nba.color.primary.100', dark: 'nba.color.primary.10' },
  'nba.bg.tertiary': { light: 'nba.color.primary.90', dark: 'nba.color.primary.20' },
  'nba.bg.quaternary': { light: 'nba.color.primary.80', dark: 'nba.color.primary.30' },
  'nba.bg.selection': { light: 'nba.color.primary.0', dark: 'nba.color.primary.100' },
  'nba.bg.badge': { light: 'nba.color.t-white.90', dark: 'nba.color.t-white.90' },
  'nba.bg.disabled': { light: 'nba.color.primary.80', dark: 'nba.color.primary.30' },
  'nba.bg.splash-screen': { light: 'nba.color.tertiary.30', dark: 'nba.color.tertiary.0' },
  'nba.bg-dark.primary': { light: 'nba.color.primary.0', dark: 'nba.color.primary.0' },
  'nba.bg-dark.secondary': { light: 'nba.color.primary.10', dark: 'nba.color.primary.10' },
  'nba.bg-dark.tertiary': { light: 'nba.color.primary.20', dark: 'nba.color.primary.20' },
  'nba.bg-dark.quaternary': { light: 'nba.color.primary.30', dark: 'nba.color.primary.30' },
  'nba.bg-inverted.primary': { light: 'nba.color.primary.0', dark: 'nba.color.primary.95' },
  'nba.bg-inverted.secondary': { light: 'nba.color.primary.10', dark: 'nba.color.primary.100' },
  'nba.bg-inverted.tertiary': { light: 'nba.color.primary.20', dark: 'nba.color.primary.90' },
  'nba.bg-inverted.quaternary': { light: 'nba.color.primary.30', dark: 'nba.color.primary.80' },
  'nba.bg-tint.primary': { light: 'nba.color.secondary.60', dark: 'nba.color.secondary.60' },
  'nba.bg-tint.secondary': { light: 'nba.color.secondary.80', dark: 'nba.color.secondary.80' },
  'nba.bg-tint.tertiary': { light: 'nba.color.secondary.50', dark: 'nba.color.secondary.50' },
  'nba.bg-tint.quaternary': { light: 'nba.color.secondary.40', dark: 'nba.color.secondary.50' },
  'nba.label.primary': { light: 'nba.color.primary.0', dark: 'nba.color.primary.100' },
  'nba.label.secondary': { light: 'nba.color.primary.40', dark: 'nba.color.primary.60' },
  'nba.label.tertiary': { light: 'nba.color.primary.60', dark: 'nba.color.primary.50' },
  'nba.label.interactive': { light: 'nba.color.tertiary.40', dark: 'nba.color.tertiary.70' },
  'nba.label.selection': { light: 'nba.color.primary.100', dark: 'nba.color.primary.0' },
  'nba.label-dark.primary': { light: 'nba.color.primary.100', dark: 'nba.color.primary.100' },
  'nba.label-dark.secondary': { light: 'nba.color.primary.60', dark: 'nba.color.primary.60' },
  'nba.label-dark.tertiary': { light: 'nba.color.primary.50', dark: 'nba.color.primary.40' },
  'nba.label-dark.quaternary': { light: 'nba.color.t-white.25', dark: 'nba.color.t-white.25' },
  'nba.label-dark.interactive': { light: 'nba.color.tertiary.40', dark: 'nba.color.tertiary.40' },
  'nba.label-inverted.primary': { light: 'nba.color.primary.100', dark: 'nba.color.primary.0' },
  'nba.label-inverted.secondary': { light: 'nba.color.primary.60', dark: 'nba.color.primary.40' },
  'nba.label-inverted.tertiary': { light: 'nba.color.primary.50', dark: 'nba.color.primary.60' },
  'nba.label-inverted.quaternary': { light: 'nba.color.t-white.20', dark: 'nba.color.t-black.5' },
  'nba.label-inverted.link': { light: 'nba.color.tertiary.70', dark: 'nba.color.tertiary.40' },
  'nba.label-tint.primary': { light: 'nba.color.secondary.0', dark: 'nba.color.secondary.0' },
  'nba.label-tint.secondary': { light: 'nba.color.secondary.20', dark: 'nba.color.secondary.20' },
  'nba.label.accent.brand': { light: '#1D428A', dark: 'nba.color.primary.100' },
  'nba.label.accent.live': { light: '#C8102E', dark: '#C8102E' },
  'nba.label.accent.splash-screen': { light: 'nba.color.primary.100', dark: 'nba.color.tertiary.40' },
  'nba.divider.moderate': { light: 'nba.color.primary.70', dark: 'nba.color.primary.40' },
  'nba.divider.subtle': { light: 'nba.color.primary.80', dark: 'nba.color.primary.20' },
  'nba.divider.prominent': { light: 'nba.color.primary.60', dark: 'nba.color.primary.50' },
  'nba.effect.blur': { light: 'nba.color.t-white.90', dark: 'nba.color.t-white.90' },
  'nba.effect.scrim': { light: 'nba.color.t-black.50', dark: 'nba.color.t-black.50' },
  'nba.effect.shadow-color-15': { light: 'nba.color.t-black.15', dark: 'nba.color.t-white.15' },
  'nba.effect.shadow-color-30': { light: 'nba.color.t-black.30', dark: 'nba.color.t-white.30' },
  'nba.feedback.bg-error.primary': { light: 'nba.color.feedback.error.90', dark: 'nba.color.feedback.error.10' },
  'nba.feedback.bg-success.primary': { light: 'nba.color.feedback.success.99', dark: 'nba.color.feedback.success.10' },
  'nba.feedback.bg-warning.primary': { light: 'nba.color.feedback.warning.99', dark: 'nba.color.feedback.warning.10' },
  'nba.feedback.label-error.primary': { light: 'nba.color.feedback.error.50', dark: 'nba.color.feedback.error.60' },
  'nba.feedback.label-error.secondary': { light: 'nba.color.feedback.error.30', dark: 'nba.color.feedback.error.70' },
  'nba.feedback.label-success.primary': { light: 'nba.color.feedback.success.50', dark: 'nba.color.feedback.success.60' },
  'nba.feedback.label-success.secondary': { light: 'nba.color.feedback.success.30', dark: 'nba.color.feedback.success.70' },
  'nba.feedback.label-warning.primary': { light: 'nba.color.feedback.warning.50', dark: 'nba.color.feedback.warning.70' },
  'nba.feedback.label-warning.secondary': { light: 'nba.color.feedback.warning.30', dark: 'nba.color.feedback.warning.60' },
  'nba.button.primary.bg': { light: 'nba.bg-inverted.secondary', dark: 'nba.bg-inverted.secondary' },
  'nba.button.primary.label': { light: 'nba.label-inverted.primary', dark: 'nba.label-inverted.primary' },
  'nba.button.primary.border-color': { light: 'nba.color.t-white.0', dark: 'nba.color.t-white.0' },
  'nba.button.secondary.bg': { light: 'nba.color.t-black.0', dark: 'nba.color.t-black.0' },
  'nba.button.secondary.label': { light: 'nba.label.primary', dark: 'nba.label.primary' },
  'nba.button.on-dark.bg': { light: 'nba.color.primary.100', dark: 'nba.color.primary.100' },
  'nba.button.on-dark.label': { light: 'nba.color.primary.0', dark: 'nba.color.primary.0' },
  'nba.button.tint.bg': { light: 'nba.bg-tint.primary', dark: 'nba.bg-tint.primary' },
  'nba.button.tint.label': { light: 'nba.label-tint.primary', dark: 'nba.label-tint.primary' },
  'nba.button.ghost.bg': { light: 'nba.color.t-white.25', dark: 'nba.color.t-white.25' },
  'nba.button.ghost.label': { light: 'nba.color.primary.100', dark: 'nba.color.primary.100' },
  'nba.button.focus-ring': { light: 'nba.label.interactive', dark: 'nba.label.interactive' },
  'nba.opacity.t-dark-4': { light: 'nba.color.t-black.5', dark: 'nba.color.t-white.5' },
  'nba.opacity.t-dark-8': { light: 'nba.color.t-black.10', dark: 'nba.color.t-white.10' },
  'nba.opacity.t-dark-10': { light: 'nba.color.t-black.10', dark: 'nba.color.t-white.10' },
  'nba.opacity.t-dark-16': { light: 'nba.color.t-black.15', dark: 'nba.color.t-white.20' },
  'nba.opacity.t-light-4': { light: 'nba.color.t-white.5', dark: 'nba.color.t-black.5' },
  'nba.opacity.t-light-8': { light: 'nba.color.t-white.10', dark: 'nba.color.t-black.10' },
  'nba.opacity.t-light-10': { light: 'nba.color.t-white.10', dark: 'nba.color.t-black.10' },
  'nba.opacity.t-light-16': { light: 'nba.color.t-white.15', dark: 'nba.color.t-black.20' },
};

/** Semantic aliases — each maps to a palette primitive or another alias. */
const SEMANTIC: Record<string, string> = {
  'nba.color.primary.0': 'nba.color.grey.0',
  'nba.color.primary.10': 'nba.color.grey.10',
  'nba.color.primary.20': 'nba.color.grey.20',
  'nba.color.primary.30': 'nba.color.grey.30',
  'nba.color.primary.40': 'nba.color.grey.40',
  'nba.color.primary.50': 'nba.color.grey.50',
  'nba.color.primary.60': 'nba.color.grey.60',
  'nba.color.primary.70': 'nba.color.grey.70',
  'nba.color.primary.80': 'nba.color.grey.80',
  'nba.color.primary.90': 'nba.color.grey.90',
  'nba.color.primary.95': 'nba.color.grey.95',
  'nba.color.primary.99': 'nba.color.grey.99',
  'nba.color.primary.100': 'nba.color.grey.100',
  'nba.color.secondary.0': 'nba.color.yellow.0',
  'nba.color.secondary.10': 'nba.color.yellow.10',
  'nba.color.secondary.20': 'nba.color.yellow.20',
  'nba.color.secondary.30': 'nba.color.yellow.30',
  'nba.color.secondary.40': 'nba.color.yellow.40',
  'nba.color.secondary.50': 'nba.color.yellow.50',
  'nba.color.secondary.60': 'nba.color.yellow.60',
  'nba.color.secondary.70': 'nba.color.yellow.70',
  'nba.color.secondary.80': 'nba.color.yellow.80',
  'nba.color.secondary.90': 'nba.color.yellow.90',
  'nba.color.secondary.95': 'nba.color.yellow.95',
  'nba.color.secondary.99': 'nba.color.yellow.99',
  'nba.color.secondary.100': 'nba.color.yellow.100',
  'nba.color.tertiary.0': 'nba.color.blue.0',
  'nba.color.tertiary.10': 'nba.color.blue.10',
  'nba.color.tertiary.20': 'nba.color.blue.20',
  'nba.color.tertiary.30': 'nba.color.blue.30',
  'nba.color.tertiary.40': 'nba.color.blue.40',
  'nba.color.tertiary.50': 'nba.color.blue.50',
  'nba.color.tertiary.60': 'nba.color.blue.60',
  'nba.color.tertiary.70': 'nba.color.blue.70',
  'nba.color.tertiary.80': 'nba.color.blue.80',
  'nba.color.tertiary.90': 'nba.color.blue.90',
  'nba.color.tertiary.95': 'nba.color.blue.95',
  'nba.color.tertiary.99': 'nba.color.blue.99',
  'nba.color.tertiary.100': 'nba.color.blue.100',
  'nba.color.feedback.success.0': 'nba.color.green.0',
  'nba.color.feedback.success.10': 'nba.color.green.10',
  'nba.color.feedback.success.20': 'nba.color.green.20',
  'nba.color.feedback.success.30': 'nba.color.green.30',
  'nba.color.feedback.success.40': 'nba.color.green.40',
  'nba.color.feedback.success.50': 'nba.color.green.50',
  'nba.color.feedback.success.60': 'nba.color.green.60',
  'nba.color.feedback.success.70': 'nba.color.green.70',
  'nba.color.feedback.success.80': 'nba.color.green.80',
  'nba.color.feedback.success.90': 'nba.color.green.90',
  'nba.color.feedback.success.95': 'nba.color.green.95',
  'nba.color.feedback.success.99': 'nba.color.green.99',
  'nba.color.feedback.success.100': 'nba.color.green.100',
  'nba.color.feedback.error.0': 'nba.color.red.0',
  'nba.color.feedback.error.10': 'nba.color.red.10',
  'nba.color.feedback.error.20': 'nba.color.red.20',
  'nba.color.feedback.error.30': 'nba.color.red.30',
  'nba.color.feedback.error.40': 'nba.color.red.40',
  'nba.color.feedback.error.50': 'nba.color.red.50',
  'nba.color.feedback.error.60': 'nba.color.red.60',
  'nba.color.feedback.error.70': 'nba.color.red.70',
  'nba.color.feedback.error.80': 'nba.color.red.80',
  'nba.color.feedback.error.90': 'nba.color.red.90',
  'nba.color.feedback.error.95': 'nba.color.red.95',
  'nba.color.feedback.error.99': 'nba.color.red.99',
  'nba.color.feedback.error.100': 'nba.color.red.100',
  'nba.color.feedback.warning.0': 'nba.color.orange.0',
  'nba.color.feedback.warning.10': 'nba.color.orange.10',
  'nba.color.feedback.warning.20': 'nba.color.orange.20',
  'nba.color.feedback.warning.30': 'nba.color.orange.30',
  'nba.color.feedback.warning.40': 'nba.color.orange.40',
  'nba.color.feedback.warning.50': 'nba.color.orange.50',
  'nba.color.feedback.warning.60': 'nba.color.orange.60',
  'nba.color.feedback.warning.70': 'nba.color.orange.70',
  'nba.color.feedback.warning.80': 'nba.color.orange.80',
  'nba.color.feedback.warning.90': 'nba.color.orange.90',
  'nba.color.feedback.warning.95': 'nba.color.orange.95',
  'nba.color.feedback.warning.99': 'nba.color.orange.99',
  'nba.color.feedback.warning.100': 'nba.color.orange.100',
};

const TOKEN_PREFIX = 'token:';
const MAX_ALIAS_DEPTH = 8;

const resolvedTokenCache = new Map<string, string | undefined>();

function tokenCacheKey(scheme: ColorScheme, value: string): string {
  return `${scheme}\0${value}`;
}

/**
 * Resolve an SDUI color value to a CSS color string.
 * - `null` / `undefined` / empty → returns `undefined` so the caller can
 *   decide a fallback.
 * - `"#RRGGBB"` / `"#RRGGBBAA"` → returned as-is.
 * - `"token:<path>"` → looked up against the semantic + palette
 *   registry; picks light or dark based on the current `ColorScheme`.
 *   Unknown tokens log `token_resolver_missing` and return `undefined`.
 */
export function resolveColorToken(
  value: string | null | undefined,
  scheme: ColorScheme,
): string | undefined {
  if (!value) return undefined;
  if (!value.startsWith(TOKEN_PREFIX)) {
    const key = tokenCacheKey(scheme, value);
    if (resolvedTokenCache.has(key)) {
      return resolvedTokenCache.get(key);
    }
    resolvedTokenCache.set(key, value);
    return value;
  }

  const key = tokenCacheKey(scheme, value);
  if (resolvedTokenCache.has(key)) {
    return resolvedTokenCache.get(key);
  }

  const name = value.slice(TOKEN_PREFIX.length);
  const entry = followAlias(name);
  if (!entry) {
    if (typeof console !== 'undefined') {
      console.warn('token_resolver_missing', { token: value });
    }
    resolvedTokenCache.set(key, undefined);
    return undefined;
  }
  const modeValue = scheme === 'dark' ? entry.dark : entry.light;
  // UI tokens may reference other aliases (e.g. 'nba.color.primary.100')
  // rather than a final hex. Resolve recursively.
  const resolved = resolveIndirection(modeValue, scheme);
  resolvedTokenCache.set(key, resolved);
  return resolved;
}

/**
 * Follow indirection on a resolved mode value. If it's already hex (#...),
 * return it. Otherwise look it up as a token name in PALETTE/SEMANTIC.
 */
function resolveIndirection(value: string, scheme: ColorScheme, depth = 0): string | undefined {
  if (depth > MAX_ALIAS_DEPTH) return undefined;
  if (value.startsWith('#')) return value;
  const entry = followAlias(value);
  if (!entry) return undefined;
  const next = scheme === 'dark' ? entry.dark : entry.light;
  return resolveIndirection(next, scheme, depth + 1);
}

function followAlias(name: string, depth = 0): PaletteEntry | undefined {
  if (depth > MAX_ALIAS_DEPTH) return undefined;
  const primitive = PALETTE[name];
  if (primitive) return primitive;
  const next = SEMANTIC[name];
  if (!next) return undefined;
  return followAlias(next, depth + 1);
}

function systemColorScheme(): ColorScheme {
  return typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
}

function storedColorScheme(): ColorScheme | undefined {
  if (typeof window === 'undefined') return undefined;
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  return stored === 'light' || stored === 'dark' ? stored : undefined;
}

export function getEffectiveColorScheme(): ColorScheme {
  if (typeof document !== 'undefined') {
    const attr = document.documentElement.dataset.theme;
    if (attr === 'light' || attr === 'dark') return attr;
  }
  return storedColorScheme() ?? systemColorScheme();
}

function applyColorScheme(scheme: ColorScheme): void {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', scheme);
    document.documentElement.dataset.theme = scheme;
    document.documentElement.style.colorScheme = scheme;
  }
}

export function setColorSchemePreference(scheme: ColorScheme): void {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', scheme);
  }
  applyColorScheme(scheme);
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(THEME_STORAGE_KEY, scheme);
    window.dispatchEvent(new CustomEvent<ColorScheme>(THEME_CHANGE_EVENT, { detail: scheme }));
  }
}

export function initializeColorSchemePreference(): void {
  applyColorScheme(storedColorScheme() ?? systemColorScheme());
}

/**
 * React hook that tracks the effective app color scheme. The app-level
 * toggle wins over OS `prefers-color-scheme`; without an override it
 * follows the OS setting. SSR-safe: returns `'light'` when unavailable.
 */
export function usePrefersColorScheme(): ColorScheme {
  const getScheme = (): ColorScheme => getEffectiveColorScheme();

  const [scheme, setScheme] = useState<ColorScheme>(getScheme);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const update = () => {
      const next = storedColorScheme() ?? systemColorScheme();
      applyColorScheme(next);
      setScheme(next);
    };
    const mq = typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : undefined;

    window.addEventListener(THEME_CHANGE_EVENT, update);
    mq?.addEventListener?.('change', update);

    update();
    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, update);
      mq?.removeEventListener?.('change', update);
    };
  }, []);

  return scheme;
}

/**
 * Resolve a team-specific color token (e.g. "nba.team.bg") to a hex string.
 * Delegates to the TeamColorRegistry for bundled team palette/mode data.
 *
 * Returns `undefined` when the token or teamId cannot be resolved.
 */
export function resolveTeamColorToken(
  token: string,
  teamId: string,
  theme: ColorScheme,
): string | undefined {
  return resolveTeamColor(token, teamId, theme) ?? undefined;
}

/**
 * Convenience wrapper — returns a closure bound to the current
 * color scheme so components can call `resolve(value)` without
 * threading `scheme` through every call site.
 */
export function useColorTokenResolver(): (value: string | null | undefined) => string | undefined {
  const scheme = usePrefersColorScheme();
  return (value) => resolveColorToken(value, scheme);
}
