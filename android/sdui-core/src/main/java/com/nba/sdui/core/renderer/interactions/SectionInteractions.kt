package com.nba.sdui.core.renderer.interactions

import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * Shared section interaction helpers.
 *
 * Resolves actions from the schema-level `section.actions` first,
 * falling back to legacy `section.data.actions` for backward compatibility.
 */
object SectionInteractions {
    @Suppress("UNCHECKED_CAST")
    fun actions(section: SduiSection): List<SduiAction> {
        val sectionLevel = section.actions
            ?.mapNotNull(::actionToSduiAction)
            ?.takeIf { it.isNotEmpty() }

        if (sectionLevel != null) return sectionLevel

        val dataLevel = section.data?.get("actions") as? List<Map<String, Any?>>
        return dataLevel.orEmpty().mapNotNull(::actionToSduiAction)
    }

    fun primaryAction(section: SduiSection, trigger: String = "onTap"): SduiAction? =
        actions(section).firstOrNull { it.trigger.equals(trigger, ignoreCase = true) }

    @Suppress("UNCHECKED_CAST")
    fun subsectionActions(section: SduiSection, subsectionId: String): List<SduiAction> {
        val subsection = section.subsections?.firstOrNull { it.id == subsectionId }
            ?: return emptyList()
        return subsection.actions.orEmpty().mapNotNull(::actionToSduiAction)
    }

    fun subsectionPrimaryAction(section: SduiSection, subsectionId: String, trigger: String = "onTap"): SduiAction? =
        subsectionActions(section, subsectionId)
            .firstOrNull { it.trigger.equals(trigger, ignoreCase = true) }
}

