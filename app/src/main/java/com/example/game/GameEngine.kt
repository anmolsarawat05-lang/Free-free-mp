package com.example.game

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.*

class GameEngine {
    // Map bounds
    val mapWidth = 2000f
    val mapHeight = 2000f

    // Match configurations
    var players = java.util.concurrent.CopyOnWriteArrayList<Player>()
    var lootItems = java.util.concurrent.CopyOnWriteArrayList<LootItem>()
    var vehicles = java.util.concurrent.CopyOnWriteArrayList<Vehicle>()
    var glooWalls = java.util.concurrent.CopyOnWriteArrayList<GlooWall>()
    var bullets = java.util.concurrent.CopyOnWriteArrayList<Bullet>()
    var playzone = Playzone()
    
    // Match events
    val killFeed = java.util.concurrent.CopyOnWriteArrayList<MatchEvent>()
    
    // Flight Path
    var planeX = 100f
    var planeY = 100f
    var planeAngle = 45f // angle in degrees
    var planeActive = true
    var planeSpeed = 12f
    var planeEndX = 1900f
    var planeEndY = 1900f
    
    // Timer
    var matchTicks = 0L
    var matchFinished = false
    var userPlacement = 50
    var winnerPlayerName = ""
    
    // Landmarks on the map
    val landmarks = listOf(
        Landmark("Military Base", 1000f, 1750f, Color(0xFF795548)),
        Landmark("Peak", 1000f, 1000f, Color(0xFFFF5722)),
        Landmark("Bimasakti Strip", 1000f, 1250f, Color(0xFF9C27B0)),
        Landmark("Clock Tower", 400f, 1500f, Color(0xFFFFEB3B)),
        Landmark("Mill", 1700f, 600f, Color(0xFFE91E63)),
        Landmark("Shipyard", 1100f, 250f, Color(0xFF00BCD4)),
        Landmark("Hangar", 300f, 1000f, Color(0xFF607D8B)),
        Landmark("Observatory", 250f, 650f, Color(0xFF3F51B5)),
        Landmark("Factory", 1300f, 1350f, Color(0xFFFF9800)),
        Landmark("Pochinok", 750f, 1600f, Color(0xFF4CAF50))
    )

    data class Landmark(val name: String, val x: Float, val y: Float, val color: Color)

    private val botNames = listOf(
        "Slayer_01", "Raptor_BR", "SniperGod", "Phantom", "GamerX",
        "Alpha_Dog", "Kelly_Fan", "Alok_Pro", "Chrono_Op", "BooyahKing",
        "Apex_Striker", "Stealthy", "Shadow_Ninja", "Bullet_Storm", "Rogue_Agent",
        "Viper_Fang", "Trigger_Happy", "Iron_Clad", "Phoenix_BR", "Tomb_Raider",
        "Ghost_Rider", "Cyber_Cop", "Doom_Bringer", "Reaper_V", "Valkyrie",
        "Titan_Ops", "Zero_Cool", "Matrix_BR", "Outlaw", "Ranger_Pro",
        "Warlord", "Terminator", "Gladiator", "Spectre", "Cobra_Strike",
        "Blaze_BR", "Storm_Rider", "Thunder_Bolt", "Wild_Card", "Vortex",
        "Ninja_Strike", "Fire_Fly", "Giga_Byte", "Red_Baron", "Frost_Bite",
        "Crimson_Claw", "Steel_Alloy", "Overlord", "Dark_Knight"
    )

    fun startNewMatch(userCharacter: CharacterId, userSkinColor: Color) {
        players.clear()
        lootItems.clear()
        vehicles.clear()
        glooWalls.clear()
        bullets.clear()
        killFeed.clear()
        matchTicks = 0L
        matchFinished = false
        userPlacement = 50
        winnerPlayerName = ""

        // Initialize Playzone
        playzone = Playzone(
            currentX = 1000f,
            currentY = 1000f,
            currentRadius = 900f,
            targetX = 1000f,
            targetY = 1000f,
            targetRadius = 900f,
            isShrinking = false,
            secondsUntilShrink = 45
        )

        // Reset flight path
        val startFromTopLeft = Math.random() > 0.5
        if (startFromTopLeft) {
            planeX = 100f
            planeY = 100f
            planeEndX = 1900f
            planeEndY = 1900f
            planeAngle = 45f
        } else {
            planeX = 1900f
            planeY = 100f
            planeEndX = 100f
            planeEndY = 1900f
            planeAngle = 135f
        }
        planeActive = true

        // Create user player (Player 1)
        val userPlayer = Player(
            id = "user",
            name = "You",
            isBot = false,
            x = planeX,
            y = planeY,
            characterId = userCharacter,
            currentSkinColor = userSkinColor,
            isParachuting = true,
            parachuteAltitude = 1000f
        )
        // Give basic melee and pistol
        userPlayer.weapons = mutableListOf(WeaponData.createMelee())
        players.add(userPlayer)

        // Create 49 bots
        for (i in 0 until 49) {
            val randomChar = CharacterId.values().random()
            val randomSkinColor = Color(
                (100..240).random(),
                (100..240).random(),
                (100..240).random()
            )
            val name = if (i < botNames.size) botNames[i] else "Bot_${i+1}"
            val bot = Player(
                id = "bot_$i",
                name = name,
                isBot = true,
                x = planeX + (Math.random() * 100 - 50).toFloat(),
                y = planeY + (Math.random() * 100 - 50).toFloat(),
                characterId = randomChar,
                currentSkinColor = randomSkinColor,
                isParachuting = true,
                parachuteAltitude = 1000f + (Math.random() * 300 - 150).toFloat()
            )
            bot.weapons = mutableListOf(WeaponData.createMelee())
            players.add(bot)
        }

        // Apply starting passive modifiers
        for (p in players) {
            if (p.characterId == CharacterId.SHIELD) {
                p.levelVest = 1
                p.shield = 50f
            }
        }

        // Spawn Loot Items near landmarks and randomly
        spawnLoot()

        // Spawn Vehicles
        spawnVehicles()

        logEvent("Match started. 50 players aboard the supply aircraft.", MatchEventType.SYSTEM)
    }

