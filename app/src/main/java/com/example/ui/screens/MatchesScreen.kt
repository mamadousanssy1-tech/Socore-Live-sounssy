package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Match
import com.example.data.model.MatchStatus
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.StadiumGreen
import com.example.ui.theme.StadiumRed
import com.example.ui.theme.StadiumYellow
import com.example.ui.viewmodel.FootballViewModel

@Composable
fun MatchesScreen(
    viewModel: FootballViewModel,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    val matches by viewModel.allMatches.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedLeagueFilter by viewModel.selectedLeagueFilter.collectAsState()

    var statusFilter by remember { mutableStateOf("Tous") } // "Tous", "En direct", "Terminés", "À venir"

    // Filter matches by date and status
    val filteredMatches = remember(matches, selectedDate, statusFilter, selectedLeagueFilter) {
        matches.filter { match ->
            val matchesDate = match.dateString == selectedDate
            val matchesStatus = when (statusFilter) {
                "En direct" -> match.status == MatchStatus.LIVE || match.status == MatchStatus.HT
                "Terminés" -> match.status == MatchStatus.FT
                "À venir" -> match.status == MatchStatus.UPCOMING
                else -> true
            }
            val matchesLeague = selectedLeagueFilter == null || match.league == selectedLeagueFilter
            matchesDate && matchesStatus && matchesLeague
        }
    }

    // Group matches by league
    val matchesByLeague = remember(filteredMatches) {
        filteredMatches.groupBy { it.league }
    }

    val leaguesList = remember(matches) {
        matches.map { it.league }.distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Top Header & App Logo ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.SportsSoccer,
                contentDescription = "Soccer Ball Logo",
                tint = StadiumGreen,
                modifier = Modifier
                    .size(32.dp)
                    .animateContentSize()
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FootDirect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text(
                text = " LIVE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StadiumGreen,
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .background(DarkGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // --- 2. Calendar / Date Selector Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Hier", "Aujourd'hui", "Demain").forEach { date ->
                val isSelected = selectedDate == date
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) StadiumGreen else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { viewModel.setDateFilter(date) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        // --- 3. Status Filters (Tous, En Direct, etc) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Tous", "En direct", "Terminés", "À venir").forEach { filter ->
                val isSelected = statusFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (isSelected) StadiumGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        )
                        .clickable { statusFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (filter == "En direct") {
                            LivePulseDot()
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) StadiumGreen else Color.Gray
                        )
                    }
                }
            }
        }

        // --- 4. League quick selector ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedLeagueFilter == null) StadiumGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                    .clickable { viewModel.setLeagueFilter(null) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Toutes Ligues",
                    fontSize = 11.sp,
                    color = if (selectedLeagueFilter == null) StadiumGreen else Color.LightGray
                )
            }

            leaguesList.forEach { league ->
                val isSelected = selectedLeagueFilter == league
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) StadiumGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.setLeagueFilter(league) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = league.substringBefore(" "), // remove emoji for filter pill
                        fontSize = 11.sp,
                        color = if (isSelected) StadiumGreen else Color.LightGray
                    )
                }
            }
        }

        // --- 5. Matches List ---
        if (matchesByLeague.isEmpty()) {
            EmptyState(statusFilter)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                matchesByLeague.forEach { (league, leagueMatches) ->
                    item {
                        LeagueHeader(leagueName = league)
                    }
                    items(leagueMatches, key = { it.id }) { match ->
                        val isFav = favorites.any { it.id == "match:${match.id}" }
                        MatchItemCard(
                            match = match,
                            isFavorite = isFav,
                            onCardClick = { onMatchClick(match) },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(
                                    id = "match:${match.id}",
                                    type = "MATCH",
                                    name = "${match.homeTeam} vs ${match.awayTeam}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeagueHeader(leagueName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(StadiumGreen, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = leagueName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun MatchItemCard(
    match: Match,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val isLive = match.status == MatchStatus.LIVE || match.status == MatchStatus.HT

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("match_item_card_${match.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- Top Row: Time, Status & Favorite ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLive) {
                        LivePulseDot()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (match.status == MatchStatus.HT) "Mi-temps" else "${match.minute}'",
                            color = StadiumRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else if (match.status == MatchStatus.FT) {
                        Text(
                            text = "Terminé",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = match.startTime,
                            color = StadiumGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite Match",
                        tint = if (isFavorite) StadiumYellow else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Middle Section: Teams & Scores ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Row(
                    modifier = Modifier
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = match.homeTeam,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TeamBadgePlaceholder(match.homeTeam, match.homeColor)
                }

                // Scores Box
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (match.status == MatchStatus.UPCOMING) {
                        Text(
                            text = "VS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StadiumGreen
                        )
                    } else {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLive) StadiumGreen else Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Away Team
                Row(
                    modifier = Modifier
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TeamBadgePlaceholder(match.awayTeam, match.awayColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = match.awayTeam,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Bottom Row: Latest commentary event (if live) ---
            if (isLive && match.events.isNotEmpty()) {
                val latestEvent = match.events.last()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${latestEvent.minute}'",
                        color = StadiumGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = latestEvent.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TeamBadgePlaceholder(teamName: String, hexColor: String) {
    val color = remember(hexColor) {
        try {
            Color(android.graphics.Color.parseColor(hexColor))
        } catch (e: Exception) {
            StadiumGreen
        }
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.7f), color)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = teamName.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}

@Composable
fun LivePulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(StadiumRed.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((6f * scale).dp)
                .clip(CircleShape)
                .background(StadiumRed)
        )
    }
}

@Composable
fun EmptyState(filter: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SportsSoccer,
                contentDescription = "No Matches",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun match trouvé",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Il n'y a aucun match correspondant au filtre '$filter' pour ce jour.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
