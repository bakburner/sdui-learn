package com.nba.sdui.core.renderer.adapters

import com.nba.sdui.core.models.generated.BoxscoreColumnDefinition
import com.nba.sdui.core.models.generated.FormField
import com.nba.sdui.core.models.generated.FormOption
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
