import type React from 'react';

const LONG_PRESS_MS = 500;

/**
 * Keyboard activation for clickable non-button wrappers (role="button").
 */
export function activationKeyboardProps(
  onActivate: () => void,
): Pick<React.HTMLAttributes<HTMLElement>, 'onKeyDown'> {
  return {
    onKeyDown: (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        onActivate();
      }
    },
  };
}

/**
 * Pointer long-press (500ms) for atomic elements on web.
 */
export function longPressPointerProps(
  onLongPress: () => void,
): Pick<
  React.HTMLAttributes<HTMLElement>,
  'onPointerDown' | 'onPointerUp' | 'onPointerLeave' | 'onPointerCancel'
> {
  let timer: ReturnType<typeof setTimeout> | undefined;

  const clear = () => {
    if (timer != null) {
      clearTimeout(timer);
      timer = undefined;
    }
  };

  return {
    onPointerDown: () => {
      clear();
      timer = setTimeout(() => {
        timer = undefined;
        onLongPress();
      }, LONG_PRESS_MS);
    },
    onPointerUp: clear,
    onPointerLeave: clear,
    onPointerCancel: clear,
  };
}
