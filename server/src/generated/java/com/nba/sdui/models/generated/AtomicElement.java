
package com.nba.sdui.models.generated;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


/**
 * Atomic UI primitive — server-composed building block for the atomic rendering layer
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "id",
    "accessibility",
    "children",
    "direction",
    "alignment",
    "crossAlignment",
    "gap",
    "padding",
    "margin",
    "backgrounds",
    "content",
    "variant",
    "weight",
    "color",
    "maxLines",
    "src",
    "aspectRatio",
    "fit",
    "placeholder",
    "width",
    "height",
    "widthMode",
    "heightMode",
    "minWidth",
    "maxWidth",
    "minHeight",
    "maxHeight",
    "layoutWrap",
    "crossAxisGap",
    "alignSelf",
    "label",
    "icon",
    "disabled",
    "actions",
    "size",
    "orientation",
    "thickness",
    "paging",
    "snapAlignment",
    "condition",
    "trueChild",
    "falseChild",
    "columns",
    "rows",
    "striped",
    "section",
    "flex",
    "breakpoint",
    "cornerRadius",
    "cornerRadii",
    "opacity",
    "shadows",
    "badge",
    "base",
    "overlays",
    "textAlign",
    "showIndicators",
    "pageIndicator",
    "monospacedDigits",
    "snapshotSeconds",
    "snapshotAt",
    "isRunning",
    "tickDirection",
    "stopAtSeconds",
    "format",
    "bindRef"
})
@Generated("jsonschema2pojo")
public class AtomicElement {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    @NotNull
    private AtomicElement.Type type;
    @JsonProperty("id")
    private String id;
    /**
     * Server-provided accessibility metadata applied natively per platform
     * 
     */
    @JsonProperty("accessibility")
    @JsonPropertyDescription("Server-provided accessibility metadata applied natively per platform")
    @Valid
    private AccessibilityProperties accessibility;
    @JsonProperty("children")
    @Valid
    private List<AtomicElement> children = new ArrayList<AtomicElement>();
    @JsonProperty("direction")
    private AtomicElement.Direction direction;
    @JsonProperty("alignment")
    private AtomicElement.Alignment alignment;
    @JsonProperty("crossAlignment")
    private AtomicElement.CrossAlignment crossAlignment;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("gap")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object gap;
    @JsonProperty("padding")
    @Valid
    private Spacing padding;
    @JsonProperty("margin")
    @Valid
    private Spacing margin;
    /**
     * Ordered array of background layers. Index 0 is the bottommost layer (Figma convention); higher indices paint on top. Web renderers must reverse the array when mapping to CSS background shorthand (CSS is top-to-bottom). Single-layer backgrounds ship as a one-element array.
     * 
     */
    @JsonProperty("backgrounds")
    @JsonPropertyDescription("Ordered array of background layers. Index 0 is the bottommost layer (Figma convention); higher indices paint on top. Web renderers must reverse the array when mapping to CSS background shorthand (CSS is top-to-bottom). Single-layer backgrounds ship as a one-element array.")
    @Valid
    private List<Object> backgrounds = new ArrayList<Object>();
    @JsonProperty("content")
    private String content;
    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text, ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image. Renderers parse this string against the primitive's enum and log a diagnostic on unrecognized values.
     * 
     */
    @JsonProperty("variant")
    @JsonPropertyDescription("Named variant preset. The vocabulary depends on the element's type: TextVariant for Text, ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image. Renderers parse this string against the primitive's enum and log a diagnostic on unrecognized values.")
    private String variant;
    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @JsonProperty("weight")
    @JsonPropertyDescription("Font weight tokens for atomic Text elements.")
    private AtomicElement.TextWeight weight;
    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    @JsonPropertyDescription("A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|token:[A-Za-z0-9][A-Za-z0-9_.-]*)$")
    private String color;
    @JsonProperty("maxLines")
    private Integer maxLines;
    @JsonProperty("src")
    private String src;
    /**
     * Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
     * 
     */
    @JsonProperty("aspectRatio")
    @JsonPropertyDescription("Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.")
    private Object aspectRatio;
    @JsonProperty("fit")
    private AtomicElement.ImageFit fit;
    @JsonProperty("placeholder")
    private String placeholder;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("width")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object width;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("height")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object height;
    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @JsonProperty("widthMode")
    @JsonPropertyDescription("Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.")
    private AtomicElement.SizingMode widthMode;
    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @JsonProperty("heightMode")
    @JsonPropertyDescription("Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.")
    private AtomicElement.SizingMode heightMode;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("minWidth")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object minWidth;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("maxWidth")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object maxWidth;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("minHeight")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object minHeight;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("maxHeight")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object maxHeight;
    /**
     * When true, enables flex-wrap on a Container. Children that overflow the main axis wrap to the next line. Only meaningful on Container elements.
     * 
     */
    @JsonProperty("layoutWrap")
    @JsonPropertyDescription("When true, enables flex-wrap on a Container. Children that overflow the main axis wrap to the next line. Only meaningful on Container elements.")
    private Boolean layoutWrap = false;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("crossAxisGap")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object crossAxisGap;
    @JsonProperty("alignSelf")
    private AtomicElement.CrossAlignment alignSelf;
    @JsonProperty("label")
    private String label;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("disabled")
    private Boolean disabled;
    @JsonProperty("actions")
    @Valid
    private List<Action> actions = new ArrayList<Action>();
    @JsonProperty("size")
    private Integer size;
    @JsonProperty("orientation")
    private AtomicElement.Orientation orientation;
    @JsonProperty("thickness")
    private Integer thickness;
    @JsonProperty("paging")
    private Boolean paging;
    @JsonProperty("snapAlignment")
    private AtomicElement.SnapAlignment snapAlignment;
    @JsonProperty("condition")
    private String condition;
    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("trueChild")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement trueChild;
    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("falseChild")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement falseChild;
    /**
     * DisplayGrid column definitions — display-only, non-interactive, server-ordered
     * 
     */
    @JsonProperty("columns")
    @JsonPropertyDescription("DisplayGrid column definitions \u2014 display-only, non-interactive, server-ordered")
    @Valid
    private List<Column> columns = new ArrayList<Column>();
    /**
     * DisplayGrid row data — each object maps column keys to pre-formatted display values
     * 
     */
    @JsonProperty("rows")
    @JsonPropertyDescription("DisplayGrid row data \u2014 each object maps column keys to pre-formatted display values")
    @Valid
    private List<Row> rows = new ArrayList<Row>();
    /**
     * Alternate row background for readability
     * 
     */
    @JsonProperty("striped")
    @JsonPropertyDescription("Alternate row background for readability")
    private Boolean striped;
    @JsonProperty("section")
    @Valid
    private Section section;
    /**
     * Flex grow factor. When set on a child of a Container, the child claims proportional space along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
     * 
     */
    @JsonProperty("flex")
    @JsonPropertyDescription("Flex grow factor. When set on a child of a Container, the child claims proportional space along the main axis (like CSS flex or Compose weight). Default 0 (size to content).")
    private Double flex;
    /**
     * Responsive breakpoint in dp/px. For Container: below this screen width, direction flips from row to column. Enables responsive layouts without client logic.
     * 
     */
    @JsonProperty("breakpoint")
    @JsonPropertyDescription("Responsive breakpoint in dp/px. For Container: below this screen width, direction flips from row to column. Enables responsive layouts without client logic.")
    private Integer breakpoint;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("cornerRadius")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object cornerRadius;
    /**
     * Per-corner cornerRadius override. When present, takes precedence over the single-value cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius / borderBottomRightRadius.
     * 
     */
    @JsonProperty("cornerRadii")
    @JsonPropertyDescription("Per-corner cornerRadius override. When present, takes precedence over the single-value cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also absent). Used for asymmetric card shapes \u2014 e.g. content-rail cards with rounded tops and square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius / borderBottomRightRadius.")
    @Valid
    private CornerRadii cornerRadii;
    /**
     * Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded states.
     * 
     */
    @JsonProperty("opacity")
    @JsonPropertyDescription("Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded states.")
    private Double opacity = 1.0D;
    /**
     * Ordered array of shadow layers. Index 0 is the outermost shadow (Figma convention); higher indices are closer to the element. Maps directly to CSS box-shadow list order. Single-layer shadows ship as a one-element array.
     * 
     */
    @JsonProperty("shadows")
    @JsonPropertyDescription("Ordered array of shadow layers. Index 0 is the outermost shadow (Figma convention); higher indices are closer to the element. Maps directly to CSS box-shadow list order. Single-layer shadows ship as a one-element array.")
    @Valid
    private List<Object> shadows = new ArrayList<Object>();
    /**
     * Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the screen-level overlays map.
     * 
     */
    @JsonProperty("badge")
    @JsonPropertyDescription("Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the screen-level overlays map.")
    @Valid
    private Badge badge;
    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("base")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement base;
    /**
     * OverlayContainer layers rendered over the base element in server order.
     * 
     */
    @JsonProperty("overlays")
    @JsonPropertyDescription("OverlayContainer layers rendered over the base element in server order.")
    @Valid
    private List<AtomicOverlay> overlays = new ArrayList<AtomicOverlay>();
    /**
     * Text alignment within the element. Used for centered headings, right-aligned numeric values. Absent means the server made no instruction; clients fall back to platform-native locale-aware leading alignment.
     * 
     */
    @JsonProperty("textAlign")
    @JsonPropertyDescription("Text alignment within the element. Used for centered headings, right-aligned numeric values. Absent means the server made no instruction; clients fall back to platform-native locale-aware leading alignment.")
    private AtomicElement.TextAlign textAlign;
    /**
     * Whether to show scroll indicators on ScrollContainer. Default false for clean carousel presentation.
     * 
     */
    @JsonProperty("showIndicators")
    @JsonPropertyDescription("Whether to show scroll indicators on ScrollContainer. Default false for clean carousel presentation.")
    private Boolean showIndicators = false;
    /**
     * Server-declared scroll page indicator presentation for ScrollContainer. Clients own local scroll state only to realize the declared affordance.
     * 
     */
    @JsonProperty("pageIndicator")
    @JsonPropertyDescription("Server-declared scroll page indicator presentation for ScrollContainer. Clients own local scroll state only to realize the declared affordance.")
    @Valid
    private PageIndicator pageIndicator;
    /**
     * Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes (scores, clocks).
     * 
     */
    @JsonProperty("monospacedDigits")
    @JsonPropertyDescription("Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes (scores, clocks).")
    private Boolean monospacedDigits = false;
    /**
     * LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.
     * 
     */
    @JsonProperty("snapshotSeconds")
    @JsonPropertyDescription("LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.")
    @DecimalMin("0")
    private Integer snapshotSeconds;
    /**
     * LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients compute elapsed = now - snapshotAt and derive the displayed value. Required when type == 'LiveClock'.
     * 
     */
    @JsonProperty("snapshotAt")
    @JsonPropertyDescription("LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients compute elapsed = now - snapshotAt and derive the displayed value. Required when type == 'LiveClock'.")
    private OffsetDateTime snapshotAt;
    /**
     * LiveClock: whether the clock is actively ticking. When true, clients run a local tick loop at their platform-native refresh cadence (~10Hz) and update the displayed value. When false, clients render snapshotSeconds verbatim.
     * 
     */
    @JsonProperty("isRunning")
    @JsonPropertyDescription("LiveClock: whether the clock is actively ticking. When true, clients run a local tick loop at their platform-native refresh cadence (~10Hz) and update the displayed value. When false, clients render snapshotSeconds verbatim.")
    private Boolean isRunning = false;
    /**
     * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds (default 0); 'up' increments from snapshotSeconds with no upper bound unless stopAtSeconds is set. Required on every LiveClock; no static schema default.
     * 
     */
    @JsonProperty("tickDirection")
    @JsonPropertyDescription("LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds (default 0); 'up' increments from snapshotSeconds with no upper bound unless stopAtSeconds is set. Required on every LiveClock; no static schema default.")
    private AtomicElement.TickDirection tickDirection;
    /**
     * LiveClock: optional clamp. For direction 'down', clock holds at this value once reached. For direction 'up', clock holds once reached. Omit to disable the clamp.
     * 
     */
    @JsonProperty("stopAtSeconds")
    @JsonPropertyDescription("LiveClock: optional clamp. For direction 'down', clock holds at this value once reached. For direction 'up', clock holds once reached. Omit to disable the clamp.")
    private Integer stopAtSeconds;
    /**
     * LiveClock display format. Clients realize using their platform's tabular-numerals typography (equivalent to TextVariant.score). Required on every LiveClock; no static schema default.
     * 
     */
    @JsonProperty("format")
    @JsonPropertyDescription("LiveClock display format. Clients realize using their platform's tabular-numerals typography (equivalent to TextVariant.score). Required on every LiveClock; no static schema default.")
    private AtomicElement.Format format;
    /**
     * Dot-path into the enclosing AtomicComposite's `data.content` object. When set, renderers resolve the leaf's canonical live field from `data.content[bindRef]` at render time and fall back to the inline value when the path is absent. Canonical field per type: Text → content, Button → label, Image → src, LiveClock → an object with {snapshotSeconds, snapshotAt, isRunning}. Placing the binding identifier on the consuming node (rather than in a centrally-declared path-into-tree) lets composers reshape the ui tree without breaking real-time updates; data-bindings on the section envelope continue to write into `content.*`.
     * 
     */
    @JsonProperty("bindRef")
    @JsonPropertyDescription("Dot-path into the enclosing AtomicComposite's `data.content` object. When set, renderers resolve the leaf's canonical live field from `data.content[bindRef]` at render time and fall back to the inline value when the path is absent. Canonical field per type: Text \u2192 content, Button \u2192 label, Image \u2192 src, LiveClock \u2192 an object with {snapshotSeconds, snapshotAt, isRunning}. Placing the binding identifier on the consuming node (rather than in a centrally-declared path-into-tree) lets composers reshape the ui tree without breaking real-time updates; data-bindings on the section envelope continue to write into `content.*`.")
    private String bindRef;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public AtomicElement.Type getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(AtomicElement.Type type) {
        this.type = type;
    }

    public AtomicElement withType(AtomicElement.Type type) {
        this.type = type;
        return this;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public AtomicElement withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Server-provided accessibility metadata applied natively per platform
     * 
     */
    @JsonProperty("accessibility")
    public AccessibilityProperties getAccessibility() {
        return accessibility;
    }

    /**
     * Server-provided accessibility metadata applied natively per platform
     * 
     */
    @JsonProperty("accessibility")
    public void setAccessibility(AccessibilityProperties accessibility) {
        this.accessibility = accessibility;
    }

    public AtomicElement withAccessibility(AccessibilityProperties accessibility) {
        this.accessibility = accessibility;
        return this;
    }

    @JsonProperty("children")
    public List<AtomicElement> getChildren() {
        return children;
    }

    @JsonProperty("children")
    public void setChildren(List<AtomicElement> children) {
        this.children = children;
    }

    public AtomicElement withChildren(List<AtomicElement> children) {
        this.children = children;
        return this;
    }

    @JsonProperty("direction")
    public AtomicElement.Direction getDirection() {
        return direction;
    }

    @JsonProperty("direction")
    public void setDirection(AtomicElement.Direction direction) {
        this.direction = direction;
    }

    public AtomicElement withDirection(AtomicElement.Direction direction) {
        this.direction = direction;
        return this;
    }

    @JsonProperty("alignment")
    public AtomicElement.Alignment getAlignment() {
        return alignment;
    }

    @JsonProperty("alignment")
    public void setAlignment(AtomicElement.Alignment alignment) {
        this.alignment = alignment;
    }

    public AtomicElement withAlignment(AtomicElement.Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    @JsonProperty("crossAlignment")
    public AtomicElement.CrossAlignment getCrossAlignment() {
        return crossAlignment;
    }

    @JsonProperty("crossAlignment")
    public void setCrossAlignment(AtomicElement.CrossAlignment crossAlignment) {
        this.crossAlignment = crossAlignment;
    }

    public AtomicElement withCrossAlignment(AtomicElement.CrossAlignment crossAlignment) {
        this.crossAlignment = crossAlignment;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("gap")
    public Object getGap() {
        return gap;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("gap")
    public void setGap(Object gap) {
        this.gap = gap;
    }

    public AtomicElement withGap(Object gap) {
        this.gap = gap;
        return this;
    }

    @JsonProperty("padding")
    public Spacing getPadding() {
        return padding;
    }

    @JsonProperty("padding")
    public void setPadding(Spacing padding) {
        this.padding = padding;
    }

    public AtomicElement withPadding(Spacing padding) {
        this.padding = padding;
        return this;
    }

    @JsonProperty("margin")
    public Spacing getMargin() {
        return margin;
    }

    @JsonProperty("margin")
    public void setMargin(Spacing margin) {
        this.margin = margin;
    }

    public AtomicElement withMargin(Spacing margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Ordered array of background layers. Index 0 is the bottommost layer (Figma convention); higher indices paint on top. Web renderers must reverse the array when mapping to CSS background shorthand (CSS is top-to-bottom). Single-layer backgrounds ship as a one-element array.
     * 
     */
    @JsonProperty("backgrounds")
    public List<Object> getBackgrounds() {
        return backgrounds;
    }

    /**
     * Ordered array of background layers. Index 0 is the bottommost layer (Figma convention); higher indices paint on top. Web renderers must reverse the array when mapping to CSS background shorthand (CSS is top-to-bottom). Single-layer backgrounds ship as a one-element array.
     * 
     */
    @JsonProperty("backgrounds")
    public void setBackgrounds(List<Object> backgrounds) {
        this.backgrounds = backgrounds;
    }

    public AtomicElement withBackgrounds(List<Object> backgrounds) {
        this.backgrounds = backgrounds;
        return this;
    }

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(String content) {
        this.content = content;
    }

    public AtomicElement withContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text, ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image. Renderers parse this string against the primitive's enum and log a diagnostic on unrecognized values.
     * 
     */
    @JsonProperty("variant")
    public String getVariant() {
        return variant;
    }

    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text, ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image. Renderers parse this string against the primitive's enum and log a diagnostic on unrecognized values.
     * 
     */
    @JsonProperty("variant")
    public void setVariant(String variant) {
        this.variant = variant;
    }

    public AtomicElement withVariant(String variant) {
        this.variant = variant;
        return this;
    }

    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @JsonProperty("weight")
    public AtomicElement.TextWeight getWeight() {
        return weight;
    }

    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @JsonProperty("weight")
    public void setWeight(AtomicElement.TextWeight weight) {
        this.weight = weight;
    }

    public AtomicElement withWeight(AtomicElement.TextWeight weight) {
        this.weight = weight;
        return this;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    public void setColor(String color) {
        this.color = color;
    }

    public AtomicElement withColor(String color) {
        this.color = color;
        return this;
    }

    @JsonProperty("maxLines")
    public Integer getMaxLines() {
        return maxLines;
    }

    @JsonProperty("maxLines")
    public void setMaxLines(Integer maxLines) {
        this.maxLines = maxLines;
    }

    public AtomicElement withMaxLines(Integer maxLines) {
        this.maxLines = maxLines;
        return this;
    }

    @JsonProperty("src")
    public String getSrc() {
        return src;
    }

    @JsonProperty("src")
    public void setSrc(String src) {
        this.src = src;
    }

    public AtomicElement withSrc(String src) {
        this.src = src;
        return this;
    }

    /**
     * Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
     * 
     */
    @JsonProperty("aspectRatio")
    public Object getAspectRatio() {
        return aspectRatio;
    }

    /**
     * Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
     * 
     */
    @JsonProperty("aspectRatio")
    public void setAspectRatio(Object aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public AtomicElement withAspectRatio(Object aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    @JsonProperty("fit")
    public AtomicElement.ImageFit getFit() {
        return fit;
    }

    @JsonProperty("fit")
    public void setFit(AtomicElement.ImageFit fit) {
        this.fit = fit;
    }

    public AtomicElement withFit(AtomicElement.ImageFit fit) {
        this.fit = fit;
        return this;
    }

    @JsonProperty("placeholder")
    public String getPlaceholder() {
        return placeholder;
    }

    @JsonProperty("placeholder")
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public AtomicElement withPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("width")
    public Object getWidth() {
        return width;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("width")
    public void setWidth(Object width) {
        this.width = width;
    }

    public AtomicElement withWidth(Object width) {
        this.width = width;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("height")
    public Object getHeight() {
        return height;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("height")
    public void setHeight(Object height) {
        this.height = height;
    }

    public AtomicElement withHeight(Object height) {
        this.height = height;
        return this;
    }

    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @JsonProperty("widthMode")
    public AtomicElement.SizingMode getWidthMode() {
        return widthMode;
    }

    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @JsonProperty("widthMode")
    public void setWidthMode(AtomicElement.SizingMode widthMode) {
        this.widthMode = widthMode;
    }

    public AtomicElement withWidthMode(AtomicElement.SizingMode widthMode) {
        this.widthMode = widthMode;
        return this;
    }

    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @JsonProperty("heightMode")
    public AtomicElement.SizingMode getHeightMode() {
        return heightMode;
    }

    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @JsonProperty("heightMode")
    public void setHeightMode(AtomicElement.SizingMode heightMode) {
        this.heightMode = heightMode;
    }

    public AtomicElement withHeightMode(AtomicElement.SizingMode heightMode) {
        this.heightMode = heightMode;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("minWidth")
    public Object getMinWidth() {
        return minWidth;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("minWidth")
    public void setMinWidth(Object minWidth) {
        this.minWidth = minWidth;
    }

    public AtomicElement withMinWidth(Object minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("maxWidth")
    public Object getMaxWidth() {
        return maxWidth;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("maxWidth")
    public void setMaxWidth(Object maxWidth) {
        this.maxWidth = maxWidth;
    }

    public AtomicElement withMaxWidth(Object maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("minHeight")
    public Object getMinHeight() {
        return minHeight;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("minHeight")
    public void setMinHeight(Object minHeight) {
        this.minHeight = minHeight;
    }

    public AtomicElement withMinHeight(Object minHeight) {
        this.minHeight = minHeight;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("maxHeight")
    public Object getMaxHeight() {
        return maxHeight;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("maxHeight")
    public void setMaxHeight(Object maxHeight) {
        this.maxHeight = maxHeight;
    }

    public AtomicElement withMaxHeight(Object maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    /**
     * When true, enables flex-wrap on a Container. Children that overflow the main axis wrap to the next line. Only meaningful on Container elements.
     * 
     */
    @JsonProperty("layoutWrap")
    public Boolean getLayoutWrap() {
        return layoutWrap;
    }

    /**
     * When true, enables flex-wrap on a Container. Children that overflow the main axis wrap to the next line. Only meaningful on Container elements.
     * 
     */
    @JsonProperty("layoutWrap")
    public void setLayoutWrap(Boolean layoutWrap) {
        this.layoutWrap = layoutWrap;
    }

    public AtomicElement withLayoutWrap(Boolean layoutWrap) {
        this.layoutWrap = layoutWrap;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("crossAxisGap")
    public Object getCrossAxisGap() {
        return crossAxisGap;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("crossAxisGap")
    public void setCrossAxisGap(Object crossAxisGap) {
        this.crossAxisGap = crossAxisGap;
    }

    public AtomicElement withCrossAxisGap(Object crossAxisGap) {
        this.crossAxisGap = crossAxisGap;
        return this;
    }

    @JsonProperty("alignSelf")
    public AtomicElement.CrossAlignment getAlignSelf() {
        return alignSelf;
    }

    @JsonProperty("alignSelf")
    public void setAlignSelf(AtomicElement.CrossAlignment alignSelf) {
        this.alignSelf = alignSelf;
    }

    public AtomicElement withAlignSelf(AtomicElement.CrossAlignment alignSelf) {
        this.alignSelf = alignSelf;
        return this;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public AtomicElement withLabel(String label) {
        this.label = label;
        return this;
    }

    @JsonProperty("icon")
    public String getIcon() {
        return icon;
    }

    @JsonProperty("icon")
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public AtomicElement withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    @JsonProperty("disabled")
    public Boolean getDisabled() {
        return disabled;
    }

    @JsonProperty("disabled")
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public AtomicElement withDisabled(Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @JsonProperty("actions")
    public List<Action> getActions() {
        return actions;
    }

    @JsonProperty("actions")
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public AtomicElement withActions(List<Action> actions) {
        this.actions = actions;
        return this;
    }

    @JsonProperty("size")
    public Integer getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Integer size) {
        this.size = size;
    }

    public AtomicElement withSize(Integer size) {
        this.size = size;
        return this;
    }

    @JsonProperty("orientation")
    public AtomicElement.Orientation getOrientation() {
        return orientation;
    }

    @JsonProperty("orientation")
    public void setOrientation(AtomicElement.Orientation orientation) {
        this.orientation = orientation;
    }

    public AtomicElement withOrientation(AtomicElement.Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    @JsonProperty("thickness")
    public Integer getThickness() {
        return thickness;
    }

    @JsonProperty("thickness")
    public void setThickness(Integer thickness) {
        this.thickness = thickness;
    }

    public AtomicElement withThickness(Integer thickness) {
        this.thickness = thickness;
        return this;
    }

    @JsonProperty("paging")
    public Boolean getPaging() {
        return paging;
    }

    @JsonProperty("paging")
    public void setPaging(Boolean paging) {
        this.paging = paging;
    }

    public AtomicElement withPaging(Boolean paging) {
        this.paging = paging;
        return this;
    }

    @JsonProperty("snapAlignment")
    public AtomicElement.SnapAlignment getSnapAlignment() {
        return snapAlignment;
    }

    @JsonProperty("snapAlignment")
    public void setSnapAlignment(AtomicElement.SnapAlignment snapAlignment) {
        this.snapAlignment = snapAlignment;
    }

    public AtomicElement withSnapAlignment(AtomicElement.SnapAlignment snapAlignment) {
        this.snapAlignment = snapAlignment;
        return this;
    }

    @JsonProperty("condition")
    public String getCondition() {
        return condition;
    }

    @JsonProperty("condition")
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public AtomicElement withCondition(String condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("trueChild")
    public AtomicElement getTrueChild() {
        return trueChild;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("trueChild")
    public void setTrueChild(AtomicElement trueChild) {
        this.trueChild = trueChild;
    }

    public AtomicElement withTrueChild(AtomicElement trueChild) {
        this.trueChild = trueChild;
        return this;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("falseChild")
    public AtomicElement getFalseChild() {
        return falseChild;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("falseChild")
    public void setFalseChild(AtomicElement falseChild) {
        this.falseChild = falseChild;
    }

    public AtomicElement withFalseChild(AtomicElement falseChild) {
        this.falseChild = falseChild;
        return this;
    }

    /**
     * DisplayGrid column definitions — display-only, non-interactive, server-ordered
     * 
     */
    @JsonProperty("columns")
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * DisplayGrid column definitions — display-only, non-interactive, server-ordered
     * 
     */
    @JsonProperty("columns")
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public AtomicElement withColumns(List<Column> columns) {
        this.columns = columns;
        return this;
    }

    /**
     * DisplayGrid row data — each object maps column keys to pre-formatted display values
     * 
     */
    @JsonProperty("rows")
    public List<Row> getRows() {
        return rows;
    }

    /**
     * DisplayGrid row data — each object maps column keys to pre-formatted display values
     * 
     */
    @JsonProperty("rows")
    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public AtomicElement withRows(List<Row> rows) {
        this.rows = rows;
        return this;
    }

    /**
     * Alternate row background for readability
     * 
     */
    @JsonProperty("striped")
    public Boolean getStriped() {
        return striped;
    }

    /**
     * Alternate row background for readability
     * 
     */
    @JsonProperty("striped")
    public void setStriped(Boolean striped) {
        this.striped = striped;
    }

    public AtomicElement withStriped(Boolean striped) {
        this.striped = striped;
        return this;
    }

    @JsonProperty("section")
    public Section getSection() {
        return section;
    }

    @JsonProperty("section")
    public void setSection(Section section) {
        this.section = section;
    }

    public AtomicElement withSection(Section section) {
        this.section = section;
        return this;
    }

    /**
     * Flex grow factor. When set on a child of a Container, the child claims proportional space along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
     * 
     */
    @JsonProperty("flex")
    public Double getFlex() {
        return flex;
    }

    /**
     * Flex grow factor. When set on a child of a Container, the child claims proportional space along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
     * 
     */
    @JsonProperty("flex")
    public void setFlex(Double flex) {
        this.flex = flex;
    }

    public AtomicElement withFlex(Double flex) {
        this.flex = flex;
        return this;
    }

    /**
     * Responsive breakpoint in dp/px. For Container: below this screen width, direction flips from row to column. Enables responsive layouts without client logic.
     * 
     */
    @JsonProperty("breakpoint")
    public Integer getBreakpoint() {
        return breakpoint;
    }

    /**
     * Responsive breakpoint in dp/px. For Container: below this screen width, direction flips from row to column. Enables responsive layouts without client logic.
     * 
     */
    @JsonProperty("breakpoint")
    public void setBreakpoint(Integer breakpoint) {
        this.breakpoint = breakpoint;
    }

    public AtomicElement withBreakpoint(Integer breakpoint) {
        this.breakpoint = breakpoint;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("cornerRadius")
    public Object getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("cornerRadius")
    public void setCornerRadius(Object cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public AtomicElement withCornerRadius(Object cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    /**
     * Per-corner cornerRadius override. When present, takes precedence over the single-value cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius / borderBottomRightRadius.
     * 
     */
    @JsonProperty("cornerRadii")
    public CornerRadii getCornerRadii() {
        return cornerRadii;
    }

    /**
     * Per-corner cornerRadius override. When present, takes precedence over the single-value cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius / borderBottomRightRadius.
     * 
     */
    @JsonProperty("cornerRadii")
    public void setCornerRadii(CornerRadii cornerRadii) {
        this.cornerRadii = cornerRadii;
    }

    public AtomicElement withCornerRadii(CornerRadii cornerRadii) {
        this.cornerRadii = cornerRadii;
        return this;
    }

    /**
     * Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded states.
     * 
     */
    @JsonProperty("opacity")
    public Double getOpacity() {
        return opacity;
    }

    /**
     * Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded states.
     * 
     */
    @JsonProperty("opacity")
    public void setOpacity(Double opacity) {
        this.opacity = opacity;
    }

    public AtomicElement withOpacity(Double opacity) {
        this.opacity = opacity;
        return this;
    }

    /**
     * Ordered array of shadow layers. Index 0 is the outermost shadow (Figma convention); higher indices are closer to the element. Maps directly to CSS box-shadow list order. Single-layer shadows ship as a one-element array.
     * 
     */
    @JsonProperty("shadows")
    public List<Object> getShadows() {
        return shadows;
    }

    /**
     * Ordered array of shadow layers. Index 0 is the outermost shadow (Figma convention); higher indices are closer to the element. Maps directly to CSS box-shadow list order. Single-layer shadows ship as a one-element array.
     * 
     */
    @JsonProperty("shadows")
    public void setShadows(List<Object> shadows) {
        this.shadows = shadows;
    }

    public AtomicElement withShadows(List<Object> shadows) {
        this.shadows = shadows;
        return this;
    }

    /**
     * Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the screen-level overlays map.
     * 
     */
    @JsonProperty("badge")
    public Badge getBadge() {
        return badge;
    }

    /**
     * Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the screen-level overlays map.
     * 
     */
    @JsonProperty("badge")
    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public AtomicElement withBadge(Badge badge) {
        this.badge = badge;
        return this;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("base")
    public AtomicElement getBase() {
        return base;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("base")
    public void setBase(AtomicElement base) {
        this.base = base;
    }

    public AtomicElement withBase(AtomicElement base) {
        this.base = base;
        return this;
    }

    /**
     * OverlayContainer layers rendered over the base element in server order.
     * 
     */
    @JsonProperty("overlays")
    public List<AtomicOverlay> getOverlays() {
        return overlays;
    }

    /**
     * OverlayContainer layers rendered over the base element in server order.
     * 
     */
    @JsonProperty("overlays")
    public void setOverlays(List<AtomicOverlay> overlays) {
        this.overlays = overlays;
    }

    public AtomicElement withOverlays(List<AtomicOverlay> overlays) {
        this.overlays = overlays;
        return this;
    }

    /**
     * Text alignment within the element. Used for centered headings, right-aligned numeric values. Absent means the server made no instruction; clients fall back to platform-native locale-aware leading alignment.
     * 
     */
    @JsonProperty("textAlign")
    public AtomicElement.TextAlign getTextAlign() {
        return textAlign;
    }

    /**
     * Text alignment within the element. Used for centered headings, right-aligned numeric values. Absent means the server made no instruction; clients fall back to platform-native locale-aware leading alignment.
     * 
     */
    @JsonProperty("textAlign")
    public void setTextAlign(AtomicElement.TextAlign textAlign) {
        this.textAlign = textAlign;
    }

    public AtomicElement withTextAlign(AtomicElement.TextAlign textAlign) {
        this.textAlign = textAlign;
        return this;
    }

    /**
     * Whether to show scroll indicators on ScrollContainer. Default false for clean carousel presentation.
     * 
     */
    @JsonProperty("showIndicators")
    public Boolean getShowIndicators() {
        return showIndicators;
    }

    /**
     * Whether to show scroll indicators on ScrollContainer. Default false for clean carousel presentation.
     * 
     */
    @JsonProperty("showIndicators")
    public void setShowIndicators(Boolean showIndicators) {
        this.showIndicators = showIndicators;
    }

    public AtomicElement withShowIndicators(Boolean showIndicators) {
        this.showIndicators = showIndicators;
        return this;
    }

    /**
     * Server-declared scroll page indicator presentation for ScrollContainer. Clients own local scroll state only to realize the declared affordance.
     * 
     */
    @JsonProperty("pageIndicator")
    public PageIndicator getPageIndicator() {
        return pageIndicator;
    }

    /**
     * Server-declared scroll page indicator presentation for ScrollContainer. Clients own local scroll state only to realize the declared affordance.
     * 
     */
    @JsonProperty("pageIndicator")
    public void setPageIndicator(PageIndicator pageIndicator) {
        this.pageIndicator = pageIndicator;
    }

    public AtomicElement withPageIndicator(PageIndicator pageIndicator) {
        this.pageIndicator = pageIndicator;
        return this;
    }

    /**
     * Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes (scores, clocks).
     * 
     */
    @JsonProperty("monospacedDigits")
    public Boolean getMonospacedDigits() {
        return monospacedDigits;
    }

    /**
     * Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes (scores, clocks).
     * 
     */
    @JsonProperty("monospacedDigits")
    public void setMonospacedDigits(Boolean monospacedDigits) {
        this.monospacedDigits = monospacedDigits;
    }

    public AtomicElement withMonospacedDigits(Boolean monospacedDigits) {
        this.monospacedDigits = monospacedDigits;
        return this;
    }

    /**
     * LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.
     * 
     */
    @JsonProperty("snapshotSeconds")
    public Integer getSnapshotSeconds() {
        return snapshotSeconds;
    }

    /**
     * LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.
     * 
     */
    @JsonProperty("snapshotSeconds")
    public void setSnapshotSeconds(Integer snapshotSeconds) {
        this.snapshotSeconds = snapshotSeconds;
    }

    public AtomicElement withSnapshotSeconds(Integer snapshotSeconds) {
        this.snapshotSeconds = snapshotSeconds;
        return this;
    }

    /**
     * LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients compute elapsed = now - snapshotAt and derive the displayed value. Required when type == 'LiveClock'.
     * 
     */
    @JsonProperty("snapshotAt")
    public OffsetDateTime getSnapshotAt() {
        return snapshotAt;
    }

    /**
     * LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients compute elapsed = now - snapshotAt and derive the displayed value. Required when type == 'LiveClock'.
     * 
     */
    @JsonProperty("snapshotAt")
    public void setSnapshotAt(OffsetDateTime snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public AtomicElement withSnapshotAt(OffsetDateTime snapshotAt) {
        this.snapshotAt = snapshotAt;
        return this;
    }

    /**
     * LiveClock: whether the clock is actively ticking. When true, clients run a local tick loop at their platform-native refresh cadence (~10Hz) and update the displayed value. When false, clients render snapshotSeconds verbatim.
     * 
     */
    @JsonProperty("isRunning")
    public Boolean getIsRunning() {
        return isRunning;
    }

    /**
     * LiveClock: whether the clock is actively ticking. When true, clients run a local tick loop at their platform-native refresh cadence (~10Hz) and update the displayed value. When false, clients render snapshotSeconds verbatim.
     * 
     */
    @JsonProperty("isRunning")
    public void setIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
    }

    public AtomicElement withIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
        return this;
    }

    /**
     * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds (default 0); 'up' increments from snapshotSeconds with no upper bound unless stopAtSeconds is set. Required on every LiveClock; no static schema default.
     * 
     */
    @JsonProperty("tickDirection")
    public AtomicElement.TickDirection getTickDirection() {
        return tickDirection;
    }

    /**
     * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds (default 0); 'up' increments from snapshotSeconds with no upper bound unless stopAtSeconds is set. Required on every LiveClock; no static schema default.
     * 
     */
    @JsonProperty("tickDirection")
    public void setTickDirection(AtomicElement.TickDirection tickDirection) {
        this.tickDirection = tickDirection;
    }

    public AtomicElement withTickDirection(AtomicElement.TickDirection tickDirection) {
        this.tickDirection = tickDirection;
        return this;
    }

    /**
     * LiveClock: optional clamp. For direction 'down', clock holds at this value once reached. For direction 'up', clock holds once reached. Omit to disable the clamp.
     * 
     */
    @JsonProperty("stopAtSeconds")
    public Integer getStopAtSeconds() {
        return stopAtSeconds;
    }

    /**
     * LiveClock: optional clamp. For direction 'down', clock holds at this value once reached. For direction 'up', clock holds once reached. Omit to disable the clamp.
     * 
     */
    @JsonProperty("stopAtSeconds")
    public void setStopAtSeconds(Integer stopAtSeconds) {
        this.stopAtSeconds = stopAtSeconds;
    }

    public AtomicElement withStopAtSeconds(Integer stopAtSeconds) {
        this.stopAtSeconds = stopAtSeconds;
        return this;
    }

    /**
     * LiveClock display format. Clients realize using their platform's tabular-numerals typography (equivalent to TextVariant.score). Required on every LiveClock; no static schema default.
     * 
     */
    @JsonProperty("format")
    public AtomicElement.Format getFormat() {
        return format;
    }

    /**
     * LiveClock display format. Clients realize using their platform's tabular-numerals typography (equivalent to TextVariant.score). Required on every LiveClock; no static schema default.
     * 
     */
    @JsonProperty("format")
    public void setFormat(AtomicElement.Format format) {
        this.format = format;
    }

    public AtomicElement withFormat(AtomicElement.Format format) {
        this.format = format;
        return this;
    }

    /**
     * Dot-path into the enclosing AtomicComposite's `data.content` object. When set, renderers resolve the leaf's canonical live field from `data.content[bindRef]` at render time and fall back to the inline value when the path is absent. Canonical field per type: Text → content, Button → label, Image → src, LiveClock → an object with {snapshotSeconds, snapshotAt, isRunning}. Placing the binding identifier on the consuming node (rather than in a centrally-declared path-into-tree) lets composers reshape the ui tree without breaking real-time updates; data-bindings on the section envelope continue to write into `content.*`.
     * 
     */
    @JsonProperty("bindRef")
    public String getBindRef() {
        return bindRef;
    }

    /**
     * Dot-path into the enclosing AtomicComposite's `data.content` object. When set, renderers resolve the leaf's canonical live field from `data.content[bindRef]` at render time and fall back to the inline value when the path is absent. Canonical field per type: Text → content, Button → label, Image → src, LiveClock → an object with {snapshotSeconds, snapshotAt, isRunning}. Placing the binding identifier on the consuming node (rather than in a centrally-declared path-into-tree) lets composers reshape the ui tree without breaking real-time updates; data-bindings on the section envelope continue to write into `content.*`.
     * 
     */
    @JsonProperty("bindRef")
    public void setBindRef(String bindRef) {
        this.bindRef = bindRef;
    }

    public AtomicElement withBindRef(String bindRef) {
        this.bindRef = bindRef;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public AtomicElement withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AtomicElement.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("accessibility");
        sb.append('=');
        sb.append(((this.accessibility == null)?"<null>":this.accessibility));
        sb.append(',');
        sb.append("children");
        sb.append('=');
        sb.append(((this.children == null)?"<null>":this.children));
        sb.append(',');
        sb.append("direction");
        sb.append('=');
        sb.append(((this.direction == null)?"<null>":this.direction));
        sb.append(',');
        sb.append("alignment");
        sb.append('=');
        sb.append(((this.alignment == null)?"<null>":this.alignment));
        sb.append(',');
        sb.append("crossAlignment");
        sb.append('=');
        sb.append(((this.crossAlignment == null)?"<null>":this.crossAlignment));
        sb.append(',');
        sb.append("gap");
        sb.append('=');
        sb.append(((this.gap == null)?"<null>":this.gap));
        sb.append(',');
        sb.append("padding");
        sb.append('=');
        sb.append(((this.padding == null)?"<null>":this.padding));
        sb.append(',');
        sb.append("margin");
        sb.append('=');
        sb.append(((this.margin == null)?"<null>":this.margin));
        sb.append(',');
        sb.append("backgrounds");
        sb.append('=');
        sb.append(((this.backgrounds == null)?"<null>":this.backgrounds));
        sb.append(',');
        sb.append("content");
        sb.append('=');
        sb.append(((this.content == null)?"<null>":this.content));
        sb.append(',');
        sb.append("variant");
        sb.append('=');
        sb.append(((this.variant == null)?"<null>":this.variant));
        sb.append(',');
        sb.append("weight");
        sb.append('=');
        sb.append(((this.weight == null)?"<null>":this.weight));
        sb.append(',');
        sb.append("color");
        sb.append('=');
        sb.append(((this.color == null)?"<null>":this.color));
        sb.append(',');
        sb.append("maxLines");
        sb.append('=');
        sb.append(((this.maxLines == null)?"<null>":this.maxLines));
        sb.append(',');
        sb.append("src");
        sb.append('=');
        sb.append(((this.src == null)?"<null>":this.src));
        sb.append(',');
        sb.append("aspectRatio");
        sb.append('=');
        sb.append(((this.aspectRatio == null)?"<null>":this.aspectRatio));
        sb.append(',');
        sb.append("fit");
        sb.append('=');
        sb.append(((this.fit == null)?"<null>":this.fit));
        sb.append(',');
        sb.append("placeholder");
        sb.append('=');
        sb.append(((this.placeholder == null)?"<null>":this.placeholder));
        sb.append(',');
        sb.append("width");
        sb.append('=');
        sb.append(((this.width == null)?"<null>":this.width));
        sb.append(',');
        sb.append("height");
        sb.append('=');
        sb.append(((this.height == null)?"<null>":this.height));
        sb.append(',');
        sb.append("widthMode");
        sb.append('=');
        sb.append(((this.widthMode == null)?"<null>":this.widthMode));
        sb.append(',');
        sb.append("heightMode");
        sb.append('=');
        sb.append(((this.heightMode == null)?"<null>":this.heightMode));
        sb.append(',');
        sb.append("minWidth");
        sb.append('=');
        sb.append(((this.minWidth == null)?"<null>":this.minWidth));
        sb.append(',');
        sb.append("maxWidth");
        sb.append('=');
        sb.append(((this.maxWidth == null)?"<null>":this.maxWidth));
        sb.append(',');
        sb.append("minHeight");
        sb.append('=');
        sb.append(((this.minHeight == null)?"<null>":this.minHeight));
        sb.append(',');
        sb.append("maxHeight");
        sb.append('=');
        sb.append(((this.maxHeight == null)?"<null>":this.maxHeight));
        sb.append(',');
        sb.append("layoutWrap");
        sb.append('=');
        sb.append(((this.layoutWrap == null)?"<null>":this.layoutWrap));
        sb.append(',');
        sb.append("crossAxisGap");
        sb.append('=');
        sb.append(((this.crossAxisGap == null)?"<null>":this.crossAxisGap));
        sb.append(',');
        sb.append("alignSelf");
        sb.append('=');
        sb.append(((this.alignSelf == null)?"<null>":this.alignSelf));
        sb.append(',');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("icon");
        sb.append('=');
        sb.append(((this.icon == null)?"<null>":this.icon));
        sb.append(',');
        sb.append("disabled");
        sb.append('=');
        sb.append(((this.disabled == null)?"<null>":this.disabled));
        sb.append(',');
        sb.append("actions");
        sb.append('=');
        sb.append(((this.actions == null)?"<null>":this.actions));
        sb.append(',');
        sb.append("size");
        sb.append('=');
        sb.append(((this.size == null)?"<null>":this.size));
        sb.append(',');
        sb.append("orientation");
        sb.append('=');
        sb.append(((this.orientation == null)?"<null>":this.orientation));
        sb.append(',');
        sb.append("thickness");
        sb.append('=');
        sb.append(((this.thickness == null)?"<null>":this.thickness));
        sb.append(',');
        sb.append("paging");
        sb.append('=');
        sb.append(((this.paging == null)?"<null>":this.paging));
        sb.append(',');
        sb.append("snapAlignment");
        sb.append('=');
        sb.append(((this.snapAlignment == null)?"<null>":this.snapAlignment));
        sb.append(',');
        sb.append("condition");
        sb.append('=');
        sb.append(((this.condition == null)?"<null>":this.condition));
        sb.append(',');
        sb.append("trueChild");
        sb.append('=');
        sb.append(((this.trueChild == null)?"<null>":this.trueChild));
        sb.append(',');
        sb.append("falseChild");
        sb.append('=');
        sb.append(((this.falseChild == null)?"<null>":this.falseChild));
        sb.append(',');
        sb.append("columns");
        sb.append('=');
        sb.append(((this.columns == null)?"<null>":this.columns));
        sb.append(',');
        sb.append("rows");
        sb.append('=');
        sb.append(((this.rows == null)?"<null>":this.rows));
        sb.append(',');
        sb.append("striped");
        sb.append('=');
        sb.append(((this.striped == null)?"<null>":this.striped));
        sb.append(',');
        sb.append("section");
        sb.append('=');
        sb.append(((this.section == null)?"<null>":this.section));
        sb.append(',');
        sb.append("flex");
        sb.append('=');
        sb.append(((this.flex == null)?"<null>":this.flex));
        sb.append(',');
        sb.append("breakpoint");
        sb.append('=');
        sb.append(((this.breakpoint == null)?"<null>":this.breakpoint));
        sb.append(',');
        sb.append("cornerRadius");
        sb.append('=');
        sb.append(((this.cornerRadius == null)?"<null>":this.cornerRadius));
        sb.append(',');
        sb.append("cornerRadii");
        sb.append('=');
        sb.append(((this.cornerRadii == null)?"<null>":this.cornerRadii));
        sb.append(',');
        sb.append("opacity");
        sb.append('=');
        sb.append(((this.opacity == null)?"<null>":this.opacity));
        sb.append(',');
        sb.append("shadows");
        sb.append('=');
        sb.append(((this.shadows == null)?"<null>":this.shadows));
        sb.append(',');
        sb.append("badge");
        sb.append('=');
        sb.append(((this.badge == null)?"<null>":this.badge));
        sb.append(',');
        sb.append("base");
        sb.append('=');
        sb.append(((this.base == null)?"<null>":this.base));
        sb.append(',');
        sb.append("overlays");
        sb.append('=');
        sb.append(((this.overlays == null)?"<null>":this.overlays));
        sb.append(',');
        sb.append("textAlign");
        sb.append('=');
        sb.append(((this.textAlign == null)?"<null>":this.textAlign));
        sb.append(',');
        sb.append("showIndicators");
        sb.append('=');
        sb.append(((this.showIndicators == null)?"<null>":this.showIndicators));
        sb.append(',');
        sb.append("pageIndicator");
        sb.append('=');
        sb.append(((this.pageIndicator == null)?"<null>":this.pageIndicator));
        sb.append(',');
        sb.append("monospacedDigits");
        sb.append('=');
        sb.append(((this.monospacedDigits == null)?"<null>":this.monospacedDigits));
        sb.append(',');
        sb.append("snapshotSeconds");
        sb.append('=');
        sb.append(((this.snapshotSeconds == null)?"<null>":this.snapshotSeconds));
        sb.append(',');
        sb.append("snapshotAt");
        sb.append('=');
        sb.append(((this.snapshotAt == null)?"<null>":this.snapshotAt));
        sb.append(',');
        sb.append("isRunning");
        sb.append('=');
        sb.append(((this.isRunning == null)?"<null>":this.isRunning));
        sb.append(',');
        sb.append("tickDirection");
        sb.append('=');
        sb.append(((this.tickDirection == null)?"<null>":this.tickDirection));
        sb.append(',');
        sb.append("stopAtSeconds");
        sb.append('=');
        sb.append(((this.stopAtSeconds == null)?"<null>":this.stopAtSeconds));
        sb.append(',');
        sb.append("format");
        sb.append('=');
        sb.append(((this.format == null)?"<null>":this.format));
        sb.append(',');
        sb.append("bindRef");
        sb.append('=');
        sb.append(((this.bindRef == null)?"<null>":this.bindRef));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.accessibility == null)? 0 :this.accessibility.hashCode()));
        result = ((result* 31)+((this.thickness == null)? 0 :this.thickness.hashCode()));
        result = ((result* 31)+((this.aspectRatio == null)? 0 :this.aspectRatio.hashCode()));
        result = ((result* 31)+((this.section == null)? 0 :this.section.hashCode()));
        result = ((result* 31)+((this.snapshotAt == null)? 0 :this.snapshotAt.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.cornerRadius == null)? 0 :this.cornerRadius.hashCode()));
        result = ((result* 31)+((this.fit == null)? 0 :this.fit.hashCode()));
        result = ((result* 31)+((this.minHeight == null)? 0 :this.minHeight.hashCode()));
        result = ((result* 31)+((this.tickDirection == null)? 0 :this.tickDirection.hashCode()));
        result = ((result* 31)+((this.children == null)? 0 :this.children.hashCode()));
        result = ((result* 31)+((this.trueChild == null)? 0 :this.trueChild.hashCode()));
        result = ((result* 31)+((this.variant == null)? 0 :this.variant.hashCode()));
        result = ((result* 31)+((this.bindRef == null)? 0 :this.bindRef.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.height == null)? 0 :this.height.hashCode()));
        result = ((result* 31)+((this.maxWidth == null)? 0 :this.maxWidth.hashCode()));
        result = ((result* 31)+((this.margin == null)? 0 :this.margin.hashCode()));
        result = ((result* 31)+((this.alignSelf == null)? 0 :this.alignSelf.hashCode()));
        result = ((result* 31)+((this.textAlign == null)? 0 :this.textAlign.hashCode()));
        result = ((result* 31)+((this.format == null)? 0 :this.format.hashCode()));
        result = ((result* 31)+((this.weight == null)? 0 :this.weight.hashCode()));
        result = ((result* 31)+((this.paging == null)? 0 :this.paging.hashCode()));
        result = ((result* 31)+((this.badge == null)? 0 :this.badge.hashCode()));
        result = ((result* 31)+((this.condition == null)? 0 :this.condition.hashCode()));
        result = ((result* 31)+((this.cornerRadii == null)? 0 :this.cornerRadii.hashCode()));
        result = ((result* 31)+((this.crossAlignment == null)? 0 :this.crossAlignment.hashCode()));
        result = ((result* 31)+((this.size == null)? 0 :this.size.hashCode()));
        result = ((result* 31)+((this.overlays == null)? 0 :this.overlays.hashCode()));
        result = ((result* 31)+((this.heightMode == null)? 0 :this.heightMode.hashCode()));
        result = ((result* 31)+((this.maxLines == null)? 0 :this.maxLines.hashCode()));
        result = ((result* 31)+((this.shadows == null)? 0 :this.shadows.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.alignment == null)? 0 :this.alignment.hashCode()));
        result = ((result* 31)+((this.snapshotSeconds == null)? 0 :this.snapshotSeconds.hashCode()));
        result = ((result* 31)+((this.actions == null)? 0 :this.actions.hashCode()));
        result = ((result* 31)+((this.color == null)? 0 :this.color.hashCode()));
        result = ((result* 31)+((this.columns == null)? 0 :this.columns.hashCode()));
        result = ((result* 31)+((this.icon == null)? 0 :this.icon.hashCode()));
        result = ((result* 31)+((this.layoutWrap == null)? 0 :this.layoutWrap.hashCode()));
        result = ((result* 31)+((this.showIndicators == null)? 0 :this.showIndicators.hashCode()));
        result = ((result* 31)+((this.content == null)? 0 :this.content.hashCode()));
        result = ((result* 31)+((this.monospacedDigits == null)? 0 :this.monospacedDigits.hashCode()));
        result = ((result* 31)+((this.maxHeight == null)? 0 :this.maxHeight.hashCode()));
        result = ((result* 31)+((this.isRunning == null)? 0 :this.isRunning.hashCode()));
        result = ((result* 31)+((this.flex == null)? 0 :this.flex.hashCode()));
        result = ((result* 31)+((this.gap == null)? 0 :this.gap.hashCode()));
        result = ((result* 31)+((this.disabled == null)? 0 :this.disabled.hashCode()));
        result = ((result* 31)+((this.placeholder == null)? 0 :this.placeholder.hashCode()));
        result = ((result* 31)+((this.stopAtSeconds == null)? 0 :this.stopAtSeconds.hashCode()));
        result = ((result* 31)+((this.direction == null)? 0 :this.direction.hashCode()));
        result = ((result* 31)+((this.padding == null)? 0 :this.padding.hashCode()));
        result = ((result* 31)+((this.striped == null)? 0 :this.striped.hashCode()));
        result = ((result* 31)+((this.pageIndicator == null)? 0 :this.pageIndicator.hashCode()));
        result = ((result* 31)+((this.orientation == null)? 0 :this.orientation.hashCode()));
        result = ((result* 31)+((this.widthMode == null)? 0 :this.widthMode.hashCode()));
        result = ((result* 31)+((this.src == null)? 0 :this.src.hashCode()));
        result = ((result* 31)+((this.minWidth == null)? 0 :this.minWidth.hashCode()));
        result = ((result* 31)+((this.crossAxisGap == null)? 0 :this.crossAxisGap.hashCode()));
        result = ((result* 31)+((this.label == null)? 0 :this.label.hashCode()));
        result = ((result* 31)+((this.rows == null)? 0 :this.rows.hashCode()));
        result = ((result* 31)+((this.falseChild == null)? 0 :this.falseChild.hashCode()));
        result = ((result* 31)+((this.breakpoint == null)? 0 :this.breakpoint.hashCode()));
        result = ((result* 31)+((this.snapAlignment == null)? 0 :this.snapAlignment.hashCode()));
        result = ((result* 31)+((this.backgrounds == null)? 0 :this.backgrounds.hashCode()));
        result = ((result* 31)+((this.width == null)? 0 :this.width.hashCode()));
        result = ((result* 31)+((this.opacity == null)? 0 :this.opacity.hashCode()));
        result = ((result* 31)+((this.base == null)? 0 :this.base.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AtomicElement) == false) {
            return false;
        }
        AtomicElement rhs = ((AtomicElement) other);
        return (((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((this.accessibility == rhs.accessibility)||((this.accessibility!= null)&&this.accessibility.equals(rhs.accessibility)))&&((this.thickness == rhs.thickness)||((this.thickness!= null)&&this.thickness.equals(rhs.thickness))))&&((this.aspectRatio == rhs.aspectRatio)||((this.aspectRatio!= null)&&this.aspectRatio.equals(rhs.aspectRatio))))&&((this.section == rhs.section)||((this.section!= null)&&this.section.equals(rhs.section))))&&((this.snapshotAt == rhs.snapshotAt)||((this.snapshotAt!= null)&&this.snapshotAt.equals(rhs.snapshotAt))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.cornerRadius == rhs.cornerRadius)||((this.cornerRadius!= null)&&this.cornerRadius.equals(rhs.cornerRadius))))&&((this.fit == rhs.fit)||((this.fit!= null)&&this.fit.equals(rhs.fit))))&&((this.minHeight == rhs.minHeight)||((this.minHeight!= null)&&this.minHeight.equals(rhs.minHeight))))&&((this.tickDirection == rhs.tickDirection)||((this.tickDirection!= null)&&this.tickDirection.equals(rhs.tickDirection))))&&((this.children == rhs.children)||((this.children!= null)&&this.children.equals(rhs.children))))&&((this.trueChild == rhs.trueChild)||((this.trueChild!= null)&&this.trueChild.equals(rhs.trueChild))))&&((this.variant == rhs.variant)||((this.variant!= null)&&this.variant.equals(rhs.variant))))&&((this.bindRef == rhs.bindRef)||((this.bindRef!= null)&&this.bindRef.equals(rhs.bindRef))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.height == rhs.height)||((this.height!= null)&&this.height.equals(rhs.height))))&&((this.maxWidth == rhs.maxWidth)||((this.maxWidth!= null)&&this.maxWidth.equals(rhs.maxWidth))))&&((this.margin == rhs.margin)||((this.margin!= null)&&this.margin.equals(rhs.margin))))&&((this.alignSelf == rhs.alignSelf)||((this.alignSelf!= null)&&this.alignSelf.equals(rhs.alignSelf))))&&((this.textAlign == rhs.textAlign)||((this.textAlign!= null)&&this.textAlign.equals(rhs.textAlign))))&&((this.format == rhs.format)||((this.format!= null)&&this.format.equals(rhs.format))))&&((this.weight == rhs.weight)||((this.weight!= null)&&this.weight.equals(rhs.weight))))&&((this.paging == rhs.paging)||((this.paging!= null)&&this.paging.equals(rhs.paging))))&&((this.badge == rhs.badge)||((this.badge!= null)&&this.badge.equals(rhs.badge))))&&((this.condition == rhs.condition)||((this.condition!= null)&&this.condition.equals(rhs.condition))))&&((this.cornerRadii == rhs.cornerRadii)||((this.cornerRadii!= null)&&this.cornerRadii.equals(rhs.cornerRadii))))&&((this.crossAlignment == rhs.crossAlignment)||((this.crossAlignment!= null)&&this.crossAlignment.equals(rhs.crossAlignment))))&&((this.size == rhs.size)||((this.size!= null)&&this.size.equals(rhs.size))))&&((this.overlays == rhs.overlays)||((this.overlays!= null)&&this.overlays.equals(rhs.overlays))))&&((this.heightMode == rhs.heightMode)||((this.heightMode!= null)&&this.heightMode.equals(rhs.heightMode))))&&((this.maxLines == rhs.maxLines)||((this.maxLines!= null)&&this.maxLines.equals(rhs.maxLines))))&&((this.shadows == rhs.shadows)||((this.shadows!= null)&&this.shadows.equals(rhs.shadows))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.alignment == rhs.alignment)||((this.alignment!= null)&&this.alignment.equals(rhs.alignment))))&&((this.snapshotSeconds == rhs.snapshotSeconds)||((this.snapshotSeconds!= null)&&this.snapshotSeconds.equals(rhs.snapshotSeconds))))&&((this.actions == rhs.actions)||((this.actions!= null)&&this.actions.equals(rhs.actions))))&&((this.color == rhs.color)||((this.color!= null)&&this.color.equals(rhs.color))))&&((this.columns == rhs.columns)||((this.columns!= null)&&this.columns.equals(rhs.columns))))&&((this.icon == rhs.icon)||((this.icon!= null)&&this.icon.equals(rhs.icon))))&&((this.layoutWrap == rhs.layoutWrap)||((this.layoutWrap!= null)&&this.layoutWrap.equals(rhs.layoutWrap))))&&((this.showIndicators == rhs.showIndicators)||((this.showIndicators!= null)&&this.showIndicators.equals(rhs.showIndicators))))&&((this.content == rhs.content)||((this.content!= null)&&this.content.equals(rhs.content))))&&((this.monospacedDigits == rhs.monospacedDigits)||((this.monospacedDigits!= null)&&this.monospacedDigits.equals(rhs.monospacedDigits))))&&((this.maxHeight == rhs.maxHeight)||((this.maxHeight!= null)&&this.maxHeight.equals(rhs.maxHeight))))&&((this.isRunning == rhs.isRunning)||((this.isRunning!= null)&&this.isRunning.equals(rhs.isRunning))))&&((this.flex == rhs.flex)||((this.flex!= null)&&this.flex.equals(rhs.flex))))&&((this.gap == rhs.gap)||((this.gap!= null)&&this.gap.equals(rhs.gap))))&&((this.disabled == rhs.disabled)||((this.disabled!= null)&&this.disabled.equals(rhs.disabled))))&&((this.placeholder == rhs.placeholder)||((this.placeholder!= null)&&this.placeholder.equals(rhs.placeholder))))&&((this.stopAtSeconds == rhs.stopAtSeconds)||((this.stopAtSeconds!= null)&&this.stopAtSeconds.equals(rhs.stopAtSeconds))))&&((this.direction == rhs.direction)||((this.direction!= null)&&this.direction.equals(rhs.direction))))&&((this.padding == rhs.padding)||((this.padding!= null)&&this.padding.equals(rhs.padding))))&&((this.striped == rhs.striped)||((this.striped!= null)&&this.striped.equals(rhs.striped))))&&((this.pageIndicator == rhs.pageIndicator)||((this.pageIndicator!= null)&&this.pageIndicator.equals(rhs.pageIndicator))))&&((this.orientation == rhs.orientation)||((this.orientation!= null)&&this.orientation.equals(rhs.orientation))))&&((this.widthMode == rhs.widthMode)||((this.widthMode!= null)&&this.widthMode.equals(rhs.widthMode))))&&((this.src == rhs.src)||((this.src!= null)&&this.src.equals(rhs.src))))&&((this.minWidth == rhs.minWidth)||((this.minWidth!= null)&&this.minWidth.equals(rhs.minWidth))))&&((this.crossAxisGap == rhs.crossAxisGap)||((this.crossAxisGap!= null)&&this.crossAxisGap.equals(rhs.crossAxisGap))))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.rows == rhs.rows)||((this.rows!= null)&&this.rows.equals(rhs.rows))))&&((this.falseChild == rhs.falseChild)||((this.falseChild!= null)&&this.falseChild.equals(rhs.falseChild))))&&((this.breakpoint == rhs.breakpoint)||((this.breakpoint!= null)&&this.breakpoint.equals(rhs.breakpoint))))&&((this.snapAlignment == rhs.snapAlignment)||((this.snapAlignment!= null)&&this.snapAlignment.equals(rhs.snapAlignment))))&&((this.backgrounds == rhs.backgrounds)||((this.backgrounds!= null)&&this.backgrounds.equals(rhs.backgrounds))))&&((this.width == rhs.width)||((this.width!= null)&&this.width.equals(rhs.width))))&&((this.opacity == rhs.opacity)||((this.opacity!= null)&&this.opacity.equals(rhs.opacity))))&&((this.base == rhs.base)||((this.base!= null)&&this.base.equals(rhs.base))));
    }

    @Generated("jsonschema2pojo")
    public enum Alignment {

        START("start"),
        CENTER("center"),
        END("end"),
        SPACE_BETWEEN("spaceBetween"),
        SPACE_AROUND("spaceAround"),
        SPACE_EVENLY("spaceEvenly");
        private final String value;
        private final static Map<String, AtomicElement.Alignment> CONSTANTS = new HashMap<String, AtomicElement.Alignment>();

        static {
            for (AtomicElement.Alignment c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Alignment(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.Alignment fromValue(String value) {
            AtomicElement.Alignment constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum CrossAlignment {

        START("start"),
        CENTER("center"),
        END("end"),
        STRETCH("stretch");
        private final String value;
        private final static Map<String, AtomicElement.CrossAlignment> CONSTANTS = new HashMap<String, AtomicElement.CrossAlignment>();

        static {
            for (AtomicElement.CrossAlignment c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        CrossAlignment(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.CrossAlignment fromValue(String value) {
            AtomicElement.CrossAlignment constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum Direction {

        ROW("row"),
        COLUMN("column");
        private final String value;
        private final static Map<String, AtomicElement.Direction> CONSTANTS = new HashMap<String, AtomicElement.Direction>();

        static {
            for (AtomicElement.Direction c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Direction(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.Direction fromValue(String value) {
            AtomicElement.Direction constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * LiveClock display format. Clients realize using their platform's tabular-numerals typography (equivalent to TextVariant.score). Required on every LiveClock; no static schema default.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Format {

        M_SS("m:ss"),
        MM_SS("mm:ss"),
        H_MM_SS("h:mm:ss");
        private final String value;
        private final static Map<String, AtomicElement.Format> CONSTANTS = new HashMap<String, AtomicElement.Format>();

        static {
            for (AtomicElement.Format c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Format(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.Format fromValue(String value) {
            AtomicElement.Format constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum ImageFit {

        COVER("cover"),
        CONTAIN("contain"),
        FILL("fill"),
        NONE("none");
        private final String value;
        private final static Map<String, AtomicElement.ImageFit> CONSTANTS = new HashMap<String, AtomicElement.ImageFit>();

        static {
            for (AtomicElement.ImageFit c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ImageFit(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.ImageFit fromValue(String value) {
            AtomicElement.ImageFit constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum Orientation {

        HORIZONTAL("horizontal"),
        VERTICAL("vertical");
        private final String value;
        private final static Map<String, AtomicElement.Orientation> CONSTANTS = new HashMap<String, AtomicElement.Orientation>();

        static {
            for (AtomicElement.Orientation c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Orientation(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.Orientation fromValue(String value) {
            AtomicElement.Orientation constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Sizing behavior along one axis. 'hug' sizes to content. 'fill' stretches to parent available space. 'fixed' uses the explicit width/height value. The correct value depends on whether width/height is also set, so there is no static schema default; an absent value means the server made no instruction and the client falls back to its platform-native intrinsic sizing rule.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum SizingMode {

        HUG("hug"),
        FILL("fill"),
        FIXED("fixed");
        private final String value;
        private final static Map<String, AtomicElement.SizingMode> CONSTANTS = new HashMap<String, AtomicElement.SizingMode>();

        static {
            for (AtomicElement.SizingMode c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        SizingMode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.SizingMode fromValue(String value) {
            AtomicElement.SizingMode constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum SnapAlignment {

        START("start"),
        CENTER("center"),
        END("end");
        private final String value;
        private final static Map<String, AtomicElement.SnapAlignment> CONSTANTS = new HashMap<String, AtomicElement.SnapAlignment>();

        static {
            for (AtomicElement.SnapAlignment c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        SnapAlignment(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.SnapAlignment fromValue(String value) {
            AtomicElement.SnapAlignment constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Text alignment within the element. Used for centered headings, right-aligned numeric values. Absent means the server made no instruction; clients fall back to platform-native locale-aware leading alignment.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum TextAlign {

        START("start"),
        CENTER("center"),
        END("end");
        private final String value;
        private final static Map<String, AtomicElement.TextAlign> CONSTANTS = new HashMap<String, AtomicElement.TextAlign>();

        static {
            for (AtomicElement.TextAlign c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TextAlign(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.TextAlign fromValue(String value) {
            AtomicElement.TextAlign constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum TextWeight {

        REGULAR("regular"),
        MEDIUM("medium"),
        SEMI_BOLD("semiBold"),
        BOLD("bold");
        private final String value;
        private final static Map<String, AtomicElement.TextWeight> CONSTANTS = new HashMap<String, AtomicElement.TextWeight>();

        static {
            for (AtomicElement.TextWeight c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TextWeight(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.TextWeight fromValue(String value) {
            AtomicElement.TextWeight constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds (default 0); 'up' increments from snapshotSeconds with no upper bound unless stopAtSeconds is set. Required on every LiveClock; no static schema default.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum TickDirection {

        DOWN("down"),
        UP("up");
        private final String value;
        private final static Map<String, AtomicElement.TickDirection> CONSTANTS = new HashMap<String, AtomicElement.TickDirection>();

        static {
            for (AtomicElement.TickDirection c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TickDirection(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.TickDirection fromValue(String value) {
            AtomicElement.TickDirection constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum Type {

        CONTAINER("Container"),
        TEXT("Text"),
        IMAGE("Image"),
        BUTTON("Button"),
        SPACER("Spacer"),
        DIVIDER("Divider"),
        SCROLL_CONTAINER("ScrollContainer"),
        CONDITIONAL("Conditional"),
        DISPLAY_GRID("DisplayGrid"),
        SECTION_SLOT("SectionSlot"),
        LIVE_CLOCK("LiveClock"),
        OVERLAY_CONTAINER("OverlayContainer");
        private final String value;
        private final static Map<String, AtomicElement.Type> CONSTANTS = new HashMap<String, AtomicElement.Type>();

        static {
            for (AtomicElement.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static AtomicElement.Type fromValue(String value) {
            AtomicElement.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
