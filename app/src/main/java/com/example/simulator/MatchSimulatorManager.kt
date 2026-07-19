package com.example.simulator

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.FootballDao
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Serializable
import kotlin.random.Random

object MatchSimulatorManager {
    private const val TAG = "MatchSimulator"
    private val simulatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var database: AppDatabase? = null
    private var simulationJob: Job? = null

    // Speed configuration (seconds per simulated minute)
    var simulationSpeedMs = 5000L // 5 seconds per tick (representing 1-2 minutes)
    var isSimulationRunning = true

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _standings = MutableStateFlow<List<LeagueStanding>>(emptyList())
    val standings: StateFlow<List<LeagueStanding>> = _standings.asStateFlow()

    fun initialize(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "footdirect_db"
            ).build()
        }

        // Load Initial Data
        _matches.value = generateInitialMatches()
        _standings.value = generateInitialStandings()

        // Start Ticking Simulation
        startSimulation(context.applicationContext)
    }

    private fun startSimulation(context: Context) {
        simulationJob?.cancel()
        simulationJob = simulatorScope.launch {
            while (isActive) {
                if (isSimulationRunning) {
                    delay(simulationSpeedMs)
                    tickMatches(context)
                } else {
                    delay(1000)
                }
            }
        }
    }

    private suspend fun tickMatches(context: Context) {
        val dao = database?.footballDao() ?: return
        val currentMatches = _matches.value.map { match ->
            if (match.status == MatchStatus.LIVE) {
                val nextMinute = match.minute + Random.nextInt(1, 3)
                
                if (nextMinute >= 90) {
                    // Match Full Time
                    val finalMatch = match.copy(
                        minute = 90,
                        status = MatchStatus.FT,
                        events = match.events + MatchEvent(
                            minute = 90,
                            type = EventType.FULL_TIME,
                            isHomeTeam = false,
                            playerName = "Arbitre",
                            description = "C'est fini ! Coup de sifflet final au terme d'un match intense. Score final: ${match.homeTeam} ${match.homeScore} - ${match.awayScore} ${match.awayTeam}."
                        )
                    )
                    triggerNotificationForMatch(context, dao, finalMatch, "Fin de match", "Match terminé: ${match.homeTeam} ${match.homeScore} - ${match.awayScore} ${match.awayTeam}")
                    updateStandingsAfterMatch(finalMatch)
                    finalMatch
                } else if (match.minute < 45 && nextMinute >= 45) {
                    // Half Time
                    match.copy(
                        minute = 45,
                        status = MatchStatus.HT,
                        events = match.events + MatchEvent(
                            minute = 45,
                            type = EventType.HALF_TIME,
                            isHomeTeam = false,
                            playerName = "Arbitre",
                            description = "Mi-temps ! Les deux équipes rejoignent les vestiaires sur ce score de ${match.homeScore} - ${match.awayScore}."
                        )
                    )
                } else {
                    // Match is ongoing
                    val status = if (match.status == MatchStatus.HT && Random.nextFloat() < 0.3f) {
                        MatchStatus.LIVE // Second half starts
                    } else {
                        match.status
                    }
                    
                    val updatedMinute = if (status == MatchStatus.LIVE) nextMinute else match.minute
                    
                    // Generate random match event
                    val eventChance = Random.nextFloat()
                    if (eventChance < 0.25f && status == MatchStatus.LIVE) {
                        val isHome = Random.nextBoolean()
                        val eventType = generateRandomEventType()
                        val event = createEventForMatch(match, updatedMinute, eventType, isHome)
                        
                        var newHomeScore = match.homeScore
                        var newAwayScore = match.awayScore
                        if (eventType == EventType.GOAL) {
                            if (isHome) newHomeScore++ else newAwayScore++
                        }

                        val updatedStats = simulateStatIncrease(
                            isHome = isHome,
                            eventType = eventType,
                            homeStats = match.homeStats,
                            awayStats = match.awayStats
                        )

                        val updatedMatch = match.copy(
                            minute = updatedMinute,
                            status = status,
                            homeScore = newHomeScore,
                            awayScore = newAwayScore,
                            events = match.events + event,
                            homeStats = updatedStats.first,
                            awayStats = updatedStats.second
                        )

                        // Trigger notifications & log if favorited
                        handleMatchEventNotification(context, dao, updatedMatch, event)
                        updatedMatch
                    } else {
                        match.copy(minute = updatedMinute, status = status)
                    }
                }
            } else if (match.status == MatchStatus.HT) {
                // Return to play
                if (Random.nextFloat() < 0.4f) {
                    match.copy(
                        status = MatchStatus.LIVE,
                        minute = 46,
                        events = match.events + MatchEvent(
                            minute = 46,
                            type = EventType.KICKOFF,
                            isHomeTeam = true,
                            playerName = "Arbitre",
                            description = "Début de la seconde période ! Le jeu reprend."
                        )
                    )
                } else {
                    match
                }
            } else if (match.status == MatchStatus.UPCOMING && match.dateString == "Aujourd'hui") {
                // Kick off upcoming matches at random times
                if (Random.nextFloat() < 0.05f) {
                    val liveMatch = match.copy(
                        status = MatchStatus.LIVE,
                        minute = 0,
                        events = listOf(
                            MatchEvent(
                                minute = 0,
                                type = EventType.KICKOFF,
                                isHomeTeam = true,
                                playerName = "Arbitre",
                                description = "Coup d'envoi donné par l'arbitre ! C'est parti pour le choc entre ${match.homeTeam} et ${match.awayTeam} !"
                            )
                        )
                    )
                    triggerNotificationForMatch(context, dao, liveMatch, "Coup d'envoi !", "Le match ${match.homeTeam} - ${match.awayTeam} vient de commencer !")
                    liveMatch
                } else {
                    match
                }
            } else {
                match
            }
        }
        
        _matches.value = currentMatches
    }

    private fun generateRandomEventType(): EventType {
        val rand = Random.nextFloat()
        return when {
            rand < 0.35f -> EventType.CHANCE
            rand < 0.65f -> EventType.YELLOW_CARD
            rand < 0.85f -> EventType.GOAL
            rand < 0.92f -> EventType.SUBSTITUTION
            rand < 0.96f -> EventType.VAR_DECISION
            else -> EventType.RED_CARD
        }
    }

    private fun simulateStatIncrease(
        isHome: Boolean,
        eventType: EventType,
        homeStats: TeamStats,
        awayStats: TeamStats
    ): Pair<TeamStats, TeamStats> {
        return if (isHome) {
            val newHome = homeStats.copy(
                shots = homeStats.shots + if (eventType == EventType.GOAL || eventType == EventType.CHANCE) 1 else 0,
                shotsOnTarget = homeStats.shotsOnTarget + if (eventType == EventType.GOAL) 1 else (if (eventType == EventType.CHANCE && Random.nextBoolean()) 1 else 0),
                fouls = homeStats.fouls + if (eventType == EventType.YELLOW_CARD || eventType == EventType.RED_CARD) 1 else 0,
                yellowCards = homeStats.yellowCards + if (eventType == EventType.YELLOW_CARD) 1 else 0,
                redCards = homeStats.redCards + if (eventType == EventType.RED_CARD) 1 else 0,
                possession = Math.min(75, homeStats.possession + Random.nextInt(-2, 3)),
                passes = homeStats.passes + Random.nextInt(5, 15)
            )
            val newAway = awayStats.copy(
                possession = 100 - newHome.possession,
                passes = awayStats.passes + Random.nextInt(5, 12)
            )
            Pair(newHome, newAway)
        } else {
            val newAway = awayStats.copy(
                shots = awayStats.shots + if (eventType == EventType.GOAL || eventType == EventType.CHANCE) 1 else 0,
                shotsOnTarget = awayStats.shotsOnTarget + if (eventType == EventType.GOAL) 1 else (if (eventType == EventType.CHANCE && Random.nextBoolean()) 1 else 0),
                fouls = awayStats.fouls + if (eventType == EventType.YELLOW_CARD || eventType == EventType.RED_CARD) 1 else 0,
                yellowCards = awayStats.yellowCards + if (eventType == EventType.YELLOW_CARD) 1 else 0,
                redCards = awayStats.redCards + if (eventType == EventType.RED_CARD) 1 else 0,
                possession = Math.min(75, awayStats.possession + Random.nextInt(-2, 3)),
                passes = awayStats.passes + Random.nextInt(5, 15)
            )
            val newHome = homeStats.copy(
                possession = 100 - newAway.possession,
                passes = homeStats.passes + Random.nextInt(5, 12)
            )
            Pair(newHome, newAway)
        }
    }

    private fun createEventForMatch(match: Match, minute: Int, type: EventType, isHome: Boolean): MatchEvent {
        val team = if (isHome) match.homeTeam else match.awayTeam
        val opponents = if (isHome) match.awayTeam else match.homeTeam
        val lineup = if (isHome) match.homeLineup else match.awayLineup
        val player = if (lineup.isNotEmpty()) lineup.filter { it.position != "Gk" }.random().name else "Joueur"

        val description = when (type) {
            EventType.GOAL -> {
                val assist = if (lineup.size > 1) lineup.filter { it.name != player && it.position != "Gk" }.random().name else null
                if (assist != null) {
                    "BUT !!! Magnifique réalisation de $player pour $team d'une frappe précise, idéalement servi par $assist ! Le stade exulte !"
                } else {
                    "BUT !!! Quel exploit individuel de $player pour $team ! Il transperce la défense adverse et trompe le gardien d'un tir puissant !"
                }
            }
            EventType.YELLOW_CARD -> {
                "Carton jaune adressé à $player ($team) pour un tacle à retardement non maîtrisé sur un contre adverse."
            }
            EventType.RED_CARD -> {
                "CARTON ROUGE ! $player ($team) est expulsé suite à une faute grossière en tant que dernier défenseur ! Match tendu !"
            }
            EventType.SUBSTITUTION -> {
                val outgoing = if (lineup.isNotEmpty()) lineup.random().name else "Sortant"
                "Changement pour $team : $outgoing cède sa place à $player sous les applaudissements du public."
            }
            EventType.VAR_DECISION -> {
                "Arbitrage vidéo (VAR) en cours... Après vérification, l'arbitre confirme la décision de jeu. Pas de penalty pour $opponents."
            }
            EventType.CHANCE -> {
                val goalie = if ((if (isHome) match.awayLineup else match.homeLineup).isNotEmpty()) {
                    (if (isHome) match.awayLineup else match.homeLineup).firstOrNull { it.position == "Gk" }?.name ?: "le gardien"
                } else "le gardien"
                "Grosse occasion pour $team ! $player déclenche une demi-volée instantanée, mais superbe parade horizontale de $goalie !"
            }
            else -> "Action notable de $player pour $team."
        }

        return MatchEvent(
            minute = minute,
            type = type,
            isHomeTeam = isHome,
            playerName = player,
            description = description
        )
    }

    private suspend fun handleMatchEventNotification(
        context: Context,
        dao: FootballDao,
        match: Match,
        event: MatchEvent
    ) {
        // We trigger alerts if the match is favorited, OR if either of the teams is favorited
        val isMatchFav = dao.isFavorite("match:${match.id}")
        val isHomeFav = dao.isFavorite("team:${match.homeTeam}")
        val isAwayFav = dao.isFavorite("team:${match.awayTeam}")

        if (isMatchFav || isHomeFav || isAwayFav) {
            val title = when (event.type) {
                EventType.GOAL -> "⚽ BUT !!! - ${match.homeTeam} ${match.homeScore}-${match.awayScore} ${match.awayTeam}"
                EventType.RED_CARD -> "🔴 Carton Rouge ! - ${event.playerName} (${if (event.isHomeTeam) match.homeTeam else match.awayTeam})"
                EventType.YELLOW_CARD -> "🟨 Carton Jaune - ${event.playerName}"
                EventType.VAR_DECISION -> "🖥️ VAR - ${match.homeTeam} vs ${match.awayTeam}"
                else -> "📢 Événement - ${match.homeTeam} vs ${match.awayTeam}"
            }

            val body = "Minute ${event.minute}': ${event.description}"

            // Create notification log in Database
            dao.insertNotificationLog(
                NotificationLog(
                    matchId = match.id,
                    title = title,
                    body = body,
                    eventType = event.type.name
                )
            )

            // Trigger actual system notification
            NotificationHelper.sendNotification(context, match.id, title, body)
            Log.d(TAG, "Notification fired for event ${event.type} in match ${match.homeTeam} vs ${match.awayTeam}")
        }
    }

    private suspend fun triggerNotificationForMatch(
        context: Context,
        dao: FootballDao,
        match: Match,
        titleType: String,
        body: String
    ) {
        val isMatchFav = dao.isFavorite("match:${match.id}")
        val isHomeFav = dao.isFavorite("team:${match.homeTeam}")
        val isAwayFav = dao.isFavorite("team:${match.awayTeam}")

        if (isMatchFav || isHomeFav || isAwayFav) {
            val title = "📢 $titleType: ${match.homeTeam} vs ${match.awayTeam}"
            dao.insertNotificationLog(
                NotificationLog(
                    matchId = match.id,
                    title = title,
                    body = body,
                    eventType = "STATE_CHANGE"
                )
            )
            NotificationHelper.sendNotification(context, match.id, title, body)
        }
    }

    private fun updateStandingsAfterMatch(match: Match) {
        val isHomeWin = match.homeScore > match.awayScore
        val isAwayWin = match.awayScore > match.homeScore
        val isDraw = match.homeScore == match.awayScore

        _standings.value = _standings.value.map { team ->
            if (team.leagueName == match.league) {
                when (team.teamName) {
                    match.homeTeam -> {
                        team.copy(
                            played = team.played + 1,
                            won = team.won + if (isHomeWin) 1 else 0,
                            drawn = team.drawn + if (isDraw) 1 else 0,
                            lost = team.lost + if (isAwayWin) 1 else 0,
                            goalsFor = team.goalsFor + match.homeScore,
                            goalsAgainst = team.goalsAgainst + match.awayScore,
                            points = team.points + (if (isHomeWin) 3 else if (isDraw) 1 else 0)
                        )
                    }
                    match.awayTeam -> {
                        team.copy(
                            played = team.played + 1,
                            won = team.won + if (isAwayWin) 1 else 0,
                            drawn = team.drawn + if (isDraw) 1 else 0,
                            lost = team.lost + if (isHomeWin) 1 else 0,
                            goalsFor = team.goalsFor + match.awayScore,
                            goalsAgainst = team.goalsAgainst + match.homeScore,
                            points = team.points + (if (isAwayWin) 3 else if (isDraw) 1 else 0)
                        )
                    }
                    else -> team
                }
            } else {
                team
            }
        }.sortedWith(
            compareByDescending<LeagueStanding> { it.points }
                .thenByDescending { it.goalsFor - it.goalsAgainst }
                .thenByDescending { it.goalsFor }
        ).mapIndexed { index, team ->
            team.copy(rank = index + 1)
        }
    }

    private fun generateInitialMatches(): List<Match> {
        return listOf(
            // --- YESTERDAY'S MATCHES (FT) ---
            Match(
                id = 101,
                league = "Ligue 1 🇫🇷",
                homeTeam = "Paris SG",
                awayTeam = "Marseille",
                homeColor = "#001C55",
                awayColor = "#22A6F2",
                homeScore = 3,
                awayScore = 1,
                status = MatchStatus.FT,
                minute = 90,
                startTime = "21:00",
                dateString = "Hier",
                homeFormation = "4-3-3",
                awayFormation = "4-2-3-1",
                homeLineup = getLineupForTeam("Paris SG", true),
                awayLineup = getLineupForTeam("Marseille", false),
                events = listOf(
                    MatchEvent(12, EventType.GOAL, true, "Bradley Barcola", "Vitinha", "BUT de Barcola ! Superbe débordement de Vitinha qui glisse le ballon dans la course de Barcola pour l'ouverture du score !"),
                    MatchEvent(35, EventType.YELLOW_CARD, false, "Leonardo Balerdi", null, "Carton jaune pour Balerdi pour contestation."),
                    MatchEvent(44, EventType.GOAL, false, "Mason Greenwood", null, "EGALISATION ! Greenwood se défait de Marquinhos et ajuste Donnarumma au premier poteau."),
                    MatchEvent(45, EventType.HALF_TIME, true, "Arbitre", null, "L'arbitre siffle la mi-temps sur ce score de parité."),
                    MatchEvent(65, EventType.GOAL, true, "Ousmane Dembélé", "Achraf Hakimi", "BUT pour Paris ! Centre tendu d'Hakimi repris de la tête plongeante par Dembélé ! 2-1 !"),
                    MatchEvent(78, EventType.RED_CARD, false, "Amine Harit", null, "Expulsion d'Harit pour un tacle à hauteur du genou sur Fabian Ruiz !"),
                    MatchEvent(85, EventType.GOAL, true, "Vitinha", null, "LE BREAK ! Frappe sublime de Vitinha des 25 mètres en pleine lucarne ! 3-1 !")
                )
            ),
            Match(
                id = 102,
                league = "Premier League 🏴󠁧󠁢󠁥󠁮󠁧󠁿",
                homeTeam = "Chelsea",
                awayTeam = "Arsenal",
                homeColor = "#034694",
                awayColor = "#EF0107",
                homeScore = 1,
                awayScore = 2,
                status = MatchStatus.FT,
                minute = 90,
                startTime = "18:30",
                dateString = "Hier",
                homeFormation = "4-2-3-1",
                awayFormation = "4-3-3",
                homeLineup = getLineupForTeam("Chelsea", true),
                awayLineup = getLineupForTeam("Arsenal", false),
                events = listOf(
                    MatchEvent(8, EventType.GOAL, false, "Bukayo Saka", "Martin Odegaard", "BUT d'Arsenal ! Saka trouve la lucarne sur une merveille de passe d'Odegaard !"),
                    MatchEvent(55, EventType.GOAL, true, "Cole Palmer", null, "BUT pour Chelsea ! Cole Palmer transforme froidement le penalty consécutif à une main dans la surface !"),
                    MatchEvent(89, EventType.GOAL, false, "Gabriel Martinelli", "Declan Rice", "BUT DE LA VICTOIRE ! Martinelli coupe un centre au second poteau et crucifie Robert Sanchez !")
                )
            ),

            // --- TODAY'S MATCHES (LIVE / UPCOMING) ---
            Match(
                id = 201,
                league = "Champions League 🏆",
                homeTeam = "Real Madrid",
                awayTeam = "Bayern Munich",
                homeColor = "#F5F5F5",
                awayColor = "#DC052D",
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.LIVE,
                minute = 18,
                startTime = "21:00",
                dateString = "Aujourd'hui",
                homeFormation = "4-3-3",
                awayFormation = "4-2-3-1",
                homeLineup = getLineupForTeam("Real Madrid", true),
                awayLineup = getLineupForTeam("Bayern Munich", false),
                events = listOf(
                    MatchEvent(0, EventType.KICKOFF, true, "Arbitre", null, "Le coup d'envoi est donné par l'arbitre au Stade Santiago Bernabéu sous une ambiance assourdissante !"),
                    MatchEvent(12, EventType.CHANCE, false, "Harry Kane", null, "Énorme arrêt de Courtois ! Tête puissante de Kane repoussée in extremis en corner !")
                )
            ),
            Match(
                id = 202,
                league = "Ligue 1 🇫🇷",
                homeTeam = "Lyon",
                awayTeam = "Monaco",
                homeColor = "#002F6C",
                awayColor = "#E60000",
                homeScore = 1,
                awayScore = 2,
                status = MatchStatus.LIVE,
                minute = 62,
                startTime = "19:00",
                dateString = "Aujourd'hui",
                homeFormation = "4-3-3",
                awayFormation = "4-4-2",
                homeLineup = getLineupForTeam("Lyon", true),
                awayLineup = getLineupForTeam("Monaco", false),
                events = listOf(
                    MatchEvent(0, EventType.KICKOFF, true, "Arbitre", null, "C'est parti à l'OL Stadium !"),
                    MatchEvent(14, EventType.GOAL, false, "Folarin Balogun", "Denis Zakaria", "BUT pour Monaco ! Balogun bat Lucas Perri d'un superbe ballon piqué !"),
                    MatchEvent(33, EventType.GOAL, true, "Alexandre Lacazette", "Corentin Tolisso", "EGALISATION DE LYON ! Le Général Lacazette catapulte le ballon au fond des filets sur un centre de Tolisso !"),
                    MatchEvent(45, EventType.HALF_TIME, true, "Arbitre", null, "Mi-temps haletante sur le score de 1-1 !"),
                    MatchEvent(46, EventType.KICKOFF, true, "Arbitre", null, "Reprise de la seconde période !"),
                    MatchEvent(51, EventType.GOAL, false, "Aleksandr Golovin", null, "BUT MAGNIFIQUE ! Coup franc chirurgical de Golovin qui contourne le mur et finit au ras du poteau !")
                )
            ),
            Match(
                id = 203,
                league = "Premier League 🏴󠁧󠁢󠁥󠁮󠁧󠁿",
                homeTeam = "Liverpool",
                awayTeam = "Man City",
                homeColor = "#C8102E",
                awayColor = "#6CABDD",
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.UPCOMING,
                minute = 0,
                startTime = "21:30",
                dateString = "Aujourd'hui",
                homeFormation = "4-3-3",
                awayFormation = "4-1-4-1",
                homeLineup = getLineupForTeam("Liverpool", true),
                awayLineup = getLineupForTeam("Man City", false),
                events = emptyList()
            ),

            // --- TOMORROW'S MATCHES (UPCOMING) ---
            Match(
                id = 301,
                league = "La Liga 🇪🇸",
                homeTeam = "Barcelona",
                awayTeam = "Atletico Madrid",
                homeColor = "#004D98",
                awayColor = "#CB3524",
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.UPCOMING,
                minute = 0,
                startTime = "16:15",
                dateString = "Demain",
                homeFormation = "4-3-3",
                awayFormation = "5-3-2",
                homeLineup = getLineupForTeam("Barcelona", true),
                awayLineup = getLineupForTeam("Atletico Madrid", false)
            ),
            Match(
                id = 302,
                league = "Ligue 1 🇫🇷",
                homeTeam = "Lens",
                awayTeam = "Lille",
                homeColor = "#FEC322",
                awayColor = "#E01E13",
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.UPCOMING,
                minute = 0,
                startTime = "20:45",
                dateString = "Demain",
                homeFormation = "3-4-2-1",
                awayFormation = "4-2-3-1",
                homeLineup = getLineupForTeam("Lens", true),
                awayLineup = getLineupForTeam("Lille", false)
            )
        )
    }

    private fun generateInitialStandings(): List<LeagueStanding> {
        val ligue1 = listOf(
            LeagueStanding(1, "Paris SG", "#001C55", 30, 21, 6, 3, 68, 24, 69, "Ligue 1 🇫🇷"),
            LeagueStanding(2, "Monaco", "#E60000", 30, 18, 5, 7, 54, 38, 59, "Ligue 1 🇫🇷"),
            LeagueStanding(3, "Marseille", "#22A6F2", 30, 15, 8, 7, 49, 32, 53, "Ligue 1 🇫🇷"),
            LeagueStanding(4, "Lille", "#E01E13", 30, 14, 10, 6, 45, 29, 52, "Ligue 1 🇫🇷"),
            LeagueStanding(5, "Lyon", "#002F6C", 30, 13, 6, 11, 42, 44, 45, "Ligue 1 🇫🇷"),
            LeagueStanding(6, "Lens", "#FEC322", 30, 12, 8, 10, 38, 32, 44, "Ligue 1 🇫🇷")
        )

        val premierLeague = listOf(
            LeagueStanding(1, "Arsenal", "#EF0107", 31, 22, 5, 4, 75, 26, 71, "Premier League 🏴󠁧󠁢󠁥󠁮󠁧󠁿"),
            LeagueStanding(2, "Man City", "#6CABDD", 31, 21, 7, 3, 79, 31, 70, "Premier League 🏴󠁧󠁢󠁥󠁮󠁧󠁿"),
            LeagueStanding(3, "Liverpool", "#C8102E", 31, 21, 6, 4, 72, 30, 69, "Premier League 🏴󠁧󠁢󠁥󠁮󠁧󠁿"),
            LeagueStanding(4, "Chelsea", "#034694", 31, 14, 9, 8, 61, 52, 51, "Premier League 🏴󠁧󠁢󠁥󠁮󠁧󠁿")
        )

        val laLiga = listOf(
            LeagueStanding(1, "Real Madrid", "#F5F5F5", 31, 24, 6, 1, 66, 20, 78, "La Liga 🇪🇸"),
            LeagueStanding(2, "Barcelona", "#004D98", 31, 21, 7, 3, 62, 32, 70, "La Liga 🇪🇸"),
            LeagueStanding(3, "Atletico Madrid", "#CB3524", 31, 19, 4, 8, 56, 35, 61, "La Liga 🇪🇸")
        )

        return ligue1 + premierLeague + laLiga
    }

    private fun getLineupForTeam(teamName: String, isHome: Boolean): List<Player> {
        val yOffset = if (isHome) 0f else 0.5f // simple coordinate placement
        return when (teamName) {
            "Paris SG" -> listOf(
                Player("Donnarumma", 1, "Gk", 0.5f, if (isHome) 0.05f else 0.95f),
                Player("Hakimi", 2, "Def", 0.15f, if (isHome) 0.2f else 0.8f),
                Player("Marquinhos", 4, "Def", 0.38f, if (isHome) 0.15f else 0.85f),
                Player("Pacho", 51, "Def", 0.62f, if (isHome) 0.15f else 0.85f),
                Player("Nuno Mendes", 25, "Def", 0.85f, if (isHome) 0.2f else 0.8f),
                Player("Vitinha", 17, "Mid", 0.5f, if (isHome) 0.35f else 0.65f),
                Player("Zaïre-Emery", 33, "Mid", 0.3f, if (isHome) 0.4f else 0.6f),
                Player("Joao Neves", 87, "Mid", 0.7f, if (isHome) 0.4f else 0.6f),
                Player("Dembélé", 10, "Fwd", 0.15f, if (isHome) 0.6f else 0.4f),
                Player("Asensio", 11, "Fwd", 0.5f, if (isHome) 0.65f else 0.35f),
                Player("Barcola", 29, "Fwd", 0.85f, if (isHome) 0.6f else 0.4f)
            )
            "Real Madrid" -> listOf(
                Player("Courtois", 1, "Gk", 0.5f, if (isHome) 0.05f else 0.95f),
                Player("Carvajal", 2, "Def", 0.15f, if (isHome) 0.2f else 0.8f),
                Player("Militao", 3, "Def", 0.38f, if (isHome) 0.15f else 0.85f),
                Player("Rüdiger", 22, "Def", 0.62f, if (isHome) 0.15f else 0.85f),
                Player("Mendy", 23, "Def", 0.85f, if (isHome) 0.2f else 0.8f),
                Player("Tchouaméni", 14, "Mid", 0.5f, if (isHome) 0.35f else 0.65f),
                Player("Valverde", 8, "Mid", 0.3f, if (isHome) 0.45f else 0.55f),
                Player("Bellingham", 5, "Mid", 0.7f, if (isHome) 0.45f else 0.55f),
                Player("Rodrygo", 11, "Fwd", 0.15f, if (isHome) 0.65f else 0.35f),
                Player("Mbappé", 9, "Fwd", 0.5f, if (isHome) 0.7f else 0.3f),
                Player("Vinícius Jr", 7, "Fwd", 0.85f, if (isHome) 0.65f else 0.35f)
            )
            "Bayern Munich" -> listOf(
                Player("Neuer", 1, "Gk", 0.5f, if (isHome) 0.05f else 0.95f),
                Player("Kimmich", 6, "Def", 0.15f, if (isHome) 0.2f else 0.8f),
                Player("Upamecano", 2, "Def", 0.38f, if (isHome) 0.15f else 0.85f),
                Player("Kim", 3, "Def", 0.62f, if (isHome) 0.15f else 0.85f),
                Player("Davies", 19, "Def", 0.85f, if (isHome) 0.2f else 0.8f),
                Player("Palhinha", 16, "Mid", 0.35f, if (isHome) 0.35f else 0.65f),
                Player("Pavlovic", 45, "Mid", 0.65f, if (isHome) 0.35f else 0.65f),
                Player("Olise", 17, "Mid", 0.2f, if (isHome) 0.5f else 0.5f),
                Player("Musiala", 42, "Mid", 0.5f, if (isHome) 0.55f else 0.45f),
                Player("Gnabry", 7, "Mid", 0.8f, if (isHome) 0.5f else 0.5f),
                Player("Harry Kane", 9, "Fwd", 0.5f, if (isHome) 0.7f else 0.3f)
            )
            "Arsenal" -> listOf(
                Player("Raya", 22, "Gk", 0.5f, if (isHome) 0.05f else 0.95f),
                Player("White", 4, "Def", 0.15f, if (isHome) 0.2f else 0.8f),
                Player("Saliba", 2, "Def", 0.38f, if (isHome) 0.15f else 0.85f),
                Player("Gabriel", 6, "Def", 0.62f, if (isHome) 0.15f else 0.85f),
                Player("Timber", 12, "Def", 0.85f, if (isHome) 0.2f else 0.8f),
                Player("Partey", 5, "Mid", 0.5f, if (isHome) 0.35f else 0.65f),
                Player("Rice", 41, "Mid", 0.3f, if (isHome) 0.45f else 0.55f),
                Player("Odegaard", 8, "Mid", 0.7f, if (isHome) 0.45f else 0.55f),
                Player("Saka", 7, "Fwd", 0.15f, if (isHome) 0.65f else 0.35f),
                Player("Havertz", 29, "Fwd", 0.5f, if (isHome) 0.7f else 0.3f),
                Player("Martinelli", 11, "Fwd", 0.85f, if (isHome) 0.65f else 0.35f)
            )
            else -> listOf(
                Player("Gardien", 1, "Gk", 0.5f, if (isHome) 0.05f else 0.95f),
                Player("Défenseur G", 3, "Def", 0.25f, if (isHome) 0.2f else 0.8f),
                Player("Défenseur D", 4, "Def", 0.75f, if (isHome) 0.2f else 0.8f),
                Player("Milieu C", 8, "Mid", 0.5f, if (isHome) 0.4f else 0.6f),
                Player("Attaquant", 9, "Fwd", 0.5f, if (isHome) 0.65f else 0.35f)
            )
        }
    }
}