    private fun spawnLoot() {
        var lootIdCounter = 0
        // Helper to spawn items
        fun addLoot(x: Float, y: Float, range: Float) {
            val rx = x + (Math.random() * range - range/2).toFloat()
            val ry = y + (Math.random() * range - range/2).toFloat()
            val cl_x = rx.coerceIn(10f, mapWidth - 10f)
            val cl_y = ry.coerceIn(10f, mapHeight - 10f)

            val typeRoll = Math.random()
            val loot = when {
                typeRoll < 0.25 -> {
                    // Weapon
                    val weaponsList = listOf(
                        WeaponData.createAR(), WeaponData.createSMG(),
                        WeaponData.createShotgun(), WeaponData.createSniper(),
                        WeaponData.createPistol()
                    )
                    val chosenWeapon = weaponsList.random()
                    LootItem("loot_${lootIdCounter++}", cl_x, cl_y, chosenWeapon.name, LootType.WEAPON, weaponData = chosenWeapon)
                }
                typeRoll < 0.50 -> {
                    // Ammo
                    val ammoTypes = listOf(
                        "AR Ammo" to LootType.AMMO,
                        "SMG Ammo" to LootType.AMMO,
                        "Shotgun Shells" to LootType.AMMO,
                        "Sniper Ammo" to LootType.AMMO
                    )
                    val choice = ammoTypes.random()
                    val qty = when (choice.first) {
                        "AR Ammo" -> 30
                        "SMG Ammo" -> 40
                        "Shotgun Shells" -> 8
                        else -> 5
                    }
                    LootItem("loot_${lootIdCounter++}", cl_x, cl_y, choice.first, choice.second, qty)
                }
                typeRoll < 0.70 -> {
                    LootItem("loot_${lootIdCounter++}", cl_x, cl_y, "Medkit", LootType.MEDKIT, 1)
                }
                typeRoll < 0.85 -> {
                    val lvl = (1..3).random()
                    LootItem("loot_${lootIdCounter++}", cl_x, cl_y, "Armor Vest Lvl $lvl", LootType.ARMOR_VEST, 1, armorLevel = lvl)
                }
                typeRoll < 0.95 -> {
                    val lvl = (1..3).random()
                    LootItem("loot_${lootIdCounter++}", cl_x, cl_y, "Helmet Lvl $lvl", LootType.ARMOR_HELMET, 1, armorLevel = lvl)
                }
                else -> {
                    LootItem("loot_${lootIdCounter++}", cl_x, cl_y, "Gloo Device", LootType.GLOO_WALL, 2)
                }
            }
            lootItems.add(loot)
        }

        // Spawn densely around landmarks (high tier zones)
        for (landmark in landmarks) {
            val isHighTier = landmark.name == "Military Base" || landmark.name == "Peak" || landmark.name == "Mill"
            val numSpawns = if (isHighTier) 22 else 12
            for (j in 0 until numSpawns) {
                addLoot(landmark.x, landmark.y, 180f)
            }
        }

        // General sparse spawns
        for (k in 0 until 100) {
            val rx = (Math.random() * (mapWidth - 100) + 50).toFloat()
            val ry = (Math.random() * (mapHeight - 100) + 50).toFloat()
            addLoot(rx, ry, 50f)
        }
    }

    private fun spawnVehicles() {
        var vehicleIdCounter = 0
        val vehicleTypes = VehicleType.values()
        
        // Spawn near roads/landmarks
        for (landmark in landmarks) {
            if (Math.random() > 0.3) {
                val type = vehicleTypes.random()
                val vx = landmark.x + (Math.random() * 120 - 60).toFloat()
                val vy = landmark.y + (Math.random() * 120 - 60).toFloat()
                vehicles.add(
                    Vehicle(
                        id = "veh_${vehicleIdCounter++}",
                        x = vx.coerceIn(20f, mapWidth - 20f),
                        y = vy.coerceIn(20f, mapHeight - 20f),
                        type = type
                    )
                )
            }
        }
        
        // General random spawns
        for (k in 0 until 12) {
            val type = vehicleTypes.random()
            val vx = (Math.random() * (mapWidth - 200) + 100).toFloat()
            val vy = (Math.random() * (mapHeight - 200) + 100).toFloat()
            vehicles.add(
                Vehicle(
                    id = "veh_${vehicleIdCounter++}",
                    x = vx,
                    y = vy,
                    type = type
                )
            )
        }
    }

    private fun logEvent(msg: String, type: MatchEventType) {
        val seconds = matchTicks / 20 // 20 ticks per second
        val min = seconds / 60
        val sec = seconds % 60
        val timeStr = String.format("%02d:%02d", min, sec)
        killFeed.add(0, MatchEvent(timeStr, msg, type))
        if (killFeed.size > 25) {
            killFeed.removeAt(killFeed.size - 1)
        }
    }

