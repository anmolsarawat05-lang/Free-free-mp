package com.example.game

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class GameRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("survival_strike_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val statsAdapter = moshi.adapter(PlayerStats::class.java)
    private val missionListAdapter = moshi.adapter<List<DailyMission>>(
        Types.newParameterizedType(List::class.java, DailyMission::class.java)
    )
    private val achievementListAdapter = moshi.adapter<List<Achievement>>(
        Types.newParameterizedType(List::class.java, Achievement::class.java)
    )

    fun getStats(): PlayerStats {
        val statsJson = prefs.getString("player_stats", null)
        return if (statsJson != null) {
            statsAdapter.fromJson(statsJson) ?: PlayerStats()
        } else {
            val defaultStats = PlayerStats()
            saveStats(defaultStats)
            defaultStats
        }
    }

    fun saveStats(stats: PlayerStats) {
        prefs.edit().putString("player_stats", statsAdapter.toJson(stats)).apply()
    }

    fun getDailyMissions(): List<DailyMission> {
        val missionsJson = prefs.getString("daily_missions", null)
        return if (missionsJson != null) {
            missionListAdapter.fromJson(missionsJson) ?: createDefaultMissions()
        } else {
            val defaults = createDefaultMissions()
            saveDailyMissions(defaults)
            defaults
        }
    }

    fun saveDailyMissions(missions: List<DailyMission>) {
        prefs.edit().putString("daily_missions", missionListAdapter.toJson(missions)).apply()
    }

    fun getAchievements(): List<Achievement> {
        val achievementsJson = prefs.getString("achievements", null)
        return if (achievementsJson != null) {
            achievementListAdapter.fromJson(achievementsJson) ?: createDefaultAchievements()
        } else {
            val defaults = createDefaultAchievements()
            saveAchievements(defaults)
            defaults
        }
    }

    fun saveAchievements(achievements: List<Achievement>) {
        prefs.edit().putString("achievements", achievementListAdapter.toJson(achievements)).apply()
    }

    private fun createDefaultMissions(): List<DailyMission> {
        return listOf(
            DailyMission("m1", "Survive for a total of 5 minutes", 300, 0, 150),
            DailyMission("m2", "Eliminate 5 opponents in matches", 5, 0, 250),
            DailyMission("m3", "Secure a BOOYAH! (#1 victory)", 1, 0, 500),
            DailyMission("m4", "Travel 2,000 meters in vehicles", 2000, 0, 200),
            DailyMission("m5", "Deploy 3 Gloo Walls in a single match", 3, 0, 150)
        )
    }

    private fun createDefaultAchievements(): List<Achievement> {
        return listOf(
            Achievement("a1", "First Blood", "Eliminate your first opponent", 1, 100, "sword"),
            Achievement("a2", "Survival Expert", "Survive 15 matches", 15, 500, "shield"),
            Achievement("a3", "Gloo Architect", "Deploy 20 total Gloo Walls", 20, 300, "build"),
            Achievement("a4", "Vehicle Rampage", "Defeat 3 enemies with vehicle roadkills", 3, 400, "directions_car"),
            Achievement("a5", "Apex Survivor", "Earn 5 BOOYAHs (Rank #1)", 5, 1000, "emoji_events")
        )
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
    }
}
