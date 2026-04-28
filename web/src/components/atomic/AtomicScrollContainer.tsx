import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';
import {
  currentFormFactor,
  resolveLayoutScalar,
} from '../../utils/LayoutTokenResolver';

/**
 * AtomicScrollContainer — renders children in a scrollable row or column.
 * Uses native CSS overflow scroll; paging via CSS scroll-snap when enabled.
 *
 * AtomicBox owns margin / padding / background / cornerRadius / shadow /
 * border / opacity. This renderer owns only the scroll layout CSS
 * (display/flex/gap/overflow/scroll-snap), passed to AtomicBox as
 * layoutStyle. When pageIndicator is declared, dots render in a separate row
 * above or below the scroll viewport (not absolutely positioned) so they do
 * not cover interactive or informational content in the paged children.
 */
export function AtomicScrollContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement {
  const isHorizontal = element.direction !== 'column';
  const children = element.children ?? [];
  const scrollRef = React.useRef<HTMLDivElement | null>(null);
  const [activePage, setActivePage] = React.useState(0);
  const [canScroll, setCanScroll] = React.useState(true);
  const resolveColor = useColorTokenResolver();
  const hasPageIndicator = element.paging === true && element.pageIndicator?.style === 'dots' && children.length > 1;
  const ff = currentFormFactor();
  const gapPx = element.gap != null ? resolveLayoutScalar(element.gap, ff) : 0;

  const layoutStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: isHorizontal ? 'row' : 'column',
    flexWrap: 'nowrap',
    gap: gapPx,
    overflowX: isHorizontal ? 'auto' : undefined,
    overflowY: isHorizontal ? undefined : 'auto',
    maxWidth: isHorizontal ? '100%' : undefined,
    maxHeight: isHorizontal ? undefined : '100%',
    minWidth: 0,
    ...(isHorizontal && element.paging ? { touchAction: 'pan-x' as const } : {}),
    ...(element.paging ? {
      scrollSnapType: isHorizontal ? 'x mandatory' : 'y mandatory',
    } : {}),
    ...(element.showIndicators === false ? { scrollbarWidth: 'none' as const } : {}),
  };

  const hideScrollbarClass = element.showIndicators === false ? 'sdui-hide-scrollbar' : undefined;

  const updateActiveFromScroll = React.useCallback((node: HTMLDivElement) => {
    const extent = isHorizontal ? node.clientWidth : node.clientHeight;
    const offset = isHorizontal ? node.scrollLeft : node.scrollTop;
    if (extent <= 0) return;
    const stride = extent + gapPx;
    if (stride <= 0) return;
    setActivePage(Math.max(0, Math.min(children.length - 1, Math.round(offset / stride))));
  }, [children.length, gapPx, isHorizontal]);

  const measureOverflow = React.useCallback((node: HTMLDivElement) => {
    const w = node.clientWidth;
    const h = node.clientHeight;
    if (w <= 0 && h <= 0) {
      return;
    }
    setCanScroll(
      (w > 0 && node.scrollWidth > w + 1) || (h > 0 && node.scrollHeight > h + 1),
    );
  }, []);

  const goToPage = React.useCallback(
    (page: number) => {
      const node = scrollRef.current;
      if (!node) return;
      const extent = isHorizontal ? node.clientWidth : node.clientHeight;
      const stride = extent + gapPx;
      const target = Math.max(0, Math.min(children.length - 1, page));
      if (isHorizontal) {
        node.scrollTo({ left: target * stride, behavior: 'smooth' });
      } else {
        node.scrollTo({ top: target * stride, behavior: 'smooth' });
      }
    },
    [children.length, gapPx, isHorizontal],
  );

  React.useLayoutEffect(() => {
    if (!element.paging) return;
    const node = scrollRef.current;
    if (!node) return;
    const onScroll = () => {
      updateActiveFromScroll(node);
      if (hasPageIndicator) {
        measureOverflow(node);
      }
    };
    onScroll();
    const ro = new ResizeObserver(() => onScroll());
    ro.observe(node);
    node.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);
    return () => {
      ro.disconnect();
      node.removeEventListener('scroll', onScroll);
      window.removeEventListener('resize', onScroll);
    };
  }, [element.paging, hasPageIndicator, children.length, measureOverflow, updateActiveFromScroll]);

  /** Desktop: map vertical wheel to horizontal scroll when scrollbar is hidden. */
  React.useEffect(() => {
    if (!element.paging || !isHorizontal) return;
    const node = scrollRef.current;
    if (!node) return;
    const onWheel = (e: WheelEvent) => {
      if (node.scrollWidth <= node.clientWidth + 1) return;
      if (Math.abs(e.deltaX) >= Math.abs(e.deltaY)) return;
      node.scrollLeft += e.deltaY;
      e.preventDefault();
    };
    node.addEventListener('wheel', onWheel, { passive: false });
    return () => node.removeEventListener('wheel', onWheel);
  }, [element.paging, isHorizontal, children.length, gapPx]);

  const childStyle: React.CSSProperties = {
    flexShrink: 0,
    ...(element.paging ? { scrollSnapAlign: element.snapAlignment ?? 'start' } : {}),
    ...(element.paging && isHorizontal
      ? { flex: '0 0 100%', minWidth: 0, display: 'flex', justifyContent: 'center', boxSizing: 'border-box' }
      : {}),
    ...(element.paging && !isHorizontal ? { height: '100%', display: 'flex', alignItems: 'center' } : {}),
  };

  const content = (
    <>
      {children.map((child, i) => (
        <div key={child.id ?? i} style={childStyle} role="listitem">
          <AtomicRouter element={child} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
        </div>
      ))}
    </>
  );

  const a11y = accessibilityProps(element.accessibility) as Record<string, unknown>;
  const showDots = hasPageIndicator && canScroll;

  if (element.paging) {
    const dotAlign = element.pageIndicator?.alignment ?? 'bottomCenter';
    const dotsAbove = isDotsRowAbove(dotAlign);
    return (
      <AtomicBox
        element={element}
        layoutStyle={showDots ? { display: 'flex', flexDirection: 'column', minWidth: 0, width: '100%' } : undefined}
        role="list"
        ariaLabel={element.accessibility?.label}
        extraProps={a11y}
      >
        {showDots && dotsAbove && (
          <PageDots
            count={children.length}
            activePage={activePage}
            alignment={dotAlign}
            color={resolveColor(element.pageIndicator?.color) ?? 'rgba(255,255,255,0.45)'}
            activeColor={resolveColor(element.pageIndicator?.activeColor) ?? '#FFFFFF'}
            onSelectPage={goToPage}
          />
        )}
        <div ref={scrollRef} style={layoutStyle} className={hideScrollbarClass}>
          {content}
          {hideScrollbarClass && (
            <style>{`.sdui-hide-scrollbar::-webkit-scrollbar { display: none; }`}</style>
          )}
        </div>
        {showDots && !dotsAbove && (
          <PageDots
            count={children.length}
            activePage={activePage}
            alignment={dotAlign}
            color={resolveColor(element.pageIndicator?.color) ?? 'rgba(255,255,255,0.45)'}
            activeColor={resolveColor(element.pageIndicator?.activeColor) ?? '#FFFFFF'}
            onSelectPage={goToPage}
          />
        )}
      </AtomicBox>
    );
  }

  return (
    <AtomicBox
      element={element}
      layoutStyle={layoutStyle}
      className={hideScrollbarClass}
      role="list"
      ariaLabel={element.accessibility?.label}
      extraProps={a11y}
    >
      {content}
      {hideScrollbarClass && (
        <style>{`.sdui-hide-scrollbar::-webkit-scrollbar { display: none; }`}</style>
      )}
    </AtomicBox>
  );
}