    // Tick update (typically runs 20 times per second)
    fun tick(
        joystickX: Float,
        joystickY: Float,
        userSprinting: Boolean,
        userCrouching: Boolean,
        userCrawling: Boolean,
        userFiring: Boolean,
        fireAngle: Float
    ) {
        if (matchFinished) return
        matchTicks++

        // Update Plane
        updatePlane()

        // Update Playzone Shrinking Timer
        updatePlayzone()

        // Update Bullets
        updateBullets()

        // Update Gloo Walls duration
        updateGlooWalls()

        // Update players (user and bots)
        val currentTime = System.currentTimeMillis()
        val user = players.first { it.id == "user" }

        for (player in players) {
            if (!player.isAlive) continue

            // Playzone Damage check
            if (matchTicks % 20L == 0L) { // every 1 second
                val distToZoneCenter = dist(player.x, player.y, playzone.currentX, playzone.currentY)
                if (distToZoneCenter > playzone.currentRadius) {
                    val damage = playzone.damagePerSecond
                    applyDamage(player, damage, "Playzone Barrier")
                }
            }

            // Footprint tracking (add every 15 ticks if alive and on ground)
            if (!player.isParachuting) {
                if (matchTicks % 15L == 0L) {
                    player.footprintHistory.add(Pair(player.x, player.y))
                    if (player.footprintHistory.size > 8) {
                        player.footprintHistory.removeAt(0)
                    }
                }
            } else {
                player.footprintHistory.clear()
            }

            // Skill duration & cooldown decay
            if (player.skillActiveRemaining > 0f) {
                player.skillActiveRemaining = max(0f, player.skillActiveRemaining - 0.05f)
                // Medic Active Healing Aura: +15 HP per second
                if (player.characterId == CharacterId.MEDIC && matchTicks % 20L == 0L) {
                    player.health = min(100f, player.health + 15f)
                }
            }
            if (player.skillCooldownRemaining > 0f) {
                player.skillCooldownRemaining = max(0f, player.skillCooldownRemaining - 0.05f)
            }

            // Medic Passive out of combat health regeneration: +2 HP every 1.5s (30 ticks)
            if (player.characterId == CharacterId.MEDIC && matchTicks % 30L == 0L) {
                if (currentTime - player.lastDamageTime > 4000L) { // 4 seconds out of combat
                    player.health = min(100f, player.health + 2f)
                }
            }

            // Parachute Phase vs Ground Phase
            if (player.isParachuting) {
                updateParachutingPlayer(player)
            } else {
                if (!player.isBot) {
                    // Update user ground mechanics
                    updateUser(joystickX, joystickY, userSprinting, userCrouching, userCrawling, userFiring, fireAngle, currentTime)
                } else {
                    // Update bot AI
                    updateBotAI(player, currentTime)
                }
            }
        }

        // Check Match Status
        checkMatchFinish()
    }

    private fun updatePlane() {
        if (!planeActive) return

        val dx = planeEndX - planeX
        val dy = planeEndY - planeY
        val distance = sqrt(dx*dx + dy*dy)

        if (distance < planeSpeed) {
            planeActive = false
            // Eject remaining bots
            for (player in players) {
                if (player.isAlive && player.isParachuting && player.id != "user") {
                    player.isParachuting = true
                    player.parachuteAltitude = 1000f
                    player.x = planeEndX
                    player.y = planeEndY
                }
            }
        } else {
            planeX += (dx / distance) * planeSpeed
            planeY += (dy / distance) * planeSpeed

            // Bots eject randomly
            for (player in players) {
                if (player.isBot && player.isAlive && player.isParachuting && player.parachuteAltitude >= 1000f) {
                    if (Math.random() < 0.015) {
                        player.x = planeX
                        player.y = planeY
                        player.parachuteAltitude = 999f // eject trigger
                    }
                }
            }
        }
    }

    private fun updatePlayzone() {
        if (matchTicks % 20L == 0L && playzone.secondsUntilShrink > 0 && !playzone.isShrinking) {
            playzone.secondsUntilShrink--
            if (playzone.secondsUntilShrink == 0) {
                // Determine next Playzone center within current Playzone
                playzone.isShrinking = true
                val maxOffset = playzone.currentRadius - playzone.targetRadius / 2f
                val randomAngle = (Math.random() * 2 * Math.PI).toFloat()
                val randomDist = (Math.random() * maxOffset).toFloat()
                
                playzone.targetRadius = max(60f, playzone.currentRadius * 0.55f)
                playzone.targetX = playzone.currentX + cos(randomAngle) * randomDist
                playzone.targetY = playzone.currentY + sin(randomAngle) * randomDist
                
                // Clamping to map limits
                playzone.targetX = playzone.targetX.coerceIn(playzone.targetRadius, mapWidth - playzone.targetRadius)
                playzone.targetY = playzone.targetY.coerceIn(playzone.targetRadius, mapHeight - playzone.targetRadius)
                
                logEvent("Safe zone is shrinking! Head inside the safe zone.", MatchEventType.SYSTEM)
                GameSound.playZoneAlert()
            }
        }

        if (playzone.isShrinking) {
            // Move current playzone center and radius towards target
            val speed = 0.5f // size decay speed per tick
            if (playzone.currentRadius > playzone.targetRadius) {
                playzone.currentRadius -= speed
                
                val dx = playzone.targetX - playzone.currentX
                val dy = playzone.targetY - playzone.currentY
                val dist = sqrt(dx*dx + dy*dy)
                if (dist > 0.1f) {
                    playzone.currentX += (dx / dist) * 0.3f
                    playzone.currentY += (dy / dist) * 0.3f
                }
            } else {
                playzone.isShrinking = false
                playzone.secondsUntilShrink = 40 // next cycle
                logEvent("Playzone stabilized. Next safe zone marked on radar.", MatchEventType.SYSTEM)
            }
        }
    }

    private fun updateParachutingPlayer(player: Player) {
        if (player.id == "user") {
            // User can steer during glide
            player.parachuteAltitude -= 5f
            if (player.parachuteAltitude <= 0f) {
                player.isParachuting = false
                player.parachuteAltitude = 0f
                logEvent("You landed safely on the battleground.", MatchEventType.SYSTEM)
            }
        } else {
            // Bot glide
            if (player.parachuteAltitude >= 1000f) return // not ejected yet
            player.parachuteAltitude -= (4f + Math.random() * 3).toFloat()
            
            // Steer bots randomly toward landmarks
            val nearestLandmark = landmarks.minByOrNull { dist(player.x, player.y, it.x, it.y) }
            if (nearestLandmark != null) {
                val dx = nearestLandmark.x - player.x
                val dy = nearestLandmark.y - player.y
                val d = sqrt(dx*dx + dy*dy)
                if (d > 10) {
                    player.x += (dx / d) * 3f
                    player.y += (dy / d) * 3f
                }
            }

            if (player.parachuteAltitude <= 0f) {
                player.isParachuting = false
                player.parachuteAltitude = 0f
                
                // Set first tactical target for the bot on the ground
                setNextBotTarget(player)
            }
        }
    }

