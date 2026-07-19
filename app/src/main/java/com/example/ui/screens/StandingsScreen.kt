package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.model.LeagueStanding
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.StadiumGreen
import com.example.ui.theme.StadiumYellow
import com.example.ui.theme.StadiumRed
import com.example.ui.viewmodel.FootballViewModel

@Composable
fun StandingsScreen(
    viewModel: FootballViewModel,
    modifier: Modifier = Modifier
) {
    val standings by viewModel.standings.collectAsState()
    var selectedLeague by remember { mutableStateOf("Ligue 1 🇫🇷") }

    val filteredStandings = remember(standings, selectedLeague) {
        standings.filter { it.leagueName == selectedLeague }
    }

    val availableLeagues = remember(standings) {
        standings.map { it.leagueName }.distinct()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header ---
        Text(
            text = "Classements",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )

        // --- League selection tabs ---
        ScrollableTabRow(
            selectedTabIndex = availableLeagues.indexOf(selectedLeague).coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = StadiumGreen,
            divider = {},
            indicator = { tabPositions ->
                val index = availableLeagues.indexOf(selectedLeague).coerceAtLeast(0)
                if (index < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = StadiumGreen
                    )
                }
            }
        ) {
            availableLeagues.forEach { league ->
                val isSelected = selectedLeague == league
                Tab(
                    selected = isSelected,
                    onClick = { selectedLeague = league },
                    text = {
                        Text(
                            text = league,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (isSelected) StadiumGreen else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Table Headers Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Équipe",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "MJ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "G",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "N",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "P",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Diff",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pts",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = StadiumGreen,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
        }

        // --- Table Rows ---
        if (filteredStandings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StadiumGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredStandings, key = { it.teamName }) { standing ->
                    StandingRow(standing = standing, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StandingRow(
    standing: LeagueStanding,
    viewModel: FootballViewModel
) {
    val isTeamFavFlow = viewModel.isFavoriteFlow("team:${standing.teamName}").collectAsState(initial = false)
    val isTeamFav = isTeamFavFlow.value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.toggleFavorite(
                    id = "team:${standing.teamName}",
                    type = "TEAM",
                    name = standing.teamName
                )
            }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            val isTopRank = standing.rank <= 3
            Text(
                text = standing.rank.toString(),
                fontSize = 13.sp,
                fontWeight = if (isTopRank) FontWeight.Black else FontWeight.Normal,
                color = if (isTopRank) StadiumGreen else Color.LightGray
            )
        }

        // Team Logo placeholder + Name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamBadgePlaceholder(standing.teamName, standing.teamColor)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = standing.teamName,
                fontSize = 14.sp,
                fontWeight = if (isTeamFav) FontWeight.Black else FontWeight.Medium,
                color = if (isTeamFav) StadiumYellow else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isTeamFav) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favori",
                    tint = StadiumYellow,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Matches Played
        Text(
            text = standing.played.toString(),
            fontSize = 13.sp,
            color = Color.LightGray,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        // Wins
        Text(
            text = standing.won.toString(),
            fontSize = 13.sp,
            color = Color.LightGray,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )

        // Draws
        Text(
            text = standing.drawn.toString(),
            fontSize = 13.sp,
            color = Color.LightGray,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )

        // Losses
        Text(
            text = standing.lost.toString(),
            fontSize = 13.sp,
            color = Color.LightGray,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )

        // Goal Difference
        val gd = standing.goalsFor - standing.goalsAgainst
        val gdSign = if (gd > 0) "+$gd" else gd.toString()
        Text(
            text = gdSign,
            fontSize = 13.sp,
            color = if (gd > 0) StadiumGreen else if (gd < 0) StadiumRed else Color.LightGray,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )

        // Points
        Text(
            text = standing.points.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}
