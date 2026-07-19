package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Sports
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FootballViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    viewModel: FootballViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchState by viewModel.selectedMatch.collectAsState()
    val match = matchState ?: return

    val favorites by viewModel.favorites.collectAsState()
    val isFav = favorites.any { it.id == "match:${match.id}" }

    var selectedTab by remember { mutableStateOf("Résumé") } // "Résumé", "Stats", "Compo", "Analyste IA"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = { Text(text = match.league, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.toggleFavorite(
                            id = "match:${match.id}",
                            type = "MATCH",
                            name = "${match.homeTeam} vs ${match.awayTeam}"
                        )
                    }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (isFav) StadiumYellow else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- 1. Scoreboard Panel ---
            ScoreboardPanel(match = match)

            // --- 2. Detail Navigation Tabs ---
            val tabs = listOf("Résumé", "Stats", "Compo", "Analyste IA")
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = StadiumGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                        color = StadiumGreen
                    )
                }
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) StadiumGreen else Color.Gray
                            )
                        }
                    )
                }
            }

            // --- 3. Tab Contents ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    "Résumé" -> MatchTimelineTab(match = match)
                    "Stats" -> MatchStatsTab(match = match)
                    "Compo" -> MatchLineupsTab(match = match)
                    "Analyste IA" -> MatchAiAnalystTab(viewModel = viewModel, match = match)
                }
            }
        }
    }
}

@Composable
fun ScoreboardPanel(match: Match) {
    val isLive = match.status == MatchStatus.LIVE || match.status == MatchStatus.HT

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Home
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamBadgePlaceholder(match.homeTeam, match.homeColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = match.homeTeam,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = match.homeFormation,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Central scores & match minutes
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LivePulseDot()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (match.status == MatchStatus.HT) "MI-TEMPS" else "EN DIRECT",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = StadiumRed,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (match.status == MatchStatus.HT) "HT" else "${match.minute}'",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StadiumGreen
                    )
                } else if (match.status == MatchStatus.FT) {
                    Text(
                        text = "TERMINÉ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                } else {
                    Text(
                        text = "À VENIR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = StadiumGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.startTime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (match.status == MatchStatus.UPCOMING) {
                    Text(
                        text = "VS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = StadiumGreen
                    )
                } else {
                    Text(
                        text = "${match.homeScore} - ${match.awayScore}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLive) StadiumGreen else Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Away
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamBadgePlaceholder(match.awayTeam, match.awayColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = match.awayTeam,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = match.awayFormation,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- TAB 1: Résumé ---
@Composable
fun MatchTimelineTab(match: Match) {
    if (match.events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Outlined.Sports, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Le match n'a pas encore commencé.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(match.events.reversed()) { event ->
                TimelineEventRow(event = event)
            }
        }
    }
}

@Composable
fun TimelineEventRow(event: MatchEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Minute
        Box(
            modifier = Modifier
                .width(42.dp)
                .background(StadiumGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${event.minute}'",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = StadiumGreen
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Icon
        val symbol = when (event.type) {
            EventType.GOAL -> "⚽"
            EventType.YELLOW_CARD -> "🟨"
            EventType.RED_CARD -> "🟥"
            EventType.SUBSTITUTION -> "🔄"
            EventType.VAR_DECISION -> "🖥️"
            EventType.CHANCE -> "⚠️"
            EventType.KICKOFF, EventType.HALF_TIME, EventType.FULL_TIME -> "📢"
            else -> "🔔"
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(text = symbol, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Event Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (event.type) {
                    EventType.GOAL -> "BUT !!!"
                    EventType.YELLOW_CARD -> "Avertissement"
                    EventType.RED_CARD -> "Carton Rouge"
                    EventType.SUBSTITUTION -> "Changement"
                    EventType.VAR_DECISION -> "Arbitrage Vidéo (VAR)"
                    EventType.CHANCE -> "Occasion Notable"
                    EventType.KICKOFF -> "Coup d'envoi"
                    EventType.HALF_TIME -> "Mi-temps"
                    EventType.FULL_TIME -> "Fin de match"
                    else -> "Événement"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (event.type == EventType.GOAL) StadiumGreen else if (event.type == EventType.RED_CARD) StadiumRed else Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.description,
                fontSize = 12.sp,
                color = Color.LightGray,
                lineHeight = 16.sp
            )
        }
    }
}

// --- TAB 2: Stats ---
@Composable
fun MatchStatsTab(match: Match) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatBarRow("Possession (%)", match.homeStats.possession, match.awayStats.possession, isPercentage = true)
        StatBarRow("Tirs au but", match.homeStats.shots, match.awayStats.shots)
        StatBarRow("Tirs Cadrés", match.homeStats.shotsOnTarget, match.awayStats.shotsOnTarget)
        StatBarRow("Passes", match.homeStats.passes, match.awayStats.passes)
        StatBarRow("Précision Passes (%)", match.homeStats.passAccuracy, match.awayStats.passAccuracy, isPercentage = true)
        StatBarRow("Fautes", match.homeStats.fouls, match.awayStats.fouls)
        StatBarRow("Corners", match.homeStats.corners, match.awayStats.corners)
        StatBarRow("Hors-jeu", match.homeStats.offsides, match.awayStats.offsides)
        StatBarRow("Cartons Jaunes", match.homeStats.yellowCards, match.awayStats.yellowCards)
        StatBarRow("Cartons Rouges", match.homeStats.redCards, match.awayStats.redCards)
    }
}

@Composable
fun StatBarRow(
    label: String,
    homeVal: Int,
    awayVal: Int,
    isPercentage: Boolean = false
) {
    val total = (homeVal + awayVal).coerceAtLeast(1)
    val homeRatio = homeVal.toFloat() / total.toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPercentage) "$homeVal%" else homeVal.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = StadiumGreen,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Start
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isPercentage) "$awayVal%" else awayVal.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Comparative Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(homeRatio.coerceAtLeast(0.01f))
                    .background(StadiumGreen)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((1f - homeRatio).coerceAtLeast(0.01f))
                    .background(Color.White)
            )
        }
    }
}

