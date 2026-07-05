package com.example.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

enum class GameScreen {
    SPLASH, LOBBY, CUSTOMIZE, STORE, BATTLEPASS, MATCH, SUMMARY
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)

    // Player Profile state
    private val _playerStats = MutableStateFlow(repository.getStats())
    val playerStats: StateFlow<PlayerStats> = _playerStats.asStateFlow()

    // Daily missions and achievements lists
    var dailyMissions by mutableStateOf(repository.getDailyMissions())
        private set
    var achievements by mutableStateOf(repository.getAchievements())
        private set

    // Active Screen state
    var currentScreen by mutableStateOf(GameScreen.SPLASH)

    // Game Simulation Engine instance
    val engine = GameEngine()

    // Interactive Game Loops & State
    private var gameLoopJob: Job? = null
    var isMatchRunning by mutableStateOf(false)
    var alivePlayersCount by mutableStateOf(50)
        private set
    var userKills by mutableStateOf(0)
        private set
    var remainingGlooWalls by mutableStateOf(1)
        private set
    var remainingMedkits by mutableStateOf(2)
        private set

    // HUD inputs
    var joystickX by mutableStateOf(0f)
    var joystickY by mutableStateOf(0f)
    var isSprinting by mutableStateOf(false)
    var isCrouching by mutableStateOf(false)
    var isCrawling by mutableStateOf(false)
    var isFiring by mutableStateOf(false)
    var fireAngle by mutableStateOf(0f)

    // Selected characters/outfits
    var selectedCharacterId by mutableStateOf(CharacterId.AERO)
    var activeSkinColor by mutableStateOf(Color(0xFF00E676)) // futuristic bright neon

    // Performance settings
    var graphicsSetting by mutableStateOf("Medium") // Low, Medium, High
    var weatherActive by mutableStateOf(true)
    var dayNightCycleTick by mutableStateOf(300f) // Day light ratio

    // Active screen alerts / toast
    var matchToastMessage by mutableStateOf("")
    private var toastJob: Job? = null

    init {
        // Load initial values from SharedPreferences
        val stats = _playerStats.value
        selectedCharacterId = CharacterId.values().firstOrNull { it.idName == stats.equippedCharacter } ?: CharacterId.AERO
        activeSkinColor = when (stats.activeWeaponSkin) {
            "neon_orange" -> Color(0xFFFF6D00)
            "plasma_cyan" -> Color(0xFF00E5FF)
            "toxic_green" -> Color(0xFF00E676)
            else -> Color(0xFF757575)
        }
    }

    fun setScreen(screen: GameScreen) {
        currentScreen = screen
        GameSound.playClick()
    }

    fun triggerToast(msg: String) {
        matchToastMessage = msg
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(3000)
            matchToastMessage = ""
        }
    }

    // Matchmaking & Launch Game
    fun startMatchmaking() {
        setScreen(GameScreen.MATCH)
        joystickX = 0f
        joystickY = 0f
        isSprinting = false
        isCrouching = false
        isCrawling = false
        isFiring = false
        fireAngle = 0f
        userKills = 0
        alivePlayersCount = 50

        // Start engine match
        engine.startNewMatch(selectedCharacterId, activeSkinColor)
        remainingGlooWalls = engine.players.first { it.id == "user" }.totalGlooWalls
        remainingMedkits = engine.players.first { it.id == "user" }.totalMedkits

        // Cancel existing and run the Game Loop at 25 FPS (40ms ticks)
        isMatchRunning = true
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (isMatchRunning) {
                engine.tick(
                    joystickX,
                    joystickY,
                    isSprinting,
                    isCrouching,
                    isCrawling,
                    isFiring,
                    fireAngle
                )

                // Sync counts and variables back to Compose UI thread
                val user = engine.players.firstOrNull { it.id == "user" }
                if (user != null) {
                    alivePlayersCount = engine.players.count { it.isAlive }
                    userKills = user.kills
                    remainingGlooWalls = user.totalGlooWalls
                    remainingMedkits = user.totalMedkits
                    
                    // Increment Day Night cycle ratio gently
                    dayNightCycleTick = (dayNightCycleTick + 0.1f) % 360f

                    // If user is eliminated or match ends, stop loop and go to Summary
                    if (!user.isAlive || engine.matchFinished) {
                        isMatchRunning = false
                        viewModelScope.launch(Dispatchers.Main) {
                            saveMatchResultsAndProgression(user)
                            setScreen(GameScreen.SUMMARY)
                        }
                    }
                }
                delay(40L)
            }
        }
    }

    fun ejectFromPlane() {
        val user = engine.players.firstOrNull { it.id == "user" }
        if (user != null && user.isParachuting && user.parachuteAltitude >= 1000f) {
            user.parachuteAltitude = 999f // ejects
            GameSound.playClick()
        }
    }

    fun deployUserGlooWall() {
        val user = engine.players.firstOrNull { it.id == "user" }
        if (user != null && user.isAlive && !user.isParachuting) {
            if (user.totalGlooWalls > 0) {
                engine.deployGlooWall("user", fireAngle)
                triggerToast("Gloo Wall deployed!")
            } else {
                triggerToast("Out of Gloo Devices!")
            }
        }
    }

    fun reloadUserWeapon() {
        val user = engine.players.firstOrNull { it.id == "user" }
        if (user != null && user.isAlive && !user.isParachuting) {
            val success = engine.reloadActiveWeapon(user)
            if (success) {
                triggerToast("Reloading weapon...")
            }
        }
    }

    fun triggerUserSkill() {
        val user = engine.players.firstOrNull { it.id == "user" }
        if (user != null && user.isAlive && !user.isParachuting) {
            val success = engine.triggerActiveSkill(user)
            if (success) {
                triggerToast("Skill Activated: ${selectedCharacterId.skillName}!")
            } else {
                triggerToast("Skill is on cooldown!")
            }
        }
    }

    fun applyUserMedkit() {
        val user = engine.players.firstOrNull { it.id == "user" }
        if (user != null && user.isAlive && !user.isParachuting) {
            val success = engine.applyUserMedkit()
            if (success) {
                triggerToast("Applying Medkit (+40 HP)")
            } else if (user.totalMedkits <= 0) {
                triggerToast("No Medkits in backpack!")
            } else {
                triggerToast("Health already full!")
            }
        }
    }

    fun toggleVehicleEntry() {
        val user = engine.players.firstOrNull { it.id == "user" } ?: return
        if (!user.isAlive || user.isParachuting) return

        if (user.drivingVehicleId != null) {
            // Exit vehicle
            val vehicle = engine.vehicles.firstOrNull { it.id == user.drivingVehicleId }
            if (vehicle != null) {
                vehicle.isBeingDriven = false
                vehicle.driverId = null
                // place user slightly adjacent
                user.x = (vehicle.x + 35f).coerceIn(10f, engine.mapWidth - 10f)
                user.y = (vehicle.y + 35f).coerceIn(10f, engine.mapHeight - 10f)
            }
            user.drivingVehicleId = null
            triggerToast("Exited vehicle.")
            GameSound.playClick()
        } else {
            // Find nearest empty vehicle
            val nearestVehicle = engine.vehicles
                .filter { !it.isBeingDriven }
                .minByOrNull { dist(user.x, user.y, it.x, it.y) }
            
            if (nearestVehicle != null && dist(user.x, user.y, nearestVehicle.x, nearestVehicle.y) < 50f) {
                nearestVehicle.isBeingDriven = true
                nearestVehicle.driverId = "user"
                user.drivingVehicleId = nearestVehicle.id
                user.x = nearestVehicle.x
                user.y = nearestVehicle.y
                triggerToast("Driving ${nearestVehicle.type.displayName}!")
                GameSound.playClick()
            } else {
                triggerToast("No drivable vehicle nearby.")
            }
        }
    }

    private fun saveMatchResultsAndProgression(user: Player) {
        val currentStats = _playerStats.value
        currentStats.totalMatches++
        currentStats.totalKills += user.kills

        val placement = engine.userPlacement
        val isVictory = placement == 1
        if (isVictory) {
            currentStats.booyahs++
        }

        // Award Gold and Rank points based on placement and kills
        val goldEarned = (50 - placement) * 10 + user.kills * 25 + (if (isVictory) 200 else 0)
        val rpEarned = (25 - placement) + user.kills * 8 + (if (isVictory) 50 else 0)
        val xpEarned = (50 - placement) * 5 + user.kills * 15 + (if (isVictory) 100 else 0)

        currentStats.gold += goldEarned
        currentStats.rankPoints = max(1000, currentStats.rankPoints + rpEarned)
        currentStats.battlePassXP += xpEarned

        // Check Battle Pass level up (e.g. 500 XP per level)
        if (currentStats.battlePassXP >= 500) {
            currentStats.battlePassLevel += currentStats.battlePassXP / 500
            currentStats.battlePassXP %= 500
        }

        // Save back
        repository.saveStats(currentStats)
        _playerStats.value = currentStats

        // Update Daily Missions progress
        val updatedMissions = dailyMissions.map { mission ->
            when (mission.id) {
                "m1" -> {
                    // Survive time (ticks to seconds)
                    val matchSec = engine.matchTicks / 20
                    mission.copy(progress = min(mission.target, mission.progress + matchSec.toInt()))
                }
                "m2" -> {
                    mission.copy(progress = min(mission.target, mission.progress + user.kills))
                }
                "m3" -> {
                    if (isVictory) mission.copy(progress = min(mission.target, mission.progress + 1)) else mission
                }
                "m4" -> {
                    // Vehicle travel simulation
                    val drove = if (user.drivingVehicleId != null) (engine.matchTicks * 0.4).toInt() else 0
                    mission.copy(progress = min(mission.target, mission.progress + drove))
                }
                "m5" -> {
                    // Gloo walls used
                    val used = 3 - user.totalGlooWalls
                    mission.copy(progress = min(mission.target, mission.progress + max(0, used)))
                }
                else -> mission
            }
        }
        dailyMissions = updatedMissions
        repository.saveDailyMissions(updatedMissions)

        // Update achievements progress
        val updatedAchievements = achievements.map { ach ->
            when (ach.id) {
                "a1" -> {
                    if (user.kills >= 1) ach.copy(progress = min(ach.target, ach.progress + 1)) else ach
                }
                "a2" -> {
                    ach.copy(progress = min(ach.target, ach.progress + 1))
                }
                "a3" -> {
                    val used = 3 - user.totalGlooWalls
                    ach.copy(progress = min(ach.target, ach.progress + max(0, used)))
                }
                "a4" -> {
                    // Simulated roadkills or vehicle damage
                    if (user.drivingVehicleId != null && user.kills > 0) ach.copy(progress = min(ach.target, ach.progress + 1)) else ach
                }
                "a5" -> {
                    if (isVictory) ach.copy(progress = min(ach.target, ach.progress + 1)) else ach
                }
                else -> ach
            }
        }
        achievements = updatedAchievements
        repository.saveAchievements(updatedAchievements)
    }

    // Purchase character
    fun unlockCharacter(character: CharacterId, cost: Int): Boolean {
        val stats = _playerStats.value
        val unlockedList = stats.unlockedCharacters.split(",")
        if (unlockedList.contains(character.idName)) return true // already unlocked

        if (stats.gold >= cost) {
            stats.gold -= cost
            stats.unlockedCharacters = "${stats.unlockedCharacters},${character.idName}"
            repository.saveStats(stats)
            _playerStats.value = stats
            GameSound.playBooyah()
            return true
        }
        return false
    }

    fun equipCharacter(character: CharacterId) {
        val stats = _playerStats.value
        val unlockedList = stats.unlockedCharacters.split(",")
        if (unlockedList.contains(character.idName)) {
            stats.equippedCharacter = character.idName
            repository.saveStats(stats)
            _playerStats.value = stats
            selectedCharacterId = character
            GameSound.playClick()
        }
    }

    // Buy / Equip Weapon Skins
    fun purchaseWeaponSkin(skinCode: String, color: Color, cost: Int): Boolean {
        val stats = _playerStats.value
        val ownedSkins = stats.ownedSkins.split(",")
        if (ownedSkins.contains(skinCode)) {
            // Equip already owned
            stats.activeWeaponSkin = skinCode
            repository.saveStats(stats)
            _playerStats.value = stats
            activeSkinColor = color
            GameSound.playClick()
            return true
        }

        if (stats.diamonds >= cost) {
            stats.diamonds -= cost
            stats.ownedSkins = "${stats.ownedSkins},$skinCode"
            stats.activeWeaponSkin = skinCode
            repository.saveStats(stats)
            _playerStats.value = stats
            activeSkinColor = color
            GameSound.playBooyah()
            return true
        }
        return false
    }

    fun claimMissionReward(missionId: String) {
        val stats = _playerStats.value
        val list = dailyMissions.map { mission ->
            if (mission.id == missionId && mission.progress >= mission.target && !mission.isClaimed) {
                stats.gold += mission.rewardGold
                repository.saveStats(stats)
                _playerStats.value = stats
                GameSound.playBooyah()
                mission.copy(isClaimed = true)
            } else {
                mission
            }
        }
        dailyMissions = list
        repository.saveDailyMissions(list)
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx*dx + dy*dy)
    }
}