    private fun setNextBotTarget(player: Player) {
        val roll = Math.random()
        if (roll < 0.45) {
            // Find nearest uncollected weapon loot item
            val nearestWeapon = lootItems
                .filter { it.type == LootType.WEAPON }
                .minByOrNull { dist(player.x, player.y, it.x, it.y) }
            if (nearestWeapon != null) {
                player.targetX = nearestWeapon.x
                player.targetY = nearestWeapon.y
            } else {
                player.targetX = playzone.currentX + (Math.random() * 200 - 100).toFloat()
                player.targetY = playzone.currentY + (Math.random() * 200 - 100).toFloat()
            }
        } else {
            // Random position closer to playzone center
            val rAngle = (Math.random() * 2 * Math.PI).toFloat()
            val rDist = (Math.random() * playzone.currentRadius * 0.7f).toFloat()
            player.targetX = playzone.currentX + cos(rAngle) * rDist
            player.targetY = playzone.currentY + sin(rAngle) * rDist
        }
        player.lastDecisionTime = System.currentTimeMillis()
    }

    private fun getPlayerSpeed(player: Player): Float {
        var baseSpeed = 2.4f
        if (player.isSprinting) baseSpeed = 4.4f
        if (player.isCrouching) baseSpeed = 1.3f
        if (player.isCrawling) baseSpeed = 0.6f

        // Aero Active: +50% speed increase
        if (player.skillActiveRemaining > 0f && player.characterId == CharacterId.AERO) {
            baseSpeed *= 1.50f
        }
        // Aero Passive: +15% permanent speed increase
        if (player.characterId == CharacterId.AERO) {
            baseSpeed *= 1.15f
        }
        return baseSpeed
    }

    private fun updateUser(
        joystickX: Float,
        joystickY: Float,
        sprinting: Boolean,
        crouching: Boolean,
        crawling: Boolean,
        firing: Boolean,
        fireAngle: Float,
        currentTime: Long
    ) {
        val user = players.first { it.id == "user" }
        user.isSprinting = sprinting && (joystickX != 0f || joystickY != 0f)
        user.isCrouching = crouching
        user.isCrawling = crawling

        // Check if inside a vehicle
        val vId = user.drivingVehicleId
        if (vId != null) {
            val vehicle = vehicles.firstOrNull { it.id == vId }
            if (vehicle != null) {
                // Update vehicle movement using joystick
                val angleRad = fireAngle * (PI / 180f).toFloat()
                val moveMagnitude = sqrt(joystickX * joystickX + joystickY * joystickY)
                if (moveMagnitude > 0.1f) {
                    vehicle.speed = min(vehicle.type.speedMultiplier * 6f, vehicle.speed + 0.3f)
                    vehicle.angle = fireAngle
                    
                    val dx = cos(angleRad) * vehicle.speed
                    val dy = sin(angleRad) * vehicle.speed
                    vehicle.x = (vehicle.x + dx).coerceIn(10f, mapWidth - 10f)
                    vehicle.y = (vehicle.y + dy).coerceIn(10f, mapHeight - 10f)
                } else {
                    vehicle.speed = max(0f, vehicle.speed - 0.2f)
                }
                
                // Keep player coordinates tied to vehicle
                user.x = vehicle.x
                user.y = vehicle.y
                user.angle = vehicle.angle
                
                // Vehicle Roadkill checks!
                if (vehicle.speed > 2f) {
                    for (other in players) {
                        if (other.isAlive && other.id != "user" && !other.isParachuting && other.drivingVehicleId == null) {
                            val d = dist(vehicle.x, vehicle.y, other.x, other.y)
                            if (d < 35f) {
                                val hitDamage = vehicle.speed * 18f
                                applyDamage(other, hitDamage, "Roadkill by You (${vehicle.type.displayName})")
                                // push back
                                other.x = (other.x + cos(angleRad) * 40f).coerceIn(10f, mapWidth - 10f)
                                  other.y = (other.y + sin(angleRad) * 40f).coerceIn(10f, mapHeight - 10f)
                            }
                        }
                    }
                }
                return
            }
        }

        // Standard walking/sprinting speed calculations using getPlayerSpeed helper
        val baseSpeed = getPlayerSpeed(user)

        val moveMagnitude = sqrt(joystickX * joystickX + joystickY * joystickY)
        if (moveMagnitude > 0.1f) {
            val dx = (joystickX / moveMagnitude) * baseSpeed
            val dy = (joystickY / moveMagnitude) * baseSpeed
            user.x = (user.x + dx).coerceIn(10f, mapWidth - 10f)
            user.y = (user.y + dy).coerceIn(10f, mapHeight - 10f)
            user.angle = fireAngle
        }

        // Auto Pickup loot items near feet
        val itemsToPick = mutableListOf<LootItem>()
        for (loot in lootItems) {
            if (dist(user.x, user.y, loot.x, loot.y) < 25f) {
                itemsToPick.add(loot)
            }
        }
        for (item in itemsToPick) {
            pickupLootItem(user, item)
        }

        // Shooting
        if (firing) {
            val activeWeapon = user.weapons.getOrNull(user.activeWeaponIndex)
            if (activeWeapon != null && activeWeapon.category != WeaponCategory.MELEE) {
                // Aero Active: 30% faster fire rate (70% delay)
                var fireDelay = activeWeapon.fireRateMs
                if (user.skillActiveRemaining > 0f && user.characterId == CharacterId.AERO) {
                    fireDelay = (fireDelay * 0.70f).toLong()
                }
                if (currentTime - user.lastShotTime >= fireDelay) {
                    if (activeWeapon.currentAmmo > 0) {
                        fireBullet(user, activeWeapon, fireAngle)
                        // Consume ammo
                        user.weapons[user.activeWeaponIndex] = activeWeapon.copy(currentAmmo = activeWeapon.currentAmmo - 1)
                        user.lastShotTime = currentTime
                    } else {
                        // Attempt auto reload
                        reloadActiveWeapon(user)
                    }
                }
            } else if (activeWeapon != null && activeWeapon.category == WeaponCategory.MELEE) {
                // Melee strike
                if (currentTime - user.lastShotTime >= activeWeapon.fireRateMs) {
                    strikeMelee(user, activeWeapon, fireAngle)
                    user.lastShotTime = currentTime
                }
            }
        }
    }

