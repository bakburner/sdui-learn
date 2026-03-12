package com.nba.sdui.core.renderer.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.TabGroupData
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.models.generated.HeroPanelData
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

enum class GamePanelVisualState { PRE, LIVE, FINAL }

data class GamePanelUiModel(
    val awayTricode: String,
    val homeTricode: String,
    val awayScore: String,
    val homeScore: String,
    val awayLogoUrl: String?,
    val homeLogoUrl: String?,
    val awayName: String?,
    val homeName: String?,
    val awayRecord: String?,
    val homeRecord: String?,
    val statusText: String,
    val recordsText: String?,
    val broadcaster: String?,
    val gameDateEt: String?,
    val leaderLines: List<String>,
    val visualState: GamePanelVisualState,
    val primaryAction: SduiAction?,
    val variant: String,
    val badgeText: String?,
    val visualLabel: String?,
    val backgroundImageUrl: String?
)

// ============ FollowingRail ============

data class FollowingRailItemUi(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val entityType: String?,
    val action: SduiAction?
)

data class FollowingRailUiModel(
    val title: String?,
    val items: List<FollowingRailItemUi>
)

// ============ SectionHeader ============

data class SectionHeaderUiModel(
    val title: String,
    val subtitle: String?,
    val action: SduiAction?
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

fun mapHeroPanel(section: SduiSection): HeroPanelData? = convert(section.data)

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
fun mapGamePanel(section: SduiSection): GamePanelUiModel? {
    val data = section.data ?: return null
    val homeTeam = data["homeTeam"] as? Map<String, Any?> ?: return null
    val awayTeam = data["awayTeam"] as? Map<String, Any?> ?: return null
    val gameStatus = (data["gameStatus"] as? Number)?.toInt() ?: 1
    val statusText = (data["gameStatusText"] as? String).orEmpty().ifBlank {
        if (gameStatus == 1) (data["gameTimeEt"] as? String).orEmpty().ifBlank { "Pregame" } else if (gameStatus == 2) "Live" else "Final"
    }
    val visualState = when (gameStatus) {
        1 -> GamePanelVisualState.PRE
        2 -> GamePanelVisualState.LIVE
        else -> GamePanelVisualState.FINAL
    }

    val leaders = data["gameLeaders"] as? Map<String, Any?>
    val homeLeader = leaders?.get("homeLeader") as? Map<String, Any?>
    val awayLeader = leaders?.get("awayLeader") as? Map<String, Any?>
    val leaderLines = listOfNotNull(
        formatLeader(awayLeader),
        formatLeader(homeLeader)
    )

    return GamePanelUiModel(
        awayTricode = (awayTeam["teamTricode"] as? String) ?: "AWY",
        homeTricode = (homeTeam["teamTricode"] as? String) ?: "HME",
        awayScore = ((awayTeam["score"] as? Number)?.toInt()?.toString()) ?: "-",
        homeScore = ((homeTeam["score"] as? Number)?.toInt()?.toString()) ?: "-",
        awayLogoUrl = awayTeam["logoUrl"] as? String,
        homeLogoUrl = homeTeam["logoUrl"] as? String,
        awayName = awayTeam["teamName"] as? String,
        homeName = homeTeam["teamName"] as? String,
        awayRecord = awayTeam["record"] as? String,
        homeRecord = homeTeam["record"] as? String,
        statusText = statusText,
        recordsText = if (visualState == GamePanelVisualState.PRE) {
            "Records: ${recordText(awayTeam)} @ ${recordText(homeTeam)}"
        } else {
            null
        },
        broadcaster = data["broadcaster"] as? String,
        gameDateEt = data["gameDateEt"] as? String,
        leaderLines = leaderLines,
        visualState = visualState,
        primaryAction = firstActionFromSection(section),
        variant = (data["variant"] as? String) ?: "standard",
        badgeText = data["badgeText"] as? String,
        visualLabel = data["visualLabel"] as? String,
        backgroundImageUrl = data["backgroundImageUrl"] as? String
    )
}

@Suppress("UNCHECKED_CAST")
fun mapFollowingRail(section: SduiSection): FollowingRailUiModel? {
    val data = section.data ?: return null
    val rawItems = data["items"] as? List<Map<String, Any?>> ?: return null
    val items = rawItems.map { item ->
        FollowingRailItemUi(
            id = (item["id"] as? String) ?: "",
            name = (item["name"] as? String) ?: "",
            imageUrl = item["imageUrl"] as? String,
            entityType = item["entityType"] as? String,
            action = (item["action"] as? Map<String, Any?>)?.let { actionToSduiAction(it) }
        )
    }
    return FollowingRailUiModel(
        title = data["title"] as? String,
        items = items
    )
}

fun mapSectionHeader(section: SduiSection): SectionHeaderUiModel? {
    val data = section.data ?: return null
    val title = (data["title"] as? String) ?: return null
    val subtitle = data["subtitle"] as? String

    @Suppress("UNCHECKED_CAST")
    val actionMap = data["action"] as? Map<String, Any?>
    val action = actionMap?.let { actionToSduiAction(it) }

    return SectionHeaderUiModel(
        title = title,
        subtitle = subtitle,
        action = action
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
    } catch (e: Exception) {
        android.util.Log.e("SectionUiAdapters", "Failed to convert data to ${T::class.java.simpleName}", e)
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

// ============ BoxscoreTable ============

data class BoxscoreColumnDef(
    val key: String,
    val label: String,
    val sortable: Boolean = true,
    val highlighted: Boolean = false,
    val width: Int? = null
)

data class BoxscorePlayerRowUi(
    val playerId: Int,
    val name: String,
    val imageUrl: String?,
    val jerseyNumber: String?,
    val position: String?,
    val starter: Boolean,
    val played: Boolean,
    val notPlayingReason: String?,
    val stats: Map<String, Any?>
)

data class BoxscoreTableUiModel(
    val teamTricode: String,
    val teamName: String,
    val teamColor: String?,
    val teamLogoUrl: String?,
    val columns: List<BoxscoreColumnDef>,
    val players: List<BoxscorePlayerRowUi>,
    val teamTotals: Map<String, Any?>?,
    val sortColumn: String?,
    val sortDirection: String,
    val sortStateKey: String?,
    val sortDirectionStateKey: String?,
    val emptyMessage: String?
)

@Suppress("UNCHECKED_CAST")
fun mapBoxscoreTable(section: SduiSection, screenState: Map<String, Any>): BoxscoreTableUiModel? {
    val data = section.data ?: return null

    val sortStateKey = data["sortStateKey"] as? String
    val sortDirectionStateKey = data["sortDirectionStateKey"] as? String
    val sortColumn = sortStateKey?.let { screenState[it] as? String }
    val sortDirection = (sortDirectionStateKey?.let { screenState[it] as? String } ?: "desc")

    val rawColumns = data["columns"] as? List<Map<String, Any?>> ?: emptyList()
    val columns = rawColumns.map { col ->
        BoxscoreColumnDef(
            key = (col["key"] as? String) ?: "",
            label = (col["label"] as? String) ?: "",
            sortable = (col["sortable"] as? Boolean) ?: true,
            highlighted = (col["highlighted"] as? Boolean) ?: false,
            width = (col["width"] as? Number)?.toInt()
        )
    }

    val rawPlayers = data["players"] as? List<Map<String, Any?>> ?: emptyList()
    val players = rawPlayers.map { p ->
        val rawStats = p["stats"] as? Map<String, Any?> ?: emptyMap()
        BoxscorePlayerRowUi(
            playerId = (p["playerId"] as? Number)?.toInt() ?: 0,
            name = (p["name"] as? String) ?: "",
            imageUrl = p["imageUrl"] as? String,
            jerseyNumber = p["jerseyNumber"] as? String,
            position = p["position"] as? String,
            starter = (p["starter"] as? Boolean) ?: false,
            played = (p["played"] as? Boolean) ?: true,
            notPlayingReason = p["notPlayingReason"] as? String,
            stats = rawStats
        )
    }

    val rawTotals = data["teamTotals"] as? Map<String, Any?>

    return BoxscoreTableUiModel(
        teamTricode = (data["teamTricode"] as? String) ?: "",
        teamName = (data["teamName"] as? String) ?: "",
        teamColor = data["teamColor"] as? String,
        teamLogoUrl = data["teamLogoUrl"] as? String,
        columns = columns,
        players = players,
        teamTotals = rawTotals,
        sortColumn = sortColumn,
        sortDirection = sortDirection,
        sortStateKey = sortStateKey,
        sortDirectionStateKey = sortDirectionStateKey,
        emptyMessage = data["emptyMessage"] as? String
    )
}

// ============ Form ============

data class FormOptionUi(
    val label: String,
    val value: String
)

data class FormFieldUi(
    val fieldId: String,
    val fieldType: String,
    val label: String?,
    val stateKey: String,
    val defaultValue: String?,
    val options: List<FormOptionUi>,
    val required: Boolean,
    val disabled: Boolean,
    val placeholder: String?
)

data class FormUiModel(
    val fields: List<FormFieldUi>,
    val submitAction: SduiAction?,
    val submitLabel: String?,
    val layout: String
)

@Suppress("UNCHECKED_CAST")
fun mapForm(section: SduiSection, screenState: Map<String, Any>): FormUiModel? {
    val data = section.data ?: return null
    val rawFields = data["fields"] as? List<Map<String, Any?>> ?: return null

    val fields = rawFields.map { f ->
        val rawOptions = f["options"] as? List<Map<String, Any?>> ?: emptyList()
        FormFieldUi(
            fieldId = (f["fieldId"] as? String) ?: (f["id"] as? String) ?: "",
            fieldType = (f["fieldType"] as? String) ?: "text",
            label = f["label"] as? String,
            stateKey = (f["stateKey"] as? String) ?: "",
            defaultValue = f["defaultValue"] as? String,
            options = rawOptions.map { o ->
                FormOptionUi(
                    label = (o["label"] as? String) ?: "",
                    value = (o["value"] as? String) ?: ""
                )
            },
            required = (f["required"] as? Boolean) ?: false,
            disabled = (f["disabled"] as? Boolean) ?: false,
            placeholder = f["placeholder"] as? String
        )
    }

    val rawSubmitAction = data["submitAction"] as? Map<String, Any?>
    val submitAction = rawSubmitAction?.let { actionToSduiAction(it) }

    return FormUiModel(
        fields = fields,
        submitAction = submitAction,
        submitLabel = data["submitLabel"] as? String,
        layout = (data["layout"] as? String) ?: "vertical"
    )
}

// ============ SeasonLeadersTable ============

data class SeasonLeadersColumnDef(
    val key: String,
    val label: String,
    val sortable: Boolean = true,
    val highlighted: Boolean = false,
    val width: Int? = null
)

data class SeasonLeadersPlayerRow(
    val rank: Int,
    val playerId: String,
    val name: String,
    val team: String,
    val imageUrl: String?,
    val stats: Map<String, Any?>
)

data class SeasonLeadersTableUiModel(
    val title: String?,
    val subtitle: String?,
    val columns: List<SeasonLeadersColumnDef>,
    val players: List<SeasonLeadersPlayerRow>,
    val totalRows: Int?,
    val page: Int?,
    val pageSize: Int?,
    val sortColumn: String?,
    val sortDirection: String?,
    val emptyMessage: String?
)

@Suppress("UNCHECKED_CAST")
fun mapSeasonLeadersTable(section: SduiSection): SeasonLeadersTableUiModel? {
    val data = section.data ?: return null

    val rawColumns = data["columns"] as? List<Map<String, Any?>> ?: emptyList()
    val columns = rawColumns.map { col ->
        SeasonLeadersColumnDef(
            key = (col["key"] as? String) ?: "",
            label = (col["label"] as? String) ?: "",
            sortable = (col["sortable"] as? Boolean) ?: true,
            highlighted = (col["highlighted"] as? Boolean) ?: false,
            width = (col["width"] as? Number)?.toInt()
        )
    }

    val rawPlayers = data["players"] as? List<Map<String, Any?>> ?: emptyList()
    val players = rawPlayers.map { p ->
        val rawStats = p["stats"] as? Map<String, Any?> ?: emptyMap()
        SeasonLeadersPlayerRow(
            rank = (p["rank"] as? Number)?.toInt() ?: 0,
            playerId = (p["playerId"] as? String) ?: "",
            name = (p["name"] as? String) ?: "",
            team = (p["team"] as? String) ?: "",
            imageUrl = p["imageUrl"] as? String,
            stats = rawStats
        )
    }

    return SeasonLeadersTableUiModel(
        title = data["title"] as? String,
        subtitle = data["subtitle"] as? String,
        columns = columns,
        players = players,
        totalRows = (data["totalRows"] as? Number)?.toInt(),
        page = (data["page"] as? Number)?.toInt(),
        pageSize = (data["pageSize"] as? Number)?.toInt(),
        sortColumn = data["sortColumn"] as? String,
        sortDirection = data["sortDirection"] as? String,
        emptyMessage = data["emptyMessage"] as? String
    )
}
