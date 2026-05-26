import { describe, expect, it } from 'vitest';
import { applyDataBindings } from './DataBindingApplier';
import type { DataBinding } from '@sdui/models';
import { Transform } from '@sdui/models';

describe('applyDataBindings liveClockSnapshot transform', () => {
  it('anchors the clock from an incoming ISO duration and running flag', () => {
    const binding: DataBinding = {
      bindings: [
        {
          sourcePath: '$.clock',
          targetPath: 'content.clock',
          transform: Transform.LiveClockSnapshot,
        },
      ],
    };

    // Mirrors the production NBA Ably linescore wire format: scalar
    // ISO-8601 duration string at $.clock plus sibling `clockRunning`
    // at the message root.
    const result = applyDataBindings(
      { content: { clock: { snapshotSeconds: 300, isRunning: false } } },
      binding,
      { clock: 'PT04M32.00S', clockRunning: '1' },
    );

    const clock = result.content.clock as Record<string, unknown>;
    expect(clock.snapshotSeconds).toBe(272);
    expect(clock.isRunning).toBe(true);
    expect(typeof clock.snapshotAt).toBe('string');
  });

  it('defaults missing running flags to paused', () => {
    const binding: DataBinding = {
      bindings: [
        {
          sourcePath: '$.clock',
          targetPath: 'content.clock',
          transform: Transform.LiveClockSnapshot,
        },
      ],
    };

    const result = applyDataBindings(
      { content: { clock: { snapshotSeconds: 300, isRunning: true } } },
      binding,
      { clock: 'Q3 04:32' },
    );

    const clock = result.content.clock as Record<string, unknown>;
    expect(clock.snapshotSeconds).toBe(272);
    expect(clock.isRunning).toBe(false);
  });

  it('skips the write when the server declares an unknown transform', () => {
    const seeded = { content: { clock: { snapshotSeconds: 300, isRunning: false } } };
    const binding: DataBinding = {
      bindings: [
        {
          sourcePath: '$.clock',
          targetPath: 'content.clock',
          transform: 'futureTransformValue' as Transform,
        },
      ],
    };

    const result = applyDataBindings(
      seeded,
      binding,
      { clock: 'PT04M32.00S' },
    );

    expect(result.content.clock).toEqual(seeded.content.clock);
  });
});