    private fun updateBotAI(bot: Player, currentTime: Long) {
        // AI Decision intervals
        val elapsed = currentTime - bot.lastDecisionTime
        
        // If in a vehicle, handle driving
        val vId = bot.drivingVehicleId
        if (vId != null) {
            val vehicle = vehicles.firstOrNull { it.id == vId }
            if (vehicle != null) {
                val dx = playzone.currentX - vehicle.x
                val dy = playzone.currentY - vehicle.y
                val distToZone = sqrt(dx*dx + dy*dy)
                
                if (distToZone < playzone.currentRadius * 0.5f) {
                    // Safe enough, exit vehicle and walk
                    vehicle.isBeingDriven = false
                    vehicle.driverId = null
                    bot.drivingVehicleId = null
                    setNextBotTarget(bot)
                } else {
                    // Drive to zone
                    vehicle.speed = vehicle.type.speedMultiplier * 4.5f
                    val angle = atan2(dy, dx)
                    vehicle.angle = angle * (180f / PI).toFloat()
                    
                    vehicle.x = (vehicle.x + cos(angle) * vehicle.speed).coerceIn(10f, mapWidth - 10f)
                    vehicle.y = (vehicle.y + sin(angle) * vehicle.speed).coerceIn(10f, mapHeight - 10f)
                    
                    bot.x = vehicle.x
                    bot.y = vehicle.y
                    bot.angle = vehicle.angle
                }
                return
            }
        }

        // Healing Logic: If low health and has medkits, stay still and heal
        if (bot.health < 40f && bot.totalMedkits > 0) {
            bot.isSprinting = false
            bot.isCrouching = true
            // stationary heal simulation
            if (elapsed > 3000L) {
                bot.health = min(100f, bot.health + 40f)
                bot.totalMedkits--
                bot.lastDecisionTime = currentTime
            }
            return
        }

        // Threat Scanning: Find nearest visible player
        var targetPlayer: Player? = null
        var minDistToTarget = 300f // bot vision range
        for (other in players) {
            if (other.isAlive && other.id != bot.id && !other.isParachuting) {
                val d = dist(bot.x, bot.y, other.x, other.y)
                if (d < minDistToTarget) {
                    targetPlayer = other
                    minDistToTarget = d
                }
            }
        }

        if (targetPlayer != null) {
            // COMBAT STATE
            bot.currentTargetPlayerId = targetPlayer.id
            val dx = targetPlayer.x - bot.x
            val dy = targetPlayer.y - bot.y
            val angleRad = atan2(dy, dx)
            bot.angle = angleRad * (180f / PI).toFloat()
            
            // Move slowly or crouch-strafe
            bot.isSprinting = false
            bot.isCrouching = Math.random() > 0.5
            
            // Flee or chase depending on health
            if (bot.health < 30f) {
                // Run opposite direction
                bot.isSprinting = true
                val speed = getPlayerSpeed(bot)
                bot.x = (bot.x - cos(angleRad) * speed).coerceIn(10f, mapWidth - 10f)
                bot.y = (bot.y - sin(angleRad) * speed).coerceIn(10f, mapHeight - 10f)
            } else {
                // Approach slightly if out of range, or stay in cover
                if (minDistToTarget > 150f) {
                    val speed = getPlayerSpeed(bot) * 0.65f
                    bot.x = (bot.x + cos(angleRad) * speed).coerceIn(10f, mapWidth - 10f)
                    bot.y = (bot.y + sin(angleRad) * speed).coerceIn(10f, mapHeight - 10f)
                }
            }

            // Bot Active Skill triggering under combat
            if (bot.skillCooldownRemaining <= 0f) {
                when (bot.characterId) {
                    CharacterId.AERO -> {
                        if (minDistToTarget < 120f || bot.health < 50f) {
                            triggerActiveSkill(bot)
                        }
                    }
                    CharacterId.SHIELD -> {
                        if (bot.health < 75f || minDistToTarget < 100f) {
                            triggerActiveSkill(bot)
                        }
                    }
                    CharacterId.HUNTER -> {
                        if (minDistToTarget < 250f) {
                            triggerActiveSkill(bot)
                        }
                    }
                    CharacterId.MEDIC -> {
                        if (bot.health < 60f) {
                            triggerActiveSkill(bot)
                        }
                    }
                }
            }

            // SHOOT AT TARGET
            val botWeapon = bot.weapons.getOrNull(bot.activeWeaponIndex) ?: WeaponData.createMelee()
            if (botWeapon.category != WeaponCategory.MELEE) {
                // Aero Active: 30% faster fire rate (70% delay)
                var fireDelay = botWeapon.fireRateMs
                if (bot.skillActiveRemaining > 0f && bot.characterId == CharacterId.AERO) {
                    fireDelay = (fireDelay * 0.70f).toLong()
                }
                if (currentTime - bot.lastShotTime >= fireDelay + (100..400).random()) {
                    if (botWeapon.currentAmmo > 0) {
                        // Fire (add some inaccuracy for bots)
                        val spreadAngle = bot.angle + (Math.random() * 16 - 8).toFloat()
                        fireBullet(bot, botWeapon, spreadAngle)
                        bot.weapons[bot.activeWeaponIndex] = botWeapon.copy(currentAmmo = botWeapon.currentAmmo - 1)
                        bot.lastShotTime = currentTime
                    } else {
                        reloadActiveWeapon(bot)
                    }
                }
            } else {
                // If melee and very close, strike
                if (minDistToTarget < 30f && currentTime - bot.lastShotTime >= botWeapon.fireRateMs) {
                    strikeMelee(bot, botWeapon, bot.angle)
                    bot.lastShotTime = currentTime
                } else if (elapsed > 1000L) {
                    // Try to pick up a gun or find safety
                    setNextBotTarget(bot)
                }
            }
        } else {
            // OUT OF COMBAT STATE: Head towards target, looting or moving to safe zone
            val distToTarget = dist(bot.x, bot.y, bot.targetX, bot.targetY)
            
            // If target reached or too long elapsed, set new target
            if (distToTarget < 15f || elapsed > 10000L) {
                // Check if there are vehicles nearby to drive
                val closeVehicle = vehicles.firstOrNull { !it.isBeingDriven && dist(bot.x, bot.y, it.x, it.y) < 60f }
                val distToZone = dist(bot.x, bot.y, playzone.currentX, playzone.currentY)
                
                if (closeVehicle != null && distToZone > playzone.currentRadius * 0.8f && Math.random() > 0.4) {
                    // Enter vehicle
                    closeVehicle.isBeingDriven = true
                    closeVehicle.driverId = bot.id
                    bot.drivingVehicleId = closeVehicle.id
                    bot.x = closeVehicle.x
                    bot.y = closeVehicle.y
                } else {
                    setNextBotTarget(bot)
                }
            } else {
                // Move towards target coordinate using getPlayerSpeed
                val dx = bot.targetX - bot.x
                val dy = bot.targetY - bot.y
                val angleRad = atan2(dy, dx)
                bot.angle = angleRad * (180f / PI).toFloat()
                
                val speed = getPlayerSpeed(bot)
                bot.x = (bot.x + cos(angleRad) * speed).coerceIn(10f, mapWidth - 10f)
                bot.y = (bot.y + sin(angleRad) * speed).coerceIn(10f, mapHeight - 10f)
                
                // Auto Pickup loot items near feet for bot
                val itemsToPick = mutableListOf<LootItem>()
                for (loot in lootItems) {
                    if (dist(bot.x, bot.y, loot.x, loot.y) < 25f) {
                        itemsToPick.add(loot)
                    }
                }
                for (item in itemsToPick) {
                    pickupLootItem(bot, item)
                }
            }
        }
    }

