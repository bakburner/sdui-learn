package com.nba.sdui.core.renderer.adapters

import com.nba.sdui.core.models.generated.BoxscoreColumnDefinition
import com.nba.sdui.core.models.generated.FormField
import com.nba.sdui.core.models.generated.FormOption
import com.nba.sdui.core.models.generated.GameLeaderData
import com.nba.sdui.core.models.generated.GamePanelDisplayConfig
import com.nba.sdui.core.models.generated.PlayerRow
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.state.SduiAction

// ============ TabGroup ============

data class TabGroupUiModel(
    val stateKey: String,
    val activeTabValue: String,
    val tabs: List<TabUiModel>,
    val activeSections: List<Section>
)

data class TabUiModel(
    val id: String,
    val label: String,
    val stateValue: String
)

enum class GamePanelVisualState { PRE, LIVE, FINAL }

data class GamePanelDisplayConfigUi(
    val logoSize: Int = 32,
    val cardHeight: Int? = null,
    val cornerRadius: Int = 12,
    val elevation: Int = 0,
    val scoreTextStyle: String = "compact",
    val background: BackgroundViewModel? = null,
    val liveBackground: BackgroundViewModel? = null,
    val badgeColor: String? = null,
    val textColor: String? = null
)

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
    val displayConfig: GamePanelDisplayConfigUi,
    val badgeText: String?,
    val visualLabel: String?,
    val variant: String? = null
)

fun mapTabGroup(section: Section, screenState: Map<String, Any>): TabGroupUiModel? {
    val data = section.data ?: return null
    val stateKey = data.stateKey ?: return null
    val tabs = data.tabs.orEmpty()
    val fallbackTabValue = tabs.firstOrNull()?.stateValue ?: tabs.firstOrNull()?.id
    val activeValue = (screenState[stateKey] as? String) ?: data.defaultTab ?: fallbackTabValue ?: return null
    val uiTabs = tabs.map { tab ->
        TabUiModel(
            id = tab.id,
            label = tab.label,
            stateValue = tab.stateValue ?: tab.id
        )
    }
    return TabGroupUiModel(
        stateKey = stateKey,
        activeTabValue = activeValue,
        tabs = uiTabs,
        activeSections = data.tabContents?.get(activeValue).orEmpty()
    )
}

fun mapTabMutateAction(stateKey: String, stateValue: String): SduiAction =
    SduiAction(
        trigger = "onTap",
        type = "mutate",
        stateKey = stateKey,
        stateValue = stateValue
    )

fun mapGamePanel(section: Section): GamePanelUiModel? {
    val data = section.data ?: return null
    val homeTeam = data.homeTeam ?: return null
    val awayTeam = data.awayTeam ?: return null
    val gameStatus = data.gameStatus?.toInt() ?: 1
    val statusText = data.gameStatusText.orEmpty().ifBlank {
        if (gameStatus == 1) data.gameTimeEt.orEmpty().ifBlank { "Pregame" }
        else if (gameStatus == 2) "Live"
        else "Final"
    }
    val visualState = when (gameStatus) {
        1 -> GamePanelVisualState.PRE
        2 -> GamePanelVisualState.LIVE
        else -> GamePanelVisualState.FINAL
    }

    val leaderLines = listOfNotNull(
        formatLeader(data.gameLeaders?.awayLeader),
        formatLeader(data.gameLeaders?.homeLeader)
    )

    return GamePanelUiModel(
        awayTricode = awayTeam.teamTricode ?: "AWY",
        homeTricode = homeTeam.teamTricode ?: "HME",
        awayScore = awayTeam.score?.toInt()?.toString() ?: "-",
        homeScore = homeTeam.score?.toInt()?.toString() ?: "-",
        awayLogoUrl = awayTeam.logoURL,
        homeLogoUrl = homeTeam.logoURL,
        awayName = awayTeam.teamName,
        homeName = homeTeam.teamName,
        awayRecord = null,
        homeRecord = null,
        statusText = statusText,
        recordsText = null,
        broadcaster = null,
        gameDateEt = null,
        leaderLines = leaderLines,
        visualState = visualState,
        primaryAction = firstActionFromSection(section),
        displayConfig = mapDisplayConfig(data.displayConfig),
        badgeText = data.badgeText,
        visualLabel = data.visualLabel,
        variant = data.variant?.value
    )
}

private fun firstActionFromSection(section: Section): SduiAction? {
    val sectionAction = section.actions?.firstOrNull()?.toSduiAction()
    if (sectionAction != null) return sectionAction
    return section.data?.actions?.firstOrNull()?.toSduiAction()
}

private fun mapDisplayConfig(config: GamePanelDisplayConfig?): GamePanelDisplayConfigUi {
    if (config == null) return GamePanelDisplayConfigUi()
    return GamePanelDisplayConfigUi(
        logoSize = config.logoSize?.toInt() ?: 32,
        cardHeight = config.cardHeight?.toInt(),
        cornerRadius = config.cornerRadius?.toInt() ?: 12,
        elevation = config.elevation?.toInt() ?: 0,
        scoreTextStyle = config.scoreTextStyle?.value ?: "compact",
        background = config.background?.toViewModel(),
        liveBackground = config.liveBackground?.toViewModel(),
        badgeColor = config.badgeColor,
        textColor = config.textColor
    )
}

