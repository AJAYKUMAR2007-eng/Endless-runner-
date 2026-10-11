package com.example.neonrunner.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.neonrunner.audio.SoundManager
import com.example.neonrunner.data.GamePreferences
import com.example.neonrunner.model.Collectible
import com.example.neonrunner.model.CollectibleType
import com.example.neonrunner.model.FloatingText
import com.example.neonrunner.model.GameStatus
import com.example.neonrunner.model.Obstacle
import com.example.neonrunner.model.ObstacleType
import com.example.neonrunner.model.Particle
import com.example.neonrunner.model.Player
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    val soundManager: SoundManager,
    val preferences: GamePreferences
) {
    val player = Player()

    var status: GameStatus by mutableStateOf(GameStatus.MENU)
        private set

    var score: Int by mutableStateOf(0)
        private set

    var coinsCollected: Int by mutableStateOf(0)
        private set

    var distanceTraveled: Float by mutableStateOf(0f)
        private set

    var currentSpeed: Float by mutableStateOf(18f)
        private set

    var isNewHighScore: Boolean by mutableStateOf(false)
        private set

    // Camera screen shake
    var shakeX: Float = 0f
        private set
    var shakeY: Float = 0f
        private set
    private var shakeMagnitude: Float = 0f

    // Active entities
    val obstacles = mutableListOf<Obstacle>()
    val collectibles = mutableListOf<Collectible>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()

    // Spawning trackers
    private var nextObstacleSpawnDistance: Float = 40f
    private var nextPowerupDistance: Float = 120f
    private var entityIdCounter: Long = 0L

    fun startGame() {
        player.reset()
        obstacles.clear()
        collectibles.clear()
        particles.clear()
        floatingTexts.clear()

        score = 0
        coinsCollected = 0
        distanceTraveled = 0f
        currentSpeed = 18f
        isNewHighScore = false
        shakeMagnitude = 0f
        shakeX = 0f
        shakeY = 0f

        nextObstacleSpawnDistance = 35f
        nextPowerupDistance = 90f

        // Initial decorative coin strip
        spawnCoinRun(lane = 0, startDist = 15f, count = 4, height = 0f)

        status = GameStatus.PLAYING
    }

    fun pauseGame() {
        if (status == GameStatus.PLAYING) {
            status = GameStatus.PAUSED
        }
    }

    fun resumeGame() {
        if (status == GameStatus.PAUSED) {
            status = GameStatus.PLAYING
        }
    }

    fun openMenu() {
        status = GameStatus.MENU
    }

    fun openInstructions() {
        status = GameStatus.INSTRUCTIONS
    }

    // --- Player Input Actions ---

    fun moveLeft() {
        if (status != GameStatus.PLAYING) return
        if (player.targetLane > -1) {
            player.targetLane -= 1
            soundManager.play(SoundManager.SoundEffect.LANE_SWITCH)
            spawnLaneShiftSparks(direction = -1f)
        }
    }

    fun moveRight() {
        if (status != GameStatus.PLAYING) return
        if (player.targetLane < 1) {
            player.targetLane += 1
            soundManager.play(SoundManager.SoundEffect.LANE_SWITCH)
            spawnLaneShiftSparks(direction = 1f)
        }
    }

    fun jump() {
        if (status != GameStatus.PLAYING) return
        if (!player.isJumping) {
            player.isJumping = true
            player.jumpVelocity = 8.2f
            player.isSliding = false
            player.slideTimer = 0f
            soundManager.play(SoundManager.SoundEffect.JUMP)
            spawnJumpSparks()
        }
    }

    fun slide() {
        if (status != GameStatus.PLAYING) return
        if (player.isJumping) {
            // Fast fall / dive downwards if in air
            player.jumpVelocity = -15f
        }
        player.isSliding = true
        player.slideTimer = 0.75f
        soundManager.play(SoundManager.SoundEffect.SLIDE)
        spawnSlideSparks()
    }

    // --- Core Game Loop Update ---

    fun update(dt: Float) {
        if (status != GameStatus.PLAYING) return
        val clampedDt = min(dt, 0.05f) // Prevent spiraling on low frame drops

        // 1. Speed acceleration
        currentSpeed = min(42f, 18f + (distanceTraveled * 0.010f))
        val frameDistance = currentSpeed * clampedDt
        distanceTraveled += frameDistance

        // Update score from distance
        score = (distanceTraveled * 1.5f).toInt() + (coinsCollected * 25)

        // 2. Camera screen shake update
        if (shakeMagnitude > 0.01f) {
            shakeMagnitude *= exp(-10f * clampedDt)
            shakeX = (Random.nextFloat() * 2f - 1f) * shakeMagnitude * 24f
            shakeY = (Random.nextFloat() * 2f - 1f) * shakeMagnitude * 24f
        } else {
            shakeMagnitude = 0f
            shakeX = 0f
            shakeY = 0f
        }

        // 3. Player physics & timers
        updatePlayer(clampedDt)

        // 4. Update & scroll obstacles
        updateObstacles(clampedDt, frameDistance)

        // 5. Update & scroll collectibles
        updateCollectibles(clampedDt, frameDistance)

        // 6. Spawning
        handleSpawning()

        // 7. Update visual particles & floating texts
        updateEffects(clampedDt)
    }

    private fun updatePlayer(dt: Float) {
        // Smooth lane interpolation
        val lerpSpeed = 16f
        player.visualLane += (player.targetLane - player.visualLane) * (1f - exp(-lerpSpeed * dt))

        // Jump physics
        if (player.isJumping) {
            val gravity = -20f
            player.jumpVelocity += gravity * dt
            player.jumpHeight += player.jumpVelocity * dt
            if (player.jumpHeight <= 0f) {
                player.jumpHeight = 0f
                player.jumpVelocity = 0f
                player.isJumping = false
                spawnJumpSparks() // Land dust
            }
        }

        // Slide timer
        if (player.isSliding) {
            player.slideTimer -= dt
            if (player.slideTimer <= 0f) {
                player.isSliding = false
                player.slideTimer = 0f
            } else {
                // Continuous slide sparks
                if (Random.nextFloat() < 0.4f) {
                    spawnSlideSparks()
                }
            }
        }

        // Powerup timers
        if (player.shieldTimer > 0f) {
            player.shieldTimer = max(0f, player.shieldTimer - dt)
        }
        if (player.magnetTimer > 0f) {
            player.magnetTimer = max(0f, player.magnetTimer - dt)
        }
        if (player.invulnerableTimer > 0f) {
            player.invulnerableTimer = max(0f, player.invulnerableTimer - dt)
        }

        // Running animation cycle
        player.runCycle = (player.runCycle + currentSpeed * dt * 0.45f) % 1.0f

        // Ambient trail particles behind runner
        if (Random.nextFloat() < 0.6f && !player.isJumping) {
            val sparkColor = if (player.hasShield) Color(0xFF00E5FF) else Color(0xFFFF007F)
            particles.add(
                Particle(
                    x = player.visualLane + (Random.nextFloat() * 0.2f - 0.1f),
                    y = -0.1f,
                    vx = (Random.nextFloat() * 2f - 1f) * 0.5f,
                    vy = Random.nextFloat() * 0.8f + 0.2f,
                    color = sparkColor,
                    life = 0f,
                    maxLife = 0.35f,
                    size = Random.nextFloat() * 5f + 4f
                )
            )
        }
    }

    private fun updateObstacles(dt: Float, frameDistance: Float) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.distance -= frameDistance

            // Collision check window (when obstacle reaches player plane at z ~ 0)
            if (!obs.hit && abs(obs.distance) < 1.1f) {
                val laneDiff = abs(player.visualLane - obs.lane)
                if (laneDiff < 0.62f) {
                    checkObstacleCollision(obs)
                }
            }

            // Despawn when far behind player
            if (obs.distance < -15f) {
                iterator.remove()
            }
        }
    }

    private fun checkObstacleCollision(obs: Obstacle) {
        var avoided = false

        when (obs.type) {
            ObstacleType.HURDLE -> {
                // Low hurdle: avoided if jumping high enough
                if (player.jumpHeight > 0.42f) {
                    avoided = true
                }
            }
            ObstacleType.OVERHEAD_BEAM -> {
                // High beam: avoided if sliding under
                if (player.isSliding) {
                    avoided = true
                }
            }
            ObstacleType.BLOCKADE -> {
                // Massive block: impossible to leap or slide under, lane switch required
                avoided = false
            }
        }

        if (avoided) {
            // Player successfully dodged obstacle!
            obs.hit = true
            score += 15
            addFloatingText("+15 DODGE", Color(0xFF00FFCC))
        } else {
            // Collision occurred!
            obs.hit = true
            handlePlayerHit()
        }
    }

    private fun handlePlayerHit() {
        if (player.hasShield) {
            // Shield absorbs the blow!
            player.shieldTimer = 0f
            player.invulnerableTimer = 1.3f
            shakeMagnitude = 0.55f
            soundManager.play(SoundManager.SoundEffect.HIT)
            spawnExplosionSparks(color = Color(0xFF00E5FF), count = 25)
            addFloatingText("SHIELD DEFLECTED!", Color(0xFF00E5FF))
            return
        }

        if (player.isInvulnerable) {
            // Grace period, no damage
            return
        }

        // Take damage
        player.health -= 1
        player.invulnerableTimer = 1.6f
        shakeMagnitude = 0.75f
        soundManager.play(SoundManager.SoundEffect.HIT)
        spawnExplosionSparks(color = Color(0xFFFF2A6D), count = 30)

        if (player.health <= 0) {
            gameOver()
        } else {
            addFloatingText("HIT! -1 LIFE", Color(0xFFFF2A6D))
        }
    }

    private fun gameOver() {
        status = GameStatus.GAME_OVER
        soundManager.play(SoundManager.SoundEffect.GAME_OVER)
        isNewHighScore = preferences.recordRun(
            score = score,
            distance = distanceTraveled.toInt(),
            coinsEarned = coinsCollected
        )
    }

    private fun updateCollectibles(dt: Float, frameDistance: Float) {
        val iterator = collectibles.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            item.distance -= frameDistance
            item.visualAngle = (item.visualAngle + dt * 180f) % 360f

            // Magnet pulling effect
            if (player.hasMagnet && !item.collected && item.distance > 0f && item.distance < 28f) {
                // Drift item towards player lane
                val laneDiff = player.visualLane - item.lane
                if (abs(laneDiff) > 0.05f) {
                    val drift = laneDiff * 4.5f * dt
                    // Slight drift handled visually or lane shift
                }
            }

            // Collection check window
            if (!item.collected && abs(item.distance) < 1.4f) {
                val laneDiff = abs(player.visualLane - item.lane)
                if (laneDiff < 0.68f) {
                    val heightMatches = if (item.height > 0.5f) {
                        player.jumpHeight > 0.35f
                    } else {
                        player.jumpHeight < 0.8f
                    }

                    if (heightMatches || (player.hasMagnet && item.type == CollectibleType.COIN)) {
                        collectItem(item)
                    }
                }
            }

            if (item.distance < -10f) {
                iterator.remove()
            }
        }
    }

    private fun collectItem(item: Collectible) {
        item.collected = true

        when (item.type) {
            CollectibleType.COIN -> {
                coinsCollected += 1
                score += 25
                soundManager.play(SoundManager.SoundEffect.COIN)
                spawnExplosionSparks(color = Color(0xFFFFD700), count = 12)
                addFloatingText("+25", Color(0xFFFFD700))
            }
            CollectibleType.SHIELD -> {
                player.shieldTimer = 9f
                score += 50
                soundManager.play(SoundManager.SoundEffect.POWERUP)
                spawnExplosionSparks(color = Color(0xFF00E5FF), count = 24)
                addFloatingText("SHIELD ACTIVE!", Color(0xFF00E5FF))
            }
            CollectibleType.MAGNET -> {
                player.magnetTimer = 10f
                score += 50
                soundManager.play(SoundManager.SoundEffect.POWERUP)
                spawnExplosionSparks(color = Color(0xFFBF55EC), count = 24)
                addFloatingText("COIN MAGNET!", Color(0xFFBF55EC))
            }
        }
    }

    // --- Spawning Logic ---

    private fun handleSpawning() {
        // Spawn obstacles ahead
        if (distanceTraveled + 110f >= nextObstacleSpawnDistance) {
            spawnObstacleWave(spawnDistance = nextObstacleSpawnDistance - distanceTraveled + 10f)

            // Dynamic spacing based on current speed for guaranteed reaction time
            val minReactionTime = 1.35f
            val spacing = max(26f, currentSpeed * minReactionTime) + Random.nextFloat() * 12f
            nextObstacleSpawnDistance += spacing
        }

        // Spawn periodic power-ups
        if (distanceTraveled + 110f >= nextPowerupDistance) {
            val powerupType = if (Random.nextBoolean()) CollectibleType.SHIELD else CollectibleType.MAGNET
            val freeLane = Random.nextInt(3) - 1
            collectibles.add(
                Collectible(
                    id = ++entityIdCounter,
                    lane = freeLane,
                    distance = nextPowerupDistance - distanceTraveled + 10f,
                    type = powerupType,
                    height = 0f
                )
            )
            nextPowerupDistance += Random.nextFloat() * 90f + 140f
        }
    }

    private fun spawnObstacleWave(spawnDistance: Float) {
        val pattern = Random.nextInt(6)
        when (pattern) {
            0 -> {
                // Single hurdle with coins arching over it
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(
                        id = ++entityIdCounter,
                        lane = lane,
                        distance = spawnDistance,
                        type = ObstacleType.HURDLE
                    )
                )
                // Coins over the hurdle encouraging a leap!
                spawnCoinRun(lane = lane, startDist = spawnDistance - 4f, count = 3, height = 0.9f)
            }
            1 -> {
                // High overhead beam with coins sliding under it
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(
                        id = ++entityIdCounter,
                        lane = lane,
                        distance = spawnDistance,
                        type = ObstacleType.OVERHEAD_BEAM
                    )
                )
                spawnCoinRun(lane = lane, startDist = spawnDistance - 3f, count = 3, height = 0f)
            }
            2 -> {
                // Solid blockade in one lane, other two clear
                val lane = Random.nextInt(3) - 1
                obstacles.add(
                    Obstacle(
                        id = ++entityIdCounter,
                        lane = lane,
                        distance = spawnDistance,
                        type = ObstacleType.BLOCKADE
                    )
                )
                // Put coins in an adjacent clear lane
                val otherLane = if (lane == 0) (if (Random.nextBoolean()) -1 else 1) else 0
                spawnCoinRun(lane = otherLane, startDist = spawnDistance - 2f, count = 4, height = 0f)
            }
            3 -> {
                // Two hurdles, one clear lane
                val safeLane = Random.nextInt(3) - 1
                for (l in -1..1) {
                    if (l != safeLane) {
                        obstacles.add(
                            Obstacle(
                                id = ++entityIdCounter,
                                lane = l,
                                distance = spawnDistance,
                                type = ObstacleType.HURDLE
                            )
                        )
                    }
                }
                spawnCoinRun(lane = safeLane, startDist = spawnDistance - 3f, count = 4, height = 0f)
            }
            4 -> {
                // One blockade + one hurdle (so player can either switch to clear lane OR jump hurdle)
                val blockadeLane = Random.nextInt(3) - 1
                val remainingLanes = listOf(-1, 0, 1).filter { it != blockadeLane }
                val hurdleLane = remainingLanes.random()

                obstacles.add(
                    Obstacle(
                        id = ++entityIdCounter,
                        lane = blockadeLane,
                        distance = spawnDistance,
                        type = ObstacleType.BLOCKADE
                    )
                )
                obstacles.add(
                    Obstacle(
                        id = ++entityIdCounter,
                        lane = hurdleLane,
                        distance = spawnDistance,
                        type = ObstacleType.HURDLE
                    )
                )
            }
            else -> {
                // Overhead beam across two lanes, one lane clear
                val safeLane = Random.nextInt(3) - 1
                for (l in -1..1) {
                    if (l != safeLane) {
                        obstacles.add(
                            Obstacle(
                                id = ++entityIdCounter,
                                lane = l,
                                distance = spawnDistance,
                                type = ObstacleType.OVERHEAD_BEAM
                            )
                        )
                    }
                }
                spawnCoinRun(lane = safeLane, startDist = spawnDistance - 2f, count = 3, height = 0f)
            }
        }
    }

    private fun spawnCoinRun(lane: Int, startDist: Float, count: Int, height: Float) {
        val step = 3.5f
        for (i in 0 until count) {
            collectibles.add(
                Collectible(
                    id = ++entityIdCounter,
                    lane = lane,
                    distance = startDist + (i * step),
                    type = CollectibleType.COIN,
                    height = height
                )
            )
        }
    }

    // --- Particle Effects & Floating Text ---

    private fun spawnLaneShiftSparks(direction: Float) {
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    x = player.visualLane,
                    y = 0.05f,
                    vx = direction * (Random.nextFloat() * 1.5f + 0.5f),
                    vy = Random.nextFloat() * 0.8f + 0.2f,
                    color = Color(0xFF00FFCC),
                    life = 0f,
                    maxLife = 0.35f,
                    size = Random.nextFloat() * 4f + 3f
                )
            )
        }
    }

    private fun spawnJumpSparks() {
        for (i in 0 until 12) {
            particles.add(
                Particle(
                    x = player.visualLane + (Random.nextFloat() * 0.4f - 0.2f),
                    y = 0f,
                    vx = (Random.nextFloat() * 2f - 1f) * 1.2f,
                    vy = Random.nextFloat() * 1.2f + 0.3f,
                    color = Color(0xFFFF007F),
                    life = 0f,
                    maxLife = 0.45f,
                    size = Random.nextFloat() * 5f + 3f
                )
            )
        }
    }

    private fun spawnSlideSparks() {
        for (i in 0 until 6) {
            particles.add(
                Particle(
                    x = player.visualLane + (Random.nextFloat() * 0.3f - 0.15f),
                    y = 0f,
                    vx = (Random.nextFloat() * 2f - 1f) * 1.8f,
                    vy = Random.nextFloat() * 0.6f + 0.2f,
                    color = Color(0xFFFFD700),
                    life = 0f,
                    maxLife = 0.3f,
                    size = Random.nextFloat() * 4f + 3f
                )
            )
        }
    }

    private fun spawnExplosionSparks(color: Color, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = player.visualLane + (Random.nextFloat() * 0.4f - 0.2f),
                    y = player.jumpHeight + (Random.nextFloat() * 0.4f),
                    vx = (Random.nextFloat() * 2f - 1f) * 2.5f,
                    vy = (Random.nextFloat() * 2f - 1f) * 2.5f,
                    color = color,
                    life = 0f,
                    maxLife = Random.nextFloat() * 0.3f + 0.3f,
                    size = Random.nextFloat() * 6f + 4f
                )
            )
        }
    }

    private fun addFloatingText(text: String, color: Color) {
        floatingTexts.add(
            FloatingText(
                id = ++entityIdCounter,
                text = text,
                x = player.visualLane,
                y = player.jumpHeight + 0.8f,
                color = color,
                life = 0f,
                maxLife = 0.85f
            )
        )
    }

    private fun updateEffects(dt: Float) {
        // Particles
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life += dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy -= 4f * dt // Gravity pulling sparks down
            if (p.life >= p.maxLife) {
                pIter.remove()
            }
        }

        // Floating texts
        val tIter = floatingTexts.iterator()
        while (tIter.hasNext()) {
            val t = tIter.next()
            t.life += dt
            t.y += 0.8f * dt // Float upward
            if (t.life >= t.maxLife) {
                tIter.remove()
            }
        }
    }
}
