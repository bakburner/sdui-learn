package com.nba.sdui.domain.composer;

import com.nba.sdui.models.generated.AdSlot;
import com.nba.sdui.models.generated.AtomicElement;
import com.nba.sdui.models.generated.Placeholder;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionSurface;
import com.nba.sdui.models.generated.Spacing;
import com.nba.sdui.models.generated.Targeting;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.DemoImageUrls;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.tokens.Tokens;

/**
 * Composes the "Home" SDUI screen — an NBA.com-style homepage built entirely
 * from AtomicComposite sections. Demonstrates that a full content-rich page
 * can be server-composed with zero client-side renderers.
 *
 * <pre>
 *   1.  HeroPanel              – featured video hero (16:9 image + play overlay + title)
 *   2.  SectionHeader          – "STORIES"
 *   3.  ContentRail            – story cards with NEW badges
 *   4.  SectionHeader          – "HEADLINES" + "See More"
 *   5.  AtomicComposite        – vertical headline text list with dividers
 *   6.  SectionHeader          – "TRENDING NOW"
 *   7.  VideoCarousel          – trending video cards
 *   8.  SectionHeader          – "2026 POSTSEASON"
 *   9.  EditorialOverlayRail   – postseason image cards with title overlay
 *  10.  SectionHeader          – "2025-26 GAME RECAPS" + "See More"
 *  11.  VideoCarousel          – game recap video cards
 *  12.  SectionHeader          – "AROUND THE NBA"
 *  13.  AtomicComposite        – vertical article card list (thumbnail + text)
 * </pre>
 */
@Component
public class HomeComposer {

    private static final Logger log = LoggerFactory.getLogger(HomeComposer.class);

    private static final String FALLBACK_HERO = DemoImageUrls.hero("game");
    private static final String FALLBACK_STORY = DemoImageUrls.avatar("story");
    private static final String FALLBACK_THUMB = DemoImageUrls.cardWide("nba");
    private static final String FALLBACK_ARTICLE = DemoImageUrls.thumb("article");

    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public HomeComposer(SduiUtils utils, SectionSurfaces surfaces, Tokens tokens) {
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(tokens);
    }

