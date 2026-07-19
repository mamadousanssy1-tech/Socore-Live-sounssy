package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Favorite
import com.example.data.model.Match
import com.example.ui.theme.StadiumGreen
import com.example.ui.theme.StadiumYellow
import com.example.ui.viewmodel.FootballViewModel

@Composable
fun FavoritesScreen(
    viewModel: FootballViewModel,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val allMatches by viewModel.allMatches.collectAsState()

    val favMatches = remember(favorites, allMatches) {
        val favMatchIds = favorites.filter { it.type == "MATCH" }.map { it.id.substringAfter("match:") }
        allMatches.filter { it.id.toString() in favMatchIds }
    }

    val favTeams = remember(favorites) {
        favorites.filter { it.type == "TEAM" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header ---
        Text(
            text = "Favoris",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )

        if (favorites.isEmpty()) {
            EmptyFavoritesState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Favorite Teams Section ---
                if (favTeams.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Mes Équipes Favorites")
                    }
                    items(favTeams, key = { it.id }) { team ->
                        FavoriteTeamItem(team = team, onRemove = {
                            viewModel.toggleFavorite(team.id, "TEAM", team.name)
                        })
                    }
                }

                // --- Favorite Matches Section ---
                if (favMatches.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Mes Matchs Favoris")
                    }
                    items(favMatches, key = { it.id }) { match ->
                        MatchItemCard(
                            match = match,
                            isFavorite = true,
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
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = StadiumGreen,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun FavoriteTeamItem(
    team: Favorite,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamBadgePlaceholder(team.name, "#1B5E20") // default color fallback
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = team.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Unfavorite team",
                    tint = StadiumYellow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyFavoritesState() {
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
                imageVector = Icons.Outlined.Star,
                contentDescription = "No Favorites",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun favori ajouté",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cliquez sur l'étoile à côté d'un match ou d'un club dans les classements pour recevoir des notifications en temps réel lors des buts, cartons et fins de matchs !",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
