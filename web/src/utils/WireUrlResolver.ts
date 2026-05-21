/**
 * Resolves server-emitted relative asset paths (e.g. `/sdui-demo/team.svg`)
 * against the SDUI composition service origin.
 *
 * Web dev serves `/sdui-demo` from `public/` same-origin; an explicit
 * [baseUrl] matches Android/iOS when the page is not same-origin with the API.
 */
export function resolveWireAssetUrl(url: string | null | undefined, baseUrl?: string | null): string | undefined {
  if (url == null || url.trim() === '') {
    return url ?? undefined;
  }
  const trimmed = url.trim();
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  let base = (baseUrl ?? '').trim().replace(/\/+$/, '');
  if (!base) {
    return trimmed;
  }
  const path = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return `${base}${path}`;
}
