package com.nba.sdui.core.renderer.interactions

import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.adapters.toSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * Shared section interaction helpers.
 *
 * Resolves actions from the schema-level `section.actions`.
 */
object SectionInteractions {
    fun actions(section: Section): List<SduiAction> =
        section.actions.orEmpty().map { it.toSduiAction() }

    fun primaryAction(section: Section, trigger: String = "onActivate"): SduiAction? {
        val list = actions(section)
        return list.firstOrNull { it.trigger.equals(trigger, ignoreCase = true) }
    }

    fun subsectionActions(section: Section, subsectionId: String): List<SduiAction> {
        val subsection = section.subsections?.firstOrNull { it.id == subsectionId }
            ?: return emptyList()
        return subsection.actions.orEmpty().map { it.toSduiAction() }
    }

    fun subsectionPrimaryAction(section: Section, subsectionId: String, trigger: String = "onActivate"): SduiAction? {
        val list = subsectionActions(section, subsectionId)
        return list.firstOrNull { it.trigger.equals(trigger, ignoreCase = true) }
    }
}