    public Screen composeHome(String traceId, String locale) {
        log.info("Composing Home screen, locale={}", locale);

        Screen response = new Screen();
        response.setId("home");
        response.setAnalyticsId("home");
        response.setSchemaVersion(schemaVersion);
        utils.applyTabDestinationNavigation(response, "home");

        List<Section> sections = new ArrayList<>();

        // ── Row 1: Hero (2/3 width) + Headlines (1/3 width) ──────────
        sections.add(buildHeroAndHeadlinesRow());

        // ── Row 2: Stories (2/3 width) + Ad Slot (1/3 width) ─────────
        sections.add(buildStoriesAndAdRow());

        // 6-7. Trending Now
        sections.add(buildSectionHeader("trending-header", "TRENDING NOW", null, null));
        sections.add(buildTrendingVideoRail());

        // 8-9. 2026 Postseason
        sections.add(buildSectionHeader("postseason-header", "2026 POSTSEASON", null, null));
        sections.add(buildPostseasonRail());

        // 10-11. Game Recaps
        sections.add(buildSectionHeader("recaps-header", "2025-26 GAME RECAPS", "See More", "nba://recaps"));
        sections.add(buildRecapsVideoRail());

        // 12-13. Around the NBA
        sections.add(buildSectionHeader("around-header", "AROUND THE NBA", null, null));
        sections.add(buildAroundTheNbaList());

        response.setSections(sections);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    /**
     * Row 1: Two-column layout — Hero carousel (left, 2/3) + Headlines (right, 1/3).
     * Collapses to single column on narrow viewports (< 768px).
     */
    private Section buildHeroAndHeadlinesRow() {
        // Hero carousel content (embedded as SectionSlot)
        String heroContentSourceId = "cms:home-hero";
        String[][] heroSlides = {
                {"hero-1", FALLBACK_HERO, "VIDEO",
                        "Playoffs First Round: Celtics vs. Heat Game 3",
                        "Jayson Tatum drops 38 as Boston takes a commanding 2-1 series lead",
                        "WATCH", "nba://video/playoffs-bos-mia-g3"},
                {"hero-2", DemoImageUrls.hero("dunk"), null,
                        "Wembanyama's Historic Rookie Season",
                        "A look back at the records broken this year",
                        "READ", "nba://article/wemby-season"},
                {"hero-3", DemoImageUrls.hero("arena"), "LIVE",
                        "Nuggets vs. Timberwolves Game 4",
                        "Western Conference Semifinals",
                        "WATCH", "nba://video/den-min-g4"}
        };
        String heroSectionId = SectionIdDeriver.derive(heroContentSourceId, "AtomicComposite");
        Section heroSection = atomicBuilder.buildCinematicHeroCarousel(
                heroSectionId, "home_hero", heroSlides);
        heroSection.setContentSourceId(heroContentSourceId);
        heroSection.setSurface(surfaces.flushSurface());

        // Headlines content (embedded as SectionSlot)
        Section headlinesSection = buildHeadlinesList();

        // Compose as responsive row
        String rowContentSourceId = "feed:home";
        String rowSectionId = SectionIdDeriver.derive(rowContentSourceId, "AtomicComposite", "row1");
        AtomicElement row = atomicBuilder.responsiveRow(tokens.spacing("lg"), 768);
        row.setId(rowSectionId);
        List<AtomicElement> children = new ArrayList<>();

        AtomicElement leftSlot = atomicBuilder.sectionSlot("row1-hero", heroSection);
        atomicBuilder.setFlex(leftSlot, 2);
        children.add(leftSlot);

        AtomicElement rightSlot = atomicBuilder.sectionSlot("row1-headlines", headlinesSection);
        atomicBuilder.setFlex(rightSlot, 1);
        children.add(rightSlot);

        row.setChildren(children);
        Section section = atomicBuilder.wrapAsComposite(rowSectionId, "home_row1", row);
        section.setContentSourceId(rowContentSourceId);
        SectionSurface surface = surfaces.flushSurface();
        Spacing margin = new Spacing();
        margin.setTop(tokens.spacing("lg"));
        surface.setMargin(margin);
        section.setSurface(surface);
        return section;
    }

    /**
     * Row 2: Two-column layout — Stories rail (left, 2/3) + Ad slot (right, 1/3).
     * Collapses to single column on narrow viewports (< 768px).
     */
    private Section buildStoriesAndAdRow() {
        // Stories rail (embedded as SectionSlot)
        Section storiesSection = buildStoriesRail();

        // Ad slot (embedded as SectionSlot)
        Section adSection = buildAdSlot("home/sidebar");

        // Compose as responsive row
        String rowContentSourceId = "feed:home";
        String rowSectionId = SectionIdDeriver.derive(rowContentSourceId, "AtomicComposite", "row2");
        AtomicElement row = atomicBuilder.responsiveRow(tokens.spacing("lg"), 768);
        row.setId(rowSectionId);
        List<AtomicElement> children = new ArrayList<>();

        AtomicElement leftSlot = atomicBuilder.sectionSlot("row2-stories", storiesSection);
        atomicBuilder.setFlex(leftSlot, 2);
        children.add(leftSlot);

        AtomicElement rightSlot = atomicBuilder.sectionSlot("row2-ad", adSection);
        atomicBuilder.setFlex(rightSlot, 1);
        children.add(rightSlot);

        row.setChildren(children);
        Section section = atomicBuilder.wrapAsComposite(rowSectionId, "home_row2", row);
        section.setContentSourceId(rowContentSourceId);
        section.setSurface(surfaces.flushSurface());
        return section;
    }

    private Section buildSectionHeader(String slug, String title,
                                           String actionLabel, String actionUri) {
        String contentSourceId = "feed:home";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug);
        Section header = atomicBuilder.buildSectionHeader(sectionId, title, null, actionLabel, actionUri);
        header.setContentSourceId(contentSourceId);
        header.setSurface(surfaces.sectionHeaderSurface());
        return header;
    }