    private fun fireBullet(owner: Player, weapon: WeaponData, angleDeg: Float) {
        val angleRad = angleDeg * (PI / 180f).toFloat()
        
        // Aero Passive: 30% faster bullet travel speed (velocity)
        var velocity = 28f
        if (owner.characterId == CharacterId.AERO) {
            velocity *= 1.30f
        }
        
        // Spawn slightly in front of the shooter
        val bx = owner.x + cos(angleRad) * 15f
        val by = owner.y + sin(angleRad) * 15f
        
        // Hunter Active: +30% damage boost
        var finalDamage = weapon.damage
        if (owner.characterId == CharacterId.HUNTER && owner.skillActiveRemaining > 0f) {
            finalDamage *= 1.30f
        }

        // Hunter Passive: +25% range increase
        var finalRange = weapon.range
        if (owner.characterId == CharacterId.HUNTER) {
            finalRange *= 1.25f
        }
        
        val bullet = Bullet(
            id = UUID.randomUUID().toString(),
            ownerId = owner.id,
            startX = bx,
            startY = by,
            currentX = bx,
            currentY = by,
            velocityX = cos(angleRad) * velocity,
            velocityY = sin(angleRad) * velocity,
            damage = finalDamage,
            maxRange = finalRange,
            color = if (owner.id == "user") Color(0xFFFF9100) else Color(0xFFFFEA00)
        )
        bullets.add(bullet)
        
        // Play shoot audio
        if (owner.id == "user") {
            GameSound.playShoot(weapon.recoilIntensity)
        }
    }

    private fun strikeMelee(owner: Player, weapon: WeaponData, angleDeg: Float) {
        val angleRad = angleDeg * (PI / 180f).toFloat()
        val reach = 35f
        val strikeX = owner.x + cos(angleRad) * reach
        val strikeY = owner.y + sin(angleRad) * reach

        for (victim in players) {
            if (victim.isAlive && victim.id != owner.id && !victim.isParachuting) {
                if (dist(strikeX, strikeY, victim.x, victim.y) < 30f) {
                    applyDamage(victim, weapon.damage, owner.name)
                    if (owner.id == "user") {
                        GameSound.playHit()
                    }
                }
            }
        }
    }

    fun deployGlooWall(creatorId: String, angleDeg: Float): Boolean {
        val creator = players.firstOrNull { it.id == creatorId } ?: return false
        if (creator.totalGlooWalls <= 0 && creatorId == "user") return false

        val angleRad = angleDeg * (PI / 180f).toFloat()
        // Spawn gloo wall 40 units ahead
        val gx = creator.x + cos(angleRad) * 45f
        val gy = creator.y + sin(angleRad) * 45f

        val gloo = GlooWall(
            id = UUID.randomUUID().toString(),
            x = gx.coerceIn(10f, mapWidth - 10f),
            y = gy.coerceIn(10f, mapHeight - 10f),
            angle = angleDeg,
            creatorId = creatorId
        )
        glooWalls.add(gloo)
        
        if (creatorId == "user") {
            creator.totalGlooWalls--
            GameSound.playClick()
        } else {
            creator.totalGlooWalls = max(0, creator.totalGlooWalls - 1)
        }
        return true
    }

