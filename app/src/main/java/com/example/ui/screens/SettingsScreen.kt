package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
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
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.StadiumBlue
import com.example.ui.theme.StadiumGreen
import com.example.ui.theme.StadiumYellow
import com.example.ui.viewmodel.FootballViewModel

@Composable
fun SettingsScreen(
    viewModel: FootballViewModel,
    modifier: Modifier = Modifier
) {
    val speedSec by viewModel.simulationSpeedSec.collectAsState()
    val isRunning by viewModel.isSimulationRunning.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Text(
            text = "Paramètres",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        // --- 1. Simulation Controller Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Simulateur Ticker",
                        tint = StadiumGreen
                    )
                    Text(
                        text = "Contrôle du Simulateur de Matchs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play / Pause Button
                Button(
                    onClick = { viewModel.toggleSimulationState() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else StadiumGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (isRunning) "PAUSER LA SIMULATION" else "DÉMARRER LA SIMULATION",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speed Slider
                Text(
                    text = "Vitesse de simulation: 1 minute = $speedSec secondes",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = speedSec.toFloat(),
                    onValueChange = { viewModel.setSimulationSpeed(it.toInt()) },
                    valueRange = 2f..15f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = StadiumGreen,
                        activeTrackColor = StadiumGreen,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Super Rapide (2s)", fontSize = 10.sp, color = Color.Gray)
                    Text("Normal (5s)", fontSize = 10.sp, color = Color.Gray)
                    Text("Réaliste (15s)", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        // --- 2. Live Scores API Integration Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Keys",
                        tint = StadiumYellow
                    )
                    Text(
                        text = "Intégration d'API Externe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cette application est équipée d'un simulateur de jeu de football de pointe fonctionnant en temps réel pour une expérience fluide. " +
                            "Si vous disposez d'un jeton pour une API de football professionnelle (ex: API-Football ou Football-Data.org), vous pouvez intégrer vos propres flux de scores réels !",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 Comment configurer vos clés ?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StadiumYellow
                        )
                        Text(
                            text = "1. Accédez au panneau 'Secrets' sur AI Studio.\n" +
                                    "2. Créez une variable d'environnement nommée 'GEMINI_API_KEY' pour activer l'Analyste IA.\n" +
                                    "3. Le système l'injectera de manière cryptée et sécurisée via BuildConfig, évitant ainsi toute fuite de clé sur le dépôt.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- 3. About / Credits ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "À propos",
                        tint = StadiumBlue
                    )
                    Text(
                        text = "À Propos de FootDirect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "FootDirect est un replica haut de gamme et immersif de l'application 'Resultados Fútbol', conçue en Jetpack Compose moderne. " +
                            "Elle intègre des technologies avancées de notification système et l'intelligence artificielle générative de Google Gemini pour créer des résumés tactiques en direct.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Version 1.0.0 • Fait avec passion pour le Football ⚽",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StadiumGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
