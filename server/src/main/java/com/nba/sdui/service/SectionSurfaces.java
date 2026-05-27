package com.nba.sdui.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Vocabulary of canonical {@code Section.surface} shapes used by composers.
 *
 * <p>Section outer chrome (margin, padding, background, cornerRadius, shadow,
 * border) flows through {@code Section.surface} and is consumed by the shared
 * SectionContainer (per ADR-015). These named factories provide a discoverable,
 * server-owned palette for composing those wrappers.
 */
@Component
public class SectionSurfaces {

    private final ObjectMapper objectMapper;
    private final SduiUtils utils;

    public SectionSurfaces(ObjectMapper objectMapper, SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.utils = utils;
    }

    /**
     * Build the default section-surface block applied by every permanent
     * section that does not override it. Clients' shared SectionContainer
     * reads this and applies platform-native equivalents. A single change
     * here retunes the entire app's rhythm (card inset, elevation, corner
     * radius) without a client release.
     *
     * <p>Default: {@code nba.spacing.lg} on all four edges of the
     * margin, raised surface background with {@code nba.radius.md}
     * corner radius and a soft 6px-radius shadow at y=2. Matches the
     * reference-app feed card treatment.
     *
     * <p>Vertical margin is the {@code lg} step (not {@code md}) so a
     * card-chromed section is separated from a flush-to-edge section
     * (like a content rail) by 2× {@code lg} of air — rail contributes
     * {@code lg} bottom, card contributes {@code lg} top. The {@code md}
     * step on the card side reads as "no spacing" next to a rail because
     * a flush rail has no visible bottom edge for the eye to latch onto;
     * the {@code lg} step makes the break read as an intentional module
     * boundary.
     */
    public ObjectNode defaultSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.set("margin", utils.spacingTokens(
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG));
        surface.put("background", "token:nba.bg.secondary");
        surface.put("cornerRadius", LayoutTokens.RADIUS_MD);

        // Inline shadow struct (no exact token match): radius:6, offsetY:2 falls
        // between nba.shadow.sm (radius:3, offsetY:1) and nba.shadow.md
        // (radius:8, offsetY:2). Schema permits the inline struct as the
        // documented escape hatch for one-off values that don't deserve a
        // registry entry; clients normalize at the edge via resolveShadowOrToken.
        ObjectNode shadow = objectMapper.createObjectNode();
        shadow.put("color", "#00000014");
        shadow.put("radius", 6);
        shadow.put("offsetX", 0);
        shadow.put("offsetY", 2);
        surface.set("shadow", shadow);

