package com.nba.sdui.core.renderer.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.TabGroupData
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.models.generated.ContentCardData
import com.nba.sdui.models.generated.ContentRailData
import com.nba.sdui.models.generated.ScoreboardHeaderData
import com.nba.sdui.models.generated.StatLineListData
import com.nba.sdui.models.generated.TeamData

private val mapper = ObjectMapper().registerKotlinModule()

data class ScoreboardHeaderUiModel(
    val awayTeam: TeamData,
    val homeTeam: TeamData,
    val statusText: String,
    val isLive: Boolean,
    val periodLabel: String?
)

data class TabGroupUiModel(
    val stateKey: String,
    val activeTabValue: String,
    val tabs: List<TabUiModel>,
    val activeSections: List<SduiSection>
)

data class TabUiModel(
    val id: String,
    val label: String,
    val stateValue: String
)

data class PromoBannerUiModel(
    val title: String?,
    val headline: String?,
    val subhead: String?,
    val imageUrl: String?,
    val primaryAction: SduiAction?
)

enum class GameCardVisualState { PRE, LIVE, FINAL }

data class GameCardUiModel(
    val awayTricode: String,
    val homeTricode: String,
    val awayScore: String,
    val homeScore: String,
    val awayLogoUrl: String?,
    val homeLogoUrl: String?,
    val statusText: String,
    val recordsText: String?,
    val leaderLines: List<String>,
    val visualState: GameCardVisualState,
    val primaryAction: SduiAction?
)

fun mapScoreboardHeader(section: SduiSection): ScoreboardHeaderUiModel? {
    val parsed = convert<ScoreboardHeaderData>(section.data) ?: return null
    return ScoreboardHeaderUiModel(
        awayTeam = parsed.awayTeam,
        homeTeam = parsed.homeTeam,
        statusText = parsed.gameStatusText,
        isLive = parsed.gameStatus == 2,
        periodLabel = if (parsed.gameStatus == 2 && parsed.period > 0) "Q${parsed.period}" else null
    )
}

fun mapStatLineList(section: SduiSection): StatLineListData? = convert(section.data)

fun mapContentCard(section: SduiSection): ContentCardData? = convert(section.data)

fun mapContentRail(section: SduiSection): ContentRailData? = convert(section.data)

fun mapTabGroup(section: SduiSection, screenState: Map<String, Any>): TabGroupUiModel? {
    val parsed = convert<TabGroupData>(section.data) ?: return null
    val activeValue = (screenState[parsed.stateKey] as? String) ?: parsed.defaultTab
    val tabs = parsed.tabs.map { tab ->
        TabUiModel(
            id = tab.id,
            label = tab.label,
            stateValue = tab.stateValue ?: tab.id
        )
    }
    return TabGroupUiModel(
        stateKey = parsed.stateKey,
        activeTabValue = activeValue,
        tabs = tabs,
        activeSections = parsed.tabContents?.get(activeValue).orEmpty()
    )
}

fun mapTabMutateAction(stateKey: String, stateValue: String): SduiAction =
    SduiAction(
        trigger = "onTap",
        type = "mutate",
        stateKey = stateKey,
        stateValue = stateValue
    )

fun mapPromoBanner(section: SduiSection): PromoBannerUiModel? {
    val data = section.data ?: return null
    return PromoBannerUiModel(
        title = data["title"] as? String,
        headline = data["headline"] as? String,
        subhead = (data["subhead"] ?: data["description"]) as? String,
        imageUrl = data["imageUrl"] as? String,
        primaryAction = firstActionFromSection(section)
    )
}

@Suppress("UNCHECKED_CAST")
fun mapGameCard(section: SduiSection): GameCardUiModel? {
    val data = section.data ?: return null
    val homeTeam = data["homeTeam"] as? Map<String, Any?> ?: return null
    val awayTeam = data["awayTeam"] as? Map<String, Any?> ?: return null
    val gameStatus = (data["gameStatus"] as? Number)?.toInt() ?: 1
    val statusText = (data["gameStatusText"] as? String).orEmpty().ifBlank {
        if (gameStatus == 1) (data["gameTimeEt"] as? String).orEmpty().ifBlank { "Pregame" } else if (gameStatus == 2) "Live" else "Final"
    }
    val visualState = when (gameStatus) {
        1 -> GameCardVisualState.PRE
        2 -> GameCardVisualState.LIVE
        else -> GameCardVisualState.FINAL
    }

    val leaders = data["gameLeaders"] as? Map<String, Any?>
    val homeLeader = leaders?.get("homeLeader") as? Map<String, Any?>
    val awayLeader = leaders?.get("awayLeader") as? Map<String, Any?>
    val leaderLines = listOfNotNull(
        formatLeader(awayLeader),
        formatLeader(homeLeader)
    )

    return GameCardUiModel(
        awayTricode = (awayTeam["teamTricode"] as? String) ?: "AWY",
        homeTricode = (homeTeam["teamTricode"] as? String) ?: "HME",
        awayScore = ((awayTeam["score"] as? Number)?.toInt()?.toString()) ?: "-",
        homeScore = ((homeTeam["score"] as? Number)?.toInt()?.toString()) ?: "-",
        awayLogoUrl = awayTeam["logoUrl"] as? String,
        homeLogoUrl = homeTeam["logoUrl"] as? String,
        statusText = statusText,
        recordsText = if (visualState == GameCardVisualState.PRE) {
            "Records: ${recordText(awayTeam)} @ ${recordText(homeTeam)}"
        } else {
            null
        },
        leaderLines = leaderLines,
        visualState = visualState,
        primaryAction = firstActionFromSection(section)
    )
}

/**
 * Resolve the primary action from section-level actions first,
 * falling back to data-level actions for backward compatibility.
 */
private fun firstActionFromSection(section: SduiSection): SduiAction? {
    val sectionAction = section.actions
        ?.firstOrNull()
        ?.let(::actionToSduiAction)
    if (sectionAction != null) return sectionAction
    return section.data?.let(::firstActionFrom)
}

private inline fun <reified T> convert(data: Map<String, Any?>?): T? {
    if (data == null) return null
    return try {
        mapper.convertValue(data, T::class.java)
    } catch (_: Exception) {
        null
    }
}

@Suppress("UNCHECKED_CAST")
private fun firstActionFrom(data: Map<String, Any?>): SduiAction? {
    val actions = data["actions"] as? List<Map<String, Any?>>
    val singleAction = data["action"] as? Map<String, Any?>
    return actionToSduiAction(actions?.firstOrNull() ?: singleAction)
}

private fun formatLeader(leader: Map<String, Any?>?): String? {
    if (leader == null) return null
    val name = leader["name"] as? String ?: return null
    val points = (leader["points"] as? Number)?.toInt() ?: 0
    val rebounds = (leader["rebounds"] as? Number)?.toInt() ?: 0
    val assists = (leader["assists"] as? Number)?.toInt() ?: 0
    return "$name - $points PTS, $rebounds REB, $assists AST"
}

private fun recordText(team: Map<String, Any?>): String {
    val wins = (team["wins"] as? Number)?.toInt()
    val losses = (team["losses"] as? Number)?.toInt()
    return if (wins != null && losses != null) "$wins-$losses" else "-"
}
