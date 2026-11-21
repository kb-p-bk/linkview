package com.linkview.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linkview.core.ui.theme.LinkViewTheme
import com.linkview.feature.backup.BackupScreen
import com.linkview.feature.cards.CardsScreen
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
 * - Wide screens (>= 600dp, e.g. Samsung Galaxy Tab): Responsive Grid that transitions to Split View on card selection
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
 * - When no card is clicked: Full-width adaptive grid below the unified search/tag header.
 * - When a card is clicked or editing: Opens split view below the header (Left: Cards Grid, Right: Detail/Editor).
 */
@Composable
private fun TabletResponsiveLayout() {
    var activeCardId by remember { mutableStateOf<String?>(null) }
    var editingCardId by remember { mutableStateOf<String?>(null) }
    var isShowingBackup by remember { mutableStateOf(false) }
    val cardJumpHistory = remember { mutableStateListOf<String>() }

    val isDetailOpen = activeCardId != null || editingCardId != null || isShowingBackup

    fun jumpToCard(targetCardId: String) {
        if (activeCardId != null && activeCardId != targetCardId) {
            cardJumpHistory.add(activeCardId!!)
        }
        editingCardId = null
        isShowingBackup = false
        activeCardId = targetCardId
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

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Area: Cards Screen with Header & Responsive Grid
        Box(
            modifier = if (isDetailOpen) {
                Modifier
                    .width(380.dp)
                    .fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            CardsScreen(
                selectedCardId = activeCardId,
                onCardClick = { cardId ->
                    jumpToCard(cardId)
                },
                onCreateCardClick = {
                    isShowingBackup = false
                    editingCardId = ""
                },
                onNavigateToBackup = {
                    editingCardId = null
                    isShowingBackup = true
                }
            )
        }

        // Right Area: Split Pane Detail / Editor / Backup (opened only when active)
        if (isDetailOpen) {
            VerticalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when {
                    isShowingBackup -> {
                        BackupScreen(
                            onNavigateBack = { isShowingBackup = false }
                        )
                    }
                    editingCardId != null -> {
                        androidx.compose.runtime.key(editingCardId) {
                            CardEditorScreen(
                                cardId = editingCardId!!.ifEmpty { null },
                                onNavigateBack = { editingCardId = null },
                                onOpenExistingCard = { existingId ->
                                    jumpToCard(existingId)
                                }
                            )
                        }
                    }
                    activeCardId != null -> {
                        CardDetailScreen(
                            cardId = activeCardId!!,
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

/**
 * Single-Pane Navigation Stack for Phones / Compact Windows
 */
@Composable
private fun PhoneSinglePaneNavHost() {
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
                    navController.navigate("detail/$targetCardId")
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
