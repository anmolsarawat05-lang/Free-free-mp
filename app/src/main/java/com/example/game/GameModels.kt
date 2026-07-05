package com.example.game

import androidx.compose.ui.graphics.Color

enum class CharacterId(
    val idName: String,
    val displayName: String,
    val skillName: String,
    val skillDesc: String,
    val passiveName: String,
    val passiveDesc: String
) {
    AERO(
        "aero", 
        "AERO (Speedster)", 
        "Adrenaline Rush", 
        "+50% Speed & +30% Fire Rate for 5s. Cooldown: 15s", 
        "Windrunner", 
        "Permanent +15% move speed & 30% faster bullet travel velocity."
    ),
    SHIELD(
        "shield", 
        "SHIELD (Defender)", 
        "Gloo Bastion", 
        "Deploys protective shield bubble & Gloo Walls for 6s. Cooldown: 20s", 
        "Titan Armor", 
        "Starts match with a Lvl 1 Vest. Shield blocks 15% more incoming damage."
    ),
    HUNTER(
        "hunter", 
        "HUNTER (Scout)", 
        "Eagle Vision", 
        "Reveals all enemies on screen and gains +30% deal damage for 6s. Cooldown: 18s", 
        "Tracker", 
        "Enemies leave visible glowing footprints. Weapon range is increased +25%."
    ),
    MEDIC(
        "medic", 
        "MEDIC (Support)", 
        "Healing Aura", 
        "Regens +15 HP/s and grants +35% damage reduction for 5s. Cooldown: 15s", 
        "Nanobot Infusion", 
        "Regens +2 HP/1.5s out of combat. Gains +30 Shield on critical health (under 35 HP)."
    )
}

enum class WeaponCategory(val displayName: String) {
    AR("Assault Rifle"),
    SMG("SMG"),
    SHOTGUN("Shotgun"),
    SNIPER("Sniper Rifle"),
    PISTOL("Pistol"),
    MELEE("Melee")
}

data class WeaponData(
    val name: String,
    val category: WeaponCategory,
    val damage: Float,
    val range: Float,
    val fireRateMs: Long,
    val recoilIntensity: Float,
    val maxAmmo: Int,
    val currentAmmo: Int = maxAmmo,
    val color: Color = Color.White
) {
    companion object {
        fun createAR() = WeaponData("M4A1", WeaponCategory.AR, 32f, 350f, 150L, 1.8f, 30)
        fun createSMG() = WeaponData("MP40", WeaponCategory.SMG, 22f, 180f, 90L, 1.2f, 40)
        fun createShotgun() = WeaponData("M1887", WeaponCategory.SHOTGUN, 75f, 90f, 600L, 5.5f, 2)
        fun createSniper() = WeaponData("AWM", WeaponCategory.SNIPER, 120f, 650f, 1500L, 10.0f, 5)
        fun createPistol() = WeaponData("USP", WeaponCategory.PISTOL, 18f, 120f, 250L, 0.8f, 12)
        fun createMelee() = WeaponData("Pan", WeaponCategory.MELEE, 25f, 30f, 400L, 0.0f, 0)
    }
}

enum class LootType {
    WEAPON, AMMO, MEDKIT, ARMOR_VEST, ARMOR_HELMET, GLOO_WALL
}

data class LootItem(
    val id: String,
    val x: Float,
    val y: Float,
    val name: String,
    val type: LootType,
    val quantity: Int = 1,
    val weaponData: WeaponData? = null,
    val armorLevel: Int = 1
)

enum class VehicleType(val displayName: String, val speedMultiplier: Float, val color: Color) {
    SPORTS_CAR("Sports Car", 2.5f, Color(0xFFFF3D00)),
    MOTORCYCLE("Motorcycle", 3.0f, Color(0xFFFFEA00)),
    JEEP("Jeep", 1.8f, Color(0xFF4CAF50))
}

data class Vehicle(
    val id: String,
    var x: Float,
    var y: Float,
    val type: VehicleType,
    var speed: Float = 0f,
    var angle: Float = 0f,
    var health: Float = 100f,
    var isBeingDriven: Boolean = false,
    var driverId: String? = null
)

