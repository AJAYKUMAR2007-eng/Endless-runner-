package com.example.neonrunner.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class GameStatus {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
    INSTRUCTIONS
}

enum class ObstacleType {
    HURDLE,        // Low laser fence - JUMP over or dodge
    OVERHEAD_BEAM, // High electric gate - SLIDE under or dodge
    BLOCKADE       // Heavy cyber pillar - DODGE lane (cannot jump or slide)
}

data class Obstacle(
    val id: Long,
    val lane: Int,              // -1 = Left, 0 = Center, 1 = Right
    var distance: Float,        // Distance ahead of player in meters
    val type: ObstacleType,
    var hit: Boolean = false
)

enum class CollectibleType {
    COIN,
    SHIELD,
    MAGNET
}

data class Collectible(
    val id: Long,
    val lane: Int,
    var distance: Float,
    val type: CollectibleType,
    val height: Float = 0f,     // 0f = ground, 1f = aerial (requires jump)
    var collected: Boolean = false,
    var visualAngle: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float,
    val size: Float
)

data class FloatingText(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float
)

class Player {
    var targetLane: Int = 0        // -1, 0, 1
    var visualLane: Float = 0f     // Smooth interpolated lane position

    var isJumping: Boolean = false
    var jumpHeight: Float = 0f     // 0f = ground, max ~1.2f
    var jumpVelocity: Float = 0f

    var isSliding: Boolean = false
    var slideTimer: Float = 0f

    var shieldTimer: Float by mutableStateOf(0f)
    var magnetTimer: Float by mutableStateOf(0f)
    var invulnerableTimer: Float = 0f

    var health: Int by mutableStateOf(3)
    val maxHealth: Int = 3

    var runCycle: Float = 0f

    val hasShield: Boolean get() = shieldTimer > 0f
    val hasMagnet: Boolean get() = magnetTimer > 0f
    val isInvulnerable: Boolean get() = invulnerableTimer > 0f || hasShield

    fun reset() {
        targetLane = 0
        visualLane = 0f
        isJumping = false
        jumpHeight = 0f
        jumpVelocity = 0f
        isSliding = false
        slideTimer = 0f
        shieldTimer = 0f
        magnetTimer = 0f
        invulnerableTimer = 0f
        health = maxHealth
        runCycle = 0f
    }
}
