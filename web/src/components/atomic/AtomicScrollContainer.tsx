import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';

/**
 * AtomicScrollContainer — renders children in a scrollable row or column.
 * Uses native CSS overflow scroll; paging via CSS scroll-snap when enabled.
 *
 * AtomicBox owns margin / padding / background / cornerRadius / shadow /
 * border / opacity. This renderer owns only the scroll layout CSS
 * (display/flex/gap/overflow/scroll-snap), passed to AtomicBox as
 * layoutStyle. When pageIndicator is declared, the scroll viewport becomes
 * an inner node so the dots can be positioned against the AtomicBox frame.
 */
export function AtomicScrollContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement {
  const isHorizontal = element.direction !== 'column';
  const children = element.children ?? [];
  const scrollRef = React.useRef<HTMLDivElement | null>(null);
  const [activePage, setActivePage] = React.useState(0);
  const resolveColor = useColorTokenResolver();
  const hasPageIndicator = element.paging === true && element.pageIndicator?.style === 'dots' && children.length > 1;

  const layoutStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: isHorizontal ? 'row' : 'column',
    flexWrap: 'nowrap',
    gap: element.gap,
    overflowX: isHorizontal ? 'auto' : undefined,
    overflowY: isHorizontal ? undefined : 'auto',
    maxWidth: isHorizontal ? '100%' : undefined,
    maxHeight: isHorizontal ? undefined : '100%',
    minWidth: 0,
    ...(element.paging ? {
      scrollSnapType: isHorizontal ? 'x mandatory' : 'y mandatory',
    } : {}),
    ...(element.showIndicators === false ? { scrollbarWidth: 'none' as const } : {}),
  };

  const hideScrollbarClass = element.showIndicators === false ? 'sdui-hide-scrollbar' : undefined;

  React.useEffect(() => {
    if (!hasPageIndicator) return;
    const node = scrollRef.current;
    if (!node) return;
    const update = () => {
      const extent = isHorizontal ? node.clientWidth : node.clientHeight;
      const offset = isHorizontal ? node.scrollLeft : node.scrollTop;
      if (extent <= 0) return;
      setActivePage(Math.max(0, Math.min(children.length - 1, Math.round(offset / extent))));
    };

    update();
    node.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
    return () => {
      node.removeEventListener('scroll', update);
      window.removeEventListener('resize', update);
    };
  }, [children.length, hasPageIndicator, isHorizontal]);

  const childStyle: React.CSSProperties = {
    flexShrink: 0,
    ...(element.paging ? { scrollSnapAlign: element.snapAlignment ?? 'start' } : {}),
    ...(element.paging ? (isHorizontal ? { width: '100%' } : { height: '100%' }) : {}),
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

  if (hasPageIndicator) {
    return (
      <AtomicBox
        element={element}
        layoutStyle={{ position: 'relative' }}
        role="list"
        ariaLabel={element.accessibility?.label}
        extraProps={accessibilityProps(element.accessibility) as Record<string, unknown>}
      >
        <div ref={scrollRef} style={layoutStyle} className={hideScrollbarClass}>
          {content}
          {hideScrollbarClass && (
            <style>{`.sdui-hide-scrollbar::-webkit-scrollbar { display: none; }`}</style>
          )}
        </div>
        <PageDots
          count={children.length}
          activePage={activePage}
          alignment={element.pageIndicator?.alignment ?? 'bottomCenter'}
          color={resolveColor(element.pageIndicator?.color) ?? 'rgba(255,255,255,0.45)'}
          activeColor={resolveColor(element.pageIndicator?.activeColor) ?? '#FFFFFF'}
        />
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
      extraProps={accessibilityProps(element.accessibility) as Record<string, unknown>}
    >
      {content}
      {hideScrollbarClass && (
        <style>{`.sdui-hide-scrollbar::-webkit-scrollbar { display: none; }`}</style>
      )}
    </AtomicBox>
  );
}

function PageDots(props: {
  count: number;
  activePage: number;
  alignment: string;
  color: string;
  activeColor: string;
}): React.ReactElement {
  const { count, activePage, alignment, color, activeColor } = props;
  return (
    <div aria-hidden="true" style={pageIndicatorStyle(alignment)}>
      {Array.from({ length: count }).map((_, index) => (
        <span
          key={index}
          style={{
            display: 'block',
            width: 6,
            height: 6,
            borderRadius: 999,
            background: index === activePage ? activeColor : color,
          }}
        />
      ))}
    </div>
  );
}

function pageIndicatorStyle(alignment: string): React.CSSProperties {
  const base: React.CSSProperties = {
    position: 'absolute',
    display: 'flex',
    gap: 6,
    padding: 8,
    pointerEvents: 'none',
  };

  switch (alignment) {
    case 'topStart': return { ...base, top: 0, left: 0 };
    case 'topCenter': return { ...base, top: 0, left: '50%', transform: 'translateX(-50%)' };
    case 'topEnd': return { ...base, top: 0, right: 0 };
    case 'centerStart': return { ...base, top: '50%', left: 0, transform: 'translateY(-50%)' };
    case 'center': return { ...base, top: '50%', left: '50%', transform: 'translate(-50%, -50%)' };
    case 'centerEnd': return { ...base, top: '50%', right: 0, transform: 'translateY(-50%)' };
    case 'bottomStart': return { ...base, bottom: 0, left: 0 };
    case 'bottomEnd': return { ...base, bottom: 0, right: 0 };
    case 'bottomCenter':
    default:
      return { ...base, bottom: 0, left: '50%', transform: 'translateX(-50%)' };
  }
}
