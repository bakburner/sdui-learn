package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.models.generated.AccessibilityProperties;
import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.AdSlot;
import com.nba.sdui.models.generated.AtomicComposite;
import com.nba.sdui.models.generated.AtomicElement;
import com.nba.sdui.models.generated.Placeholder;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.Spacing;
import com.nba.sdui.models.generated.SubscribeUpsell;
import com.nba.sdui.models.generated.SubscriptionTier;
import com.nba.sdui.models.generated.Targeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.nba.sdui.domain.AccessibilityHelper.*;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;

/**
 * Composes the "Watch" SDUI screen — video / streaming hub organised into tabs.
 *
 * Layout:
 *   TabGroup with 3 tabs: Featured, NBA TV, League Pass
 *   Each tab contains a mix of ContentRails, GamePanels and PromoBanners.
 */
@Component
public class WatchComposer {

    private static final Logger log = LoggerFactory.getLogger(WatchComposer.class);
    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final ScoreboardPort scoreboardPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public WatchComposer(ObjectMapper objectMapper,
                         ScoreboardPort scoreboardPort,
                         SduiUtils utils,
                         SectionSurfaces surfaces,
                         Tokens tokens) {
        this.objectMapper = objectMapper;
        this.scoreboardPort = scoreboardPort;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
    }