function isDotsRowAbove(alignment: string): boolean {
  return (
    alignment === 'topStart' ||
    alignment === 'topCenter' ||
    alignment === 'topEnd'
  );
}

function PageDots(props: {
  count: number;
  activePage: number;
  alignment: string;
  color: string;
  activeColor: string;
  onSelectPage: (index: number) => void;
}): React.ReactElement {
  const { count, activePage, alignment, color, activeColor, onSelectPage } = props;
  return (
    <div
      role="tablist"
      aria-label="Pages"
      style={pageIndicatorFlowStyle(alignment)}
    >
      {Array.from({ length: count }).map((_, index) => (
        <button
          key={index}
          type="button"
          role="tab"
          aria-selected={index === activePage}
          tabIndex={index === activePage ? 0 : -1}
          onClick={() => onSelectPage(index)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onSelectPage(index);
            }
          }}
          style={{
            display: 'block',
            width: 6,
            height: 6,
            padding: 0,
            border: 'none',
            borderRadius: 999,
            background: index === activePage ? activeColor : color,
            cursor: 'pointer',
            flexShrink: 0,
          }}
        />
      ))}
    </div>
  );
}

function pageIndicatorFlowStyle(alignment: string): React.CSSProperties {
  const base: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    gap: 6,
    padding: 8,
    width: '100%',
    boxSizing: 'border-box',
    flexShrink: 0,
  };

  let justify: React.CSSProperties['justifyContent'] = 'center';
  switch (alignment) {
    case 'topStart':
    case 'bottomStart':
    case 'centerStart':
      justify = 'flex-start';
      break;
    case 'topEnd':
    case 'bottomEnd':
    case 'centerEnd':
      justify = 'flex-end';
      break;
    case 'topCenter':
    case 'bottomCenter':
    case 'center':
    default:
      justify = 'center';
  }
  return { ...base, justifyContent: justify };
}
