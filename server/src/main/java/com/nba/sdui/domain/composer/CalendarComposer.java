package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.domain.port.StatsPort;

/**
 * Composes the modal full-calendar SDUI screen.
 */
@Component
public class CalendarComposer {

    private static final Logger log = LoggerFactory.getLogger(CalendarComposer.class);

    private final ObjectMapper objectMapper;
    private final SeasonCalendarService seasonCalendarService;
    private final StatsPort statsPort;
    private final SduiUtils utils;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public CalendarComposer(ObjectMapper objectMapper,
                            SeasonCalendarService seasonCalendarService,
                            StatsPort statsPort,
                            SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.seasonCalendarService = seasonCalendarService;
        this.statsPort = statsPort;
        this.utils = utils;
    }

    public Screen composeCalendar(String traceId, String locale) {
        return composeCalendar(traceId, locale, null);
    }

    public Screen composeCalendar(String traceId, String locale, String selectedDateParam) {
        LocalDate defaultDate = seasonCalendarService.currentLeagueDate();
        LocalDate selectedDate = resolveSelectedDate(selectedDateParam, defaultDate);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "calendar");
        response.put("analyticsId", "calendar");
        response.put("schemaVersion", schemaVersion);

        ObjectNode state = objectMapper.createObjectNode();
        state.put("calendar_selected_date", selectedDate.toString());
        response.set("state", state);

        ArrayNode sections = objectMapper.createArrayNode();
        sections.add(objectMapper.valueToTree(buildCalendarMonthListSection(selectedDate, defaultDate)));
        response.set("sections", sections);

        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        try {
            return objectMapper.treeToValue(response, Screen.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to bind composed Calendar screen to Screen.class", e);
        }
    }

    private Section buildCalendarMonthListSection(LocalDate selectedDate, LocalDate defaultDate) {
        String contentSourceId = "server:games-calendar";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "CalendarMonthList");

        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.CALENDAR_MONTH_LIST);
        section.setContentSourceId(contentSourceId);
        section.setAnalyticsId("games_calendar_month_list");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "calendar_selected_date");
        data.put("selectedDate", selectedDate.toString());
        data.put("defaultDate", defaultDate.toString());
        data.put("minDate", seasonCalendarService.seasonStart().toString());
        data.put("maxDate", seasonCalendarService.seasonEnd().toString());

        // TODO: Populate from schedule API so each in-season date can expose gameCount/hasTeamGame.
        data.set("dateMetadata", buildDateMetadata());

        ObjectNode onDateSelected = objectMapper.createObjectNode();
        onDateSelected.put("trigger", "onActivate");
        onDateSelected.put("type", "navigate");
        onDateSelected.put("targetUri", "nba://games?date={{calendar_selected_date}}");
        data.set("onDateSelected", onDateSelected);

        section.setData(data);
        return section;
    }

    /**
     * Build the {@code dateMetadata} object for the CalendarMonthList section.
     *
     * <p>Fetches the season game counts via a single CDN season-schedule call. Only dates
     * with games and within the current season window are included. {@code hasTeamGame}
     * remains {@code false} until a favorite-team context is available in the request
     * envelope or profile state — inferring favorite teams on the client is prohibited.
     */
    private ObjectNode buildDateMetadata() {
        Map<LocalDate, Integer> gameCounts;
        try {
            gameCounts = statsPort.getSeasonGameCounts();
        } catch (Exception e) {
            log.warn("Could not fetch season game counts for dateMetadata: {}", e.getMessage());
            gameCounts = Map.of();
        }
        ObjectNode metadata = objectMapper.createObjectNode();
        for (Map.Entry<LocalDate, Integer> entry : gameCounts.entrySet()) {
            LocalDate date = entry.getKey();
            if (!seasonCalendarService.isInSeason(date)) continue;
            ObjectNode dayMeta = objectMapper.createObjectNode();
            dayMeta.put("gameCount", entry.getValue());
            dayMeta.put("hasTeamGame", false);
            metadata.set(date.toString(), dayMeta);
        }
        return metadata;
    }

    private LocalDate resolveSelectedDate(String selectedDateParam, LocalDate defaultDate) {
        if (selectedDateParam == null || selectedDateParam.isBlank()) {
            return defaultDate;
        }
        try {
            LocalDate parsed = LocalDate.parse(selectedDateParam);
            if (!seasonCalendarService.isInSeason(parsed)) {
                log.warn("Calendar selected date {} is out of season range [{}, {}], falling back to {}",
                        parsed, seasonCalendarService.seasonStart(), seasonCalendarService.seasonEnd(), defaultDate);
                return defaultDate;
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Failed to parse calendar selected date '{}', falling back to {}",
                    selectedDateParam, defaultDate);
            return defaultDate;
        }
    }
}
