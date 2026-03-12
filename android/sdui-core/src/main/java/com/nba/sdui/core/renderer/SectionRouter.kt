package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.SduiSection
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
    modifier: Modifier = Modifier
) {
    Log.v("SectionRouter", "Routing section: id=${section.id}, type=${section.type}")
    
    when (section.type) {
        "ScoreboardHeader" -> {
            ScoreboardHeaderRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }
        
        "StatLine" -> {
            StatLineRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }
        
        "HeroPanel" -> {
            HeroPanelRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }
        
        "ContentRail" -> {
            ContentRailRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }
        
        "TabGroup" -> {
            TabGroupRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange,
                modifier = modifier
            )
        }
        
        "PromoBanner" -> {
            PromoBannerRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "GamePanel" -> {
            GamePanelRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "Row" -> {
            RowRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange,
                modifier = modifier
            )
        }

        "BoxscoreTable" -> {
            BoxscoreTableRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange,
                modifier = modifier
            )
        }

        "Form" -> {
            FormRenderer(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange,
                modifier = modifier
            )
        }

        "FollowingRail" -> {
            FollowingRailRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "FeaturedGamePanel" -> {
            FeaturedGamePanelRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "SectionHeader" -> {
            SectionHeaderRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "VideoCarousel" -> {
            VideoCarouselRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "NbaTvSchedule" -> {
            NbaTvScheduleRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "SubscribeBanner" -> {
            SubscribeBannerRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "SubscribeHero" -> {
            SubscribeHeroRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "AdSlot" -> {
            AdSlotRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "SeasonLeadersTable" -> {
            SeasonLeadersTableRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
        }

        "ErrorState" -> {
            ErrorStateRenderer(
                section = section,
                onAction = onAction,
                modifier = modifier
            )
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
    "ScoreboardHeader",
    "StatLine",
    "HeroPanel",
    "ContentRail",
    "TabGroup",
    "PromoBanner",
    "GamePanel",
    "Row",
    "BoxscoreTable",
    "Form",
    "FollowingRail",
    "FeaturedGamePanel",
    "SectionHeader",
    "VideoCarousel",
    "NbaTvSchedule",
    "SubscribeBanner",
    "SubscribeHero",
    "AdSlot",
    "SeasonLeadersTable",
    "ErrorState"
)