data class GlooWall(
    val id: String,
    val x: Float,
    val y: Float,
    val angle: Float,
    val creatorId: String,
    var health: Float = 150f,
    var remainingTicks: Int = 300 // about 15 seconds at 20fps
)

data class Playzone(
    var currentX: Float = 500f,
    var currentY: Float = 500f,
    var currentRadius: Float = 600f,
    var targetX: Float = 500f,
    var targetY: Float = 500f,
    var targetRadius: Float = 600f,
    var isShrinking: Boolean = false,
    var secondsUntilShrink: Int = 45,
    val damagePerSecond: Float = 4f
)

data class Player(
    val id: String,
    val name: String,
    val isBot: Boolean,
    var x: Float,
    var y: Float,
    var health: Float = 100f,
    var shield: Float = 0f, // From Armor
    var kills: Int = 0,
    var isAlive: Boolean = true,
    var activeWeaponIndex: Int = 0,
    var weapons: MutableList<WeaponData> = mutableListOf(WeaponData.createMelee()),
    var totalMedkits: Int = 2,
    var totalGlooWalls: Int = 1,
    var totalAmmoAR: Int = 60,
    var totalAmmoSMG: Int = 80,
    var totalAmmoShotgun: Int = 10,
    var totalAmmoSniper: Int = 15,
    var statusText: String = "Alive",
    var angle: Float = 0f,
    var isSprinting: Boolean = false,
    var isCrouching: Boolean = false,
    var isCrawling: Boolean = false,
    
    // Skill state
    var skillCooldownRemaining: Float = 0f,
    var skillActiveRemaining: Float = 0f,
    
    // AI navigation/behavior state
    var targetX: Float = 500f,
    var targetY: Float = 500f,
    var lastShotTime: Long = 0L,
    var lastDecisionTime: Long = 0L,
    var currentTargetPlayerId: String? = null,
    var drivingVehicleId: String? = null,
    var isParachuting: Boolean = true,
    var parachuteAltitude: Float = 1000f, // start altitude
    
    // Gear levels
    var levelVest: Int = 0,
    var levelHelmet: Int = 0,
    
    // Skins/Cosmetics
    var characterId: CharacterId = CharacterId.AERO,
    var currentSkinColor: Color = Color.Cyan,
    
    // Footprint tracking for Hunter passive skill
    val footprintHistory: MutableList<Pair<Float, Float>> = java.util.concurrent.CopyOnWriteArrayList(),
    
    // Out of combat & active passive states
    var lastDamageTime: Long = 0L,
    var lastShieldBurstTime: Long = 0L
)

data class PlayerStats(
    val id: Int = 1,
    var totalMatches: Int = 0,
    var booyahs: Int = 0,
    var totalKills: Int = 0,
    var gold: Int = 1200,
    var diamonds: Int = 150,
    var rankPoints: Int = 1000, // Bronze starts at 1000
    var battlePassLevel: Int = 1,
    var battlePassXP: Int = 0,
    var equippedCharacter: String = "aero",
    var activeWeaponSkin: String = "standard",
    var unlockedCharacters: String = "aero", // comma separated
    var ownedSkins: String = "standard" // comma separated
) {
    fun getRankName(): String {
        return when {
            rankPoints < 1200 -> "Bronze I"
            rankPoints < 1400 -> "Silver II"
            rankPoints < 1600 -> "Gold III"
            rankPoints < 1800 -> "Platinum IV"
            rankPoints < 2200 -> "Heroic"
            else -> "Grandmaster"
        }
    }
}

data class MatchEvent(
    val timeFormatted: String,
    val message: String,
    val type: MatchEventType
)

enum class MatchEventType {
    SYSTEM, ELIMINATION, BOOYAH, DROP
}

data class DailyMission(
    val id: String,
    val description: String,
    val target: Int,
    var progress: Int,
    val rewardGold: Int,
    var isClaimed: Boolean = false
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val rewardGold: Int,
    val iconName: String,
    var progress: Int = 0
)

data class Bullet(
    val id: String,
    val ownerId: String,
    val startX: Float,
    val startY: Float,
    var currentX: Float,
    var currentY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val damage: Float,
    val maxRange: Float,
    var distanceTraveled: Float = 0f,
    val color: Color = Color(0xFFFFD54F)
)