    private fun updateBullets() {
        val bulletsToRemove = mutableListOf<Bullet>()
        for (bullet in bullets) {
            val lastX = bullet.currentX
            val lastY = bullet.currentY
            
            bullet.currentX += bullet.velocityX
            bullet.currentY += bullet.velocityY
            bullet.distanceTraveled += sqrt(bullet.velocityX*bullet.velocityX + bullet.velocityY*bullet.velocityY)

            // Hit checks against Gloo Walls
            var hitSomething = false
            for (gloo in glooWalls) {
                val d = dist(bullet.currentX, bullet.currentY, gloo.x, gloo.y)
                if (d < 30f) {
                    gloo.health -= bullet.damage
                    hitSomething = true
                    break
                }
            }

            if (hitSomething) {
                bulletsToRemove.add(bullet)
                continue
            }

            // Hit checks against active defensive energy shield bubbles (SHIELD character active)
            for (shieldPlayer in players) {
                if (shieldPlayer.isAlive && shieldPlayer.characterId == CharacterId.SHIELD && shieldPlayer.skillActiveRemaining > 0f) {
                    if (bullet.ownerId != shieldPlayer.id) {
                        val d = dist(bullet.currentX, bullet.currentY, shieldPlayer.x, shieldPlayer.y)
                        if (d < 35f) { // Energy bubble radius
                            hitSomething = true
                            break
                        }
                    }
                }
            }

            if (hitSomething) {
                bulletsToRemove.add(bullet)
                continue
            }

            // Hit checks against players
            for (victim in players) {
                if (victim.isAlive && victim.id != bullet.ownerId && !victim.isParachuting) {
                    val d = dist(bullet.currentX, bullet.currentY, victim.x, victim.y)
                    if (d < 22f) { // hitbox radius
                        val shooterName = players.firstOrNull { it.id == bullet.ownerId }?.name ?: "Unknown"
                        applyDamage(victim, bullet.damage, shooterName)
                        bulletsToRemove.add(bullet)
                        hitSomething = true
                        break
                    }
                }
            }

            if (hitSomething) continue

            // Max range or out of bounds check
            if (bullet.distanceTraveled >= bullet.maxRange ||
                bullet.currentX < 0 || bullet.currentX > mapWidth ||
                bullet.currentY < 0 || bullet.currentY > mapHeight) {
                bulletsToRemove.add(bullet)
            }
        }
        bullets.removeAll(bulletsToRemove)
    }

    private fun updateGlooWalls() {
        val destroyed = mutableListOf<GlooWall>()
        for (gloo in glooWalls) {
            gloo.remainingTicks--
            if (gloo.remainingTicks <= 0 || gloo.health <= 0f) {
                destroyed.add(gloo)
            }
        }
        glooWalls.removeAll(destroyed)
    }