// --- TAB 3: Compositions (Lineups) ---
@Composable
fun MatchLineupsTab(match: Match) {
    if (match.homeLineup.isEmpty() || match.awayLineup.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Compositions d'équipes indisponibles.", color = Color.Gray)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchOnyx)
        ) {
            // Tactical Pitch Drawing Canvas with players
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, StadiumGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(PitchSurface)
            ) {
                // Soccer Pitch lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Outer border
                    drawRect(color = StadiumGreen.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))

                    // Center line
                    drawLine(
                        color = StadiumGreen.copy(alpha = 0.3f),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Center circle
                    drawCircle(
                        color = StadiumGreen.copy(alpha = 0.3f),
                        radius = 45.dp.toPx(),
                        center = Offset(w / 2, h / 2),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Goal boxes
                    drawRect(
                        color = StadiumGreen.copy(alpha = 0.2f),
                        topLeft = Offset(w / 4, 0f),
                        size = Size(w / 2, 50.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawRect(
                        color = StadiumGreen.copy(alpha = 0.2f),
                        topLeft = Offset(w / 4, h - 50.dp.toPx()),
                        size = Size(w / 2, 50.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Render Home Team (Top half 0..0.45)
                match.homeLineup.forEach { player ->
                    PlayerJersey(
                        player = player,
                        color = try { Color(android.graphics.Color.parseColor(match.homeColor)) } catch (e: Exception) { StadiumBlue },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                // Render Away Team (Bottom half 0.55..1)
                match.awayLineup.forEach { player ->
                    PlayerJersey(
                        player = player,
                        color = try { Color(android.graphics.Color.parseColor(match.awayColor)) } catch (e: Exception) { StadiumRed },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }

            // Simple Legend List
            Text(
                text = "Disposition Tactique : ${match.homeFormation} vs ${match.awayFormation}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun BoxScope.PlayerJersey(
    player: Player,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Coordinate offsets based on dimensions
    val xPercent = player.x
    val yPercent = player.y

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val px = (xPercent * maxWidth.value).dp
        val py = (yPercent * maxHeight.value).dp

        Column(
            modifier = modifier
                .offset(x = px - 20.dp, y = py - 20.dp)
                .width(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = player.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// --- TAB 4: Analyste IA (Gemini Interface) ---
@Composable
fun MatchAiAnalystTab(
    viewModel: FootballViewModel,
    match: Match
) {
    val analysisState by viewModel.aiAnalysisState.collectAsState()
    val customState by viewModel.customQuestionState.collectAsState()

    var userQuestion by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Intro Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = "Analyste IA",
                    tint = StadiumGreen,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "Analyste Tactique IA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Analyse poussée par le modèle Gemini 3.5-Flash.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- SECTION A: Tactical Brief ---
        Text(
            text = "Rapport Tactique d'Avant/Pendant Match",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = StadiumGreen
        )

        when (val state = analysisState) {
            is UiState.Idle -> {
                Button(
                    onClick = { viewModel.runAiAnalysis(match) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen, contentColor = Color.Black)
                ) {
                    Text("GÉNÉRER LE RAPPORT IA DIRECT", fontWeight = FontWeight.Black)
                }
            }
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = StadiumGreen)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("L'analyste IA rédige sa chronique...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            is UiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = state.data,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
            is UiState.Error -> {
                Text(
                    text = "Erreur d'analyse: ${state.message}",
                    color = StadiumRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

        // --- SECTION B: Custom Q&A ---
        Text(
            text = "Poser une question tactique à l'IA",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = StadiumGreen
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userQuestion,
                onValueChange = { userQuestion = it },
                placeholder = { Text("Ex: Le banc de touche de Paris peut-il faire la différence ?", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StadiumGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                maxLines = 2
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StadiumGreen)
                    .clickable {
                        viewModel.askCustomQuestion(match, userQuestion)
                        userQuestion = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Envoyer",
                    tint = PitchOnyx,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        when (val qState = customState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StadiumGreen, modifier = Modifier.size(24.dp))
                }
            }
            is UiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PitchSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💡 Réponse de l'Analyste IA :",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StadiumYellow
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = qState.data,
                            fontSize = 13.sp,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            is UiState.Error -> {
                Text(
                    text = "Erreur: ${qState.message}",
                    color = StadiumRed,
                    fontSize = 12.sp
                )
            }
            else -> {}
        }
    }
}