        return surface;
    }

    /**
     * Surface for {@code AdSlot} sections — server-owned outer chrome per
     * AGENTS.md §4.2. Sharp corners, no shadow; {@code nba.spacing.lg}
     * margin on all edges; inner padding {@code md} horizontal and
     * {@code lg} top/bottom (equal vertical rhythm for the disclosure
     * label + creative). The client fills the padded width and derives
     * height from {@code data.sizes[0]} aspect ratio only.
     */
    public ObjectNode adSlotSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.set("margin", utils.spacingTokens(
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG));
        surface.set("padding", utils.spacingTokens(
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_MD,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_MD));
        surface.put("background", "token:nba.bg.secondary");
        surface.put("cornerRadius", 0);
        return surface;
    }

    /**
     * Build a flush (no wrapper chrome) surface block. Used for sections
     * that should render edge-to-edge (hero videos, full-bleed images).
     */
    public ObjectNode flushSurface() {
        return objectMapper.createObjectNode();
    }

    /**
     * Flush surface for refreshed game schedule card tiles — sharp corners, no
     * shadow, secondary background, zero margin. Cards rendered with this surface
     * sit edge-to-edge in the schedule list; inter-card spacing is owned by the
     * parent list's {@code gap} rather than by per-card margin.
     */
    public ObjectNode gameCardFlushSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.put("cornerRadius", 0);
        surface.put("background", "token:nba.bg.secondary");
        return surface;
    }

    /**
     * Square, full-bleed strip: token secondary background and padding, no margin.
     * Composed from standard {@link SectionSurface} fields (not a tab-specific wire type).
     */
    public ObjectNode secondaryStripSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.put("cornerRadius", 0);
        surface.put("background", "token:nba.bg.secondary");
        surface.set("padding", utils.spacingTokens(
                LayoutTokens.SPACING_SM,
                LayoutTokens.SPACING_MD,
                LayoutTokens.SPACING_XS,
                LayoutTokens.SPACING_MD));
        return surface;
    }

    /**
     * Same strip spacing as {@link #secondaryStripSurface()} but without an
     * explicit wrapper background, so the section sits on the screen's default
     * surface.
     */
    public ObjectNode stripSurfaceWithoutBackground() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.put("cornerRadius", 0);
        surface.set("padding", utils.spacingTokens(
                LayoutTokens.SPACING_SM,
                LayoutTokens.SPACING_MD,
                LayoutTokens.SPACING_XS,
                LayoutTokens.SPACING_MD));
        return surface;
    }

    /**
     * Build a surface block for subscription upsell sections — standard
     * section rhythm + branded gradient background + inner padding so
     * the caller's content lays out flush against the surface edges.
     * Used by SubscribeBanner and SubscribeHero composers.
     *
     * <p>The {@code padding} parameter is a raw integer because callers
     * pass values (20, 24) that fall between {@code nba.spacing.lg} (16)
     * and {@code nba.spacing.xl} (32). These are intentional one-off
     * promo-card insets with no exact token mapping; if the callsites
     * converge on a single value, promote it to a new spacing token
     * rather than continuing to pass it inline.
     */
    public ObjectNode subscribeSurface(String topColor, String bottomColor, int padding) {
        ObjectNode surface = defaultSurface();
        ObjectNode gradient = objectMapper.createObjectNode();
        ArrayNode colors = objectMapper.createArrayNode();
        colors.add(topColor);
        colors.add(bottomColor);
        gradient.set("colors", colors);
        gradient.put("direction", "vertical");
        surface.set("background", gradient);
        surface.set("padding", utils.spacingSymmetric(padding, padding));
        return surface;
    }

    /**
     * Token-driven solid-color promo surface — same chrome family as
     * {@link #defaultSurface()} (rounded card on {@code nba.bg.secondary}
     * with the standard {@code lg} margin and subtle shadow), with the
     * background color and inner padding selected by the caller from the
     * design-system token vocabulary. Used by the Games-screen League
     * Pass promo so it stacks with the same visual rhythm as the live
     * and finished game cards below it.
     */
    public ObjectNode promoCardSurface(String backgroundToken, String paddingToken) {
        ObjectNode surface = defaultSurface();
        surface.put("background", backgroundToken);
        surface.set("padding", utils.spacingTokens(
                paddingToken, paddingToken, paddingToken, paddingToken));
        return surface;
    }

    /**
     * Build a surface block for the VideoPlayer section — flush
     * edge-to-edge rectangle with a dark background behind the player
     * area. No margin (the player hugs its siblings) and no corner
     * radius (a rounded video frame is jarring against square content
     * below and makes the tap target feel like a card instead of an
     * embedded player). The player's content sizing (16:9 aspect) is
     * owned by the renderer and by `data.displayConfig`, not by this
     * surface block.
     */
    public ObjectNode videoPlayerSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.put("background", "#1A1F2E");
        return surface;
    }

    /**
     * Build a surface block for a card-chromed, vertically-stacked
     * composite section (e.g. {@code NbaTvSchedule}) — same {@code lg}
     * vertical rhythm as {@link #railSurface()}, plus {@code lg}
     * horizontal margin, a rounded sunken-surface background, and an
     * {@code nba.radius.md} corner radius. Use
     * this for composites that are NOT horizontal-scrolling: rails bleed
     * edge-to-edge on purpose (so off-screen cards peek in), but a
     * vertical-list composite should sit inside a card like {@code AdSlot}
     * does. The background/radius live on the surface (not on the root
     * container) so the chrome is discoverable at the section envelope
     * level and the inner atomic tree stays content-only.
     */
    public ObjectNode cardSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.set("margin", utils.spacingTokens(
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG,
                LayoutTokens.SPACING_LG));
        surface.put("background", "token:nba.bg.tertiary");
        surface.put("cornerRadius", LayoutTokens.RADIUS_MD);
        return surface;
    }

    /**
     * Build a minimal surface block that only provides vertical margin
     * for breathing room between flush-to-the-edge atomic composite
     * sections (content rails, video carousels, section headers). Used
     * where the composite's root Container already owns its own inner
     * chrome (padding, title treatment) but consecutive rails would
     * otherwise touch each other vertically.
     *
     * <p>Outputs only {@code margin: {top, bottom}} — no background,
     * no corner radius, no shadow. That keeps the composite's internal
     * styling untouched and avoids double-chrome.
     */
    public ObjectNode railSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        ObjectNode margin = objectMapper.createObjectNode();
        // nba.spacing.lg (phone:16) top/bottom — pairs with the same
        // token on `defaultSurface` (AdSlot) to give 2× spacing.lg of
        // air between a rail and the card-chromed section that follows
        // it. A flush rail has no visible bottom edge, so the eye reads
        // less spacing than the pixel count suggests; the doubled lg
        // step reads as an intentional module boundary rather than an
        // orphan row.
        margin.put("top", LayoutTokens.SPACING_LG);
        margin.put("bottom", LayoutTokens.SPACING_LG);
        surface.set("margin", margin);
        return surface;
    }

    /**
     * Build a surface block for a SectionHeader that titles the rail or
     * section immediately below it. A SectionHeader is semantically part
     * of the module that follows — the header and its rail should read
     * as one unit.
     *
     * <p>The gap between a header and its content is owned here, so every
     * screen has the same header→content rhythm. If the rail below needs
     * more or less air from the header, adjust this single constant rather
     * than tweaking {@link #railSurface()} — that one is scoped to
     * rail→preceding-section spacing, which is a different rhythm.
     *
     * <p>Emits {@code margin.top = nba.spacing.lg (phone 16), margin.bottom = nba.spacing.md (phone 12)}:
     * <ul>
     *   <li>Top {@code nba.spacing.lg} — pairs with the same token on
     *       preceding card-chromed sections (AdSlot, GamePanel) to
     *       produce a 2× spacing.lg module break before the header.</li>
     *   <li>Bottom {@code nba.spacing.md} — combined with the following
     *       rail's {@code spacing.lg} top margin this produces
     *       lg + md (28pt on phone) between the header surface and the
     *       rail surface. Previously this was 0 (header→rail gap was
     *       just the rail's own top margin) but on device the header's
     *       title line sat too close to the top of the first rail card.
     *       The {@code md} step here reads as "the title belongs to the
     *       rail" without looking flush. Originally tried 8pt raw — was
     *       snapped up to {@code md} (12) so every surface scalar is a
     *       design-token reference per AGENTS.md §3.6.</li>
     * </ul>
     */
    public ObjectNode sectionHeaderSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        ObjectNode margin = objectMapper.createObjectNode();
        margin.put("top", LayoutTokens.SPACING_LG);
        margin.put("bottom", LayoutTokens.SPACING_MD);
        surface.set("margin", margin);
        return surface;
    }

    /**
     * Build a surface block for GamePanel cards — standard card
     * chrome (horizontal inset + subtle shadow + rounded corners)
     * with a soft linear gradient background that gives the matchup
     * its own visual weight versus surrounding flat content. Used by
     * every GamePanel composer site (For You hero, live rails,
     * scoreboard rows, Game Detail scoreboard strip).
     *
     * <p>The gradient is intentionally token-backed so clients resolve it
     * against their active light/dark theme. It blends from a raised card
     * surface into the promo tint, which gives dark-mode cards a visible
     * edge against the black canvas without competing with SubscribeHero /
     * SubscribeBanner's strong brand gradient.
     */
    public ObjectNode gamePanelSurface() {
        ObjectNode surface = defaultSurface();

        ObjectNode gradient = objectMapper.createObjectNode();
        ArrayNode colors = objectMapper.createArrayNode();
        colors.add(ColorTokens.SURFACE_RAISED);
        colors.add(ColorTokens.SURFACE_PROMO);
        gradient.set("colors", colors);
        gradient.put("direction", "diagonal");
        surface.set("background", gradient);

        return surface;
    }
}
