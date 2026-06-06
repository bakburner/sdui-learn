package com.nba.sdui.service;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.BoxscoreComposer;
import com.nba.sdui.domain.composer.GameDetailComposer;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;

class GameDetailComposerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void liveGameDetailResponseDoesNotEmitTopLevelType() throws Exception {
        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        BoxscoreComposer boxscoreComposer = mock(BoxscoreComposer.class);
        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);
        SectionRefreshService sectionRefreshService = mock(SectionRefreshService.class);
        GameDetailComposer composer = new GameDetailComposer(new StatsApiAdapter(statsApiClient), boxscoreComposer, sectionRefreshService, utils, surfaces, TestTokens.INSTANCE);
        ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");

        JsonNode liveBoxscore = objectMapper.readTree("""
                {
                  "game": {
                    "gameStatus": 1,
                    "gameStatusText": "7:00 PM ET",
                    "gameClock": "",
                    "awayTeam": {
                      "teamId": 1610612738,
                      "teamTricode": "BOS",
                      "teamName": "Celtics",
                      "wins": 10,
                      "losses": 5,
                      "score": 101,
                      "players": []
                    },
                    "homeTeam": {
                      "teamId": 1610612747,
                      "teamTricode": "LAL",
                      "teamName": "Lakers",
                      "wins": 9,
                      "losses": 6,
                      "score": 99,
                      "players": []
                    }
                  }
                }
                """);

        when(statsApiClient.getBoxscore("123")).thenReturn(liveBoxscore);
        when(boxscoreComposer.buildBoxscoreTableSection(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    Section section = new Section();
                    section.setId("boxscore-" + invocation.getArgument(3, String.class));
                    section.setType(Section.Type.fromValue("BoxscoreTable"));
                    section.setData(objectMapper.createObjectNode());
                    return section;
                });

        GameDetailComposer.GameDetailResult result = composer.composeGameDetail("123", "A", "1.0", "trace-1", "en");

        assertNotNull(result.response());
        ObjectNode wire = (ObjectNode) objectMapper.valueToTree(result.response());
        assertFalse(wire.has("type"), "screen payload must not emit a top-level type field");
        verify(statsApiClient, times(2)).getBoxscore("123");
        verify(boxscoreComposer, times(2))
                .buildBoxscoreTableSection(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }
}