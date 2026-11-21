package com.linkview.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linkview.core.ui.theme.LinkViewTheme
import com.linkview.feature.backup.BackupScreen
import com.linkview.feature.cards.CardsGridContent
import com.linkview.feature.cards.CardsHeader
import com.linkview.feature.cards.CardsScreen
import com.linkview.feature.cards.CardsViewModel
import com.linkview.feature.detail.CardDetailScreen
import com.linkview.feature.editor.CardEditorScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkViewTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LinkViewAdaptiveApp()
                }
            }
        }
    }
}

/**
 * Top-level adaptive scaffold:
 * - Wide screens (>= 600dp, e.g. Samsung Galaxy Tab): Full-width header with responsive split view underneath
 * - Compact screens (< 600dp, phones): Single-Pane Navigation Stack
 */
@Composable
fun LinkViewAdaptiveApp() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 600.dp

        if (isExpandedTablet) {
            TabletResponsiveLayout()
        } else {
            PhoneSinglePaneNavHost()
        }
    }
}

/**
 * Responsive Layout for Tablets:
 * - Full-width search and tag header at the top across the entire width.
 * - Under the header:
 *   - When no card is clicked: Full-width adaptive cards grid.
 *   - When a card is clicked or editing: Split view under the header (Left: Cards Grid, Right: Detail/Editor).
 */
@Composable
private fun TabletResponsiveLayout(
    cardsViewModel: CardsViewModel = hiltViewModel()
) {
    val uiState by cardsViewModel.uiState.collectAsStateWithLifecycle()
    var activeCardId by remember { mutableStateOf<String?>(null) }
    var editingCardId by remember { mutableStateOf<String?>(null) }
    var isShowingBackup by remember { mutableStateOf(false) }
    val cardJumpHistory = remember { mutableStateListOf<String>() }

    val isDetailOpen = activeCardId != null || editingCardId != null || isShowingBackup

    fun jumpToCard(targetCardId: String) {
        val resolvedId = cardsViewModel.resolveCardId(targetCardId)
        if (activeCardId != null && activeCardId != resolvedId) {
            cardJumpHistory.add(activeCardId!!)
        }
        editingCardId = null
        isShowingBackup = false
        activeCardId = resolvedId
    }

    fun handleTabletBack(): Boolean {
        if (isShowingBackup) {
            isShowingBackup = false
            return true
        }
        if (editingCardId != null) {
            editingCardId = null
            return true
        }
        if (cardJumpHistory.isNotEmpty()) {
            activeCardId = cardJumpHistory.removeAt(cardJumpHistory.lastIndex)
            return true
        }
        if (activeCardId != null) {
            activeCardId = null
            return true
        }
        return false
    }

    BackHandler(enabled = isDetailOpen) {
        handleTabletBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // FULL WIDTH HEADER ACROSS THE TOP
        CardsHeader(
            uiState = uiState,
            onSearchQueryChanged = { cardsViewModel.onSearchQueryChanged(it) },
            onScopeSelected = { cardsViewModel.onScopeSelected(it) },
            onTagSearchQueryChanged = { cardsViewModel.onTagSearchQueryChanged(it) },
            onClearTagFilters = { cardsViewModel.clearTagFilters() },
            onToggleTagFilter = { cardsViewModel.toggleTagFilter(it) },
            onAddAndSelectTag = { cardsViewModel.addAndSelectCustomTag(it) },
            onCreateCardClick = {
                isShowingBackup = false
                editingCardId = ""
            },
            onNavigateToBackup = {
                editingCardId = null
                isShowingBackup = true
            }
        )

        // DIVIDER BETWEEN SEARCH HEADER AND CARDS SECTION
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // UNDER THE HEADER: SPLIT VIEW WHEN ACTIVE, FULL-WIDTH GRID WHEN NOT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Left area: Cards Grid (~38% when detail open, full width when closed)
            Box(
                modifier = Modifier
                    .weight(if (isDetailOpen) 0.38f else 1f)
                    .fillMaxHeight()
            ) {
                CardsGridContent(
                    uiState = uiState,
                    selectedCardId = activeCardId,
                    onCardClick = { cardId ->
                        jumpToCard(cardId)
                    },
                    onLoadMore = {
                        cardsViewModel.loadMoreCards()
                    }
                )
            }

            // Right area: Detail / Editor / Backup
            if (isDetailOpen) {
                VerticalDivider()

                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = when {
                            isShowingBackup -> "backup"
                            editingCardId != null -> "editor_$editingCardId"
                            activeCardId != null -> "detail_$activeCardId"
                            else -> "empty"
                        },
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "RightPaneContent"
                    ) { target ->
                        when {
                            target == "backup" -> {
                                BackupScreen(
                                    onNavigateBack = { isShowingBackup = false }
                                )
                            }
                            target.startsWith("editor_") -> {
                                val editorCardId = target.removePrefix("editor_").ifEmpty { null }
                                CardEditorScreen(
                                    cardId = editorCardId,
                                    onNavigateBack = { editingCardId = null },
                                    onOpenExistingCard = { existingId ->
                                        jumpToCard(existingId)
                                    }
                                )
                            }
                            target.startsWith("detail_") -> {
                                val detailCardId = target.removePrefix("detail_")
                                CardDetailScreen(
                                    cardId = detailCardId,
                                    showBackButton = cardJumpHistory.isNotEmpty(),
                                    onClose = {
                                        activeCardId = null
                                        cardJumpHistory.clear()
                                    },
                                    onNavigateBack = { handleTabletBack() },
                                    onEditCard = { cardId -> editingCardId = cardId },
                                    onNavigateToCard = { targetId -> jumpToCard(targetId) },
                                    onDeleteSuccess = {
                                        activeCardId = null
                                        cardJumpHistory.clear()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single-Pane Navigation Stack for Phones / Compact Windows
 */
@Composable
private fun PhoneSinglePaneNavHost(
    cardsViewModel: CardsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "cards",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("cards") {
            CardsScreen(
                onCardClick = { cardId ->
                    navController.navigate("detail/$cardId")
                },
                onCreateCardClick = {
                    navController.navigate("editor")
                },
                onNavigateToBackup = {
                    navController.navigate("backup")
                }
            )
        }

        composable(
            route = "detail/{cardId}",
            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
            CardDetailScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() },
                onEditCard = { editCardId ->
                    navController.navigate("editor?cardId=$editCardId")
                },
                onNavigateToCard = { targetCardId ->
                    val resolvedId = cardsViewModel.resolveCardId(targetCardId)
                    navController.navigate("detail/$resolvedId")
                },
                onDeleteSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "editor?cardId={cardId}",
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")
            CardEditorScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() },
                onOpenExistingCard = { existingId ->
                    navController.navigate("detail/$existingId") {
                        popUpTo("cards")
                    }
                }
            )
        }

        composable("backup") {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