    private Section buildStoriesRail() {
        String contentSourceId = "cms:home-stories";
        // cards: [id, title, imageUrl, badgeText, targetUri]
        String[][] cards = {
                {"story-1", "Celtics Advance",
                        DemoImageUrls.cardTall("celtics"),
                        null, "nba://article/celtics-advance"},
                {"story-2", "Jokic Triple-Double",
                        DemoImageUrls.cardTall("nuggets"),
                        "NEW", "nba://article/jokic-triple"},
                {"story-3", "Rookie Sensation",
                        DemoImageUrls.cardTall("spurs"),
                        "NEW", "nba://article/wemby-record"},
                {"story-4", "Trade Rumors",
                        DemoImageUrls.cardTall("trade"),
                        null, "nba://article/trade-rumors"},
                {"story-5", "Draft Preview",
                        DemoImageUrls.cardTall("draft"),
                        null, "nba://article/draft-preview"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section rail = atomicBuilder.buildOverlayStoryRail(
                sectionId, "home_stories", "STORIES", cards);
        rail.setContentSourceId(contentSourceId);
        rail.setSurface(surfaces.railSurface());
        return rail;
    }

    /**
     * Vertical list of headline text rows separated by dividers.
     * Includes its own "HEADLINES" header since it's embedded in a SectionSlot.
     * Each row is tappable, navigating to the article.
     */
    private Section buildHeadlinesList() {
        String contentSourceId = "cms:home-headlines";
        String[][] headlines = {
                {"Celtics' Tatum named Eastern Conference Player of the Week", "nba://article/tatum-potw"},
                {"NBA announces schedule changes for Conference Finals", "nba://article/schedule-changes"},
                {"Injury report: Durant listed as questionable for Game 4", "nba://article/durant-injury"},
                {"All-NBA Team voting deadline approaches", "nba://article/all-nba-voting"},
                {"G League Finals set: Ignite vs. Blue Coats", "nba://article/gleague-finals"}
        };

        AtomicElement root = atomicBuilder.container("column", null, null);
        atomicBuilder.widthMode(root, "fill");
        root.setPadding(atomicBuilder.padding(
                tokens.spacing("lg"),
                tokens.spacing("lg"),
                8, // §3.6: no semantic spacing token for 8
                8  // §3.6: no semantic spacing token for 8
        ));
        List<AtomicElement> children = new ArrayList<>();

        // Inline header
        children.add(atomicBuilder.text("HEADLINES", "titleMedium", "bold", tokens.color("nba.label.primary"), null));
        children.add(atomicBuilder.spacer(tokens.spacing("md")));

        for (int i = 0; i < headlines.length; i++) {
            String headline = headlines[i][0];
            String uri = headlines[i][1];

            AtomicElement row = atomicBuilder.container("row", "start", "center");
            atomicBuilder.widthMode(row, "fill");
            row.setPadding(atomicBuilder.padding(
                    0, // §3.6: no semantic value for zero
                    0, // §3.6: no semantic value for zero
                    tokens.spacing("md"),
                    tokens.spacing("md")
            ));
            row.setActions(atomicBuilder.singleActionArray(atomicBuilder.tapNavigate(uri)));
            List<AtomicElement> rowChildren = new ArrayList<>();
            rowChildren.add(atomicBuilder.text(headline, "bodyMedium", "medium", tokens.color("nba.label.primary"), 2));
            row.setChildren(rowChildren);
            children.add(row);

            if (i < headlines.length - 1) {
                children.add(divider());
            }
        }

        root.setChildren(children);
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.wrapAsComposite(sectionId, "home_headlines", root);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildTrendingVideoRail() {
        String contentSourceId = "cms:home-trending";
        String[][] videos = {
                {"trend-1", "Top 10 Plays of the Night", "Last night's best moments",
                        FALLBACK_THUMB, "3:45", null, "nba://video/top10-tonight"},
                {"trend-2", "Playoff Buzzer Beaters", "The most clutch shots",
                        FALLBACK_THUMB, "5:12", null, "nba://video/buzzer-beaters"},
                {"trend-3", "Dunk Contest Preview", "Rising stars show off",
                        FALLBACK_THUMB, "2:30", "NEW", "nba://video/dunk-preview"},
                {"trend-4", "Coach Interviews", "Post-game reactions",
                        FALLBACK_THUMB, "8:15", null, "nba://video/coach-interviews"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section rail = atomicBuilder.buildVideoCarousel(
                sectionId, "home_trending", null, null, videos);
        rail.setContentSourceId(contentSourceId);
        rail.setSurface(surfaces.railSurface());
        return rail;
    }

    private Section buildPostseasonRail() {
        String contentSourceId = "cms:home-postseason";
        // cards: [id, title, subtitle, imageUrl, badgeText, targetUri]
        String[][] cards = {
                {"post-1", "Eastern Conference Finals", null, FALLBACK_THUMB,
                        null, "nba://playoffs/east-finals"},
                {"post-2", "Western Conference Finals", null, FALLBACK_THUMB,
                        null, "nba://playoffs/west-finals"},
                {"post-3", "First Round Highlights", null, FALLBACK_THUMB,
                        null, "nba://playoffs/first-round"},
                {"post-4", "Playoff Bracket", null, FALLBACK_THUMB,
                        null, "nba://playoffs/bracket"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section rail = atomicBuilder.buildEditorialOverlayRail(
                sectionId, "home_postseason", null, cards);
        rail.setContentSourceId(contentSourceId);
        rail.setSurface(surfaces.railSurface());
        return rail;
    }

    private Section buildRecapsVideoRail() {
        String contentSourceId = "cms:home-recaps";
        String[][] videos = {
                {"recap-1", "BOS 112 - MIA 98", "Celtics take Game 3",
                        FALLBACK_THUMB, "2:48:00", null, "nba://video/recap-bos-mia-g3"},
                {"recap-2", "DEN 105 - MIN 101", "Jokic leads Nuggets",
                        FALLBACK_THUMB, "2:35:00", null, "nba://video/recap-den-min"},
                {"recap-3", "NYK 118 - PHI 109", "Knicks even series",
                        FALLBACK_THUMB, "2:22:00", null, "nba://video/recap-nyk-phi"},
                {"recap-4", "OKC 99 - DAL 95", "Thunder take 3-1 lead",
                        FALLBACK_THUMB, "2:15:00", null, "nba://video/recap-okc-dal"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section rail = atomicBuilder.buildVideoCarousel(
                sectionId, "home_recaps", null, null, videos);
        rail.setContentSourceId(contentSourceId);
        rail.setSurface(surfaces.railSurface());
        return rail;
    }

    /**
     * Vertical list of article cards: thumbnail on the left, headline + 
     * description + timestamp on the right.
     */
    private Section buildAroundTheNbaList() {
        String[][] articles = {
                {"around-1", "Lakers eyeing major free agent signing this summer",
                        "Multiple sources confirm Los Angeles is preparing a max-level offer",
                        "2h ago", FALLBACK_ARTICLE, "nba://article/lakers-free-agent"},
                {"around-2", "Warriors' young core impresses in summer league",
                        "Jonathan Kuminga and Moses Moody show growth",
                        "4h ago", FALLBACK_ARTICLE, "nba://article/warriors-young-core"},
                {"around-3", "Commissioner announces expansion timeline",
                        "Las Vegas and Seattle remain frontrunners for new franchises",
                        "6h ago", FALLBACK_ARTICLE, "nba://article/expansion-timeline"},
                {"around-4", "Vintage jerseys making a comeback",
                        "Teams across the league bring back classic looks for next season",
                        "8h ago", FALLBACK_ARTICLE, "nba://article/vintage-jerseys"}
        };

        AtomicElement root = atomicBuilder.container("column", null, null);
        atomicBuilder.widthMode(root, "fill");
        root.setPadding(atomicBuilder.padding(
                tokens.spacing("lg"),
                tokens.spacing("lg"),
                8, // §3.6: no semantic spacing token for 8
                8  // §3.6: no semantic spacing token for 8
        ));
        List<AtomicElement> children = new ArrayList<>();

        for (int i = 0; i < articles.length; i++) {
            String id = articles[i][0];
            String headline = articles[i][1];
            String description = articles[i][2];
            String timestamp = articles[i][3];
            String imageUrl = articles[i][4];
            String targetUri = articles[i][5];

            AtomicElement row = atomicBuilder.container("row", "start", "start");
            row.setId(id);
            atomicBuilder.widthMode(row, "fill");
            row.setPadding(atomicBuilder.padding(
                    0, // §3.6: no semantic value for zero
                    0, // §3.6: no semantic value for zero
                    tokens.spacing("md"),
                    tokens.spacing("md")
            ));
            row.setActions(atomicBuilder.singleActionArray(atomicBuilder.tapNavigate(targetUri)));

            List<AtomicElement> rowChildren = new ArrayList<>();

            // Thumbnail
            AtomicElement img = atomicBuilder.image(imageUrl, 80, 80, "cover");
            img.setCornerRadius(8); // §3.6: no semantic token mapping for corner radius 8
            rowChildren.add(img);

            // Text column
            AtomicElement textCol = atomicBuilder.container("column", null, null);
            textCol.setPadding(atomicBuilder.padding(
                    tokens.spacing("md"),
                    0, // §3.6: no semantic value for zero
                    0, // §3.6: no semantic value for zero
                    0  // §3.6: no semantic value for zero
            ));
            textCol.setFlex(1.0);
            List<AtomicElement> textChildren = new ArrayList<>();
            textChildren.add(atomicBuilder.text(headline, "bodyMedium", "semiBold", tokens.color("nba.label.primary"), 2));
            textChildren.add(atomicBuilder.text(description, "bodySmall", null, tokens.color("nba.label.secondary"), 2));
            textChildren.add(atomicBuilder.text(timestamp, "labelSmall", null, tokens.color("nba.label.tertiary"), null));
            textCol.setChildren(textChildren);
            rowChildren.add(textCol);

            row.setChildren(rowChildren);
            children.add(row);

            if (i < articles.length - 1) {
                children.add(divider());
            }
        }

        root.setChildren(children);
        String contentSourceId = "cms:home-around";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.wrapAsComposite(sectionId, "home_around", root);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    /** AdSlot section — ad placement primitive (ADR-007). */
    private Section buildAdSlot(String adUnitPath) {
        String contentSourceId = "ads:gam-" + adUnitPath.replace("/", "_");
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");

        Section section = new Section();
        section.setId(sectionId);
        section.setContentSourceId(contentSourceId);
        section.setType(Section.Type.AD_SLOT);
        section.setAnalyticsId(sectionId.replaceAll("[~:=]", "_"));
        section.setSurface(surfaces.adSlotSurface());

        AdSlot data = new AdSlot();
        data.setProvider("gam");
        data.setAdUnitPath(adUnitPath);
        data.setSizes(List.of(List.of(300, 250), List.of(300, 600)));

        Targeting targeting = new Targeting();
        targeting.setAdditionalProperty("section", "home");
        targeting.setAdditionalProperty("position", "sidebar");
        data.setTargeting(targeting);

        data.setCollapseOnEmpty(true);
        data.setLabel("Advertisement");

        Placeholder placeholder = new Placeholder();
        placeholder.setBackgroundColor(tokens.color("nba.bg.tertiary"));
        placeholder.setText("Advertisement");
        data.setPlaceholder(placeholder);

        section.setData(data);
        return section;
    }

    private AtomicElement divider() {
        return new AtomicElement().withType(AtomicElement.Type.fromValue("Divider"));
    }
}
