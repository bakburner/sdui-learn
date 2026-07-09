import { useState } from 'react'
import './PlatformCode.css'

type Platform = 'react' | 'swift' | 'kotlin'

interface PlatformCodeProps {
  selectedElement: string | null
}

export function PlatformCode({ selectedElement }: PlatformCodeProps) {
  const [platform, setPlatform] = useState<Platform>('react')

  const elementType = selectedElement || 'Container'
  const code = PLATFORM_CODE[elementType]?.[platform] || PLATFORM_CODE['Container'][platform]

  return (
    <div className="platform-code">
      <div className="platform-header">
        <h4 className="platform-title">Platform Integration</h4>
        <div className="platform-tabs">
          <button
            className={`platform-tab ${platform === 'react' ? 'active' : ''}`}
            onClick={() => setPlatform('react')}
          >
            <span className="platform-tab-icon">⚛</span>
            React / Web
          </button>
          <button
            className={`platform-tab ${platform === 'swift' ? 'active' : ''}`}
            onClick={() => setPlatform('swift')}
          >
            <span className="platform-tab-icon">🍎</span>
            SwiftUI / iOS
          </button>
          <button
            className={`platform-tab ${platform === 'kotlin' ? 'active' : ''}`}
            onClick={() => setPlatform('kotlin')}
          >
            <span className="platform-tab-icon">🤖</span>
            Compose / Android
          </button>
        </div>
      </div>

      <div className="platform-body">
        <div className="platform-context">
          <span className="context-element">Rendering: <strong>{elementType}</strong></span>
          <span className="context-file">{code.file}</span>
        </div>
        <pre className="platform-pre">
          <code>{code.source}</code>
        </pre>
        <div className="platform-notes">
          {code.notes.map((note, i) => (
            <div key={i} className="platform-note">
              <span className="note-marker">→</span>
              <span>{note}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

interface CodeEntry {
  file: string
  source: string
  notes: string[]
}

const PLATFORM_CODE: Record<string, Record<Platform, CodeEntry>> = {
  Container: {
    react: {
      file: 'web/src/components/atomic/AtomicContainer.tsx',
      source: `function AtomicContainer({ element, state, onAction, depth }) {
  const isRow = element.direction === 'row';
  const gapPx = resolveLayoutScalar(element.gap, formFactor);

  const layoutStyle = {
    display: 'flex',
    flexDirection: isRow ? 'row' : 'column',
    gap: gapPx,
    justifyContent: mapAlignment(element.alignment),
    alignItems: mapCrossAlignment(element.crossAlignment),
  };

  return (
    <AtomicBox element={element} layoutStyle={layoutStyle}>
      {element.children?.map((child, i) => (
        <AtomicRouter
          key={child.id ?? i}
          element={child}
          state={state}
          onAction={onAction}
          depth={depth + 1}
        />
      ))}
    </AtomicBox>
  );
}`,
      notes: [
        'AtomicBox handles all box-model styling (padding, bg, radius, shadow)',
        'Container only owns flex layout — direction, gap, alignment',
        'Children recursively render via AtomicRouter',
        'Depth guard at MAX_TREE_DEPTH = 6 prevents runaway recursion',
      ],
    },
    swift: {
      file: 'ios/Sources/SduiCore/Rendering/Atomic/AtomicContainerView.swift',
      source: `struct AtomicContainerView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        let layout = element.direction == "row"
            ? AnyLayout(HStackLayout(spacing: resolvedGap))
            : AnyLayout(VStackLayout(spacing: resolvedGap))

        layout {
            ForEach(element.children ?? [], id: \\.id) { child in
                AtomicRouter(
                    element: child,
                    screenState: screenState,
                    onAction: onAction,
                    depth: depth + 1
                )
            }
        }
        .frame(maxWidth: fillWidth ? .infinity : nil,
               alignment: resolvedAlignment)
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    private var resolvedGap: CGFloat {
        LayoutTokenResolver.spacing(element.gap)
    }
}`,
      notes: [
        'Uses AnyLayout to switch between HStack/VStack based on direction',
        'atomicBox modifier applies padding, background, cornerRadius, shadow',
        'LayoutTokenResolver resolves spacing tokens to CGFloat values',
        'SwiftUI handles native layout, animations, and accessibility',
      ],
    },
    kotlin: {
      file: 'android/sdui-core/.../atomic/AtomicContainer.kt',
      source: `@Composable
fun AtomicContainer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    depth: Int,
) {
    val gapDp = LayoutTokenResolver.spacing(element.gap)
    val arrangement = mapArrangement(element.alignment, gapDp)
    val crossAlignment = mapCrossAlignment(element.crossAlignment)

    AtomicBox(element = element, screenState = screenState) {
        if (element.direction == "row") {
            Row(
                horizontalArrangement = arrangement,
                verticalAlignment = crossAlignment
            ) {
                element.children?.forEach { child ->
                    AtomicRouter(child, screenState, onAction, depth + 1)
                }
            }
        } else {
            Column(
                verticalArrangement = arrangement,
                horizontalAlignment = crossAlignment
            ) {
                element.children?.forEach { child ->
                    AtomicRouter(child, screenState, onAction, depth + 1)
                }
            }
        }
    }
}`,
      notes: [
        'AtomicBox composable wraps with padding, background, shape, shadow',
        'Uses native Compose Row/Column based on direction',
        'LayoutTokenResolver maps schema tokens to dp values',
        'All children recursively rendered through AtomicRouter',
      ],
    },
  },
  Text: {
    react: {
      file: 'web/src/components/atomic/AtomicText.tsx',
      source: `function AtomicText({ element, onAction }) {
  // Resolve typography from token registry
  const spec = LayoutTokenRegistry.typographyVariants[\`nba.typography.\${element.variant}\`];
  const category = LayoutTokenRegistry.typographyCategories[spec.categoryRef];

  const textStyle = {
    fontSize: webSizeToCss(spec.size.web),   // clamp() for fluid sizing
    fontWeight: category.weight,
    lineHeight: category.lineHeight,
    fontFamily: FAMILY_REF_TO_FONT[category.familyRef],
    textTransform: category.textCase === 'uppercase' ? 'uppercase' : undefined,
    color: resolveColor(element.color),
  };

  return (
    <AtomicBox element={element}>
      <span style={textStyle}>
        {element.content}
      </span>
    </AtomicBox>
  );
}`,
      notes: [
        'Typography variant resolved from schema/typography-tokens.json',
        'Fluid font sizing via CSS clamp() for responsive web',
        'Color resolved through token system (supports hex + named tokens)',
        'maxLines uses -webkit-line-clamp for text truncation',
      ],
    },
    swift: {
      file: 'ios/Sources/SduiCore/Rendering/Atomic/AtomicTextView.swift',
      source: `struct AtomicTextView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        Text(element.content ?? "")
            .font(font(for: resolvedVariant))
            .fontWeight(weight(for: element.weight))
            .foregroundColor(ColorTokenResolver.resolve(element.color))
            .lineLimit(element.maxLines)
            .multilineTextAlignment(resolvedTextAlignment)
            .applyActionTriggers(element.actions, onAction: onAction)
            .sduiAccessibility(element.accessibility)
            .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    private func font(for variant: TextVariant?) -> Font {
        guard let v = variant else { return .body }
        return TypographyResolver.font(variant: v)
    }
}`,
      notes: [
        'Native SwiftUI Text with .font(), .fontWeight(), .lineLimit()',
        'TypographyResolver maps variant to the correct Font from token registry',
        'Accessibility is automatically wired via .sduiAccessibility()',
        'Actions handled by .applyActionTriggers() modifier (tap, long-press)',
      ],
    },
    kotlin: {
      file: 'android/sdui-core/.../atomic/AtomicText.kt',
      source: `@Composable
fun AtomicText(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val baseStyle = mapTypographyVariant(element.variant)
    val textColor = ColorTokenResolver.resolve(element.color)
    val textAlign = when (element.textAlign) {
        Align.Center -> TextAlign.Center
        Align.End -> TextAlign.End
        else -> TextAlign.Start
    }

    AtomicBox(element = element, screenState = screenState) {
        Text(
            text = element.content.orEmpty(),
            style = baseStyle,
            color = textColor,
            fontWeight = mapFontWeight(element.weight),
            textAlign = textAlign,
            maxLines = element.maxLines ?: Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .applyAccessibility(element.accessibility)
                .applyActions(element.actions, onAction)
        )
    }
}`,
      notes: [
        'Uses Material3 Text composable with TextStyle from variant mapping',
        'ColorTokenResolver handles hex colors and named design tokens',
        'Accessibility applied via Modifier.semantics {} extensions',
        'Typography variants map to the same token registry as iOS and web',
      ],
    },
  },
  Image: {
    react: {
      file: 'web/src/components/atomic/AtomicImage.tsx',
      source: `function AtomicImage({ element, onAction }) {
  const style = {
    width: element.width,
    height: element.height,
    objectFit: mapContentScale(element.contentScale),
    borderRadius: resolveRadius(element.cornerRadius),
  };

  return (
    <AtomicBox element={element}>
      <img
        src={element.url}
        alt={element.alt ?? ''}
        style={style}
        loading="lazy"
      />
    </AtomicBox>
  );
}`,
      notes: [
        'Standard <img> with lazy loading for performance',
        'contentScale maps to CSS object-fit (contain, cover, etc.)',
        'alt text populated from schema for accessibility',
        'Falls back to empty container on load failure (never crashes)',
      ],
    },
    swift: {
      file: 'ios/Sources/SduiCore/Rendering/Atomic/AtomicImageView.swift',
      source: `struct AtomicImageView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        AsyncImage(url: URL(string: element.url ?? "")) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .aspectRatio(contentMode: resolvedContentMode)
            case .failure:
                Color.clear
            case .empty:
                ProgressView()
            @unknown default:
                EmptyView()
            }
        }
        .frame(width: element.width.map(CGFloat.init),
               height: element.height.map(CGFloat.init))
        .clipShape(RoundedRectangle(cornerRadius: resolvedRadius))
        .sduiAccessibility(element.accessibility, fallbackLabel: element.alt)
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }
}`,
      notes: [
        'Native AsyncImage with loading states (progress, failure, success)',
        'contentMode maps: fit → .fit, fill → .fill, crop → .fill + clipped',
        'Accessibility uses alt text as fallback VoiceOver label',
        'Dimensions from schema as CGFloat (logical points)',
      ],
    },
    kotlin: {
      file: 'android/sdui-core/.../atomic/AtomicImage.kt',
      source: `@Composable
fun AtomicImage(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    AtomicBox(element = element, screenState = screenState) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(element.url)
                .crossfade(true)
                .build(),
            contentDescription = element.alt,
            contentScale = mapContentScale(element.contentScale),
            modifier = Modifier
                .size(element.width?.dp, element.height?.dp)
                .clip(RoundedCornerShape(resolveRadius(element.cornerRadius)))
                .applyAccessibility(element.accessibility)
                .applyActions(element.actions, onAction)
        )
    }
}`,
      notes: [
        'Uses Coil AsyncImage with crossfade transition',
        'contentDescription provides TalkBack accessibility',
        'Dimensions and radius resolved through token system',
        'Error state handled by Coil (shows nothing, never crashes)',
      ],
    },
  },
  Button: {
    react: {
      file: 'web/src/components/atomic/AtomicButton.tsx',
      source: `function AtomicButton({ element, onAction }) {
  const handleClick = () => {
    if (element.actions?.length) {
      onAction(element.actions);
    }
  };

  const className = \`sdui-button sdui-button--\${element.variant ?? 'primary'}\`;

  return (
    <AtomicBox element={element}>
      <button
        className={className}
        onClick={handleClick}
        disabled={element.disabled}
        aria-label={element.accessibility?.label ?? element.label}
      >
        {element.icon && <SduiIcon name={element.icon} />}
        {element.label}
      </button>
    </AtomicBox>
  );
}`,
      notes: [
        'Fires all actions in the array on click (navigate + analytics together)',
        'Variant maps to CSS class for styling (primary, secondary, text, destructive)',
        'disabled state prevents interaction and applies visual treatment',
        'aria-label from accessibility properties or falls back to label text',
      ],
    },
    swift: {
      file: 'ios/Sources/SduiCore/Rendering/Atomic/AtomicButtonView.swift',
      source: `struct AtomicButtonView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        Button(action: { fireActions() }) {
            HStack(spacing: 4) {
                if let icon = element.icon {
                    SduiIcon(name: icon)
                }
                Text(element.label ?? "")
                    .font(TypographyResolver.font(variant: .button))
            }
        }
        .buttonStyle(SduiButtonStyle(variant: resolvedVariant))
        .disabled(element.disabled == true)
        .sduiAccessibility(element.accessibility, fallbackLabel: element.label)
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    private func fireActions() {
        element.actions?.forEach { onAction($0) }
    }
}`,
      notes: [
        'Native SwiftUI Button with custom SduiButtonStyle per variant',
        'Actions array iterated — each action dispatched to action handler',
        'Disabled state handled natively by SwiftUI .disabled() modifier',
        'Typography uses the button variant from the token registry',
      ],
    },
    kotlin: {
      file: 'android/sdui-core/.../atomic/AtomicButton.kt',
      source: `@Composable
fun AtomicButton(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val onClick = {
        element.actions?.forEach { action ->
            onAction(SduiAction.from(action))
        }
    }

    AtomicBox(element = element, screenState = screenState) {
        when (element.variant) {
            "primary" -> Button(onClick = onClick, enabled = element.disabled != true) {
                ButtonContent(element)
            }
            "secondary" -> OutlinedButton(onClick = onClick, enabled = element.disabled != true) {
                ButtonContent(element)
            }
            "text" -> TextButton(onClick = onClick, enabled = element.disabled != true) {
                ButtonContent(element)
            }
            else -> Button(onClick = onClick) { ButtonContent(element) }
        }
    }
}`,
      notes: [
        'Maps variant to Material3 Button, OutlinedButton, or TextButton',
        'Actions dispatched through SduiAction.from() factory for type safety',
        'enabled/disabled maps directly to Compose button state',
        'ButtonContent renders optional icon + label with correct typography',
      ],
    },
  },
  Divider: {
    react: {
      file: 'web/src/components/atomic/AtomicDivider.tsx',
      source: `function AtomicDivider({ element }) {
  const isVertical = element.direction === 'vertical';
  const style = {
    [isVertical ? 'width' : 'height']: element.thickness ?? 1,
    background: element.color ?? 'var(--divider)',
    [isVertical ? 'height' : 'width']: '100%',
  };

  return <AtomicBox element={element}><div style={style} role="separator" /></AtomicBox>;
}`,
      notes: [
        'Semantic role="separator" for accessibility',
        'Direction-aware: vertical for sidebar-style dividers',
        'Color defaults to theme divider token if not specified',
        'Thickness defaults to 1px (hairline)',
      ],
    },
    swift: {
      file: 'ios/Sources/SduiCore/Rendering/Atomic/AtomicDividerView.swift',
      source: `struct AtomicDividerView: View {
    let element: AtomicElement

    var body: some View {
        Divider()
            .background(ColorTokenResolver.resolve(element.color) ?? Color("divider"))
            .atomicBox(element, screenState: [:], onAction: { _ in })
    }
}`,
      notes: [
        'Uses native SwiftUI Divider for platform-consistent appearance',
        'Color override via ColorTokenResolver',
        'Thickness handled by frame modifier in AtomicBox when specified',
      ],
    },
    kotlin: {
      file: 'android/sdui-core/.../atomic/AtomicDivider.kt',
      source: `@Composable
fun AtomicDivider(element: AtomicElement, screenState: Map<String, Any>, onAction: (SduiAction) -> Unit) {
    AtomicBox(element = element, screenState = screenState) {
        HorizontalDivider(
            thickness = (element.thickness ?: 1).dp,
            color = ColorTokenResolver.resolve(element.color)
                ?: MaterialTheme.colorScheme.outlineVariant
        )
    }
}`,
      notes: [
        'Material3 HorizontalDivider with configurable thickness',
        'Falls back to theme outlineVariant color',
        'Vertical variant uses VerticalDivider composable',
      ],
    },
  },
  LiveClock: {
    react: {
      file: 'web/src/components/atomic/AtomicLiveClock.tsx',
      source: `function AtomicLiveClock({ element }) {
  const [seconds, setSeconds] = useState(element.snapshotSeconds ?? 0);

  useEffect(() => {
    if (!element.isRunning) return;
    const tick = element.tickDirection === 'up' ? 1 : -1;
    const interval = setInterval(() => {
      setSeconds(s => {
        const next = s + tick;
        if (element.stopAtSeconds != null && next === element.stopAtSeconds) {
          clearInterval(interval);
        }
        return next;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [element.isRunning, element.tickDirection, element.stopAtSeconds]);

  return (
    <AtomicBox element={element}>
      <span className="sdui-live-clock">{formatTime(seconds, element.format)}</span>
    </AtomicBox>
  );
}`,
      notes: [
        'Client-side interval ticks from server-provided snapshot value',
        'No network round-trips needed — server seeds, client counts',
        'Supports count-up (shot clock) and count-down (game clock)',
        'stopAtSeconds auto-pauses when reached (e.g. end of quarter)',
      ],
    },
    swift: {
      file: 'ios/Sources/SduiCore/Rendering/Atomic/AtomicLiveClockView.swift',
      source: `struct AtomicLiveClockView: View {
    let element: AtomicElement
    @State private var elapsed: TimeInterval = 0
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        Text(formattedTime)
            .font(TypographyResolver.font(variant: .bodyMedium))
            .monospacedDigit()
            .onReceive(timer) { _ in
                guard element.isRunning == true else { return }
                elapsed += (element.tickDirection == "up" ? 1 : -1)
            }
    }

    private var formattedTime: String {
        let total = (element.snapshotSeconds ?? 0) + Int(elapsed)
        return ClockFormatter.format(seconds: total, format: element.format)
    }
}`,
      notes: [
        'SwiftUI Timer.publish() for client-side ticking',
        '.monospacedDigit() prevents layout jitter as numbers change',
        'Respects isRunning flag — pauses when game is in timeout',
        'Format string controls mm:ss vs h:mm:ss display',
      ],
    },
    kotlin: {
      file: 'android/sdui-core/.../atomic/AtomicLiveClock.kt',
      source: `@Composable
fun AtomicLiveClock(element: AtomicElement, screenState: Map<String, Any>, onAction: (SduiAction) -> Unit) {
    var elapsed by remember { mutableIntStateOf(0) }
    val tick = if (element.tickDirection == "up") 1 else -1

    LaunchedEffect(element.isRunning) {
        if (element.isRunning != true) return@LaunchedEffect
        while (isActive) {
            delay(1000L)
            elapsed += tick
            if (element.stopAtSeconds != null &&
                (element.snapshotSeconds ?: 0) + elapsed == element.stopAtSeconds) break
        }
    }

    AtomicBox(element = element, screenState = screenState) {
        Text(
            text = formatClock((element.snapshotSeconds ?: 0) + elapsed, element.format),
            style = MaterialTheme.typography.bodyMedium,
            fontFeatureSettings = "tnum"
        )
    }
}`,
      notes: [
        'LaunchedEffect coroutine for tick loop (cancels on recomposition)',
        'Tabular numbers (tnum) prevent layout shift during count',
        'Tick direction and stop conditions from server payload',
        'Zero network usage — pure client-side rendering from snapshot',
      ],
    },
  },
}
