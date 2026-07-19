package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class MatchStatus {
    UPCOMING,
    LIVE,
    HT, // Half-Time
    FT  // Full-Time
}

enum class EventType {
    GOAL,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION,
    VAR_DECISION,
    KICKOFF,
    HALF_TIME,
    FULL_TIME,
    PENALTY_MISSED,
    CHANCE
}

data class MatchEvent(
    val minute: Int,
    val type: EventType,
    val isHomeTeam: Boolean,
    val playerName: String,
    val assistName: String? = null,
    val description: String
) : Serializable

data class Player(
    val name: String,
    val number: Int,
    val position: String, // "Gk", "Def", "Mid", "Fwd"
    val x: Float, // Tactician coordinate on field (0..1)
    val y: Float  // Tactician coordinate on field (0..1)
) : Serializable

data class TeamStats(
    val shots: Int,
    val shotsOnTarget: Int,
    val possession: Int, // e.g. 55 for 55%
    val passes: Int,
    val passAccuracy: Int, // percentage
    val fouls: Int,
    val yellowCards: Int,
    val redCards: Int,
    val corners: Int,
    val offsides: Int
) : Serializable {
    companion object {
        fun defaultHome() = TeamStats(12, 5, 52, 450, 85, 10, 1, 0, 4, 2)
        fun defaultAway() = TeamStats(10, 4, 48, 410, 82, 12, 2, 0, 3, 1)
    }
}

data class Match(
    val id: Int,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeColor: String, // Hex color for team primary (e.g., "#0000FF")
    val awayColor: String, // Hex color for team primary
    val homeScore: Int,
    val awayScore: Int,
    val status: MatchStatus,
    val minute: Int,
    val startTime: String, // e.g., "21:00"
    val dateString: String, // "Hier", "Aujourd'hui", "Demain"
    val events: List<MatchEvent> = emptyList(),
    val homeStats: TeamStats = TeamStats.defaultHome(),
    val awayStats: TeamStats = TeamStats.defaultAway(),
    val homeLineup: List<Player> = emptyList(),
    val awayLineup: List<Player> = emptyList(),
    val homeFormation: String = "4-3-3",
    val awayFormation: String = "4-2-3-1"
) : Serializable

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val id: String, // e.g. "team:PSG" or "match:42"
    val type: String, // "TEAM" or "MATCH"
    val name: String, // team or match name
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val eventType: String // "GOAL", "CARD", etc.
)

data class LeagueStanding(
    val rank: Int,
    val teamName: String,
    val teamColor: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int,
    val leagueName: String
)
