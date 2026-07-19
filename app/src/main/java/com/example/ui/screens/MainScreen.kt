package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.model.Match
import com.example.ui.theme.PitchOnyx
import com.example.ui.theme.StadiumGreen
import com.example.ui.viewmodel.FootballViewModel

enum class MainTab(val title: String) {
    MATCHS("Matchs"),
    CLASSEMENTS("Classements"),
    FAVORIS("Favoris"),
    ALERTES("Alertes"),
    PARAMETRES("Paramètres")
}

@Composable
fun MainScreen(
    viewModel: FootballViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainTab.MATCHS) }
    val selectedMatch by viewModel.selectedMatch.collectAsState()

    // Slide Transitions for Details Screen overlay
    AnimatedContent(
        targetState = selectedMatch,
        transitionSpec = {
            if (targetState != null) {
                // Slide detail screen in from bottom / right
                slideInVertically(animationSpec = tween(300)) { it } + fadeIn() togetherWith
                        slideOutVertically(animationSpec = tween(300)) { -it } + fadeOut()
            } else {
                // Slide out back to list
                slideInVertically(animationSpec = tween(300)) { -it } + fadeIn() togetherWith
                        slideOutVertically(animationSpec = tween(300)) { it } + fadeOut()
            }
        },
        label = "match_details_transition"
    ) { activeMatch ->
        if (activeMatch != null) {
            MatchDetailScreen(
                viewModel = viewModel,
                onBackClick = { viewModel.selectMatch(null) }
            )
        } else {
            // Main Bottom Navigation shell
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = StadiumGreen
                    ) {
                        MainTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            val icon = when (tab) {
                                MainTab.MATCHS -> if (isSelected) Icons.Filled.SportsSoccer else Icons.Outlined.SportsSoccer
                                MainTab.CLASSEMENTS -> if (isSelected) Icons.Filled.List else Icons.Outlined.List
                                MainTab.FAVORIS -> if (isSelected) Icons.Filled.Star else Icons.Outlined.Star
                                MainTab.ALERTES -> if (isSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications
                                MainTab.PARAMETRES -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = { Icon(imageVector = icon, contentDescription = tab.title) },
                                label = { Text(text = tab.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PitchOnyx,
                                    selectedTextColor = StadiumGreen,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = StadiumGreen
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainTab.MATCHS -> MatchesScreen(
                            viewModel = viewModel,
                            onMatchClick = { viewModel.selectMatch(it) }
                        )
                        MainTab.CLASSEMENTS -> StandingsScreen(
                            viewModel = viewModel
                        )
                        MainTab.FAVORIS -> FavoritesScreen(
                            viewModel = viewModel,
                            onMatchClick = { viewModel.selectMatch(it) }
                        )
                        MainTab.ALERTES -> AlertsScreen(
                            viewModel = viewModel,
                            onMatchClick = { viewModel.selectMatch(it) }
                        )
                        MainTab.PARAMETRES -> SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
