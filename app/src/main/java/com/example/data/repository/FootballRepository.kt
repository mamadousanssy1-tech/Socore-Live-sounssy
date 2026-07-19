package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FootballDao
import com.example.data.model.*
import com.example.data.remote.GeminiClient
import com.example.simulator.MatchSimulatorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class FootballRepository(private val context: Context, private val database: AppDatabase) {
    private val dao = database.footballDao()

    // Match and standing feeds (from the live simulator)
    val matches: StateFlow<List<Match>> = MatchSimulatorManager.matches
    val standings: StateFlow<List<LeagueStanding>> = MatchSimulatorManager.standings

    // Favorites feeds
    val allFavorites: Flow<List<Favorite>> = dao.getAllFavorites()

    fun isFavoriteFlow(id: String): Flow<Boolean> = dao.isFavoriteFlow(id)

    suspend fun toggleFavorite(id: String, type: String, name: String) {
        val exists = dao.isFavorite(id)
        if (exists) {
            dao.deleteFavoriteById(id)
        } else {
            dao.insertFavorite(Favorite(id = id, type = type, name = name))
        }
    }

    // Notifications feeds
    val notificationLogs: Flow<List<NotificationLog>> = dao.getAllNotificationLogs()

    suspend fun markNotificationAsRead(id: Int) = dao.markAsRead(id)

    suspend fun markAllNotificationsAsRead() = dao.markAllAsRead()

    suspend fun clearAllNotifications() = dao.clearAllNotifications()

    // Live AI Analyst
    suspend fun getAiAnalysis(match: Match): String {
        val systemPrompt = """
            Tu es un analyste tactique de football expert, lyrique et passionné pour l'application FootDirect.
            Rédige une analyse tactique vivante, réaliste et pointue du match actuel.
            Utilise un ton immersif, digne des meilleurs journalistes sportifs français (comme L'Équipe ou RMC Sport).
            Fais référence aux équipes (${match.homeTeam} et ${match.awayTeam}), au score actuel (${match.homeScore} - ${match.awayScore}), à la minute (${match.minute}'), aux formations tactiques (${match.homeFormation} et ${match.awayFormation}) et aux événements clés (buts, cartons, penaltys).
            Donne ton avis d'expert sur ce qui va se passer ensuite (pronostic, changements tactiques conseillés).
        """.trimIndent()

        val eventsSummary = match.events.joinToString("\n") { "- Min ${it.minute}': ${it.description}" }
        val userPrompt = """
            Analyse le match actuel :
            Match : ${match.homeTeam} (Formation : ${match.homeFormation}) vs ${match.awayTeam} (Formation : ${match.awayFormation})
            Score : ${match.homeScore} - ${match.awayScore}
            Minute : ${match.minute}'
            Statistiques ${match.homeTeam} : Tirs(${match.homeStats.shots}), Cadrés(${match.homeStats.shotsOnTarget}), Possession(${match.homeStats.possession}%)
            Statistiques ${match.awayTeam} : Tirs(${match.awayStats.shots}), Cadrés(${match.awayStats.shotsOnTarget}), Possession(${match.awayStats.possession}%)
            Composition ${match.homeTeam} : ${match.homeLineup.joinToString { "${it.name} (${it.position})" }}
            Composition ${match.awayTeam} : ${match.awayLineup.joinToString { "${it.name} (${it.position})" }}
            
            Événements clés :
            $eventsSummary
            
            Donne-moi une analyse détaillée et un pronostic pour la suite de la rencontre.
        """.trimIndent()

        return GeminiClient.askAnalyst(systemPrompt, userPrompt)
    }

    suspend fun askCustomQuestion(match: Match, question: String): String {
        val systemPrompt = """
            Tu es l'analyste tactique IA de l'application FootDirect. Réponds de façon concise (2-3 paragraphes maximum), vivante et experte à la question de l'utilisateur sur le match ${match.homeTeam} vs ${match.awayTeam} (Score: ${match.homeScore}-${match.awayScore}, Minute: ${match.minute}').
        """.trimIndent()

        val eventsSummary = match.events.joinToString("\n") { "- Min ${it.minute}': ${it.description}" }
        val userPrompt = """
            Match : ${match.homeTeam} vs ${match.awayTeam}
            Score : ${match.homeScore} - ${match.awayScore}
            Minute : ${match.minute}'
            Événements :
            $eventsSummary
            
            Question de l'utilisateur : $question
        """.trimIndent()

        return GeminiClient.askAnalyst(systemPrompt, userPrompt)
    }
}
