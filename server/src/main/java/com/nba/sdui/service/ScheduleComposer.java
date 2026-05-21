package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Composes the Schedule screen — a date-filterable game list.
 *
 * Layout:
 *   1. Form (season picker + month picker)
 *   2. Game list (AtomicComposite per game day group)
 *
 * Uses mock data from schema/examples/schedule-2024-25.json.
 */
@Component
public class ScheduleComposer {

    private static final Logger log = LoggerFactory.getLogger(ScheduleComposer.class);

    private final ObjectMapper objectMapper;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ScheduleComposer(ObjectMapper objectMapper, SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    public JsonNode composeSchedule(String traceId, String locale) {
        log.info("Composing Schedule screen, locale={}", locale);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "schedule");
        response.put("analyticsId", "schedule");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        utils.applyTabDestinationNavigation(response, "schedule");

        // Initial chip selections. See Phase D.1 scope note in
        // docs/plans/ios-led-ux-rebuild.md — /sdui/schedule does not yet parse
        // query params, so these values are cosmetic; they drive chip
        // highlighting on first render only.
        ObjectNode state = objectMapper.createObjectNode();
        state.put("schedule_season", "2024-25");
        state.put("schedule_year", "2024");
        state.put("schedule_month", "");
        state.put("schedule_day", "");
        response.set("state", state);

        ArrayNode sections = objectMapper.createArrayNode();

        sections.add(buildFilterForm(locale));

        JsonNode mockData = loadScheduleData();
        if (mockData != null) {
            addGameSections(sections, mockData, locale);
        }

        response.set("sections", sections);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    private ObjectNode buildFilterForm(String locale) {
        String contentSourceId = "feed:schedule";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "Form", "filters");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "Form");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "schedule_filters");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("layout", "vertical");

        // Form submit fires on chip change via onChange actions on each
        // field; keep submitAction present for schema validity. The
        // controller doesn't parse query params yet — chip taps are
        // cosmetic until the data-wiring plan lands (Phase D.1 scope note).
        ObjectNode submitAction = objectMapper.createObjectNode();
        submitAction.put("trigger", "onSubmit");
        submitAction.put("type", "refresh");
        data.set("submitAction", submitAction);
        data.put("submitLabel", SduiUtils.getLocalizedString(locale, "filter.apply"));

        ArrayNode fields = objectMapper.createArrayNode();

        String all = SduiUtils.getLocalizedString(locale, "filter.all");

        fields.add(buildChipsField("schedule_season", SduiUtils.getLocalizedString(locale, "filter.season"),
                new String[][]{
                        {"2024-25", "2024-25"}, {"2023-24", "2023-24"},
                        {"2022-23", "2022-23"}, {"2021-22", "2021-22"}
                }));

        fields.add(buildChipsField("schedule_year", SduiUtils.getLocalizedString(locale, "filter.year"),
                new String[][]{
                        {"2024", "2024"}, {"2025", "2025"}
                }));

        fields.add(buildChipsField("schedule_month", SduiUtils.getLocalizedString(locale, "filter.month"),
                new String[][]{
                        {"", all},
                        {"10", SduiUtils.getLocalizedString(locale, "month.october")},
                        {"11", SduiUtils.getLocalizedString(locale, "month.november")},
                        {"12", SduiUtils.getLocalizedString(locale, "month.december")},
                        {"1", SduiUtils.getLocalizedString(locale, "month.january")},
                        {"2", SduiUtils.getLocalizedString(locale, "month.february")},
                        {"3", SduiUtils.getLocalizedString(locale, "month.march")},
                        {"4", SduiUtils.getLocalizedString(locale, "month.april")}
                }));

        fields.add(buildChipsField("schedule_day", SduiUtils.getLocalizedString(locale, "filter.day"),
                new String[][]{
                        {"", all}, {"1", "1"}, {"5", "5"}, {"10", "10"},
                        {"15", "15"}, {"20", "20"}, {"25", "25"}, {"30", "30"}
                }));

        data.set("fields", fields);
        section.set("data", data);
        return section;
    }

    /**
     * Emits a schema-compliant {@code FormField} with {@code fieldType: "select"}
     * and {@code variant: "chips"}. Clients resolve chips natively (iOS
     * capsules, Material {@code FilterChip}, web pill buttons).
     */
    private ObjectNode buildChipsField(String stateKey, String label, String[][] options) {
        ObjectNode field = objectMapper.createObjectNode();
        field.put("fieldId", stateKey);
        field.put("fieldType", "select");
        field.put("variant", "chips");
        field.put("label", label);
        field.put("stateKey", stateKey);

        ArrayNode opts = objectMapper.createArrayNode();
        for (String[] opt : options) {
            ObjectNode o = objectMapper.createObjectNode();
            o.put("value", opt[0]);
            o.put("label", opt[1]);
            opts.add(o);
        }
        field.set("options", opts);
        return field;
    }

    private void addGameSections(ArrayNode sections, JsonNode mockData, String locale) {
        JsonNode games = mockData.path("games");
        if (!games.isArray()) return;

        String currentDate = "";
        for (JsonNode game : games) {
            String date = game.path("date").asText("");
            if (!date.equals(currentDate)) {
                currentDate = date;
                String headerContentSourceId = "feed:schedule";
                String headerSectionId = SectionIdDeriver.derive(headerContentSourceId, "AtomicComposite", "header-" + date);
                ObjectNode header = atomicBuilder.buildSectionHeader(
                        headerSectionId, formatDate(date, locale), null, null, null);
                header.put("contentSourceId", headerContentSourceId);
                header.set("surface", utils.sectionHeaderSurface());
                sections.add(header);
            }
            sections.add(buildGameCard(game));
        }
    }

    private ObjectNode buildGameCard(JsonNode game) {
        String gameId = game.path("gameId").asText("unknown");
        String status = game.path("status").asText("Scheduled");
        JsonNode away = game.path("awayTeam");
        JsonNode home = game.path("homeTeam");
        boolean finalGame = "Final".equals(status);
        String statusText = "Final".equals(status) ? "Final" : game.path("time").asText("TBD");
        String targetUri = game.path("targetUri").asText(null);
        if (targetUri == null || targetUri.isBlank()) {
            log.warn("Schedule game {} missing server targetUri; row will not declare a tap action", gameId);
        }

        String contentSourceId = "stats-api:schedule-game-" + gameId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");

        String[] row = {
                sectionId,
                away.path("teamTricode").asText(""),
                away.path("teamName").asText(""),
                away.path("seed").asText(null),
                finalGame ? String.valueOf(game.path("awayScore").asInt(0)) : null,
                away.path("logoUrl").asText(null),
                home.path("teamTricode").asText(""),
                home.path("teamName").asText(""),
                home.path("seed").asText(null),
                finalGame ? String.valueOf(game.path("homeScore").asInt(0)) : null,
                home.path("logoUrl").asText(null),
                statusText,
                game.path("seriesText").asText(null),
                game.path("broadcastLogoUrls").asText(null),
                targetUri,
                game.path("overflowUri").asText(null)
        };
        ObjectNode section = atomicBuilder.buildGameScheduleRow(
                sectionId,
                "schedule_game_" + gameId,
                row);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.railSurface());
        return section;
    }

    private ObjectNode buildTeamColumn(JsonNode team) {
        ObjectNode col = objectMapper.createObjectNode();
        col.put("type", "Container");
        col.put("direction", "column");
        col.put("alignment", "center");
        col.put("crossAlignment", "center");
        ArrayNode children = objectMapper.createArrayNode();

        int teamId = team.path("teamId").asInt(0);
        ObjectNode logo = objectMapper.createObjectNode();
        logo.put("type", "Image");
        logo.put("src", SduiUtils.teamLogoUrl(teamId));
        logo.put("width", 48);
        logo.put("height", 48);
        logo.put("fit", "contain");
        AccessibilityHelper.addImage(objectMapper, logo,
                team.path("teamTricode").asText("Team") + " logo");
        children.add(logo);

        ObjectNode spacer = objectMapper.createObjectNode();
        spacer.put("type", "Spacer");
        spacer.put("height", 4);
        children.add(spacer);

        ObjectNode name = objectMapper.createObjectNode();
        name.put("type", "Text");
        name.put("content", team.path("teamTricode").asText("???"));
        name.put("variant", "bodySmall");
        name.put("weight", "bold");
        name.put("color", ColorTokens.TEXT_PRIMARY);
        name.put("textAlign", "center");
        children.add(name);

        col.set("children", children);
        return col;
    }

    private JsonNode loadScheduleData() {
        try {
            return utils.loadExampleResponse("schedule-2024-25");
        } catch (Exception e) {
            log.warn("Failed to load schedule mock data: {}", e.getMessage());
            return null;
        }
    }

    private String formatDate(String isoDate, String locale) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            Locale loc = Locale.forLanguageTag(locale != null ? locale : "en");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", loc);
            return date.format(fmt);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning raw value", isoDate);
            return isoDate;
        }
    }

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }
}