    public Screen composeWatch(String traceId, String locale) {
        log.info("Composing Watch screen, locale={}", locale);

        Screen response = new Screen();
        response.setId("watch");
        response.setAnalyticsId("watch");
        response.setSchemaVersion(schemaVersion);
        utils.applyTabDestinationNavigation(response, "watch");

        List<Section> sections = new ArrayList<>();

        // Single TabGroup section that contains the three tabs
        sections.add(objectMapper.convertValue(buildTabGroup(), Section.class));

        response.setSections(sections);

        // Subnav is edge-to-edge under top navigation — no horizontal screen inset.
        Spacing insets = new Spacing();
        insets.setBottom(tokens.spacing("lg"));
        response.setContentInsets(insets);

        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Tab group ──────────────────────────────────────────────────────

    private ObjectNode buildTabGroup() {
        String contentSourceId = "feed:watch";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "TabGroup");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "TabGroup");
        section.put("analyticsId", "watch_tabs");
        section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "watch_active_tab");
        data.put("defaultTab", "featured");

        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add(buildTab("watch-featured", "Featured", "watch_active_tab", "featured"));
        tabs.add(buildTab("watch-nbatv", "NBA TV", "watch_active_tab", "nbatv"));
        tabs.add(buildTab("watch-leaguepass", "League Pass", "watch_active_tab", "leaguepass"));
        data.set("tabs", tabs);

        ObjectNode tabContents = objectMapper.createObjectNode();
        tabContents.set("featured", buildFeaturedSections());
        tabContents.set("nbatv", buildNbaTvSections());
        tabContents.set("leaguepass", buildLeaguePassSections());
        data.set("tabContents", tabContents);

        section.set("data", data);
        section.set("subsections", utils.tabSelectSubsections(tabs, "watch_active_tab"));
        section.set("surface", objectMapper.valueToTree(surfaces.stripSurfaceWithoutBackground()));

        return section;
    }

    private ObjectNode buildTab(String id, String label, String stateKey, String stateValue) {
        ObjectNode tab = objectMapper.createObjectNode();
        tab.put("id", id);
        tab.put("label", label);
        tab.put("stateKey", stateKey);
        tab.put("stateValue", stateValue);
        return tab;
    }

    // ── Featured tab sections ─────────────────────────────────────────

    private ArrayNode buildFeaturedSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // Ad slot at top
        sections.add(objectMapper.valueToTree(buildAdSlot("watch/featured/top")));

        addVideoCarousel(sections, "cms:watch-highlights", "Tonight's Highlights", null,
                new String[][]{
                        {"fh-1", "Game Recap: BOS vs LAL", "Full game highlights",
                                FALLBACK_THUMB,
                                "9:45", null, "nba://video/bos-lal-recap"},
                        {"fh-2", "Clutch Moments", "The best clutch plays tonight",
                                FALLBACK_THUMB,
                                "4:12", null, "nba://video/clutch-moments"},
                        {"fh-3", "Rookie Watch", "Top rookie performances",
                                FALLBACK_THUMB,
                                "3:20", "NEW", "nba://video/rookie-watch"},
                        {"fh-4", "Top 10 Plays", "Last night's best moments",
                                FALLBACK_THUMB,
                                "2:45", null, "nba://video/top10-plays"}
                });

        // Inline subscription upsell
        sections.add(objectMapper.valueToTree(buildSubscribeUpsellBanner(
                "NBA League Pass",
                "Watch every out-of-market game live or on demand",
                FALLBACK_THUMB,
                "Subscribe Now", "nba://subscribe/league-pass")));

        addVideoCarousel(sections, "cms:watch-originals", "NBA Originals", null,
                new String[][]{
                        {"fo-1", "Open Court", "Inside the NBA culture",
                                FALLBACK_THUMB,
                                "22:00", null, "nba://video/open-court"},
                        {"fo-2", "NBA Countdown", "Pre-game analysis",
                                FALLBACK_THUMB,
                                "15:30", null, "nba://video/countdown"},
                        {"fo-3", "The Reel", "Behind-the-scenes with players",
                                FALLBACK_THUMB,
                                "18:45", null, "nba://video/the-reel"}
                });

        // Ad slot mid-page
        sections.add(objectMapper.valueToTree(buildAdSlot("watch/featured/mid")));

        addContentRail(sections, "cms:watch-stories", "Trending Stories",
                new String[][]{
                        {"fs-1", "Trade Deadline Preview", "Who's on the move?",
                                FALLBACK_THUMB,
                                "article", null, "nba://article/trade-deadline"},
                        {"fs-2", "All-Star Voting Update", "Current vote leaders",
                                FALLBACK_THUMB,
                                "article", null, "nba://article/allstar-voting"},
                        {"fs-3", "Playoff Picture", "Updated standings breakdown",
                                FALLBACK_THUMB,
                                "article", null, "nba://article/playoff-picture"}
                });

        return sections;
    }

    // ── NBA TV tab sections ────────────────────────────────────────────

    private ArrayNode buildNbaTvSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // NBA TV Schedule (hero + time-slot list)
        sections.add(objectMapper.valueToTree(buildNbaTvSchedule()));

        addVideoCarousel(sections, "cms:nbatv-shows", "Popular Shows", null,
                new String[][]{
                        {"nbatv-s1", "NBA GameTime", "Nightly highlights & analysis",
                                FALLBACK_THUMB,
                                "60:00", null, "nba://watch/gametime"},
                        {"nbatv-s2", "NBA Inside Stuff", "Player features and interviews",
                                FALLBACK_THUMB,
                                "30:00", null, "nba://watch/inside-stuff"},
                        {"nbatv-s3", "NBA Action", "Weekly highlights show",
                                FALLBACK_THUMB,
                                "30:00", null, "nba://watch/nba-action"}
                });

        sections.add(objectMapper.valueToTree(buildAdSlot("watch/nbatv/mid")));

        addVideoCarousel(sections, "cms:nbatv-classics", "Classic Games", "Relive the greatest moments",
                new String[][]{
                        {"nbatv-c1", "2016 Finals Game 7", "CLE vs GSW — The Block, The Shot, The Stop",
                                FALLBACK_THUMB,
                                "2:48:00", null, "nba://video/2016-finals-g7"},
                        {"nbatv-c2", "Kobe's 81 Points", "Jan 22, 2006 vs Toronto Raptors",
                                FALLBACK_THUMB,
                                "2:15:00", null, "nba://video/kobe-81"},
                        {"nbatv-c3", "2013 Finals Game 6", "MIA vs SAS — Ray Allen's clutch three",
                                FALLBACK_THUMB,
                                "2:35:00", null, "nba://video/2013-finals-g6"}
                });

        return sections;
    }

    // ── League Pass tab sections ───────────────────────────────────────

    private ArrayNode buildLeaguePassSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // Full-screen subscription upsell with pricing tiers
        sections.add(objectMapper.valueToTree(buildSubscribeUpsellHero()));

        sections.add(objectMapper.valueToTree(buildSectionHeader("live-games", "Live Games", null, null)));

        // Pull real games if available
        addLiveGamePanels(sections);

        addVideoCarousel(sections, "cms:lp-condensed", "Condensed Games", "Full games in ~12 minutes",
                new String[][]{
                        {"lp-c1", "BOS vs MIA Condensed", "Full condensed game",
                                FALLBACK_THUMB,
                                "12:34", null, "nba://video/bos-mia-condensed"},
                        {"lp-c2", "DEN vs PHX Condensed", "Full condensed game",
                                FALLBACK_THUMB,
                                "11:58", null, "nba://video/den-phx-condensed"},
                        {"lp-c3", "LAL vs GSW Condensed", "Full condensed game",
                                FALLBACK_THUMB,
                                "12:10", null, "nba://video/lal-gsw-condensed"}
                });

        // Ad slot
        sections.add(objectMapper.valueToTree(buildAdSlot("watch/leaguepass/mid")));

        return sections;
    }

    private void addLiveGamePanels(ArrayNode sections) {
        try {
            ScoreboardResponse scoreboard = scoreboardPort.getScoreboard();
            if (scoreboard != null) {
                int count = 0;
                for (Game g : scoreboard.getGames()) {
                    if (count >= 4) break;
                    sections.add(objectMapper.valueToTree(buildGamePanel(g)));
                    count++;
                }
                if (count > 0) return;
            }
        } catch (Exception e) {
            log.warn("Failed to build live game cards: {}", e.getMessage());
        }

        // Mock fallback
        sections.add(objectMapper.valueToTree(mockGamePanel("lp-g1", "GSW", 1610612744, "LAC", 1610612746, "Q2 8:30", 2)));
        sections.add(objectMapper.valueToTree(mockGamePanel("lp-g2", "MIA", 1610612748, "NYK", 1610612752, "8:00 PM ET", 1)));
    }

    // ── Reusable section builders ──────────────────────────────────────

    private Section buildSectionHeader(String slug, String title,
                                           String actionLabel, String actionUri) {
        String contentSourceId = "feed:watch";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug + "-header");
        Section section = atomicBuilder.buildSectionHeader(sectionId, title, null, actionLabel, actionUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.sectionHeaderSurface());
        return section;
    }

    private Section buildSectionHeader(String slug, String title, String subtitle,
                                           String actionLabel, String actionUri) {
        String contentSourceId = "feed:watch";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug + "-header");
        Section section = atomicBuilder.buildSectionHeader(sectionId, title, subtitle, actionLabel, actionUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.sectionHeaderSurface());
        return section;
    }

    /**
     * Emit a titled content rail as TWO sections — a SectionHeader followed
     * by a title-less rail. The SectionHeader's surface
     * ({@link SduiUtils#sectionHeaderSurface()}) is the single source of
     * truth for the title→rail gap app-wide, so every screen has the same
     * header rhythm. Callers that pass a null title get just the rail.
     */
    private void addContentRail(ArrayNode sections, String contentSourceId, String title, String[][] cards) {
        if (title != null) {
            sections.add(objectMapper.valueToTree(buildSectionHeader(contentSourceId, title, null, null)));
        }
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildContentRail(sectionId, null, null, cards);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        sections.add(objectMapper.valueToTree(section));
    }

    /**
     * Emit a titled video carousel as TWO sections — a SectionHeader
     * (carrying both title and subtitle) followed by a title-less carousel.
     * Same rationale as {@link #addContentRail}: one header surface, one
     * consistent app-wide rhythm.
     */
    private void addVideoCarousel(ArrayNode sections, String contentSourceId, String title,
                                   String subtitle, String[][] items) {
        if (title != null) {
            sections.add(objectMapper.valueToTree(buildSectionHeader(contentSourceId, title, subtitle, null, null)));
        }
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildVideoCarousel(sectionId, null, null, null, items);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        sections.add(objectMapper.valueToTree(section));
    }

    private Section buildPromoBanner(String contentSourceId, String headline, String subhead,
                                         String backgroundUrl, String targetUri) {
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildPromoBanner(sectionId, null, null, headline, subhead,
                null, "Learn More", targetUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.subscribeSurface(
                "#0C1B3A",
                tokens.color("nba.label.accent.brand"),
                20));
        return section;
    }

    private Section buildGamePanel(Game game) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        String contentSourceId = "stats-api:game-" + gameId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        int gameStatus = game.getGameStatus();
        Section section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                null,
                "standard",
                gameId,
                gameStatus,
                game.getGameStatusText() != null ? game.getGameStatusText() : "",
                null,
                atomicBuilder.gamePanelTeam(game.getAwayTeam()),
                atomicBuilder.gamePanelTeam(game.getHomeTeam()),
                null,
                "nba://game/" + gameId,
                staticRefreshPolicy(),
                null,
                surfaces.gamePanelSurface());
        section.setContentSourceId(contentSourceId);
        return section;
    }

    private Section mockGamePanel(String mockId, String awayTri, int awayTeamId,
                                     String homeTri, int homeTeamId,
                                     String statusText, int gameStatus) {
        String contentSourceId = "stats-api:game-" + mockId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        int awayScore = gameStatus == 2 ? 55 : 0;
        int homeScore = gameStatus == 2 ? 48 : 0;
        Section section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                null,
                "standard",
                mockId,
                gameStatus,
                statusText,
                null,
                new AtomicCompositeBuilder.GamePanelTeam(awayTri, awayScore, SduiUtils.teamLogoUrl(awayTeamId)),
                new AtomicCompositeBuilder.GamePanelTeam(homeTri, homeScore, SduiUtils.teamLogoUrl(homeTeamId)),
                null,
                "nba://game/" + mockId,
                staticRefreshPolicy(),
                null,
                surfaces.gamePanelSurface());
        section.setContentSourceId(contentSourceId);
        return section;
    }

    // ── New section type builders ──────────────────────────────────────

    /** NbaTvSchedule section — hero promo + time-slot list. */
    private Section buildNbaTvSchedule() {
        String contentSourceId = "cms:nbatv-schedule";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] slots = {
                {"slot-1", "NBA GameTime", "Highlights & analysis", "19:00", "true", "nba://watch/nbatv-live"},
                {"slot-2", "NBA Inside Stuff", "Player features", "20:00", "false", "nba://watch/inside-stuff"},
                {"slot-3", "CLE vs NYK", "Eastern Conference matchup", "20:30", "false", "nba://game/0022400612"},
                {"slot-4", "NBA Action", "Weekly highlights show", "23:00", "false", "nba://watch/nba-action"}
        };
        Section section = atomicBuilder.buildNbaTvSchedule(
                sectionId, null,
                FALLBACK_THUMB,
                "NBA GameTime",
                "LIVE — Nightly highlights & analysis",
                true, slots);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.cardSurface());
        return section;
    }

    /**
     * SubscribeUpsell section, inline-banner layout.
     *
     * Reserved SDK integration point: the visible surface is expressed as an
     * atomic tree under {@code data.ui}. {@code data.ctaAction} is retained as
     * the pre-SDK fallback action; once the IAP SDK lands, it takes over the
     * CTA tap and reads the IAP product identifiers from a future
     * {@code data.tiers} list.
     */
    private Section buildSubscribeUpsellBanner(String title, String subtitle,
                                             String backgroundUrl, String ctaLabel,
                                             String ctaUri) {
        String contentSourceId = "product:league-pass-banner";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SubscribeUpsell");
        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.SUBSCRIBE_UPSELL);
        section.setContentSourceId(contentSourceId);
        section.setRefreshPolicy(staticRefreshPolicy());
        section.setSurface(surfaces.subscribeSurface(
                tokens.color("nba.label.accent.brand"),
                "#862633",
                20));

        Action ctaAction = new Action();
        ctaAction.setTrigger(Action.ActionTrigger.ON_ACTIVATE);
        ctaAction.setType(Action.ActionType.NAVIGATE);
        ctaAction.setTargetUri(ctaUri);

        AtomicElement root = atomicBuilder.container("column", "start", "start");
        atomicBuilder.widthMode(root, "fill");
        root.setGap(tokens.spacing("sm"));
        List<AtomicElement> children = new ArrayList<>();
        AtomicElement titleText = atomicBuilder.text(title, "titleMedium", "bold", "#FFFFFF", null);
        applyHeading(titleText, title, 2);
        children.add(titleText);
        if (subtitle != null) {
            children.add(atomicBuilder.text(subtitle, "bodySmall", null, "rgba(255,255,255,0.85)", null));
        }
        children.add(atomicBuilder.spacer(tokens.spacing("md")));
        children.add(atomicBuilder.button(ctaLabel, "primary", ctaAction));
        root.setChildren(children);

        SubscribeUpsell data = new SubscribeUpsell();
        data.setUi(root);
        data.setCtaAction(ctaAction);
        section.setData(data);
        return section;
    }

    /**
     * SubscribeUpsell section, full-screen-hero layout with pricing tiers.
     *
     * Reserved SDK integration point: the full visible surface (logo, title,
     * subtitle, feature list, tier cards) is expressed as an atomic tree
     * under {@code data.ui}. {@code data.tiers} is retained for the future
     * IAP SDK to bind product identifiers; the renderer reads nothing from
     * it today.
     */
    private Section buildSubscribeUpsellHero() {
        String contentSourceId = "product:league-pass";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SubscribeUpsell");
        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.SUBSCRIBE_UPSELL);
        section.setContentSourceId(contentSourceId);
        section.setRefreshPolicy(staticRefreshPolicy());
        section.setSurface(surfaces.subscribeSurface(
                "#0C1B3A",
                tokens.color("nba.label.accent.brand"),
                24));

        AtomicElement root = buildSubscribeUpsellHeroUi(
                "NBA League Pass",
                "Your courtside seat to every game",
                FALLBACK_THUMB,
                new String[]{
                        "Watch every out-of-market game live or on demand",
                        "Choose home or away broadcast feeds",
                        "NBA TV included with all plans",
                        "Condensed game replays — full game in ~12 minutes"
                },
                new TierSpec[]{
                        new TierSpec("League Pass", "$14.99/mo", "MOST POPULAR",
                                new String[]{"Live & on-demand out-of-market games", "NBA TV included"},
                                "Subscribe", "nba://subscribe/league-pass/standard"),
                        new TierSpec("League Pass Premium", "$22.99/mo", "BEST VALUE",
                                new String[]{"Everything in League Pass",
                                        "No in-game ads on select feeds",
                                        "Up to 4 simultaneous streams"},
                                "Subscribe", "nba://subscribe/league-pass/premium")
                });

        List<SubscriptionTier> tiers = new ArrayList<>();
        tiers.add(tierProductId("lp-standard", "League Pass", "$14.99/mo"));
        tiers.add(tierProductId("lp-premium", "League Pass Premium", "$22.99/mo"));

        SubscribeUpsell data = new SubscribeUpsell();
        data.setUi(root);
        data.setTiers(tiers);
        section.setData(data);
        return section;
    }

    /**
     * Build the atomic tree for a SubscribeUpsell hero layout. Extracted so
     * both the Watch composer and the demo composer can share the same
     * visual composition.
     */
    private AtomicElement buildSubscribeUpsellHeroUi(String title, String subtitle, String logoUrl,
                                             String[] features, TierSpec[] tierSpecs) {
        AtomicElement root = atomicBuilder.container("column", "start", "center");
        root.setGap(8); // §3.6: no semantic spacing token for 8
        atomicBuilder.widthMode(root, "fill");
        // Cap reading-width so the hero stays compact on wide viewports (web/tablet/TV);
        // alignSelf=center keeps it horizontally centered within the section surface.
        // No-op on phone, where the natural surface width is already <480.
        root.setMaxWidth(480);
        root.setAlignSelf(AtomicElement.CrossAlignment.CENTER);
        List<AtomicElement> children = new ArrayList<>();

        if (logoUrl != null) {
            AtomicElement logoImg = atomicBuilder.image(logoUrl, 0, 64, "contain");
            applyHidden(logoImg);
            children.add(logoImg);
            children.add(atomicBuilder.spacer(tokens.spacing("md")));
        }
        AtomicElement heroTitle = atomicBuilder.text(title, "headlineMedium", "bold", tokens.color("nba.label-dark.primary"), null);
        applyHeading(heroTitle, title, 2);
        children.add(heroTitle);
        if (subtitle != null) {
            children.add(atomicBuilder.text(subtitle, "bodyLarge", null, tokens.color("nba.label-dark.primary"), null));
        }
        children.add(atomicBuilder.spacer(tokens.spacing("lg")));

        AtomicElement featuresCol = atomicBuilder.container("column", "start", "start");
        featuresCol.setGap(8); // §3.6: no semantic spacing token for 8
        atomicBuilder.widthMode(featuresCol, "fill");
        List<AtomicElement> featureChildren = new ArrayList<>();
        for (String feature : features) {
            AtomicElement row = atomicBuilder.container("row", "start", "center");
            row.setGap(8); // §3.6: no semantic spacing token for 8
            atomicBuilder.widthMode(row, "fill");
            List<AtomicElement> rowChildren = new ArrayList<>();
            rowChildren.add(atomicBuilder.text("✓", "bodyLarge", "bold", "token:nba.color.feedback.success.70", null));
            AtomicElement featureText = atomicBuilder.text(feature, "bodyLarge", null, tokens.color("nba.label-dark.primary"), null);
            featureText.setFlex(1.0);
            rowChildren.add(featureText);
            row.setChildren(rowChildren);
            featureChildren.add(row);
        }
        featuresCol.setChildren(featureChildren);
        children.add(featuresCol);

        children.add(atomicBuilder.spacer(20));

        AtomicElement tiersCol = atomicBuilder.container("column", "start", "stretch");
        tiersCol.setGap(tokens.spacing("lg"));
        atomicBuilder.widthMode(tiersCol, "fill");
        List<AtomicElement> tierChildren = new ArrayList<>();
        for (TierSpec t : tierSpecs) {
            tierChildren.add(buildTierUi(t));
        }
        tiersCol.setChildren(tierChildren);
        children.add(tiersCol);

        root.setChildren(children);
        return root;
    }

    /** Build an atomic Container that visually represents one subscription tier. */
    private AtomicElement buildTierUi(TierSpec t) {
        AtomicElement card = atomicBuilder.container("column", "start", "start");
        card.setGap(6); // §3.6: no semantic spacing token for 6
        card.setBackground(tokens.color("nba.color.t-white.10"));
        card.setCornerRadius(tokens.radius("lg"));
        card.setPadding(atomicBuilder.padding(
                22, // §3.6: no semantic spacing token for 22
                22, // §3.6: no semantic spacing token for 22
                20, // §3.6: no semantic spacing token for 20
                20  // §3.6: no semantic spacing token for 20
        ));
        atomicBuilder.widthMode(card, "fill");

        List<AtomicElement> cardChildren = new ArrayList<>();
        if (t.badgeText != null) {
            cardChildren.add(atomicBuilder.text(t.badgeText, "labelMedium", "bold", tokens.color("nba.color.secondary.70"), null));
        }
        AtomicElement tierName = atomicBuilder.text(t.name, "titleLarge", "bold", tokens.color("nba.label-dark.primary"), null);
        applyHeading(tierName, t.name, 3);
        cardChildren.add(tierName);
        cardChildren.add(atomicBuilder.text(t.price, "headlineSmall", "bold", tokens.color("nba.label-dark.primary"), null));

        if (t.features != null) {
            for (String f : t.features) {
                cardChildren.add(atomicBuilder.text("• " + f, "bodyMedium", null, tokens.color("nba.label-dark.primary"), null));
            }
        }
        cardChildren.add(atomicBuilder.spacer(10));

        Action tierAction = new Action();
        tierAction.setTrigger(Action.ActionTrigger.ON_ACTIVATE);
        tierAction.setType(Action.ActionType.NAVIGATE);
        tierAction.setTargetUri(t.ctaUri);
        cardChildren.add(atomicBuilder.button(t.ctaLabel, "primary", tierAction));

        card.setChildren(cardChildren);
        return card;
    }

    private SubscriptionTier tierProductId(String id, String name, String price) {
        SubscriptionTier tier = new SubscriptionTier();
        tier.setId(id);
        tier.setName(name);
        tier.setPrice(price);
        return tier;
    }

    /** Compact value object describing one subscription tier for the hero UI builder. */
    private static final class TierSpec {
        final String name, price, badgeText;
        final String[] features;
        final String ctaLabel, ctaUri;

        TierSpec(String name, String price, String badgeText,
                 String[] features, String ctaLabel, String ctaUri) {
            this.name = name;
            this.price = price;
            this.badgeText = badgeText;
            this.features = features;
            this.ctaLabel = ctaLabel;
            this.ctaUri = ctaUri;
        }
    }

    /** AdSlot section — ad placement primitive (ADR-007). */
    private Section buildAdSlot(String adUnitPath) {
        String contentSourceId = "ads:gam-" + adUnitPath.replace("/", "-");
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");
        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.AD_SLOT);
        section.setContentSourceId(contentSourceId);
        section.setRefreshPolicy(staticRefreshPolicy());
        section.setSurface(surfaces.adSlotSurface());

        AdSlot data = new AdSlot();
        data.setProvider("gam");
        data.setAdUnitPath("/21765378015/nba.com/" + adUnitPath);

        List<List<Integer>> sizes = new ArrayList<>();
        sizes.add(List.of(320, 50));
        sizes.add(List.of(728, 90));
        data.setSizes(sizes);

        Targeting targeting = new Targeting();
        targeting.setAdditionalProperty("section", "watch");
        data.setTargeting(targeting);

        data.setCollapseOnEmpty(true);
        data.setLabel("Advertisement");

        Placeholder placeholder = new Placeholder();
        placeholder.setBackgroundColor("token:nba.bg.tertiary");
        placeholder.setText("Advertisement");
        data.setPlaceholder(placeholder);

        section.setData(data);
        return section;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }

    private RefreshPolicy staticRefreshPolicy() {
        RefreshPolicy rp = new RefreshPolicy();
        rp.setType(RefreshPolicy.RefreshType.STATIC);
        return rp;
    }

    private static void applyHeading(AtomicElement el, String label, int level) {
        if (el == null || label == null || label.isBlank()) return;
        AccessibilityProperties a = new AccessibilityProperties();
        a.setLabel(label);
        a.setRole(AccessibilityProperties.Role.HEADING);
        a.setHeadingLevel(Math.max(1, Math.min(6, level)));
        el.setAccessibility(a);
    }

    private static void applyHidden(AtomicElement el) {
        if (el == null) return;
        AccessibilityProperties a = new AccessibilityProperties();
        a.setHidden(true);
        el.setAccessibility(a);
    }
}
