import { describe, expect, it } from 'vitest';
import { resolveWireAssetUrl } from './WireUrlResolver';

describe('resolveWireAssetUrl', () => {
  it('leaves absolute URLs unchanged', () => {
    expect(resolveWireAssetUrl('https://cdn.example.com/a.png', 'http://localhost:8080')).toBe(
      'https://cdn.example.com/a.png',
    );
  });

  it('joins relative paths to base', () => {
    expect(resolveWireAssetUrl('/sdui-demo/team.svg?v=1', 'http://10.0.2.2:8080')).toBe(
      'http://10.0.2.2:8080/sdui-demo/team.svg?v=1',
    );
  });

  it('returns relative path when base is empty', () => {
    expect(resolveWireAssetUrl('/sdui-demo/x.svg', '')).toBe('/sdui-demo/x.svg');
  });
});
