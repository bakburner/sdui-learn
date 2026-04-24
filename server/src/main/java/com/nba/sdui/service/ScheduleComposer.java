package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        response.put("title", "Schedule");
        response.put("analyticsId", "schedule");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("schedule"));

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

        sections.add(buildFilterForm());

        JsonNode mockData = loadScheduleData();
        if (mockData != null) {
            addGameSections(sections, mockData);
        }

        response.set("sections", sections);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    private ObjectNode buildFilterForm() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "schedule-filters");
        section.put("type", "Form");
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
        data.put("submitLabel", "Apply");

        ArrayNode fields = objectMapper.createArrayNode();

        fields.add(buildChipsField("schedule_season", "Season",
                new String[][]{
                        {"2024-25", "2024-25"}, {"2023-24", "2023-24"},
                        {"2022-23", "2022-23"}, {"2021-22", "2021-22"}
                }));

        fields.add(buildChipsField("schedule_year", "Year",
                new String[][]{
                        {"2024", "2024"}, {"2025", "2025"}
                }));

        fields.add(buildChipsField("schedule_month", "Month",
                new String[][]{
                        {"", "All"}, {"10", "October"}, {"11", "November"}, {"12", "December"},
                        {"1", "January"}, {"2", "February"}, {"3", "March"}, {"4", "April"}
                }));

        fields.add(buildChipsField("schedule_day", "Day",
                new String[][]{
                        {"", "All"}, {"1", "1"}, {"5", "5"}, {"10", "10"},
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

    private void addGameSections(ArrayNode sections, JsonNode mockData) {
        JsonNode games = mockData.path("games");
        if (!games.isArray()) return;

        String currentDate = "";
        for (JsonNode game : games) {
            String date = game.path("date").asText("");
            if (!date.equals(currentDate)) {
                currentDate = date;
                ObjectNode header = atomicBuilder.buildSectionHeader(
                        "schedule-header-" + date, formatDate(date), null, null, null);
                header.set("surface", utils.sectionHeaderSurface());
                sections.add(header);
            }
            sections.add(buildGameCard(game));
        }
    }

    private ObjectNode buildGameCard(JsonNode game) {
        String gameId = game.path("gameId").asText("unknown");
        String status = game.path("status").asText("Scheduled");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "Container");
        root.put("direction", "row");
        root.put("alignment", "center");
        root.put("crossAlignment", "center");
        root.put("background", ColorTokens.SURFACE_CANVAS);
        root.put("cornerRadius", 12);

        ObjectNode shadowObj = objectMapper.createObjectNode();
        shadowObj.put("color", "#00000014");
        shadowObj.put("radius", 4);
        shadowObj.put("offsetX", 0);
        shadowObj.put("offsetY", 2);
        root.set("shadow", shadowObj);

        ObjectNode pad = objectMapper.createObjectNode();
        pad.put("start", 16); pad.put("end", 16); pad.put("top", 12); pad.put("bottom", 12);
        root.set("padding", pad);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + gameId);
        actions.add(action);
        root.set("actions", actions);

        ArrayNode children = objectMapper.createArrayNode();

        // Away team column
        children.add(buildTeamColumn(game.path("awayTeam")));

        // Score / time center
        ObjectNode center = objectMapper.createObjectNode();
        center.put("type", "Container");
        center.put("direction", "column");
        center.put("alignment", "center");
        center.put("crossAlignment", "center");
        center.put("flex", 1);
        ArrayNode centerChildren = objectMapper.createArrayNode();

        if ("Final".equals(status)) {
            int awayScore = game.path("awayScore").asInt(0);
            int homeScore = game.path("homeScore").asInt(0);
            ObjectNode scoreText = objectMapper.createObjectNode();
            scoreText.put("type", "Text");
            scoreText.put("content", awayScore + " - " + homeScore);
            scoreText.put("variant", "headlineMedium");
            scoreText.put("weight", "bold");
            scoreText.put("color", ColorTokens.TEXT_PRIMARY);
            scoreText.put("monospacedDigits", true);
            scoreText.put("textAlign", "center");
            centerChildren.add(scoreText);

            ObjectNode statusEl = objectMapper.createObjectNode();
            statusEl.put("type", "Text");
            statusEl.put("content", "Final");
            statusEl.put("variant", "labelSmall");
            statusEl.put("color", ColorTokens.TEXT_SECONDARY);
            statusEl.put("textAlign", "center");
            centerChildren.add(statusEl);
        } else {
            ObjectNode timeEl = objectMapper.createObjectNode();
            timeEl.put("type", "Text");
            timeEl.put("content", game.path("time").asText("TBD"));
            timeEl.put("variant", "bodyMedium");
            timeEl.put("weight", "semiBold");
            timeEl.put("color", ColorTokens.TEXT_PRIMARY);
            timeEl.put("textAlign", "center");
            centerChildren.add(timeEl);
        }
        center.set("children", centerChildren);
        children.add(center);

        // Home team column
        children.add(buildTeamColumn(game.path("homeTeam")));

        root.set("children", children);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "schedule-game-" + gameId);
        section.put("type", "AtomicComposite");
        section.put("analyticsId", "schedule_game_" + gameId);
        section.set("refreshPolicy", staticPolicy());
        ObjectNode data = objectMapper.createObjectNode();
        data.set("ui", root);
        section.set("data", data);
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

    private String formatDate(String isoDate) {
        try {
            String[] parts = isoDate.split("-");
            String month = switch (parts[1]) {
                case "01" -> "January";
                case "02" -> "February";
                case "03" -> "March";
                case "04" -> "April";
                case "10" -> "October";
                case "11" -> "November";
                case "12" -> "December";
                default -> parts[1];
            };
            return month + " " + Integer.parseInt(parts[2]) + ", " + parts[0];
        } catch (Exception e) {
            return isoDate;
        }
    }

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }
}
