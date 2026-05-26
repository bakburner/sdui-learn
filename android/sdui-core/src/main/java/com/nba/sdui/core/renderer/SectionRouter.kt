package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.atomic.AtomicRouter
import com.nba.sdui.core.renderer.atomic.LocalCompositeContent
import com.nba.sdui.core.renderer.sections.*
import com.nba.sdui.core.state.SduiAction

/**
 * Section Router - Maps section types to their renderers.
 * 
 * This is the core of the SDUI contract. The router is a when statement
 * mapping type strings to composable functions. Unknown section types
 * are skipped gracefully with a warning log.
 */
@Composable
fun SectionRouter(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier,
    sectionSlotDepth: Int = 0
) {
    Log.d("SectionRouter", "Routing section: id=${section.id}, type=${section.type}")

    // Every section, permanent or AtomicComposite, is wrapped by
    // SectionContainer so outer chrome is server-driven via
    // `section.surface`. `SectionContainer` is a no-op when
    // `surface` is null, so AtomicComposites whose root Container
    // already carries its own padding/background/shadow are
    // unaffected — composers opt into outer margin/chrome by
    // emitting a `surface` block on the section envelope.
    when (section.type) {

        "CalendarStrip" -> SectionContainer(section.surface, modifier) {
            CalendarStripRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "TabGroup" -> SectionContainer(section.surface, modifier) {
            TabGroupRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "BoxscoreTable" -> SectionContainer(section.surface, modifier) {
            BoxscoreTableRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "Form" -> SectionContainer(section.surface, modifier) {
            FormRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "SubscribeBanner" -> SectionContainer(section.surface, modifier) {
            SubscribeBannerRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "SubscribeHero" -> SectionContainer(section.surface, modifier) {
            SubscribeHeroRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "AdSlot" -> SectionContainer(section.surface, modifier) {
            AdSlotRenderer(
                section = section,
                onAction = onAction
            )
        }

        "SeasonLeadersTable" -> SectionContainer(section.surface, modifier) {
            SeasonLeadersTableRenderer(
                section = section,
                onAction = onAction
            )
        }

        "VideoPlayer" -> SectionContainer(section.surface, modifier) {
            VideoPlayerStub(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "AtomicComposite" -> {
            val root = section.data?.ui
            if (root != null) {
                SectionContainer(section.surface, modifier) {
                    CompositionLocalProvider(LocalCompositeContent provides section.data?.content) {
                        AtomicRouter(
                            element = root,
                            screenState = screenState,
                            onAction = onAction,
                            onStateChange = onStateChange,
                            sectionSlotDepth = sectionSlotDepth
                        )
                    }
                }
            } else {
                Log.w("SectionRouter", "AtomicComposite section ${section.id} has no parsable root element")
            }
        }
        
        else -> {
            // Unknown section type - skip gracefully
            Log.w("SectionRouter", "Unknown section type: ${section.type}, skipping section ${section.id}")
            // Render nothing for unknown types
        }
    }
}

/**
 * List of all supported section types.
 * Used for contract testing to verify router coverage.
 */
val SUPPORTED_SECTION_TYPES = setOf(
    "CalendarStrip",
    "TabGroup",
    "BoxscoreTable",
    "Form",
    "SubscribeBanner",
    "SubscribeHero",
    "AdSlot",
    "SeasonLeadersTable",
    "VideoPlayer",
    "AtomicComposite"
)
