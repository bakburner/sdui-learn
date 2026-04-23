package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.AtomicElementParser
import com.nba.sdui.core.renderer.atomic.AtomicRouter
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
    section: SduiSection,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier,
    sectionSlotDepth: Int = 0
) {
    Log.d("SectionRouter", "Routing section: id=${section.id}, type=${section.type}")

    // Every section, permanent or AtomicComposite, is wrapped by
    // SectionContainer so outer chrome is server-driven via
    // `section.display` (§15.3). `SectionContainer` is a no-op when
    // `display` is null, so AtomicComposites whose root Container
    // already carries its own padding/background/shadow are
    // unaffected — composers opt into outer margin/chrome by
    // emitting a `display` block on the section envelope.
    when (section.type) {


        "TabGroup" -> SectionContainer(section.display, modifier) {
            TabGroupRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "GamePanel" -> SectionContainer(section.display, modifier) {
            GamePanelRenderer(
                section = section,
                onAction = onAction
            )
        }

        "BoxscoreTable" -> SectionContainer(section.display, modifier) {
            BoxscoreTableRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "Form" -> SectionContainer(section.display, modifier) {
            FormRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }

        "SubscribeBanner" -> SectionContainer(section.display, modifier) {
            SubscribeBannerRenderer(
                section = section,
                onAction = onAction
            )
        }

        "SubscribeHero" -> SectionContainer(section.display, modifier) {
            SubscribeHeroRenderer(
                section = section,
                onAction = onAction
            )
        }

        "AdSlot" -> SectionContainer(section.display, modifier) {
            AdSlotRenderer(
                section = section,
                onAction = onAction
            )
        }

        "SeasonLeadersTable" -> SectionContainer(section.display, modifier) {
            SeasonLeadersTableRenderer(
                section = section,
                onAction = onAction
            )
        }

        "VideoPlayer" -> SectionContainer(section.display, modifier) {
            VideoPlayerStub(
                section = section,
                onAction = onAction
            )
        }

        "AtomicComposite" -> {
            val root = AtomicElementParser.parse(section.data)
            if (root != null) {
                SectionContainer(section.display, modifier) {
                    AtomicRouter(
                        element = root,
                        screenState = screenState,
                        onAction = onAction,
                        modifier = Modifier,
                        onStateChange = onStateChange,
                        sectionSlotDepth = sectionSlotDepth
                    )
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
    "TabGroup",
    "GamePanel",
    "BoxscoreTable",
    "Form",
    "SubscribeBanner",
    "SubscribeHero",
    "AdSlot",
    "SeasonLeadersTable",
    "VideoPlayer",
    "AtomicComposite"
)
