package com.example

import com.example.neonrunner.model.Collectible
import com.example.neonrunner.model.CollectibleType
import com.example.neonrunner.model.GameStatus
import com.example.neonrunner.model.Obstacle
import com.example.neonrunner.model.ObstacleType
import com.example.neonrunner.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun player_initialState_isCorrect() {
        val player = Player()
        assertEquals(0, player.targetLane)
        assertEquals(0f, player.jumpHeight, 0.001f)
        assertFalse(player.isJumping)
        assertFalse(player.isSliding)
        assertEquals(3, player.health)
        assertFalse(player.hasShield)
        assertFalse(player.hasMagnet)
    }

    @Test
    fun player_reset_restoresDefaultState() {
        val player = Player()
        player.targetLane = -1
        player.health = 1
        player.isJumping = true
        player.shieldTimer = 5f

        player.reset()

        assertEquals(0, player.targetLane)
        assertEquals(3, player.health)
        assertFalse(player.isJumping)
        assertEquals(0f, player.shieldTimer, 0.001f)
    }

    @Test
    fun obstacle_types_areValid() {
        val hurdle = Obstacle(id = 1L, lane = 0, distance = 20f, type = ObstacleType.HURDLE)
        val beam = Obstacle(id = 2L, lane = 1, distance = 25f, type = ObstacleType.OVERHEAD_BEAM)
        val block = Obstacle(id = 3L, lane = -1, distance = 30f, type = ObstacleType.BLOCKADE)

        assertEquals(ObstacleType.HURDLE, hurdle.type)
        assertEquals(ObstacleType.OVERHEAD_BEAM, beam.type)
        assertEquals(ObstacleType.BLOCKADE, block.type)
    }

    @Test
    fun collectible_types_areValid() {
        val coin = Collectible(id = 1L, lane = 0, distance = 15f, type = CollectibleType.COIN)
        val shield = Collectible(id = 2L, lane = 1, distance = 40f, type = CollectibleType.SHIELD)

        assertEquals(CollectibleType.COIN, coin.type)
        assertEquals(CollectibleType.SHIELD, shield.type)
        assertFalse(coin.collected)
    }

    @Test
    fun gameStatus_transitions() {
        val statuses = listOf(
            GameStatus.MENU,
            GameStatus.PLAYING,
            GameStatus.PAUSED,
            GameStatus.GAME_OVER,
            GameStatus.INSTRUCTIONS
        )
        assertEquals(5, statuses.size)
    }
}
