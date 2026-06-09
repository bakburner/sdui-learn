package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.Form;
import com.nba.sdui.models.generated.FormField;
import com.nba.sdui.models.generated.FormOption;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionSurface;
import com.nba.sdui.models.generated.State;

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

    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ScheduleComposer(SduiUtils utils, SectionSurfaces surfaces, Tokens tokens) {
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(tokens);
    }

    public Screen composeSchedule(String traceId, String locale) {
        log.info("Composing Schedule screen, locale={}", locale);

        Screen response = new Screen();
        response.setId("schedule");
        response.setAnalyticsId("schedule");
        response.setSchemaVersion(schemaVersion);
        utils.applyTabDestinationNavigation(response, "schedule");

        // Initial chip selections. See Phase D.1 scope note in
        // docs/plans/ios-led-ux-rebuild.md — /sdui/schedule does not yet parse
        // query params, so these values are cosmetic; they drive chip
        // highlighting on first render only.
        State state = new State();
        state.setAdditionalProperty("schedule_season", "2024-25");
        state.setAdditionalProperty("schedule_year", "2024");
        state.setAdditionalProperty("schedule_month", "");
        state.setAdditionalProperty("schedule_day", "");
        response.setState(state);

        List<Section> sections = new ArrayList<>();

        sections.add(buildFilterForm(locale));

        JsonNode mockData = loadScheduleData();
        if (mockData != null) {
            addGameSections(sections, mockData, locale);
        }

        response.setSections(sections);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    private Section buildFilterForm(String locale) {
        String contentSourceId = "feed:schedule";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "Form", "filters");
        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.FORM);
        section.setContentSourceId(contentSourceId);
        section.setAnalyticsId("schedule_filters");
        section.setRefreshPolicy(List.of(staticPolicy()));

        Form data = new Form();
        data.setLayout(Form.Layout.VERTICAL);

        // Form submit fires on chip change via onChange actions on each
        // field; keep submitAction present for schema validity. The
        // controller doesn't parse query params yet — chip taps are
        // cosmetic until the data-wiring plan lands (Phase D.1 scope note).
        Action submitAction = new Action();
        submitAction.setTrigger(Action.ActionTrigger.fromValue("onSubmit"));
        submitAction.setType(Action.ActionType.fromValue("refresh"));
        data.setSubmitAction(submitAction);
        data.setSubmitLabel(SduiUtils.getLocalizedString(locale, "filter.apply"));

        List<FormField> fields = new ArrayList<>();

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

        data.setFields(fields);
        section.setData(data);
        return section;
    }

    /**
     * Emits a schema-compliant {@code FormField} with {@code fieldType: "select"}
     * and {@code variant: "chips"}. Clients resolve chips natively (iOS
     * capsules, Material {@code FilterChip}, web pill buttons).
     */
    private FormField buildChipsField(String stateKey, String label, String[][] options) {
        FormField field = new FormField();
        field.setFieldId(stateKey);
        field.setFieldType(FormField.FieldType.SELECT);
        field.setVariant(FormField.SelectVariant.CHIPS);
        field.setLabel(label);
        field.setStateKey(stateKey);

        List<FormOption> opts = new ArrayList<>();
        for (String[] opt : options) {
            FormOption o = new FormOption();
            o.setValue(opt[0]);
            o.setLabel(opt[1]);
            opts.add(o);
        }
        field.setOptions(opts);
        return field;
    }

    private void addGameSections(List<Section> sections, JsonNode mockData, String locale) {
        JsonNode games = mockData.path("games");
        if (!games.isArray()) return;

        String currentDate = "";
        for (JsonNode game : games) {
            String date = game.path("date").asText("");
            if (!date.equals(currentDate)) {
                currentDate = date;
                String headerContentSourceId = "feed:schedule";
                String headerSectionId = SectionIdDeriver.derive(
                        headerContentSourceId, "AtomicComposite",
                        "header" + date.replace("-", ""));
                Section header = atomicBuilder.buildSectionHeader(
                        headerSectionId, formatDate(date, locale), null, null, null);
                header.setContentSourceId(headerContentSourceId);
                header.setSurface(surfaces.sectionHeaderSurface());
                sections.add(header);
            }
            sections.add(buildGameCard(game));
        }
    }

    private Section buildGameCard(JsonNode game) {
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
        Section section = atomicBuilder.buildGameScheduleRow(
                sectionId,
                "schedule_game_" + gameId,
                row);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private JsonNode loadScheduleData() {
        try {
            return utils.loadExampleByFilename("schedule-2024-25.json");
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

    private RefreshPolicy staticPolicy() {
        RefreshPolicy rp = new RefreshPolicy();
        rp.setType(RefreshPolicy.RefreshType.STATIC);
        return rp;
    }
}