    private fun applyDamage(victim: Player, rawDamage: Float, shooterName: String) {
        if (!victim.isAlive) return

        victim.lastDamageTime = System.currentTimeMillis()

        // Medic Active Ability: 35% damage reduction while inside healing aura
        var finalDamage = rawDamage
        if (victim.characterId == CharacterId.MEDIC && victim.skillActiveRemaining > 0f) {
            finalDamage *= 0.65f
        }

        // Armor mitigation calculations
        if (victim.shield > 0f) {
            // Mitigate vest
            var blockRatio = when (victim.levelVest) {
                1 -> 0.30f
                2 -> 0.50f
                3 -> 0.70f
                else -> 0.0f
            }
            // SHIELD Passive: Titan Armor increases armor mitigation ratio by an extra 15%
            if (victim.characterId == CharacterId.SHIELD) {
                blockRatio = min(0.95f, blockRatio + 0.15f)
            }
            val shieldDamage = finalDamage * blockRatio
            victim.shield = max(0f, victim.shield - shieldDamage)
            finalDamage -= shieldDamage
        }

        victim.health = max(0f, victim.health - finalDamage)

        // Medic Passive: Emergency Nanobot Shield Burst
        // If critical health (< 35 HP), instantly get +30 shield (40s cooldown)
        if (victim.isAlive && victim.characterId == CharacterId.MEDIC && victim.health < 35f) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - victim.lastShieldBurstTime > 40000L) {
                victim.shield = min(150f, victim.shield + 30f)
                victim.lastShieldBurstTime = currentTime
                if (victim.id == "user") {
                    logEvent("Nanobot Shield Burst Activated (+30 Shield)!", MatchEventType.SYSTEM)
                }
            }
        }

        // Hit FX
        if (victim.id == "user") {
            GameSound.playHit()
        }

        if (victim.health <= 0f) {
            victim.isAlive = false
            victim.statusText = "Eliminated"
            
            // Drop player's inventory as floor loot
            dropPlayerLoot(victim)
            
            // Increment killer kills count
            val killer = players.firstOrNull { it.name == shooterName }
            if (killer != null) {
                killer.kills++
            }

            val weaponUsed = killer?.weapons?.getOrNull(killer.activeWeaponIndex)?.name ?: "Weapon"
            logEvent("${victim.name} eliminated by $shooterName with $weaponUsed", MatchEventType.ELIMINATION)
            
            if (victim.id == "user") {
                GameSound.playEliminated()
                userPlacement = players.count { it.isAlive } + 1
            }
        }
    }

    private fun dropPlayerLoot(victim: Player) {
        // Create 2-3 floor loot items at the coordinates
        var counter = 0
        val activeWep = victim.weapons.getOrNull(victim.activeWeaponIndex)
        if (activeWep != null && activeWep.category != WeaponCategory.MELEE) {
            lootItems.add(
                LootItem(
                    id = "dropped_w_${victim.id}_${counter++}",
                    x = victim.x + (Math.random() * 20 - 10).toFloat(),
                    y = victim.y + (Math.random() * 20 - 10).toFloat(),
                    name = activeWep.name,
                    type = LootType.WEAPON,
                    weaponData = activeWep
                )
            )
        }
        if (victim.totalMedkits > 0) {
            lootItems.add(
                LootItem(
                    id = "dropped_m_${victim.id}_${counter++}",
                    x = victim.x + (Math.random() * 20 - 10).toFloat(),
                    y = victim.y + (Math.random() * 20 - 10).toFloat(),
                    name = "Medkit",
                    type = LootType.MEDKIT,
                    quantity = victim.totalMedkits
                )
            )
        }
    }

    private fun pickupLootItem(player: Player, item: LootItem) {
        when (item.type) {
            LootType.WEAPON -> {
                val wep = item.weaponData ?: return
                // Check if we can replace or append
                if (player.weapons.size < 3) {
                    player.weapons.add(wep)
                } else {
                    // Replace active weapon (excluding melee)
                    if (player.activeWeaponIndex != 0) {
                        player.weapons[player.activeWeaponIndex] = wep
                    } else {
                        player.weapons.add(wep)
                        if (player.weapons.size > 3) {
                            player.weapons.removeAt(1)
                        }
                    }
                }
                player.activeWeaponIndex = player.weapons.size - 1
            }
            LootType.AMMO -> {
                when (item.name) {
                    "AR Ammo" -> player.totalAmmoAR += item.quantity
                    "SMG Ammo" -> player.totalAmmoSMG += item.quantity
                    "Shotgun Shells" -> player.totalAmmoShotgun += item.quantity
                    else -> player.totalAmmoSniper += item.quantity
                }
            }
            LootType.MEDKIT -> {
                player.totalMedkits += item.quantity
            }
            LootType.ARMOR_VEST -> {
                player.levelVest = max(player.levelVest, item.armorLevel)
                player.shield = when (player.levelVest) {
                    1 -> 50f
                    2 -> 100f
                    3 -> 150f
                    else -> 0f
                }
            }
            LootType.ARMOR_HELMET -> {
                player.levelHelmet = max(player.levelHelmet, item.armorLevel)
            }
            LootType.GLOO_WALL -> {
                player.totalGlooWalls += item.quantity
            }
        }
        // Remove from floor list
        lootItems.remove(item)
        
        if (player.id == "user") {
            GameSound.playClick()
        }
    }

    fun reloadActiveWeapon(player: Player): Boolean {
        val weapon = player.weapons.getOrNull(player.activeWeaponIndex) ?: return false
        if (weapon.category == WeaponCategory.MELEE) return false
        if (weapon.currentAmmo >= weapon.maxAmmo) return false

        // Determine backup reserve
        val category = weapon.category
        val required = weapon.maxAmmo - weapon.currentAmmo
        var available = 0

        when (category) {
            WeaponCategory.AR -> {
                available = player.totalAmmoAR
                val fill = min(required, available)
                player.totalAmmoAR -= fill
                player.weapons[player.activeWeaponIndex] = weapon.copy(currentAmmo = weapon.currentAmmo + fill)
            }
            WeaponCategory.SMG -> {
                available = player.totalAmmoSMG
                val fill = min(required, available)
                player.totalAmmoSMG -= fill
                player.weapons[player.activeWeaponIndex] = weapon.copy(currentAmmo = weapon.currentAmmo + fill)
            }
            WeaponCategory.SHOTGUN -> {
                available = player.totalAmmoShotgun
                val fill = min(required, available)
                player.totalAmmoShotgun -= fill
                player.weapons[player.activeWeaponIndex] = weapon.copy(currentAmmo = weapon.currentAmmo + fill)
            }
            WeaponCategory.SNIPER -> {
                available = player.totalAmmoSniper
                val fill = min(required, available)
                player.totalAmmoSniper -= fill
                player.weapons[player.activeWeaponIndex] = weapon.copy(currentAmmo = weapon.currentAmmo + fill)
            }
            WeaponCategory.PISTOL -> {
                // Unlimited reload for basic pistol to ensure playability
                player.weapons[player.activeWeaponIndex] = weapon.copy(currentAmmo = weapon.maxAmmo)
            }
            else -> {}
        }
        
        if (player.id == "user") {
            GameSound.playClick()
        }
        return true
    }

    fun triggerActiveSkill(player: Player): Boolean {
        if (player.skillCooldownRemaining > 0f) return false

        when (player.characterId) {
            CharacterId.AERO -> {
                player.skillActiveRemaining = 5f // 5s adrenaline speed
                player.skillCooldownRemaining = 15f
            }
            CharacterId.SHIELD -> {
                // Deploy protective wall circle around player coordinates
                deployGlooWall(player.id, player.angle)
                deployGlooWall(player.id, player.angle + 45f)
                deployGlooWall(player.id, player.angle - 45f)
                player.skillActiveRemaining = 6f // 6s defensive bubble
                player.skillCooldownRemaining = 20f
            }
            CharacterId.HUNTER -> {
                player.skillActiveRemaining = 6f // 6s reveal & damage boost
                player.skillCooldownRemaining = 18f
            }
            CharacterId.MEDIC -> {
                player.skillActiveRemaining = 5f // 5s healing aura
                player.skillCooldownRemaining = 15f
                GameSound.playHeal()
            }
        }
        
        if (player.id == "user") {
            GameSound.playHeal()
        }
        return true
    }

    fun applyUserMedkit(): Boolean {
        val user = players.first { it.id == "user" }
        if (user.totalMedkits <= 0) return false
        if (user.health >= 100f) return false

        user.health = min(100f, user.health + 40f)
        user.totalMedkits--
        GameSound.playHeal()
        return true
    }

    private fun checkMatchFinish() {
        val aliveCount = players.count { it.isAlive }
        val user = players.first { it.id == "user" }

        if (!user.isAlive && !matchFinished) {
            matchFinished = true
            winnerPlayerName = players.firstOrNull { it.isAlive }?.name ?: "Opponent"
            logEvent("Defeat! You placed #$userPlacement.", MatchEventType.SYSTEM)
        } else if (aliveCount == 1 && user.isAlive && !matchFinished) {
            matchFinished = true
            userPlacement = 1
            winnerPlayerName = "You"
            logEvent("BOOYAH! You are the sole survivor!", MatchEventType.BOOYAH)
            GameSound.playBooyah()
        }
    }

    // Distance utility
    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx*dx + dy*dy)
    }
}