private fun formatLeader(leader: GameLeaderData?): String? {
    if (leader == null) return null
    val name = leader.name ?: return null
    val points = leader.points?.toInt() ?: 0
    val rebounds = leader.rebounds?.toInt() ?: 0
    val assists = leader.assists?.toInt() ?: 0
    return "$name - $points PTS, $rebounds REB, $assists AST"
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

fun mapBoxscoreTable(section: Section, screenState: Map<String, Any>): BoxscoreTableUiModel? {
    val data = section.data ?: return null

    val sortStateKey = data.sortStateKey
    val sortDirectionStateKey = data.sortDirectionStateKey
    val sortColumn = sortStateKey?.let { screenState[it] as? String }
    val sortDirection = (sortDirectionStateKey?.let { screenState[it] as? String } ?: data.sortDirection?.value) ?: "desc"

    val columns = data.columns.orEmpty().map(::mapBoxscoreColumn)
    val players = data.players.orEmpty().map(::mapBoxscorePlayer)

    return BoxscoreTableUiModel(
        teamTricode = data.teamTricode ?: "",
        teamName = data.teamName ?: "",
        teamColor = data.teamColor,
        teamLogoUrl = data.teamLogoURL,
        columns = columns,
        players = players,
        teamTotals = data.teamTotals,
        sortColumn = sortColumn,
        sortDirection = sortDirection,
        sortStateKey = sortStateKey,
        sortDirectionStateKey = sortDirectionStateKey,
        emptyMessage = data.emptyMessage
    )
}

private fun mapBoxscoreColumn(column: BoxscoreColumnDefinition): BoxscoreColumnDef =
    BoxscoreColumnDef(
        key = column.key ?: "",
        label = column.label ?: "",
        sortable = column.sortable ?: true,
        highlighted = column.highlighted ?: false,
        width = column.width?.toIntOrNull()
    )

private fun mapBoxscorePlayer(player: PlayerRow): BoxscorePlayerRowUi =
    BoxscorePlayerRowUi(
        playerId = player.playerID.toIntOrNull() ?: 0,
        name = player.name,
        imageUrl = player.imageURL,
        jerseyNumber = player.jerseyNumber,
        position = player.position,
        starter = player.starter ?: false,
        played = true,
        notPlayingReason = null,
        stats = player.stats
    )

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
    val placeholder: String?,
    val variant: String? = null
)

data class FormUiModel(
    val fields: List<FormFieldUi>,
    val submitAction: SduiAction?,
    val submitLabel: String?,
    val layout: String
)

fun mapForm(section: Section, screenState: Map<String, Any>): FormUiModel? {
    val data = section.data ?: return null
    val fields = data.fields.orEmpty().map(::mapFormField)

    return FormUiModel(
        fields = fields,
        submitAction = data.submitAction?.toSduiAction(),
        submitLabel = data.submitLabel,
        layout = data.layout?.value ?: "vertical"
    )
}

private fun mapFormField(field: FormField): FormFieldUi =
    FormFieldUi(
        fieldId = field.fieldID,
        fieldType = field.fieldType.value,
        label = field.label,
        stateKey = field.stateKey,
        defaultValue = null,
        options = field.options.orEmpty().map(::mapFormOption),
        required = field.required ?: false,
        disabled = field.disabled ?: false,
        placeholder = field.placeholder,
        variant = field.variant?.value
    )

private fun mapFormOption(option: FormOption): FormOptionUi =
    FormOptionUi(
        label = option.label ?: "",
        value = option.value ?: ""
    )

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

fun mapSeasonLeadersTable(section: Section): SeasonLeadersTableUiModel? {
    val data = section.data ?: return null

    val columns = data.columns.orEmpty().map(::mapSeasonLeadersColumn)
    val players = data.players.orEmpty().map(::mapSeasonLeadersPlayer)

    return SeasonLeadersTableUiModel(
        title = data.title,
        subtitle = data.subtitle,
        columns = columns,
        players = players,
        totalRows = data.totalRows?.toInt(),
        page = data.page?.toInt(),
        pageSize = data.pageSize?.toInt(),
        sortColumn = data.sortColumn,
        sortDirection = data.sortDirection?.value,
        emptyMessage = data.emptyMessage
    )
}

private fun mapSeasonLeadersColumn(column: BoxscoreColumnDefinition): SeasonLeadersColumnDef =
    SeasonLeadersColumnDef(
        key = column.key ?: "",
        label = column.label ?: "",
        sortable = column.sortable ?: true,
        highlighted = column.highlighted ?: false,
        width = column.width?.toIntOrNull()
    )

private fun mapSeasonLeadersPlayer(player: PlayerRow): SeasonLeadersPlayerRow =
    SeasonLeadersPlayerRow(
        rank = player.rank?.toInt() ?: 0,
        playerId = player.playerID,
        name = player.name,
        team = player.team ?: "",
        imageUrl = player.imageURL,
        stats = player.stats
    )
